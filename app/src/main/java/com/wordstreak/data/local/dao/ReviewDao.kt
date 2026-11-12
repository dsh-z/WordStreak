package com.wordstreak.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wordstreak.data.local.entity.ReviewState
import com.wordstreak.data.local.entity.Word

@Dao
interface ReviewDao {
    @Upsert
    suspend fun insertOrUpdate(state: ReviewState)

    @Query("SELECT * FROM review_states WHERE dueDate <= :date ORDER BY dueDate ASC")
    suspend fun getDueWords(date: String): List<ReviewState>

    @Query("SELECT * FROM review_states WHERE wordId = :wordId")
    suspend fun getByWordId(wordId: Long): ReviewState?

    /** 待复习单词：没有复习状态的新词，或已到期（dueDate <= 今天）的单词。只读查询，不改表结构。 */
    @Query(
        "SELECT w.* FROM words w LEFT JOIN review_states rs ON rs.wordId = w.id " +
            "WHERE rs.wordId IS NULL OR rs.dueDate <= :date " +
            "ORDER BY (rs.dueDate IS NULL) DESC, rs.dueDate ASC"
    )
    suspend fun getDueWordsWithWord(date: String): List<Word>

    /** 待复习数量（含从复习的新词）。 */
    @Query(
        "SELECT COUNT(*) FROM words w LEFT JOIN review_states rs ON rs.wordId = w.id " +
            "WHERE rs.wordId IS NULL OR rs.dueDate <= :date"
    )
    suspend fun countDueWords(date: String): Int
}
