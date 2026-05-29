package com.example.music_wyy.di

import androidx.room.Room
import com.example.music_wyy.data.local.AppDatabase
import com.example.music_wyy.data.local.dao.PlaylistDao
import com.example.music_wyy.data.local.dao.SongDao
import com.example.music_wyy.data.local.datastore.CookieStore
import com.example.music_wyy.data.remote.NeteaseApi
import com.example.music_wyy.data.repository.PlaylistRepository
import com.example.music_wyy.data.repository.PlaylistRepositoryImpl
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

val appModule = module {

    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

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

    single {
        val okHttp: OkHttpClient = get()
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .client(okHttp)
            .build()
    }

    single {
        val retrofit: Retrofit = get()
        retrofit.create(NeteaseApi::class.java)
    }

    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "music_wyy.db",
        ).build()
    }

    single<PlaylistDao> { get<AppDatabase>().playlistDao() }
    single<SongDao> { get<AppDatabase>().songDao() }

    single { CookieStore(androidContext()) }

    single<PlaylistRepository> { PlaylistRepositoryImpl(get()) }

    // ViewModels
    factory { com.example.music_wyy.ui.login.LoginViewModel(get(), get()) }
}

