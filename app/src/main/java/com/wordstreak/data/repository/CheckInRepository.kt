package com.wordstreak.data.repository

import com.wordstreak.data.local.converter.DateValidator
import com.wordstreak.data.local.dao.CheckInDao
import com.wordstreak.data.local.entity.CheckIn
import kotlinx.coroutines.flow.Flow

/** 打卡仓库：保证同一自然日最多一条打卡记录。 */
class CheckInRepository(private val dao: CheckInDao) {

    /**
     * 记录打卡。若当天已存在打卡记录（date 主键冲突），返回 false 且不新增。
     */
    suspend fun checkIn(
        date: String,
        goalSnapshot: Int,
        completedCount: Int,
        completed: Boolean = completedCount >= goalSnapshot
    ): Boolean {
        DateValidator.requireValid(date)
        require(goalSnapshot >= 0) { "目标值不能为负" }
        require(completedCount >= 0) { "完成数不能为负" }
        val rowId = dao.insert(
            CheckIn(
                date = date,
                completed = completed,
                goalSnapshot = goalSnapshot,
                completedCount = completedCount,
                createdAt = System.currentTimeMillis()
            )
        )
        return rowId != -1L
    }

    suspend fun getByDate(date: String): CheckIn? {
        DateValidator.requireValid(date)
        return dao.getByDate(date)
    }

    fun observeAll(): Flow<List<CheckIn>> = dao.getAll()

    suspend fun recent(limit: Int): List<CheckIn> = dao.getRecentCheckIns(limit)

    suspend fun count(): Int = dao.count()
}
