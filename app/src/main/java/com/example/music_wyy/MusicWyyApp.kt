package com.example.music_wyy

import android.app.Application
import com.example.music_wyy.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MusicWyyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MusicWyyApp)
            modules(appModule)
        }
    }
}
