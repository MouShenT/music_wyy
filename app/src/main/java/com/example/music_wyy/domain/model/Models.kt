package com.example.music_wyy.domain.model

@JvmInline
value class SongId(val value: String)

@JvmInline
value class PlaylistId(val value: String)

data class Song(
    val id: SongId,
    val name: String,
    val artist: String?,
    val album: String?,
    val duration: Int?,
    val isAvailable: Boolean = true,
)

data class Playlist(
    val id: PlaylistId,
    val name: String,
    val songCount: Int,
    val coverUrl: String?,
    val creator: String?,
    val description: String?,
)
