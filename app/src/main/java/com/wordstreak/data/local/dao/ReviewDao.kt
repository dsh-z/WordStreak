package com.wordstreak.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wordstreak.data.local.entity.ReviewState

@Dao
interface ReviewDao {
    @Upsert
    suspend fun insertOrUpdate(state: ReviewState)

    @Query("SELECT * FROM review_states WHERE dueDate <= :date ORDER BY dueDate ASC")
    suspend fun getDueWords(date: String): List<ReviewState>

    @Query("SELECT * FROM review_states WHERE wordId = :wordId")
    suspend fun getByWordId(wordId: Long): ReviewState?
}
