package com.wordstreak.data.repository

import com.wordstreak.data.local.converter.DateValidator
import com.wordstreak.data.local.dao.DailyStatDao
import com.wordstreak.data.local.entity.DailyStat
import kotlinx.coroutines.flow.Flow

/** 统计仓库：每日学习统计的读写。 */
class StatisticsRepository(private val dao: DailyStatDao) {

    suspend fun getByDate(date: String): DailyStat? {
        DateValidator.requireValid(date)
        return dao.getByDate(date)
    }

    fun observeAll(): Flow<List<DailyStat>> = dao.getAll()

    suspend fun upsert(stat: DailyStat) {
        DateValidator.requireValid(stat.date)
        dao.upsert(stat)
    }
}
