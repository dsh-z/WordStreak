package com.wordstreak.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wordstreak.data.local.entity.CheckIn
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {
    /** date 为主键，配合 IGNORE：同一自然日重复打卡不会产生第二条记录（返回 -1）。 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(checkIn: CheckIn): Long

    @Query("SELECT * FROM check_ins WHERE date = :date")
    suspend fun getByDate(date: String): CheckIn?

    @Query("SELECT * FROM check_ins ORDER BY date DESC")
    fun getAll(): Flow<List<CheckIn>>

    @Query("SELECT * FROM check_ins ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentCheckIns(limit: Int): List<CheckIn>

    @Query("SELECT COUNT(*) FROM check_ins")
    suspend fun count(): Int
}
