package com.mcc.mycounter.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mcc.mycounter.data.entities.Consolidation
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsolidationDao {

    @Query("SELECT * FROM consolidations WHERE counterId = :counterId ORDER BY closedAt DESC")
    fun observeForCounter(counterId: Long): Flow<List<Consolidation>>

    @Query("SELECT * FROM consolidations WHERE counterId = :counterId ORDER BY closedAt DESC LIMIT 1")
    suspend fun lastForCounter(counterId: Long): Consolidation?

    @Query("SELECT * FROM consolidations WHERE counterId = :counterId ORDER BY closedAt ASC")
    suspend fun getAllForCounter(counterId: Long): List<Consolidation>

    @Insert
    suspend fun insert(consolidation: Consolidation): Long

    @Query("DELETE FROM consolidations WHERE counterId = :counterId")
    suspend fun deleteAllForCounter(counterId: Long)

    @Query("SELECT COUNT(*) FROM consolidations WHERE counterId = :counterId")
    suspend fun countForCounter(counterId: Long): Int
}
