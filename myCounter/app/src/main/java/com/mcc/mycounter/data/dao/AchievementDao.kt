package com.mcc.mycounter.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mcc.mycounter.data.entities.Achievement
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements WHERE counterId = :counterId ORDER BY periodEndedAt DESC")
    fun observeForCounter(counterId: Long): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE counterId = :counterId ORDER BY periodEndedAt DESC LIMIT 1")
    suspend fun lastForCounter(counterId: Long): Achievement?

    @Query("SELECT * FROM achievements WHERE counterId = :counterId ORDER BY periodEndedAt DESC LIMIT :limit")
    suspend fun lastNForCounter(counterId: Long, limit: Int): List<Achievement>

    @Query("SELECT * FROM achievements WHERE counterId = :counterId ORDER BY periodEndedAt ASC")
    suspend fun getAllForCounter(counterId: Long): List<Achievement>

    @Insert
    suspend fun insert(achievement: Achievement): Long

    @Query("DELETE FROM achievements WHERE counterId = :counterId")
    suspend fun deleteAllForCounter(counterId: Long)

    @Query("SELECT COUNT(*) FROM achievements WHERE counterId = :counterId AND outcome = 'SUCCESS'")
    suspend fun countSuccessForCounter(counterId: Long): Int
}
