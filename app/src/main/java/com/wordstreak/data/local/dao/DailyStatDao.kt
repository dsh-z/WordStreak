package com.wordstreak.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wordstreak.data.local.entity.DailyStat
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStatDao {
    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getByDate(date: String): DailyStat?

    @Query("SELECT * FROM daily_stats ORDER BY date ASC")
    fun getAll(): Flow<List<DailyStat>>

    @Upsert
    suspend fun upsert(stat: DailyStat)
}
