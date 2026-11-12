package com.wordstreak.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wordstreak.data.local.entity.StudyRecord
import com.wordstreak.data.local.entity.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: StudySession): Long

    @Update
    suspend fun updateSession(session: StudySession)

    @Query("SELECT * FROM study_sessions WHERE id = :id")
    suspend fun getSession(id: Long): StudySession?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecord(record: StudyRecord): Long

    @Query("SELECT * FROM study_records WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    suspend fun getRecordsBySession(sessionId: Long): List<StudyRecord>

    @Query(
        "SELECT * FROM study_records WHERE sessionId IN " +
            "(SELECT id FROM study_sessions WHERE date = :date) ORDER BY createdAt ASC"
    )
    suspend fun getDailyRecords(date: String): List<StudyRecord>

    @Query("SELECT * FROM study_records WHERE wordId = :wordId ORDER BY createdAt DESC")
    suspend fun getRecordsByWord(wordId: Long): List<StudyRecord>

    /** 某日期区间内有学习记录（存在 study_records）的日期集合，用于月度日历标记。只读查询，不改表结构。 */
    @Query(
        "SELECT DISTINCT s.date FROM study_sessions s WHERE s.date BETWEEN :start AND :end " +
            "AND EXISTS (SELECT 1 FROM study_records r WHERE r.sessionId = s.id)"
    )
    fun observeActiveDatesBetween(start: String, end: String): Flow<List<String>>

    /** 某天的学习记录（Flow），用于每日统计的实时更新。 */
    @Query(
        "SELECT * FROM study_records WHERE sessionId IN " +
            "(SELECT id FROM study_sessions WHERE date = :date) ORDER BY createdAt ASC"
    )
    fun observeDailyRecords(date: String): Flow<List<StudyRecord>>
}
