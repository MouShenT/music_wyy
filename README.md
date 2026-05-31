# 墨韵音乐 · InkRhythm Music

<p align="center">
  <b>开源 Android 音乐播放器 | AI 智能搜索 | 网易云音乐歌单管理</b><br>
  <em>Open-source Android Music Player · AI-Powered Search · Netease Cloud Music Playlist Manager</em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-7.0%2B-green?logo=android" alt="Android">
  <img src="https://img.shields.io/badge/Kotlin-2.1%2B-blueviolet?logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose-blue?logo=jetpackcompose" alt="Compose">
  <img src="https://img.shields.io/badge/Arch-Clean_Architecture-orange" alt="Architecture">
  <img src="https://img.shields.io/badge/AI-DeepSeek_LLM-6366f1" alt="AI">
  <img src="https://img.shields.io/badge/Backend-FastAPI-teal?logo=fastapi" alt="FastAPI">
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License">
</p>

---

An open-source Android music player and playlist management tool for Netease Cloud Music (网易云音乐), featuring AI-powered natural language song search, batch playlist import, karaoke-style lyric display, and fully automated daily check-in.

基于 Kotlin + Jetpack Compose + Clean Architecture 开发的开源 Android 音乐播放器与网易云音乐歌单管理工具。支持 AI 自然语言找歌、文本列表批量导入歌单、卡拉 OK 模式歌词显示、全自动每日签到。

## ✨ 核心功能 · Features

| 功能 Feature | 说明 Description |
|-------------|-----------------|
| 🎵 **完整音乐播放器** | 基于 Media3 (ExoPlayer)，支持流式播放、歌词同步、离线缓存、三种循环模式、MiniPlayer 迷你播放器 |
| 🤖 **AI 智能找歌** | 基于 DeepSeek LLM 的自然语言搜索，LLM 生成关键词 + 网易云 API 并行验证，消除 100% 虚构歌曲 |
| 📋 **批量导入歌单** | 粘贴任意格式「歌名 - 歌手」文本列表，多并发并行精确搜索匹配，一键创建歌单 |
| 🎤 **卡拉 OK 歌词** | LRC 逐字同步、中英翻译切换、沉浸式全屏模式、歌词一键导出分享 |
| ⏰ **全自动签到** | WorkManager 后台定时签到、云贝任务、每日 300 首刷歌打卡 |
| 💬 **私信 & 云贝** | 私信对话列表、云贝积分查询、任务中心 |
| 🌐 **AI 后端服务** | FastAPI + Redis 缓存 + httpx 异步并发，7 天 TTL 缓存加速重复查询 |

## 📱 与官方 App 对比 · vs Official App

| 功能点 | 网易云官方 | 墨韵音乐 |
|--------|:--------:|:--------:|
| 外部歌单导入 | 链接/OCR截图/本地扫描 | **文本列表批量导入 + API 精确匹配** |
| AI 语义搜索 | 仅关键词精确匹配 | **LLM 自然语言搜索 (DeepSeek)** |
| 歌词导出 | 仅在线查看 | **LRC 文本导出 + 翻译切换** |
| 自动签到 | 需手动操作 | **WorkManager 定时自动执行** |
| 批量创建歌单 | 无文本输入接口 | **文本列表一键批量导入** |

## 🏗️ 技术栈 · Tech Stack

| 类别 Category | 选型 Stack |
|-------------|-----------|
| 语言 Language | Kotlin 2.1+ |
| UI 框架 | Jetpack Compose + Material Design 3 |
| 架构 Architecture | MVVM + UDF (单向数据流) + Clean Architecture |
| 依赖注入 DI | Koin |
| 数据库 Database | Room + KSP |
| 网络 Networking | Retrofit + OkHttp + kotlinx.serialization |
| 异步处理 Async | Kotlin Coroutines + Flow |
| 媒体播放 Media | AndroidX Media3 (ExoPlayer) |
| 后台任务 Background | WorkManager |
| 图片加载 Image | Coil 3 |
| AI 后端 AI Backend | Python FastAPI + DeepSeek API + Redis |
| 最低 / 目标 SDK | API 24 (Android 7.0) → API 36 (Android 15) |

## 🧱 系统架构 · Architecture

```
┌──────────────────────────────────────────┐
│  UI Layer (Compose Screens + ViewModels) │
│  · StateFlow + UDF 单向数据流            │
│  · Navigation Compose 导航              │
│  · Material Design 3 暗色主题           │
├──────────────────────────────────────────┤
│  Domain Layer (UseCase / Model)          │
│  · 领域模型: Song, Playlist              │
│  · Repository 接口                       │
├──────────────────────────────────────────┤
│  Data Layer (Repository / DataSource)    │
│  · Room (本地持久化)                     │
│  · DataStore (配置/令牌)                 │
│  · Retrofit (远程 API 代理)             │
│  · ExoPlayer (流媒体播放)                │
└──────────────────────────────────────────┘
```

## 🧩 功能模块 · Modules (12 个)

| # | 模块 Module | 说明 Description |
|---|------------|-----------------|
| 1 | **登录 Login** | Cookie 授权登录，DataStore 持久化令牌 (Token storage) |
| 2 | **首页 Home** | 音乐搜索、AI 找歌入口、数据概览 Dashboard |
| 3 | **歌单列表 Playlists** | 用户歌单浏览、下拉刷新 (Pull-to-refresh) |
| 4 | **歌单详情 Detail** | 歌曲列表、流式播放、添加到其他歌单 |
| 5 | **批量创建 Batch Create** | 文本导入搜索、并行匹配、一键创建歌单 |
| 6 | **AI 找歌 AI Search** | 自然语言搜索、LLM + API 双层验证 (Dual verification) |
| 7 | **音乐播放器 Player** | 播放控制、三种循环模式、MiniPlayer |
| 8 | **歌词显示 Lyrics** | LRC 逐字同步、翻译切换、沉浸式模式 |
| 9 | **自动签到 Auto Sign-in** | WorkManager 定时签到/云贝/每日刷歌 |
| 10 | **私信中心 Messages** | 私信列表、对话详情 |
| 11 | **云贝中心 Yunbei** | 积分查询、任务列表 |
| 12 | **个人中心 Profile** | 用户信息、设置、退出登录 |

## 🤖 AI 找歌架构 · AI Search Pipeline

```
用户输入 "进击的巨人所有 OP 和 ED"
        ↓
  DeepSeek LLM 生成 30+ 个多维度搜索关键词
        ↓
  httpx 6 并发搜索 Netease CloudSearch API
        ↓
  按 track ID 去重 → 返回真实可播放的歌曲列表
        ↓
  Redis 缓存 (7 天 TTL) → 相同查询秒级响应
```

> **核心设计**：每首歌必须经过网易云 API 实际搜索验证，消除 100% 的 LLM 虚构歌曲 (AI hallucination elimination).

## 🚀 快速开始 · Quick Start

### 环境要求 · Requirements

| Android SDK | API 36 | |

后端部署在 Linux (内网)，App 通过内网直连，不经过公网。

### 构建 · Build

```bash
git clone https://github.com/MouShenT/music_wyy.git
cd music_wyy

# 构建 Debug APK
./gradlew assembleDebug

# 运行测试
./gradlew test
```

1. 用 Android Studio 打开项目目录
2. 等待 Gradle 同步完成
3. 确保后端 NeteaseCloudMusicApi 服务已启动（Docker）
4. 选择模拟器或真机运行

### 后端部署 · Backend Deployment

后端服务需要部署 NeteaseCloudMusicApi (Docker, 端口 3000) 和 AI 服务 (FastAPI + Redis).

```bash
# AI 后端服务
cd ai/docker-compose.yml
docker compose up -d
```

## 📁 项目结构 · Project Structure

```
music_wyy/
├── app/src/main/java/com/example/music_wyy/
│   ├── MainActivity.kt              # 入口 Activity
│   ├── MusicWyyApp.kt               # Application (Koin 初始化)
│   ├── MainApp.kt                   # 主 Composable (MiniPlayer 集成)
│   ├── di/AppModule.kt              # Koin 依赖注入配置
│   ├── domain/model/                # 领域模型 (Domain models)
│   ├── session/UserSession.kt       # 全局会话状态管理
│   ├── data/
│   │   ├── local/                   # Room DAO, DataStore, SongCache
│   │   ├── remote/                  # Retrofit API 接口定义
│   │   └── repository/              # 数据仓库实现 (Repository impl)
│   ├── background/                  # WorkManager Workers
│   └── ui/
│       ├── ai/                      # AI 找歌 (DeepSeek natural language search)
│       ├── automation/              # 自动签到 (Automated check-in)
│       ├── home/                    # 首页 (Home dashboard)
│       ├── login/                   # 登录 (Cookie auth)
│       ├── lyric/                   # 歌词显示 (Karaoke-style lyrics)
│       ├── message/                 # 私信中心 (Message center)
│       ├── navigation/              # 导航图 (Navigation graph)
│       ├── player/                  # 播放器 (Media3 ExoPlayer + MiniPlayer)
│       ├── playlist/                # 歌单 + 批量创建 (Batch create)
│       ├── profile/                 # 个人中心 (User profile)
│       ├── settings/                # 设置 (App settings)
│       ├── theme/                   # B&W 极简主题 (Minimalist theme)
│       └── yunbei/                  # 云贝中心 (Yunbei points)
├── ai/
│   ├── main.py                      # FastAPI 服务 (280 行)
│   └── docker-compose.yml           # Redis + FastAPI 部署
├── gradle/libs.versions.toml        # 版本目录
├── build.gradle.kts
└── settings.gradle.kts
```

## 🔗 相关链接 · Links

- [NeteaseCloudMusicApi](https://github.com/Binaryify/NeteaseCloudMusicApi) — 网易云音乐 API 代理服务
- [Jetpack Compose](https://developer.android.com/compose) — Android 声明式 UI 框架
- [Media3 ExoPlayer](https://developer.android.com/media/media3) — Android 媒体播放库
- [DeepSeek API](https://platform.deepseek.com) — AI 大语言模型服务
- [FastAPI](https://fastapi.tiangolo.com) — Python 高性能异步 Web 框架

## 📄 License

MIT License — 开源免费，可自由使用、修改、分发。
