package com.example.music_wyy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val name: String,
    val artist: String?,
    val album: String?,
    val duration: Int?,
    val isAvailable: Boolean = true,
)
