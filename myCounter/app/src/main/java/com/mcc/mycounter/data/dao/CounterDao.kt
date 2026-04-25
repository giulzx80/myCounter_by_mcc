package com.mcc.mycounter.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mcc.mycounter.data.entities.Counter
import kotlinx.coroutines.flow.Flow

@Dao
interface CounterDao {

    @Query("SELECT * FROM counters ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<Counter>>

    @Query("SELECT * FROM counters ORDER BY updatedAt DESC")
    suspend fun getAll(): List<Counter>

    @Query("SELECT * FROM counters WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Counter?

    @Query("SELECT * FROM counters WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<Counter?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(counter: Counter): Long

    @Update
    suspend fun update(counter: Counter)

    @Query("DELETE FROM counters WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE counters SET currentValue = :value, lastTapAt = :ts, updatedAt = :ts WHERE id = :id")
    suspend fun updateValue(id: Long, value: Long, ts: Long)

    @Query("UPDATE counters SET currentValue = :value, lastConsolidatedAt = :ts, updatedAt = :ts WHERE id = :id")
    suspend fun applyConsolidationReset(id: Long, value: Long, ts: Long)

    @Query("SELECT COUNT(*) FROM counters")
    suspend fun count(): Int
}
