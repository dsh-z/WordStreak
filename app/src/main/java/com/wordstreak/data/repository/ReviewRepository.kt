package com.wordstreak.data.repository

import com.wordstreak.data.local.converter.DateValidator
import com.wordstreak.data.local.dao.ReviewDao
import com.wordstreak.data.local.entity.ReviewState
import com.wordstreak.data.local.entity.Word
import com.wordstreak.domain.usecase.ReviewScheduler

/**
 * 复习仓库：待复习列表 + 评分后更新下次复习时间（ReviewState）。
 * 复用 AGAIN/HARD/GOOD/EASY；不改数据库结构。
 */
class ReviewRepository(private val reviewDao: ReviewDao) {

    fun observeDueWords(date: String): kotlinx.coroutines.flow.Flow<List<Word>> =
        kotlinx.coroutines.flow.flow { emit(loadDueWords(date)) }

    suspend fun loadDueWords(date: String): List<Word> {
        DateValidator.requireValid(date)
        return reviewDao.getDueWordsWithWord(date)
    }

    suspend fun countDueWords(date: String): Int {
        DateValidator.requireValid(date)
        return reviewDao.countDueWords(date)
    }

    suspend fun getReviewState(wordId: Long): ReviewState? = reviewDao.getByWordId(wordId)

    /**
     * 记录一次复习评分，更新该单词的下次到期日/间隔/复习次数。
     * @return 更新后的 ReviewState。
     */
    suspend fun review(word: Word, result: String, today: String): ReviewState {
        require(result in ReviewScheduler.RESULTS) { "不支持的评分：$result" }
        DateValidator.requireValid(today)
        val existing = reviewDao.getByWordId(word.id)
        val intervalDays = ReviewScheduler.nextIntervalDays(result)
        val dueDate = ReviewScheduler.nextDueDate(result, today)
        val updated = ReviewState(
            wordId = word.id,
            dueDate = dueDate,
            difficulty = existing?.difficulty,
            stability = existing?.stability,
            intervalDays = intervalDays,
            lastReviewedAt = System.currentTimeMillis(),
            reviewCount = (existing?.reviewCount ?: 0) + 1
        )
        reviewDao.insertOrUpdate(updated)
        return updated
    }

    companion object {
        val RESULTS = ReviewScheduler.RESULTS
    }
}
