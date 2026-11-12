package com.wordstreak.data.repository

import com.wordstreak.data.local.converter.DateValidator
import com.wordstreak.data.local.dao.ReviewDao
import com.wordstreak.data.local.dao.StudyDao
import com.wordstreak.data.local.dao.WordDao
import com.wordstreak.data.local.entity.ReviewState
import com.wordstreak.data.local.entity.StudyRecord
import com.wordstreak.data.local.entity.StudySession
import kotlinx.coroutines.flow.Flow

/** 学习仓库：会话、学习记录、复习状态。 */
class StudyRepository(
    private val studyDao: StudyDao,
    private val reviewDao: ReviewDao,
    private val wordDao: WordDao
) {

    suspend fun startSession(session: StudySession): Long {
        DateValidator.requireValid(session.date)
        return studyDao.insertSession(session)
    }

    suspend fun finishSession(session: StudySession) = studyDao.updateSession(session)

    suspend fun getSession(id: Long): StudySession? = studyDao.getSession(id)

    /**
     * 写入学习记录时会快照单词拼写/释义；即使之后单词或词库被删除，历史记录仍可追溯。
     */
    suspend fun addRecord(record: StudyRecord): Long {
        require(record.result in RESULTS) { "非法的学习结果：${record.result}，应为 $RESULTS" }
        val word = record.wordId?.let { wordDao.getById(it) }
        val enriched = record.copy(
            wordSpelling = record.wordSpelling ?: word?.spelling,
            wordMeaning = record.wordMeaning ?: word?.meaning
        )
        return studyDao.insertRecord(enriched)
    }

    suspend fun recordsBySession(sessionId: Long): List<StudyRecord> =
        studyDao.getRecordsBySession(sessionId)

    suspend fun dailyRecords(date: String): List<StudyRecord> {
        DateValidator.requireValid(date)
        return studyDao.getDailyRecords(date)
    }

    suspend fun saveReviewState(state: ReviewState) {
        DateValidator.requireValid(state.dueDate)
        reviewDao.insertOrUpdate(state)
    }

    suspend fun dueWords(date: String): List<ReviewState> {
        DateValidator.requireValid(date)
        return reviewDao.getDueWords(date)
    }

    suspend fun reviewState(wordId: Long): ReviewState? = reviewDao.getByWordId(wordId)

    /** 月度日历：某区间内有学习记录的日期（Flow，实时）。 */
    fun observeActiveDatesBetween(start: String, end: String): Flow<List<String>> =
        studyDao.observeActiveDatesBetween(start, end)

    /** 每日统计：某天的学习记录（Flow，实时）。 */
    fun observeDailyRecords(date: String): Flow<List<StudyRecord>> =
        studyDao.observeDailyRecords(date)

    companion object {
        val RESULTS = setOf("AGAIN", "HARD", "GOOD", "EASY")
    }
}
