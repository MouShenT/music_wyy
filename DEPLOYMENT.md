# 部署方案 — AI Service v2 + 批处理优化

> 版本: v2.0 | 日期: 2026-05-30

---

## 一、v2 核心改进

### AI 搜索：从 LLM 虚构 → 真实验证

| 项目 | v1 (旧) | v2 (新) |
|------|---------|---------|
| 歌曲来源 | LLM 凭记忆生成 | Netease API 实际搜索 |
| 幻觉问题 | 严重（编造 OST 轨名） | **0% 幻觉**（每首经 API 验证） |
| 搜索方式 | 无验证 | LLM 生成 25-40 个关键词 → 6 路并行搜索 |
| 返回数据 | name + artist | name + artist + **track ID** + album |
| 缓存隔离 | 无版本标识 | v2 缓存前缀，旧缓存自动失效 |
| 速度 | 15-90s (纯 LLM) | 2-8s (LLM 关键词 + 并行搜索) |

### 批处理：串行 → 并行 + 容错 + 报告

| 项目 | v1 (旧) | v2 (新) |
|------|---------|---------|
| 搜索方式 | 串行 (400ms×N) | 3 路并行 (快 2-3 倍) |
| 单首失败 | 整批中断 | 跳过继续 |
| 未找到报告 | 不告知 | 结果消息中列出所有未导入歌曲 |
| 批次大小 | 100 首/批 | 1000 首/批 |

---

## 二、部署步骤

### 在 Kali 上执行

```bash
# 方式 1: 一键部署（推荐）
bash /mnt/hgfs/aaopenclow-share/music_wyy/deploy_ai.sh

# 方式 2: 手动部署
cp /mnt/hgfs/aaopenclow-share/music_wyy/ai/ai_service/main.py /opt/wyy_ai/ai_service/main.py
systemctl restart wyy-ai

# 验证
curl http://127.0.0.1:8100/health
# 期望: {"status":"ok","version":"v2","model":"deepseek-chat","llm_ready":true,"redis":"connected"}

# 清除旧缓存（重要！v1 缓存含虚构歌曲）
curl -X DELETE http://127.0.0.1:8100/api/cache -H "Content-Type: application/json" -d '{}'
```

### App 端

Android 端代码已修改完成并编译通过。重新安装 APK 即可使用新接口。

```bash
# 编译
cd /mnt/hgfs/aaopenclow-share/music_wyy
./gradlew assembleDebug

# APK 位置
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 三、新 API 流程

```
用户输入: "进击的巨人所有歌曲"
         │
         ▼
┌─────────────────────────────────┐
│ Step 1: 检查 v2 缓存 (Redis)    │
│ 命中 → 直接返回（2-10ms）        │
└──────────────┬──────────────────┘
               │ 未命中
               ▼
┌─────────────────────────────────┐
│ Step 2: DeepSeek 生成关键词      │
│ 输入: "进击的巨人所有歌曲"        │
│ 输出: ["進撃の巨人 OP",          │
│        "紅蓮の弓矢",             │
│        "Linked Horizon 進撃",    │
│        "澤野弘之 進撃",          │
│        ...共 25-40 个]           │
│ 耗时: ~2-5s                     │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│ Step 3: 并行搜索 Netease API     │
│ 6 路并发调用 /cloudsearch        │
│ 按 track ID 去重                 │
│ 耗时: ~0.5-3s                   │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│ Step 4: 返回结果 + 写入缓存       │
│ [{id: 18614888, name: "紅蓮の弓矢",│
│   artist: "Linked Horizon",     │
│   album: "自由への進撃"}, ...]   │
│ 缓存 TTL: 7 天                  │
└─────────────────────────────────┘
```

---

## 四、API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/health` | 健康检查（含 version 字段，v2） |
| POST | `/api/chat` | 歌曲搜索（新流程） |
| GET | `/api/history` | 历史记录 |
| DELETE | `/api/cache` | 清除缓存（清除 v1+v2） |
| GET | `/api/stats` | 缓存统计 |

### POST /api/chat

```bash
# 请求
curl -X POST http://127.0.0.1:8100/api/chat \
  -H "Content-Type: application/json" \
  -d '{"query": "进击的巨人所有歌曲"}'

# 响应
{
  "songs": [
    {"name": "紅蓮の弓矢", "artist": "Linked Horizon", "id": 18614888, "album": "自由への進撃"},
    ...
  ],
  "total": 45,
  "from_cache": false,
  "keywords_used": 32
}
```

---

## 五、关键文件

| 文件 | 位置 | 说明 |
|------|------|------|
| AI 服务源码 | `ai/ai_service/main.py` | v2 主程序 |
| 部署脚本 | `deploy_ai.sh` | Kali 一键部署 |
| 批处理 VM | `app/.../BatchCreateViewModel.kt` | 并行搜索 + 未找到报告 |
| 首页 VM | `app/.../HomeViewModel.kt` | 支持 AI ID 直接播放 |
| AI API 模型 | `app/.../NeteaseAiApi.kt` | AiSong 新增 id/album 字段 |

---

## 六、回滚方案

```bash
# 恢复 v1 备份
cp /opt/wyy_ai/ai_service/main.py.bak.v1 /opt/wyy_ai/ai_service/main.py
systemctl restart wyy-ai
```
