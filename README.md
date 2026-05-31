# 墨韵音乐 (MusicWyy)

网易云音乐歌单管理与自动化工具 Android 应用，基于 Kotlin + Jetpack Compose + Clean Architecture。

## 核心功能

| 功能 | 说明 |
|------|------|
| **自由文本批量导入** | 粘贴任意格式的「歌名 - 歌手」列表，3并发并行精确搜索匹配，一键创建歌单 |
| **标准 MP3 下载** | 通过 API 获取音频流 URL，下载为标准 MP3 格式（突破官方 .ncm 加密限制） |
| **AI 智能找歌** | 基于 DeepSeek LLM 的自然语言搜索，LLM 生成关键词 + 网易云 API 并行验证，消除 100% 虚构歌曲 |
| **歌词导出与翻译** | LRC 歌词解析、翻译切换、一键导出文本并通过系统分享 |
| **自动签到系统** | WorkManager 后台定时签到、云贝任务、300首刷歌打卡 |
| **完整音乐播放器** | 基于 Media3 (ExoPlayer)，支持流式播放、歌词同步、离线缓存、LRU 缓存淘汰 |

## 与官方 App 对比

| 功能点 | 网易云官方 App | 墨韵音乐 |
|--------|:------------:|:--------:|
| 外部歌单导入 | 链接/OCR截图/本地扫描/云盘 | **文本列表批量导入 + API 精确匹配** |
| 截图 OCR 识别 | 准确率约 50%，日韩歌曲常失败 | **API 精确搜索，不受图质影响** |
| 歌曲下载 | 加密 .ncm 格式 | **标准 MP3 格式** |
| 歌词导出 | 仅在线查看 | **LRC 文本导出 + 翻译切换** |
| AI 语义搜索 | 仅关键词精确匹配 | **LLM 自然语言搜索** |
| 自动签到 | 需手动操作 | **WorkManager 定时自动** |
| 批量创建歌单 | 无文本输入接口 | **文本列表一键批量导入** |

## 技术栈

| 类别 | 选型 |
|------|------|
| 语言 | Kotlin 2.1+ |
| UI | Jetpack Compose + Material Design 3 |
| 架构 | MVVM + UDF + Clean Architecture |
| DI | Koin |
| 数据库 | Room + KSP |
| 网络 | Retrofit + OkHttp + kotlinx.serialization |
| 异步 | Kotlin Coroutines + Flow |
| 媒体播放 | Media3 (ExoPlayer) |
| 后台任务 | WorkManager |
| 图片加载 | Coil 3 |
| 图表 | Vico |
| 最低 SDK | Android 7.0 (API 24) |
| 目标 SDK | Android 15 (API 36) |

## 系统架构

```
┌─────────────────────────────────────────┐
│ UI Layer (Compose Screens + ViewModels) │
│  · StateFlow + UDF 单向数据流           │
│  · Navigation Compose 导航              │
│  · Material Design 3 暗色主题           │
├─────────────────────────────────────────┤
│ Domain Layer (UseCase / Model)          │
│  · 领域模型: Song, Playlist             │
│  · Repository 接口                      │
├─────────────────────────────────────────┤
│ Data Layer (Repository / DataSource)    │
│  · Room (本地持久化)                    │
│  · DataStore (配置/令牌存储)            │
│  · Retrofit (远程 API)                  │
│  · ExoPlayer (媒体播放)                 │
└─────────────────────────────────────────┘
```

## 功能模块 (12个)

1. **登录** — Cookie 授权登录，DataStore 持久化令牌
2. **首页** — 音乐搜索、AI 找歌、数据概览
3. **歌单列表** — 用户歌单浏览、下拉刷新
4. **歌单详情** — 歌曲列表、播放、添加到歌单
5. **批量创建** — 文本导入搜索、3并发并行、一键创建歌单
6. **AI 找歌** — 自然语言搜索、LLM + API 双层验证
7. **音乐播放器** — 播放控制、三种循环模式、MiniPlayer
8. **歌词显示** — LRC 逐字同步、翻译切换、沉浸式模式
9. **自动签到** — WorkManager 定时签到/云贝/刷歌
10. **私信中心** — 私信列表、对话详情
11. **云贝中心** — 积分查询、任务列表
12. **个人中心** — 用户信息、设置、退出登录

## AI 找歌架构

```
用户输入 "进击的巨人所有OP和ED"
    ↓
LLM 生成 30+ 个多维度搜索关键词
    ↓
httpx 6并发并行搜索网易云 cloudsearch API
    ↓
按 track ID 去重 → 返回真实可播放的歌曲列表
    ↓
Redis 缓存 (7天TTL) → 相同查询秒级响应
```

**关键设计**：每首歌必须经过网易云 API 实际搜索验证，消除 100% 的 LLM 虚构歌曲。

## 后端服务

| 服务 | 说明 |
|------|------|
| NeteaseCloudMusicApi | Docker 部署，端口 3000，提供网易云 API 代理 |
| AI 服务 | FastAPI + DeepSeek API (:8100) + Redis (:6380) |

后端部署在 Kali Linux (内网 192.168.117.149)，App 通过内网直连，不经过公网。

## 项目结构

```
music_wyy/
├── app/src/main/java/com/example/music_wyy/
│   ├── MainActivity.kt              # 入口 Activity
│   ├── MusicWyyApp.kt               # Application (Koin 初始化)
│   ├── MainApp.kt                   # 主 Composable (MiniPlayer 集成)
│   ├── di/AppModule.kt              # Koin 依赖注入配置
│   ├── domain/model/                # 领域模型
│   ├── session/UserSession.kt       # 全局会话状态管理
│   ├── data/
│   │   ├── local/                   # Room, DataStore, SongCache
│   │   ├── remote/                  # Retrofit API 接口
│   │   └── repository/              # 数据仓库实现
│   ├── background/                  # WorkManager Workers
│   └── ui/                          # Compose UI (12 个模块)
├── ai/                              # AI 服务后端
│   ├── ai_service/main.py           # FastAPI 服务 (280行)
│   └── docker-compose.yml           # Redis + FastAPI 部署
├── gradle/libs.versions.toml        # 版本目录
└── build.gradle.kts                 # 项目构建配置
```

## 开发环境

| 工具 | 说明 |
|------|------|
| Android Studio | 2025/2026 |
| JDK | 17 |
| Gradle | 9.3.1 (Kotlin DSL + Version Catalog) |
| Android SDK | API 36 |

## 构建与运行

1. 克隆项目后，用 Android Studio 打开
2. 等待 Gradle 同步完成
3. 确保后端 NeteaseCloudMusicApi 服务已启动
4. 选择模拟器或真机运行

```bash
# 构建 Debug APK
./gradlew assembleDebug

# 运行测试
./gradlew test
```

## 参考资料

- [NeteaseCloudMusicApi](https://github.com/Binaryify/NeteaseCloudMusicApi)
- [Android Jetpack](https://developer.android.com/jetpack)
- [Jetpack Compose Material3](https://developer.android.com/compose/designsystems/material3)
- [Media3 (ExoPlayer)](https://developer.android.com/media/media3)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-guide.html)
