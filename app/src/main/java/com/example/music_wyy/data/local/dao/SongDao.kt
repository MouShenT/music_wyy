package com.example.music_wyy.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.music_wyy.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM songs ORDER BY name")
    fun observeAll(): Flow<List<SongEntity>>

    @Upsert
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM songs WHERE isAvailable = 0")
    fun observeUnavailableSongs(): Flow<List<SongEntity>>
}
