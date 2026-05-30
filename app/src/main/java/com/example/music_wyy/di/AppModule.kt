package com.example.music_wyy.di

import androidx.room.Room
import com.example.music_wyy.data.local.AppDatabase
import com.example.music_wyy.data.local.dao.PlaylistDao
import com.example.music_wyy.data.local.dao.SongDao
import com.example.music_wyy.data.local.datastore.CookieStore
import com.example.music_wyy.data.remote.NeteaseAiApi
import com.example.music_wyy.data.remote.NeteaseApi
import com.example.music_wyy.data.repository.PlaylistRepository
import com.example.music_wyy.data.repository.PlaylistRepositoryImpl
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

val appModule = module {

    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    // ── OkHttp Clients ──

    single {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    // 网易云 API Retrofit
    single(named("netease")) {
        val okHttp: OkHttpClient = get()
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .client(okHttp)
            .build()
    }

    // AI 服务 Retrofit（超时更长，LLM 推理需要等待）
    single(named("ai")) {
        val aiOkHttp = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()
        val json: Json = get()
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8100/")
            .client(aiOkHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    single {
        val retrofit: Retrofit = get(named("netease"))
        retrofit.create(NeteaseApi::class.java)
    }

    single {
        val aiRetrofit: Retrofit = get(named("ai"))
        aiRetrofit.create(NeteaseAiApi::class.java)
    }

    // ── Room ──

    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "music_wyy.db",
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    single<PlaylistDao> { get<AppDatabase>().playlistDao() }
    single<SongDao> { get<AppDatabase>().songDao() }

    // ── DataStore / Session ──

    single { CookieStore(androidContext()) }
    single { com.example.music_wyy.data.local.datastore.SettingsStore(androidContext()) }
    single { com.example.music_wyy.data.local.SongCache(androidContext(), get()) }
    single { com.example.music_wyy.background.AutomationScheduler(androidContext()) }
    single { com.example.music_wyy.session.UserSession() }

    // ── Repository ──

    single<PlaylistRepository> { PlaylistRepositoryImpl(get()) }

    // ── ViewModels ──

    factory { com.example.music_wyy.ui.home.HomeViewModel(get(), get(), get()) }
    factory { com.example.music_wyy.ui.login.LoginViewModel(get(), get(), get()) }
    factory { com.example.music_wyy.ui.playlist.PlaylistViewModel(get(), get(), get()) }
    factory { com.example.music_wyy.ui.playlist.PlaylistDetailViewModel(get(), get()) }
    factory { com.example.music_wyy.ui.playlist.BatchCreateViewModel(get(), get(), get()) }
    factory { com.example.music_wyy.ui.automation.AutomationViewModel(get(), get(), get(), get()) }
    factory { com.example.music_wyy.ui.profile.ProfileViewModel(get(), get()) }
    factory { com.example.music_wyy.ui.ai.AiSearchViewModel(get()) }
    factory { com.example.music_wyy.ui.lyric.LyricViewModel(get(), get()) }
    factory { com.example.music_wyy.ui.message.MsgViewModel(get(), get(), get()) }
    factory { com.example.music_wyy.ui.yunbei.YunbeiViewModel(get(), get()) }
    factory { com.example.music_wyy.ui.player.PlayerViewModel(get(), get(), get()) }
}
