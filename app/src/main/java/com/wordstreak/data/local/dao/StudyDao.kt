package com.wordstreak.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wordstreak.data.local.entity.StudyRecord
import com.wordstreak.data.local.entity.StudySession

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
}
