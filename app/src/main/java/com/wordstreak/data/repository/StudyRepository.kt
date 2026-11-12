package com.wordstreak.data.repository

import com.wordstreak.data.local.converter.DateValidator
import com.wordstreak.data.local.dao.ReviewDao
import com.wordstreak.data.local.dao.StudyDao
import com.wordstreak.data.local.dao.WordDao
import com.wordstreak.data.local.entity.ReviewState
import com.wordstreak.data.local.entity.StudyRecord
import com.wordstreak.data.local.entity.StudySession

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

    suspend fun finishSession(session: StudySession) {
        studyDao.updateSession(session)
    }

    suspend fun getSession(id: Long): StudySession? = studyDao.getSession(id)

    /**
     * 写入学习记录时会快照单词拼写/释义；即使之后单词或词库被删除，历史记录仍可追溯。
     */
    suspend fun addRecord(record: StudyRecord): Long {
        return studyDao.insertRecord(withWordSnapshot(record))
    }

    /**
     * Save the rating and session progress in one Room transaction.
     * The word snapshot is prepared first; both database writes then commit or roll back together.
     */
    suspend fun saveRatingAndSession(record: StudyRecord, session: StudySession): Long {
        return studyDao.insertRecordAndUpdateSession(withWordSnapshot(record), session)
    }

    private suspend fun withWordSnapshot(record: StudyRecord): StudyRecord {
        require(record.result in RESULTS) { "非法的学习结果：${record.result}，应为 $RESULTS" }
        val word = record.wordId?.let { wordDao.getById(it) }
        return record.copy(
            wordSpelling = record.wordSpelling ?: word?.spelling,
            wordMeaning = record.wordMeaning ?: word?.meaning
        )
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

    companion object {
        val RESULTS = setOf("AGAIN", "HARD", "GOOD", "EASY")
    }
}
