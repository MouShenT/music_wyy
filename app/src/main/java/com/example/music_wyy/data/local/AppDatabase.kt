package com.example.music_wyy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.music_wyy.data.local.dao.PlaylistDao
import com.example.music_wyy.data.local.dao.SongDao
import com.example.music_wyy.data.local.entity.PlaylistEntity
import com.example.music_wyy.data.local.entity.SongEntity

@Database(
    entities = [PlaylistEntity::class, SongEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun songDao(): SongDao
}
