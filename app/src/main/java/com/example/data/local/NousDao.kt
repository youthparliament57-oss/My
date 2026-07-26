package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NousDao {
    @Query("SELECT * FROM thoughts ORDER BY timestamp DESC")
    fun getThoughts(): Flow<List<ThoughtEntity>>

    @Query("SELECT * FROM thoughts WHERE id = :id")
    suspend fun getThoughtById(id: Long): ThoughtEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThought(thought: ThoughtEntity): Long

    @Query("DELETE FROM thoughts WHERE id = :id")
    suspend fun deleteThought(id: Long)

    @Query("SELECT * FROM connections")
    fun getConnections(): Flow<List<ConnectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnection(connection: ConnectionEntity)

    @Query("DELETE FROM connections WHERE id = :id")
    suspend fun deleteConnection(id: Long)

    @Query("DELETE FROM connections WHERE sourceId = :thoughtId OR targetId = :thoughtId")
    suspend fun deleteConnectionsForThought(thoughtId: Long)
}
