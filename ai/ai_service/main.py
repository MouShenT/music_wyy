"""
网易云音乐 AI 服务 — LLM 对话式歌曲搜索
FastAPI + DeepSeek/OpenClaw Gateway + Redis 缓存

支持两种 LLM 接入模式：
  1. Gateway 模式: 通过 OpenClaw 网关代理（设置 GATEWAY_HOST + GATEWAY_PORT）
  2. Direct 模式: 直接调用 DeepSeek/OpenAI API（设置 DEEPSEEK_API_KEY）
"""
import os
import json
import re
import hashlib
import logging
from datetime import datetime, timezone

import redis.asyncio as aioredis
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from openai import AsyncOpenAI
from pydantic import BaseModel

# ── 日志 ──
logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("wyy-ai")

app = FastAPI(title="WYY AI Service", version="1.1")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# ── 配置 ──
GATEWAY_HOST = os.environ.get("GATEWAY_HOST", "")
GATEWAY_PORT = os.environ.get("GATEWAY_PORT", "18789")
LLM_API_KEY = os.environ.get("LLM_API_KEY") or os.environ.get("DEEPSEEK_API_KEY", "")
LLM_BASE_URL = os.environ.get("LLM_BASE_URL", "https://api.deepseek.com")
LLM_MODEL = os.environ.get("LLM_MODEL", "deepseek-chat")
REDIS_URL = os.environ.get("REDIS_URL", "redis://127.0.0.1:6380")

# 缓存 TTL（秒）：7 天
CACHE_TTL = int(os.environ.get("CACHE_TTL", "604800"))

_client = None
_client_mode = None  # "gateway" | "direct"


def get_client() -> AsyncOpenAI:
    """延迟初始化 LLM 客户端，自动检测接入模式"""
    global _client, _client_mode

    if _client is not None:
        return _client

    if GATEWAY_HOST:
        base_url = f"http://{GATEWAY_HOST}:{GATEWAY_PORT}/v1"
        _client = AsyncOpenAI(base_url=base_url, api_key="gateway")
        _client_mode = "gateway"
        logger.info("LLM 客户端初始化: Gateway 模式 (%s)", base_url)
        return _client

    if not LLM_API_KEY:
        raise HTTPException(
            status_code=503,
            detail="LLM 未配置: 请设置 DEEPSEEK_API_KEY 或 GATEWAY_HOST 环境变量",
        )

    _client = AsyncOpenAI(api_key=LLM_API_KEY, base_url=LLM_BASE_URL)
    _client_mode = "direct"
    logger.info("LLM 客户端初始化: Direct 模式 (model=%s)", LLM_MODEL)
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


# ── LLM Prompt ──

SYSTEM_PROMPT = """You are a music encyclopedia specializing in anime, TV series, films, and video game soundtracks. Your task is to compile an EXHAUSTIVE list of songs related to the user's query.

## MANDATORY EXHAUSTIVE POLICY
You MUST list EVERY relevant song you know. This is critical — the user needs a COMPLETE collection for batch import into a playlist. Missing songs = failed task. If a work has 5 seasons and 3 movies, you must cover ALL of them. Stop only when you have truly exhausted your knowledge, not when you feel the list is "long enough."

## CATEGORY CHECKLIST (go through EACH mentally before outputting)
For each season/movie/version of the work:
- Opening themes (OP)
- Ending themes (ED)
- Insert songs (插入曲)
- Character songs / image songs (角色歌/印象曲)
- Original soundtrack key tracks (OST, instrumental with recognizable titles)
- Movie/OVA/SP theme songs
- Game adaptations: BGM, theme songs, character songs
- Cover versions / special editions / remixes by official artists

## OUTPUT RULES
1. Output ONLY a raw JSON array: [{"name": "Title", "artist": "Artist"}, ...]
2. Use original language for song titles (Japanese/English/Chinese as officially released)
3. Use official artist names — no abbreviations unless that IS the official name
4. Do NOT wrap in markdown code blocks or add any commentary
5. Do NOT skip songs because they seem "obscure" — include everything you know
6. Do NOT merge songs from different seasons/movies — list them separately
7. If the user's query is vague, interpret it broadly and cover all related works
8. Minimum 30 songs unless the work genuinely has fewer. Most anime/TV series have 20-60+"""



# ── 数据模型 ──

class ChatRequest(BaseModel):
    query: str
    language: str = "zh"


class AiSong(BaseModel):
    name: str
    artist: str


class ChatResponse(BaseModel):
    songs: list[AiSong] = []
    total: int = 0
    from_cache: bool = False


class CacheClearRequest(BaseModel):
    query: str = ""


class ErrorResponse(BaseModel):
    error: str
    detail: str = ""


# ── 工具函数 ──

def _cache_key(query: str) -> str:
    return f"wyy:ai:{hashlib.md5(query.encode()).hexdigest()[:12]}"


def _extract_json_array(content: str) -> list:
    """从 LLM 返回内容中提取 JSON 数组"""
    content = content.strip()

    # 去掉 markdown 代码块
    if content.startswith("```"):
        lines = content.split("\n")
        content = "\n".join(lines[1:]) if len(lines) > 1 else content
        if content.endswith("```"):
            content = content[:-3]
        content = content.strip()
        # 去掉可能的语言标识（json）
        if content.startswith("json"):
            content = content[4:].strip()

    # 直接解析 JSON 数组
    if content.startswith("[") and content.endswith("]"):
        return json.loads(content)

    # 正则提取 JSON 数组
    match = re.search(r"\[.*\]", content, re.DOTALL)
    if match:
        return json.loads(match.group())

    raise ValueError(f"无法从 LLM 返回中提取 JSON 数组，前 200 字符: {content[:200]}")


# ── API 端点 ──


@app.get("/health")
async def health():
    """健康检查"""
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
        "model": LLM_MODEL,
        "llm_mode": _client_mode or "uninitialized",
        "llm_ready": llm_ready,
        "redis": redis_status,
    }


@app.get("/api/history")
async def history(limit: int = 20):
    """获取历史查询记录"""
    try:
        r = await get_redis()
        items = await r.lrange("wyy:ai:history", 0, limit - 1)
        return {
            "items": [json.loads(i) for i in items],
            "total": len(items),
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"读取历史失败: {e}")


@app.post("/api/chat", response_model=ChatResponse)
async def chat(req: ChatRequest):
    """对话式歌曲搜索"""
    query = req.query.strip()
    if not query:
        raise HTTPException(status_code=400, detail="query 不能为空")
    if len(query) > 500:
        raise HTTPException(status_code=400, detail="query 不能超过 500 字符")

    # 检查缓存
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
            )
    except Exception as e:
        logger.warning("缓存读取失败，继续 LLM 调用: %s", e)

    # LLM 推理
    try:
        client = get_client()
        logger.info("LLM 请求: model=%s query=%.100s", LLM_MODEL, query)

        response = await client.chat.completions.create(
            model=LLM_MODEL,
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": query},
            ],
            temperature=0.5,
            max_tokens=8192,
        )

        content = response.choices[0].message.content.strip()
        songs_data = _extract_json_array(content)

        # 验证数据格式
        validated = []
        for s in songs_data:
            if isinstance(s, dict) and "name" in s and "artist" in s:
                validated.append({"name": str(s["name"]), "artist": str(s["artist"])})

        if not validated:
            raise ValueError("LLM 未返回有效歌曲")

        logger.info("LLM 返回 %d 首歌", len(validated))

        # 写入缓存
        try:
            r = await get_redis()
            pipe = r.pipeline()
            pipe.setex(_cache_key(query), CACHE_TTL, json.dumps(validated, ensure_ascii=False))
            pipe.lpush(
                "wyy:ai:history",
                json.dumps(
                    {
                        "query": query,
                        "song_count": len(validated),
                        "created_at": datetime.now(timezone.utc).isoformat(),
                    },
                    ensure_ascii=False,
                ),
            )
            pipe.ltrim("wyy:ai:history", 0, 99)
            await pipe.execute()
        except Exception as e:
            logger.warning("缓存写入失败: %s", e)

        return ChatResponse(
            songs=[AiSong(**s) for s in validated],
            total=len(validated),
            from_cache=False,
        )

    except HTTPException:
        raise
    except ValueError as e:
        logger.error("JSON 解析失败: %s", e)
        raise HTTPException(status_code=502, detail=str(e))
    except Exception as e:
        logger.error("LLM 调用失败: %s", e)
        raise HTTPException(status_code=500, detail=f"LLM 服务异常: {e}")


@app.delete("/api/cache")
async def clear_cache(req: CacheClearRequest = CacheClearRequest()):
    """清除缓存"""
    try:
        r = await get_redis()
        if req.query:
            key = _cache_key(req.query)
            await r.delete(key)
            return {"status": "ok", "deleted": key}
        else:
            # 清除所有 AI 缓存
            keys = await r.keys("wyy:ai:*")
            if keys:
                await r.delete(*keys)
            return {"status": "ok", "deleted_count": len(keys)}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"清除缓存失败: {e}")


@app.get("/api/stats")
async def stats():
    """缓存统计"""
    try:
        r = await get_redis()
        keys = await r.keys("wyy:ai:*")
        history_len = await r.llen("wyy:ai:history")
        cache_keys = [k for k in keys if k.startswith("wyy:ai:") and len(k) == 20]
        return {
            "total_cache_entries": len(cache_keys),
            "history_entries": history_len,
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取统计失败: {e}")
