"""
网易云音乐 AI 服务 v2 — 关键词搜索 + Netease API 验证
FastAPI + DeepSeek + httpx 并行搜索 + Redis 缓存

核心流程:
  1. LLM 生成搜索关键词（利用 LLM 的知识广度覆盖 OP/ED/OST/角色歌等）
  2. 并行搜索网易云 API 验证真实性（消除 LLM 幻觉）
  3. 按 track ID 去重返回

消除了 100% 的虚构歌曲问题：每首歌都经过 Netease API 实际搜索验证。
"""
import os
import json
import re
import hashlib
import asyncio
import logging
import time
from datetime import datetime, timezone

import redis.asyncio as aioredis
import httpx
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from openai import AsyncOpenAI
from pydantic import BaseModel

# ── 日志 ──
logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("wyy-ai")

app = FastAPI(title="WYY AI Service", version="2.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# ── 配置 ──
LLM_API_KEY = os.environ.get("LLM_API_KEY") or os.environ.get("DEEPSEEK_API_KEY", "")
LLM_BASE_URL = os.environ.get("LLM_BASE_URL", "https://api.deepseek.com")
LLM_MODEL = os.environ.get("LLM_MODEL", "deepseek-chat")
REDIS_URL = os.environ.get("REDIS_URL", "redis://127.0.0.1:6380")
NETEASE_API = os.environ.get("NETEASE_API", "http://127.0.0.1:3000")
CACHE_TTL = int(os.environ.get("CACHE_TTL", "604800"))
MAX_CONCURRENT = int(os.environ.get("MAX_CONCURRENT", "6"))
CACHE_VERSION = "v2"  # 缓存版本：v1 旧数据自动失效

_client = None


def get_client() -> AsyncOpenAI:
    global _client
    if _client is not None:
        return _client
    if not LLM_API_KEY:
        raise HTTPException(status_code=503, detail="LLM 未配置")
    _client = AsyncOpenAI(api_key=LLM_API_KEY, base_url=LLM_BASE_URL)
    logger.info("LLM 客户端: Direct (model=%s)", LLM_MODEL)
    return _client


_redis = None


async def get_redis():
    global _redis
    if _redis is None:
        _redis = aioredis.from_url(REDIS_URL, decode_responses=True)
        try:
            await _redis.ping()
            logger.info("Redis 连接成功: %s", REDIS_URL)
        except Exception as e:
            logger.warning("Redis 连接失败: %s", e)
    return _redis


# ── LLM Prompt — 关键词生成 ──

SYSTEM_PROMPT = """You are a search keyword generator for finding music on NetEase Cloud Music (网易云音乐).

## YOUR TASK
Given a user's request about anime/TV series/film/game music, generate a COMPREHENSIVE list of search keywords that will find ALL related songs on a Chinese music platform.

## KEYWORD GENERATION STRATEGY
Generate keywords from these angles (cover ALL that apply):
1. **Song titles** — known OP/ED/insert song titles in original language (Japanese/English/Chinese)
2. **Artist + Work** — e.g. "Linked Horizon 進撃", "RADWIMPS 君の名は"
3. **Work + Type** — e.g. "進撃の巨人 OP", "钢之炼金术师 ED", "鬼灭之刃 主题曲"
4. **Character names** — for character songs: e.g. "エレン 角色歌"
5. **Composer + Work** — e.g. "澤野弘之 進撃", "梶浦由記 Fate"
6. **Alternative titles** — search in Japanese, Chinese, English, and common abbreviations
7. **Season-specific** — e.g. "進撃の巨人 Season2 OP", "鬼灭之刃 遊郭編 OP"
8. **Movie/OVA titles** — e.g. "君の名は 主題歌", "鬼灭之刃 无限列车 ED"
9. **Generic work search** — just the work name in different languages

## RULES
- Generate 25-40 keywords for broad coverage (fewer for niche works with <10 songs)
- Each keyword should be searchable on a Chinese music platform
- Prioritize specific, high-yield keywords over vague ones
- Mix languages: Japanese (original), Chinese (for Chinese platform), English
- Include variations: full name, common abbreviation, translated title

## OUTPUT FORMAT
Output ONLY a raw JSON array of strings:
["keyword1", "keyword2", "keyword3", ...]

No markdown, no commentary, just the array."""


# ── 数据模型 ──

class ChatRequest(BaseModel):
    query: str
    language: str = "zh"


class AiSong(BaseModel):
    name: str
    artist: str
    id: int = 0
    album: str = ""


class ChatResponse(BaseModel):
    songs: list[AiSong] = []
    total: int = 0
    from_cache: bool = False
    keywords_used: int = 0


class CacheClearRequest(BaseModel):
    query: str = ""


# ── 工具函数 ──

def _cache_key(query: str) -> str:
    return f"wyy:ai:{CACHE_VERSION}:{hashlib.md5(query.encode()).hexdigest()[:12]}"


def _extract_json_array(content: str) -> list:
    """从 LLM 返回内容中提取 JSON 数组"""
    content = content.strip()
    if content.startswith("```"):
        lines = content.split("\n")
        content = "\n".join(lines[1:]) if len(lines) > 1 else content
        if content.endswith("```"):
            content = content[:-3]
        content = content.strip()
        if content.lower().startswith("json"):
            content = content[4:].strip()
    if content.startswith("[") and content.endswith("]"):
        return json.loads(content)
    match = re.search(r"\[.*\]", content, re.DOTALL)
    if match:
        return json.loads(match.group())
    raise ValueError(f"无法提取 JSON 数组，前 200 字符: {content[:200]}")


async def _search_netease(keyword: str, limit: int = 8) -> list[dict]:
    """搜索网易云 API 获取真实歌曲"""
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.get(
                f"{NETEASE_API}/cloudsearch",
                params={"keywords": keyword, "type": 1, "limit": limit},
            )
            data = resp.json()
            songs = []
            if data.get("code") == 200 and data.get("result", {}).get("songs"):
                for s in data["result"]["songs"]:
                    artists = "/".join(a["name"] for a in s.get("ar", [])[:3])
                    songs.append({
                        "id": s["id"],
                        "name": s["name"],
                        "artist": artists,
                        "album": s.get("al", {}).get("name", ""),
                    })
            return songs
    except Exception as e:
        logger.warning("搜索失败 [%s]: %s", keyword[:40], e)
        return []


async def _search_all_keywords(keywords: list[str]) -> list[dict]:
    """并行搜索所有关键词，去重后返回"""
    semaphore = asyncio.Semaphore(MAX_CONCURRENT)

    async def search_one(kw: str) -> list[dict]:
        async with semaphore:
            return await _search_netease(kw)

    # 去重关键词
    unique_kw = list(dict.fromkeys(kw.strip() for kw in keywords if kw.strip()))
    logger.info("开始并行搜索 %d 个关键词 (并发=%d)", len(unique_kw), MAX_CONCURRENT)

    t0 = time.time()
    results = await asyncio.gather(*[search_one(kw) for kw in unique_kw])
    elapsed = time.time() - t0

    # 按 track ID 去重，保留首次出现
    seen_ids: set[int] = set()
    songs: list[dict] = []
    for batch in results:
        for s in batch:
            if s["id"] not in seen_ids:
                seen_ids.add(s["id"])
                songs.append(s)

    logger.info("搜索完成: %d 关键词 → %d 首唯一歌曲 (%.1fs)",
                len(unique_kw), len(songs), elapsed)
    return songs


# ── API 端点 ──


@app.get("/health")
async def health():
    redis_status = "disconnected"
    try:
        r = await get_redis()
        await r.ping()
        redis_status = "connected"
    except Exception as e:
        redis_status = f"error: {e}"

    llm_ready = False
    try:
        get_client()
        llm_ready = True
    except HTTPException:
        pass

    return {
        "status": "ok",
        "version": CACHE_VERSION,
        "model": LLM_MODEL,
        "llm_ready": llm_ready,
        "redis": redis_status,
        "netease_api": NETEASE_API,
    }


@app.get("/api/history")
async def history(limit: int = 20):
    try:
        r = await get_redis()
        items = await r.lrange("wyy:ai:history", 0, limit - 1)
        return {"items": [json.loads(i) for i in items], "total": len(items)}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"读取历史失败: {e}")


@app.post("/api/chat", response_model=ChatResponse)
async def chat(req: ChatRequest):
    """对话式歌曲搜索 v2 — 关键词生成 + Netease 验证"""
    query = req.query.strip()
    if not query:
        raise HTTPException(status_code=400, detail="query 不能为空")
    if len(query) > 500:
        raise HTTPException(status_code=400, detail="query 不能超过 500 字符")

    # ── Step 1: 检查缓存 ──
    try:
        r = await get_redis()
        cached = await r.get(_cache_key(query))
        if cached:
            songs_data = json.loads(cached)
            logger.info("缓存命中: %s (%d 首)", query[:50], len(songs_data))
            return ChatResponse(
                songs=[AiSong(**s) for s in songs_data],
                total=len(songs_data),
                from_cache=True,
                keywords_used=0,
            )
    except Exception as e:
        logger.warning("缓存读取失败: %s", e)

    # ── Step 2: LLM 生成搜索关键词 ──
    try:
        client = get_client()
        logger.info("LLM 请求关键词: %.100s", query)

        response = await client.chat.completions.create(
            model=LLM_MODEL,
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": query},
            ],
            temperature=0.3,
            max_tokens=4096,
        )

        content = response.choices[0].message.content.strip()
        keywords = _extract_json_array(content)

        # 验证是字符串列表
        keywords = [str(k) for k in keywords if isinstance(k, str) and str(k).strip()]
        if not keywords:
            raise ValueError("LLM 未返回有效关键词")

        logger.info("LLM 生成 %d 个关键词", len(keywords))

    except HTTPException:
        raise
    except ValueError as e:
        logger.error("关键词提取失败: %s", e)
        raise HTTPException(status_code=502, detail=f"LLM 关键词提取失败: {e}")
    except Exception as e:
        logger.error("LLM 调用失败: %s", e)
        raise HTTPException(status_code=500, detail=f"LLM 服务异常: {e}")

    # ── Step 3: 并行搜索网易云 API ──
    try:
        songs_data = await _search_all_keywords(keywords)
    except Exception as e:
        logger.error("搜索网易云失败: %s", e)
        raise HTTPException(status_code=500, detail=f"搜索网易云 API 失败: {e}")

    if not songs_data:
        return ChatResponse(songs=[], total=0, from_cache=False, keywords_used=len(keywords))

    # ── Step 4: 写入缓存 ──
    try:
        r = await get_redis()
        pipe = r.pipeline()
        pipe.setex(_cache_key(query), CACHE_TTL, json.dumps(songs_data, ensure_ascii=False))
        pipe.lpush(
            "wyy:ai:history",
            json.dumps({
                "query": query,
                "song_count": len(songs_data),
                "keywords_used": len(keywords),
                "created_at": datetime.now(timezone.utc).isoformat(),
            }, ensure_ascii=False),
        )
        pipe.ltrim("wyy:ai:history", 0, 99)
        await pipe.execute()
    except Exception as e:
        logger.warning("缓存写入失败: %s", e)

    return ChatResponse(
        songs=[AiSong(**s) for s in songs_data],
        total=len(songs_data),
        from_cache=False,
        keywords_used=len(keywords),
    )


@app.delete("/api/cache")
async def clear_cache(req: CacheClearRequest = CacheClearRequest()):
    """清除缓存（包括旧 v1 和新 v2）"""
    try:
        r = await get_redis()
        if req.query:
            key = _cache_key(req.query)
            await r.delete(key)
            # 同时尝试删除旧版 key
            old_key = key.replace(f"wyy:ai:{CACHE_VERSION}:", "wyy:ai:")
            await r.delete(old_key)
            return {"status": "ok", "deleted": [key, old_key]}
        else:
            # 清除所有缓存（v1 + v2）
            keys_v2 = await r.keys("wyy:ai:v2:*")
            keys_v1 = await r.keys("wyy:ai:*")
            # v1 keys 可能包含 history，排除
            all_keys = set(keys_v2 + [k for k in keys_v1 if not k.endswith(":history")])
            deleted = 0
            for key in all_keys:
                await r.delete(key)
                deleted += 1
            # 保留 history
            return {"status": "ok", "deleted_count": deleted, "note": "已清除所有歌曲缓存（v1/v2），历史记录保留"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"清除缓存失败: {e}")


@app.get("/api/stats")
async def stats():
    try:
        r = await get_redis()
        keys = await r.keys("wyy:ai:v2:*")
        history_len = await r.llen("wyy:ai:history")
        return {
            "cache_version": CACHE_VERSION,
            "total_cache_entries": len(keys),
            "history_entries": history_len,
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取统计失败: {e}")
