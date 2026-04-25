package com.mcc.mycounter.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mcc.mycounter.data.entities.TapEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface TapEventDao {

    @Query("SELECT * FROM tap_events WHERE counterId = :counterId ORDER BY timestamp DESC")
    fun observeForCounter(counterId: Long): Flow<List<TapEvent>>

    @Query("SELECT * FROM tap_events WHERE counterId = :counterId AND timestamp >= :fromMs ORDER BY timestamp ASC")
    suspend fun getInRange(counterId: Long, fromMs: Long): List<TapEvent>

    @Query("SELECT * FROM tap_events WHERE counterId = :counterId ORDER BY timestamp ASC")
    suspend fun getAllForCounter(counterId: Long): List<TapEvent>

    @Insert
    suspend fun insert(event: TapEvent): Long

    @Query("DELETE FROM tap_events WHERE counterId = :counterId")
    suspend fun deleteAllForCounter(counterId: Long)

    @Query("DELETE FROM tap_events WHERE counterId = :counterId AND timestamp < :beforeMs")
    suspend fun deleteOlderThan(counterId: Long, beforeMs: Long)

    @Query("SELECT COUNT(*) FROM tap_events WHERE counterId = :counterId")
    suspend fun countForCounter(counterId: Long): Int
}
