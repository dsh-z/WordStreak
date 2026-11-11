package com.wordstreak.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.wordstreak.data.local.dao.WordDao
import com.wordstreak.data.local.entity.Word
import kotlinx.coroutines.flow.Flow

/** 单词仓库：负责空字段校验与同词库重复单词的业务校验。 */
class WordRepository(private val dao: WordDao) {

    fun observeByBook(bookId: Long): Flow<List<Word>> = dao.getByBookId(bookId)

    fun search(bookId: Long, query: String): Flow<List<Word>> = dao.search(bookId, query)

    fun favorites(): Flow<List<Word>> = dao.getFavorites()

    suspend fun getById(id: Long): Word? = dao.getById(id)

    suspend fun getByBook(bookId: Long): List<Word> = dao.getByBookIdOnce(bookId)

    suspend fun wordCount(bookId: Long): Int = dao.getWordCount(bookId)

    suspend fun addWord(word: Word): Long {
        validate(word)
        val now = System.currentTimeMillis()
        val prepared = word.copy(
            spelling = word.spelling.trim(),
            meaning = word.meaning.trim(),
            createdAt = if (word.createdAt == 0L) now else word.createdAt,
            updatedAt = now
        )
        // 大小写不敏感的同词库重复校验（"Apple" 与 "apple" 视为重复）
        if (dao.countBySpellingIgnoreCase(prepared.bookId, prepared.spelling) > 0) {
            throw IllegalStateException("同一词库已存在单词（忽略大小写）：${prepared.spelling}")
        }
        return try {
            dao.insert(prepared)
        } catch (e: SQLiteConstraintException) {
            // (bookId, spelling) 唯一索引冲突 → 同词库重复单词
            throw IllegalStateException("同一词库已存在单词：${prepared.spelling}", e)
        }
    }

    suspend fun updateWord(word: Word) {
        validate(word)
        dao.update(word.copy(spelling = word.spelling.trim(), meaning = word.meaning.trim(), updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteWord(word: Word) = dao.delete(word)

    private fun validate(word: Word) {
        require(word.spelling.trim().isNotEmpty()) { "单词拼写不能为空" }
        require(word.meaning.trim().isNotEmpty()) { "单词释义不能为空" }
    }
}
