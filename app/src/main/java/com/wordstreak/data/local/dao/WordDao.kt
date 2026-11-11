package com.wordstreak.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wordstreak.data.local.entity.Word
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(word: Word): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(words: List<Word>): List<Long>

    @Update
    suspend fun update(word: Word)

    @Delete
    suspend fun delete(word: Word)

    @Query("SELECT * FROM words WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getByBookId(bookId: Long): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE bookId = :bookId ORDER BY createdAt DESC")
    suspend fun getByBookIdOnce(bookId: Long): List<Word>

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun getById(id: Long): Word?

    @Query(
        "SELECT * FROM words WHERE bookId = :bookId AND " +
            "(spelling LIKE '%' || :query || '%' OR meaning LIKE '%' || :query || '%') " +
            "ORDER BY spelling ASC"
    )
    fun search(bookId: Long, query: String): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE favorite = 1 ORDER BY spelling ASC")
    fun getFavorites(): Flow<List<Word>>

    @Query("SELECT COUNT(*) FROM words WHERE bookId = :bookId")
    suspend fun getWordCount(bookId: Long): Int

    @Query("SELECT COUNT(*) FROM words WHERE bookId = :bookId AND spelling = :spelling")
    suspend fun countBySpelling(bookId: Long, spelling: String): Int

    /** 同词库内忽略大小写的重复计数（用于业务层大小写重复校验）。 */
    @Query("SELECT COUNT(*) FROM words WHERE bookId = :bookId AND spelling = :spelling COLLATE NOCASE")
    suspend fun countBySpellingIgnoreCase(bookId: Long, spelling: String): Int
}
