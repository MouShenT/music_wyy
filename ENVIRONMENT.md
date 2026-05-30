# 网易云工具 App — Kali 运行环境

> 最后更新: 2026-05-30 23:40 CST

---

## 一、网络拓扑

```
┌─────────────────────────────────────────────────────────┐
│  Android App (手机端)                                    │
│  IP: 192.168.117.135                                     │
│  └─ AppModule.kt: readTimeout 120s                      │
└──────────────┬──────────────────────────────────────────┘
               │ WiFi / 局域网
┌──────────────▼──────────────────────────────────────────┐
│  Kali VM                                                 │
│  主机名: root                                            │
│  内核: Linux 6.18.5+kali-amd64                          │
│  OS: Kali GNU/Linux Rolling                             │
│                                                          │
│  IP: 192.168.117.149                                     │
│                                                          │
│  ┌─ wyy-ai (systemd) ───────── :8100 ────────────────┐ │
│  │  FastAPI + Uvicorn                                 │ │
│  │  LLM: DeepSeek Chat (Direct)                       │ │
│  │  工作目录: /opt/wyy_ai/ai_service/                 │ │
│  └────────────────────────────────────────────────────┘ │
│                                                          │
│  ┌─ netease_api (Docker) ───── :3000 ────────────────┐ │
│  │  Node.js Express, host 网络模式                     │ │
│  │  版本: v4.32.0, 314 个 API 端点                     │ │
│  └────────────────────────────────────────────────────┘ │
│                                                          │
│  ┌─ wyy_redis (Docker) ── 127.0.0.1:6380 ────────────┐ │
│  │  Redis 7-alpine, AI 查询缓存 TTL 7 天               │ │
│  └────────────────────────────────────────────────────┘ │
│                                                          │
│  ┌─ DeepSeek API 云端 ─ api.deepseek.com ────────────┐ │
│  │  model: deepseek-chat                              │ │
│  │  API Key: /root/.openclaw/.env                     │ │
│  └────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
```

---

## 二、AI 对话服务 (`wyy-ai`)

### 基本信息

| 项目 | 值 |
|------|-----|
| 管理方式 | systemd systemctl restart wyy-ai\` |
| 服务文件 | /etc/systemd/system/wyy-ai.service\` |
| 监听地址 | 0.0.0.0:8100 |
| 运行用户 | root |
| 重启策略 | always, RestartSec=5s |
| 工作目录 | /opt/wyy_ai/ai_service/ |
| Python 环境 | /opt/wyy_ai/venv/ (Python 3.13) |
| 框架 | FastAPI 0.136.3 |
| ASGI 服务器 | Uvicorn 0.48.0 |

### 健康检查

```bash
curl http://127.0.0.1:8100/health
# {"status":"ok","model":"deepseek-chat","llm_mode":"direct","llm_ready":true,"redis":"connected"}
```

### API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /health | 健康检查 |
| POST | /api/chat | 对话式歌曲搜索（核心） |
| GET | /api/history | 历史查询记录 ?limit=20\` |
| DELETE | /api/cache | 清除缓存（带 query 或全清） |
| GET | /api/stats | 缓存统计 |

### 环境变量 /etc/systemd/system/wyy-ai.service\` 注入）

```bash
LLM_BASE_URL=https://api.deepseek.com
LLM_MODEL=deepseek-chat
REDIS_URL=redis://127.0.0.1:6380
DEEPSEEK_API_KEY=<从 /root/.openclaw/.env 读取>
```

### LLM 推理参数

| 参数 | 值 |
|------|-----|
| model | deepseek-chat |
| temperature | 0.5 |
| max_tokens | 8192 |
| 连接模式 | Direct（直接调 DeepSeek API） |

### 缓存配置

| 参数 | 值 |
|------|-----|
| Redis URL | redis://127.0.0.1:6380 |
| 缓存 TTL | 604800 秒（7 天） |
| 缓存 Key | wyy:ai:{MD5前12位} |
| 历史记录 Key | wyy:ai:history（LRANGE，最多100条） |

### Python 依赖 /opt/wyy_ai/venv/）

```
fastapi      0.136.3
uvicorn      0.48.0
openai       2.38.0
redis        8.0.0
pydantic     2.13.4
httpx        0.28.1
```

### 关键源码

| 文件 | 说明 |
|------|------|
| /opt/wyy_ai/ai_service/main.py | 生产运行（实际部署位置） |
| /opt/wyy_ai/ai_service/requirements.txt | Python 依赖 |
| /mnt/hgfs/aaopenclow-share/music_wyy/ai/ai_service/main.py | 开发共享（修改入口） |

### 代码同步流程

```bash
# 1. 在宿主机修改共享文件夹中的 main.py
# 2. 在 Kali 执行:
cp /mnt/hgfs/aaopenclow-share/music_wyy/ai/ai_service/main.py /opt/wyy_ai/ai_service/main.py
systemctl restart wyy-ai

# 3. 验证:
curl http://127.0.0.1:8100/health
journalctl -u wyy-ai -f
```

---

## 三、网易云音乐 API 代理 (`netease_api`)

### 基本信息

| 项目 | 值 |
|------|-----|
| 管理方式 | Docker docker restart netease_api\` |
| 镜像 | binaryify/netease_cloud_music_api:latest |
| 版本 | **v4.32.0**（手动从 v4.27.0 升级） |
| 网络模式 | **host**（直接使用宿主机网络） |
| 端口 | 0.0.0.0:3000 |
| 运行时 | Node.js 22.14.0 |
| 框架 | Express 4.18.2 |
| HTTP 客户端 | Axios 1.5.1 |
| 端点数量 | 314 个 |
| 重启策略 | unless-stopped |

### 版本升级说明

Docker 镜像内置 v4.27.0，已手动升级到 v4.32.0：

```bash
# 容器重建后会回退到 v4.27.0，需重新执行以下步骤:
docker exec netease_api npm install NeteaseCloudMusicApi@latest
docker exec netease_api sh -c "
  cp -r /app/node_modules/NeteaseCloudMusicApi/server/* /app/server/
  cp -r /app/node_modules/NeteaseCloudMusicApi/module/* /app/module/
  sed -i 's/\"version\": \"4.27.0\"/\"version\": \"4.32.0\"/' /app/package.json
  cp /app/node_modules/NeteaseCloudMusicApi/server.js /app/server.js 2>/dev/null
"
docker restart netease_api
# 验证: 日志中不应出现"请及时更新"提示
```

### 关键 API 端点

#### 歌曲/音乐
| 路径 | 说明 |
|------|------|
| /song/url | 获取歌曲播放 URL |
| /song/url/v1 | 新版（支持 Hi-Res/杜比/超清母带等音质） |
| /song/download/url | 客户端下载 URL |
| /song/download/url/v1 | 新版下载（免费歌曲可下 Hi-Res） |
| /song/detail | 歌曲详情（含 fee/音质信息） |
| /lyric | 获取歌词 |

#### 搜索
| 路径 | 说明 |
|------|------|
| /search | 综合搜索 |
| /search/suggest | 搜索建议 |
| /search/hot | 热搜列表 |
| /search/hot/detail | 热搜详情 |

#### 登录
| 路径 | 说明 |
|------|------|
| /login/cellphone | 手机登录 |
| /login | 邮箱登录 |
| /login/qr/key | 二维码 Key 生成 |
| /login/qr/create | 二维码生成 |
| /login/qr/check | 二维码状态检测 |
| /login/refresh | 刷新登录态 |
| /login/status | 登录状态检查 |
| /logout | 退出登录 |
| /register/anonimous | 游客登录 |

#### 歌单
| 路径 | 说明 |
|------|------|
| /playlist/detail | 歌单详情 |
| /playlist/create | 新建歌单 |
| /playlist/tracks | 歌单所有歌曲 |
| /playlist/subscribe | 收藏/取消收藏歌单 |
| /user/playlist | 用户歌单 |
| /top/playlist | 精品歌单 |

#### 歌手/专辑/MV
| 路径 | 说明 |
|------|------|
| /artist/songs | 歌手歌曲 |
| /artist/album | 歌手专辑 |
| /artist/mv | 歌手 MV |
| /album | 专辑内容 |
| /mv/url | MV 播放地址 |
| /video/url | 视频播放地址 |

### socket 信息

#### fee 字段（版权标识）
| 值 | 含义 |
|----|------|
| 0 | 免费/无版权 |
| 1 | VIP 专享 |
| 4 | 需购买专辑 |
| 8 | 非会员低音质播放，会员高音质+下载 |

#### level 字段（音质等级，v1 接口）
| 值 | 含义 |
|----|------|
| standard | 标准 |
| higher | 较高 |
| exhigh | 极高 |
| lossless | 无损 |
| hires | Hi-Res |
| jyeffect | 高清环绕声 |
| sky | 沉浸环绕声 |
| dolby | 杜比全景声 |
| jymaster | 超清母带 |

### 调用示例

```bash
# 搜索
curl "http://localhost:3000/search?keywords=晴天&type=1"

# 获取歌曲URL（免费歌曲）
curl "http://localhost:3000/song/url/v1?id=33894312&level=lossless"

# 带cookie请求VIP歌曲
curl "http://localhost:3000/song/url/v1?id=xxx&level=hires&cookie=MUSIC_U=xxxx"
```

### 加密协议（已知公开密钥）

```javascript
// 所有密钥在JS源码中明文可见
presetKey  = '0CoJUm6Qyw8W8jud'        // AES-CBC 预设密钥
eapiKey    = 'e82ckenh8dichen8'         // eapi ECB 密钥
linuxapiKey = 'rFgB&h#%2?^eDg:Q'        // linux API 密钥
IV         = '0102030405060708'          // 固定 IV
// RSA 公钥也硬编码在 JS 中
```

---

## 四、Redis 缓存 (`wyy_redis`)

### 基本信息

| 项目 | 值 |
|------|-----|
| 容器名 | wyy_redis |
| 镜像 | redis:7-alpine |
| 端口 | 127.0.0.1:6380（仅本地） |
| 数据卷 | wyy_redis_data |
| 健康检查 | redis-cli ping（5秒间隔） |
| 重启策略 | unless-stopped |

### 验证命令

```bash
docker exec wyy_redis redis-cli ping    # 期望: PONG
```

---

## 五、运维命令速查

```bash
# ── 状态检查 ──
curl http://127.0.0.1:8100/health                # AI 服务健康
curl http://localhost:3000/                       # 网易云 API
docker exec wyy_redis redis-cli ping              # Redis
docker ps --filter "name=wyy|name=netease" --format "table {{.Names}} {{.Status}}"

# ── 重启服务 ──
systemctl restart wyy-ai                         # AI 服务
docker restart netease_api                       # 网易云 API
docker restart wyy_redis                         # Redis

# ── 日志查看 ──
journalctl -u wyy-ai -f                          # AI 服务实时日志
docker logs -f netease_api                       # API 实时日志

# ── 代码更新 ──
cp /mnt/hgfs/aaopenclow-share/music_wyy/ai/ai_service/main.py /opt/wyy_ai/ai_service/main.py
systemctl restart wyy-ai

# ── 清除AI缓存 ──
curl -X DELETE http://127.0.0.1:8100/api/cache
```

---

## 六、文件目录结构

```
/mnt/hgfs/aaopenclow-share/music_wyy/    ← VMware 共享文件夹（宿主机↔Kali）
├── ai/
│   ├── ai_service/
│   │   ├── main.py                      ← AI 服务源码（修改入口）
│   │   ├── requirements.txt             ← Python 依赖
│   │   └── Dockerfile                    ← (废弃，改用 systemd)
│   ├── docker-compose.yml               ← (废弃，改用 systemd)
│   ├── data/                             ← 数据目录
│   ├── ANDROID_INTEGRATION.md            ← Android 集成文档
│   └── wyy-ai.service                   ← systemd 服务文件参考
├── app/                                  ← Android App 源码
├── NeteaseCloudMusicAPI.md               ← API 文档 (v4.27)
├── NeteaseCloudMusicAPI_v4.32.0.md       ← API 文档 (v4.32)
├── CLAUDE.md                             ← Claude Code 指引
├── deploy_ai.sh                          ← 部署脚本
├── ENVIRONMENT.md                        ← 本文档
└── ...

/opt/wyy_ai/                              ← AI 服务生产部署目录
├── ai_service/
│   └── main.py                           ← 实际运行的源码
├── venv/                                  ← Python 虚拟环境
│   └── bin/
│       └── uvicorn                       ← ASGI 服务器
└── ...

/etc/systemd/system/
└── wyy-ai.service                        ← systemd 单元文件

/root/.openclaw/
└── .env                                   ← DeepSeek API Key
```

---

## 七、部署架构决策

| 决策 | 原因 |
|------|------|
| wyy-ai 用 systemd 而非 Docker | Docker 内 pip install 慢、网络问题，systemd 更可控 |
| netease_api 用 host 网络 | bridge 模式 DNS 解析 music.163.com 失败 |
| Redis 用 Docker | 轻量，独立管理，data 持久化 |
| LLM 用 Direct 模式 | 直连 DeepSeek API，不需要 OpenClaw Gateway 代理 |
| 代码走共享文件夹 | 宿主机 Win/Mac 编辑 → Kali 同步，方便开发 |

---

## 八、注意事项

1. **netease_api 容器重建会回退** — Docker 镜像内置 v4.27.0，重建后需手动升级到 v4.32.0
2. **wyy-ai 代码同步必须手动 cp** — 共享文件夹修改不会自动同步到 /opt/
3. **DEEPSEEK_API_KEY 在 .env 中** — 不要泄露，不要提交到 git
4. **Redis 仅绑定 127.0.0.1** — 外部不可直接访问
5. **App 端超时已改 120s** — LLM 推理可能长达 60-90 秒，不要改回去
6. **免费歌曲可下 Hi-Res** — 使用 /song/download/url/v1，VIP 歌曲需要会员 cookie
