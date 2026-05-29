package com.example.music_wyy.data.repository

import com.example.music_wyy.data.local.dao.PlaylistDao
import com.example.music_wyy.data.local.entity.PlaylistEntity
import com.example.music_wyy.domain.model.Playlist
import com.example.music_wyy.domain.model.PlaylistId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface PlaylistRepository {
    fun observePlaylists(): Flow<List<Playlist>>
    suspend fun deletePlaylist(id: String)
}

class PlaylistRepositoryImpl(
    private val dao: PlaylistDao,
) : PlaylistRepository {

    override fun observePlaylists(): Flow<List<Playlist>> = dao.observeAll().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun deletePlaylist(id: String) = dao.delete(id)
}

private fun PlaylistEntity.toDomain() = Playlist(
    id = PlaylistId(id),
    name = name,
    songCount = songCount,
    coverUrl = coverUrl,
    creator = creator,
    description = description,
)
