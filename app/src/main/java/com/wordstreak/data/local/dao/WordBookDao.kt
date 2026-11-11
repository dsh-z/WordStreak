package com.wordstreak.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wordstreak.data.local.entity.WordBook
import kotlinx.coroutines.flow.Flow

@Dao
interface WordBookDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(book: WordBook): Long

    @Update
    suspend fun update(book: WordBook)

    @Delete
    suspend fun delete(book: WordBook)

    @Query("SELECT * FROM word_books ORDER BY createdAt DESC")
    fun getAll(): Flow<List<WordBook>>

    @Query("SELECT * FROM word_books ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<WordBook>

    @Query("SELECT * FROM word_books WHERE id = :id")
    suspend fun getById(id: Long): WordBook?

    @Query("SELECT COUNT(*) FROM word_books")
    suspend fun count(): Int
}
