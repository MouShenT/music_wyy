# CLAUDE.md — 网易云音乐工具 App

> Android 原生项目 · Kotlin + Jetpack Compose · Clean Architecture
> 最后更新: 2026-05-29

---

## 项目概述

网易云音乐歌单管理 + 自动化工具。核心功能：批量歌单管理、自动签到/刷歌、歌单数据分析、跨平台迁移、监控告警。

后端 API 服务：NeteaseCloudMusicApi（Node.js），App 通过 HTTP REST 内网访问，**不经过公网**。

---

## 技术栈（固定版本）

| 类别 | 选型 | 版本 |
|------|------|------|
| 语言 | Kotlin | 2.1+ |
| UI | Jetpack Compose + Material3 | BOM 2025+ |
| 架构模式 | MVVM + UDF（单向数据流） | — |
| DI | Hilt | 最新稳定版 |
| 数据库 | Room + KSP | 最新稳定版 |
| 网络 | Retrofit + OkHttp + kotlinx.serialization | 最新 |
| 异步 | Coroutines + Flow | 1.9+ |
| 构建 | Gradle Kotlin DSL + Version Catalog | AGP 8.7+ |
| 后台任务 | WorkManager | 最新 |
| 图表 | Vico 或 MPAndroidChart | 最新 |
| 本地存储 | DataStore (Preferences) | 最新 |

---

## Kotlin 代码规范

### 核心原则
- **`val` 优先**：默认不可变，`var` 仅在必要时使用且需注释说明
- **禁止 `!!`**：用 `?.`、`?:`、`requireNotNull()` 替代
- **禁止 `lateinit var`**：除 Hilt DI 入口外一律不用，用 `by lazy` 或构造注入
- **禁止 `GlobalScope`**：所有协程绑定到生命周期（`viewModelScope`、`lifecycleScope`）
- **类型安全**：用 `@JvmInline value class` 封装 ID（如 `SongId`、`PlaylistId`），不用裸 `Long`/`String`
- **sealed interface** 表示状态和错误，不用枚举在大 when 中

### 空安全
```kotlin
// ✅ 正确
val name = user?.name ?: return
val token = requireNotNull(authToken) { "Auth token missing" }

// ❌ 禁止
val name = user!!.name
```

### 协程
```kotlin
// ViewModel 中
viewModelScope.launch { ... }

// Compose 中
LaunchedEffect(key) { ... }

// Flow 收集
val state by viewModel.state.collectAsStateWithLifecycle()

// ❌ 禁止
GlobalScope.launch { ... }
```

### 取消处理
```kotlin
try {
    // 业务逻辑
} catch (e: CancellationException) {
    throw e  // 必须重新抛出
} catch (e: SpecificException) {
    // 处理特定异常
}
```

---

## Jetpack Compose 规范

### 组件设计
```kotlin
// ✅ Stateful wrapper（Screen 级别）
@Composable
fun PlaylistScreen(viewModel: PlaylistViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PlaylistContent(state = state, onEvent = viewModel::onEvent)
}

// ✅ Stateless content（可预览、可测试）
@Composable
fun PlaylistContent(state: PlaylistUiState, onEvent: (PlaylistEvent) -> Unit) {
    // UI here
}
```

### 状态提升
- 叶子组件不持有 `mutableStateOf`，所有状态从参数传入
- `remember` 仅用于 UI 内部状态（如动画、焦点）
- `rememberSaveable` 用于需跨配置变更保持的状态

### 副作用 API
| 场景 | 使用 |
|------|------|
| 初始化/监听 lifecycle | `LaunchedEffect(key)` |
| 需要清理的资源 | `DisposableEffect(key)` |
| 非 Compose 回调 → Compose state | `SideEffect { }` |
| 需记住跨重组的值 | `rememberUpdatedState` |

### 禁止
- `LazyColumn` 中不用稳定 key → 必须传 `key = { it.id }`
- Compose 中不用硬编码字符串 → 用 `stringResource(R.string.xxx)`
- 不在 Composable 中直接调用 suspend 函数 → 用 `LaunchedEffect` 或 `produceState`
- `collectAsState()` 不用 lifecycle-aware 版本 → **必须**用 `collectAsStateWithLifecycle()`

---

## 架构规范

### 分层
```
UI（Screen + ViewModel）
 ↓ StateFlow<UiState> / ↑ Event
Domain（UseCase）
 ↓ Flow<T> / ↑ suspend fun
Data（Repository → Room / Retrofit）
```

### ViewModel
```kotlin
@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val getPlaylistsUseCase: GetPlaylistsUseCase,
    private val addSongsUseCase: AddSongsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(PlaylistUiState())
    val state: StateFlow<PlaylistUiState> = _state.asStateFlow()

    fun onEvent(event: PlaylistEvent) {
        when (event) {
            is PlaylistEvent.AddSongs -> addSongs(event.songs)
            // ...
        }
    }
}
```

### UseCase
```kotlin
class GetPlaylistsUseCase @Inject constructor(
    private val repo: PlaylistRepository,
) {
    operator fun invoke(): Flow<List<Playlist>> = repo.getPlaylists()
}
```

### Repository
```kotlin
interface PlaylistRepository {
    fun getPlaylists(): Flow<List<Playlist>>
    suspend fun addSongs(playlistId: String, songIds: List<String>)
}

class PlaylistRepositoryImpl @Inject constructor(
    private val api: NeteaseApi,
    private val dao: PlaylistDao,
) : PlaylistRepository {
    // offline-first: 先写 Room，再同步 API
}
```

---

## 网络层规范

```kotlin
// Retrofit 接口
interface NeteaseApi {
    @GET("/login/status")
    suspend fun checkLogin(): LoginStatusResponse

    @GET("/user/playlist")
    suspend fun getUserPlaylists(@Query("uid") uid: String): PlaylistResponse

    @POST("/playlist/tracks")
    suspend fun manageTracks(@Body body: TrackManageRequest): TrackManageResponse
}

// OkHttp 拦截器：自动附加 Cookie
class CookieInterceptor(private val cookieStore: CookieStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("Cookie", cookieStore.getCookie())
            .build()
        return chain.proceed(request)
    }
}
```

- 请求间隔 ≥ 500ms（由 OkHttp Interceptor 控制，防止 429）
- 所有 API 返回 `Result<T>` 密封类
- Cookie 持久化到 DataStore，不存明文密码

---

## Room 规范

```kotlin
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val songCount: Int,
    val coverUrl: String?,
)

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY name")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Upsert
    suspend fun upsertAll(playlists: List<PlaylistEntity>)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: String)
}
```

- DAO 方法暴露 `Flow<T>`（观察）或 `suspend fun`（写入）
- 不用 `LiveData`，不用 RxJava
- 所有 DAO 操作在主线程安全（事务用 `@Transaction`）

---

## 测试规范

```kotlin
// ViewModel 测试
@Test
fun `add songs updates state`() = runTest {
    val viewModel = PlaylistViewModel(fakeRepo)
    viewModel.onEvent(PlaylistEvent.AddSongs(listOf(song1)))
    // advanceUntilIdle() 等待挂起函数完成
    assertEquals(expectedState, viewModel.state.value)
}

// Repository 测试 — 用真实 in-memory Room
@Test
fun `getPlaylists returns cached data`() = runTest {
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    val repo = PlaylistRepositoryImpl(api, db.dao())
    // ...
}
```

- 用 `runTest`，不用 `runBlocking`
- Repository 测试用真实内存数据库，不 mock DAO
- API 测试用 MockWebServer（OkHttp），不 mock Retrofit 接口

---

## Gradle 规范

```kotlin
// settings.gradle.kts（不用 Groovy）
// gradle/libs.versions.toml 管理所有版本
// 版本号固定，不用 `+` 或 `latest.release`

plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}
```

---

## 禁止清单

| ❌ 禁止 | 替代 |
|---------|------|
| `!!` 操作符 | `?.` / `?:` / `requireNotNull()` |
| `GlobalScope` | `viewModelScope` / `lifecycleScope` |
| `LiveData` | `StateFlow` / `SharedFlow` |
| `kapt` | KSP（`com.google.devtools.ksp`） |
| Gson / Jackson | `kotlinx.serialization` |
| Groovy build 文件 | Kotlin DSL（`build.gradle.kts`） |
| `runBlocking`（测试） | `runTest` |
| `lateinit var` | 构造注入 / `by lazy` |
| 裸 `Thread` / `ExecutorService` | Coroutines |
| Compose `collectAsState()` | `collectAsStateWithLifecycle()` |
| 硬编码中文字符串 | `stringResource(R.string.xxx)` |
| `catch (e: Exception)` | 捕获具体异常类型 |
| 吃掉 `CancellationException` | 必须 `throw e` |
| `_state.value = _state.value.copy(...)` | `_state.update { it.copy(...) }` |
| 密码明文存储 | DataStore 加密存储 |

---

## 项目可用 Skills（38 个已安装到 `.claude/skills/`）

触发方式：直接 `/skill-name` 调用，或对话中提到相关主题时 Claude 自动匹配加载。

### UI / 设计（🆕 新安装）
| Skill | 来源 | 用途 |
|-------|------|------|
| **material-3** | hamen/material-3-skill (⭐857) | MD3 组件/令牌/主题/合规审计，Compose 优先 |
| **frontend-design** | anthropics/skills | 防 AI slop，强制美学方向选择 |
| **theme-factory** | anthropics/skills | 10 套预设专业主题（Ocean/Arctic/Galaxy...） |
| **hue** | dominikmartn/hue | 从品牌 URL/截图自动生成设计系统 |

### Compose / Kotlin
| Skill 来源 | 覆盖范围 |
|-----------|---------|
| **chrisbanes/skills** | Compose 状态/副作用/动画/性能/测试/Slot API（16 个） |
| **android/skills** | Navigation 3/Edge-to-Edge/CameraX/Adaptive/Testing（13 个） |
| **Kotlin/kotlin-agent-skills** | Java→Kotlin/AGP 9 迁移/CocoaPods→SPM（4 个） |

---

## UI 设计规范（🆕）

### Material Design 3（唯一标准）
- **Material3 强制**，不使用 Material2 或自定义风格
- 使用 `MaterialTheme` + `MaterialTheme.colorScheme` 统一令牌
- 动态取色（Dynamic Color）作为默认方案
- 暗黑模式必须支持（`darkColorScheme`）
- 边到边（edge-to-edge）作为默认布局

### 组件与状态
```kotlin
// ✅ 所有交互状态完整
Button(
    onClick = { ... },
    enabled = true,
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    ),
    shape = MaterialTheme.shapes.medium,
)

// ❌ 禁止：缺少 disabled/hover/pressed 状态
// ❌ 禁止：硬编码颜色值（如 Color(0xFF1A73E8)）
```

### 字体与排版
- 使用 `MaterialTheme.typography` 的字体等级（display/headline/title/body/label）
- 不硬编码 `FontFamily` 或 `FontSize`
- 中文优先使用系统默认中文字体

### Shape & Elevation
```kotlin
// 卡片用 medium，FAB 用 large，Chip 用 small
Card(shape = MaterialTheme.shapes.medium,
     elevation = CardDefaults.cardElevation(defaultElevation = 2.dp))
```

### 过渡动画
- 页面切换使用 `AnimatedContent` 或共享元素过渡
- 列表项动画使用 `animateItemPlacement()`
- 加载状态使用 M3 的骨架屏模式

### MD3 合规检查
使用 `/material-3 audit ` 命令可对页面进行 10 维评分：
颜色令牌/字体/形状/高度/组件适配/布局响应/导航/Motion/无障碍/主题一致性。

---

## 开发环境

| 工具 | 位置 |
|------|------|
| Android Studio | `D:\software\AndroidStudio2025` / `2026` |
| Android SDK | `D:\software\AndroidStudioData\sdk` (API 36) |
| JDK 23 | `D:\software\jdk\jdk23` |
| Gradle | wrapper 9.3.1（`.gradle` 已缓存） |
| NeteaseCloudMusicApi | 本地 `npm start` → `localhost:3000` |

---

## 编辑器提示

- 缩进：4 空格
- 最大行宽：120 字符
- 命名：变量/函数 camelCase，类 PascalCase
- 公开 API 需 KDoc（一行即可）
- 导入用 `*` 通配符仅限 `androidx.compose.*` 和 `kotlinx.coroutines.*`
