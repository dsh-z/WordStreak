package com.wordstreak.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.wordstreak.data.local.dao.WordDao
import com.wordstreak.data.local.entity.Word
import kotlinx.coroutines.flow.Flow

/** 单词仓库：负责字段校验与同词库大小写不敏感的重复校验。 */
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
        val prepared = word.copy(spelling = word.spelling.trim(), meaning = word.meaning.trim(),
            phonetic = word.phonetic?.trim()?.ifEmpty { null },
            partOfSpeech = word.partOfSpeech?.trim()?.ifEmpty { null },
            example = word.example?.trim()?.ifEmpty { null },
            exampleTranslation = word.exampleTranslation?.trim()?.ifEmpty { null },
            createdAt = if (word.createdAt == 0L) now else word.createdAt, updatedAt = now)
        if (dao.countBySpellingIgnoreCase(prepared.bookId, prepared.spelling) > 0) {
            throw IllegalStateException("同一词库已存在单词（忽略大小写）：${prepared.spelling}")
        }
        return try { dao.insert(prepared) }
        catch (e: SQLiteConstraintException) { throw IllegalStateException("同一词库已存在单词：${prepared.spelling}", e) }
    }

    suspend fun updateWord(word: Word) {
        validate(word)
        val prepared = word.copy(spelling = word.spelling.trim(), meaning = word.meaning.trim(),
            phonetic = word.phonetic?.trim()?.ifEmpty { null },
            partOfSpeech = word.partOfSpeech?.trim()?.ifEmpty { null },
            example = word.example?.trim()?.ifEmpty { null },
            exampleTranslation = word.exampleTranslation?.trim()?.ifEmpty { null },
            updatedAt = System.currentTimeMillis())
        if (dao.countBySpellingIgnoreCaseExceptId(prepared.bookId, prepared.spelling, prepared.id) > 0) {
            throw IllegalStateException("同一词库已存在单词（忽略大小写）：${prepared.spelling}")
        }
        try { dao.update(prepared) }
        catch (e: SQLiteConstraintException) { throw IllegalStateException("同一词库已存在单词：${prepared.spelling}", e) }
    }

    suspend fun deleteWord(word: Word) = dao.delete(word)

    private fun validate(word: Word) {
        require(word.spelling.trim().isNotEmpty()) { "单词拼写不能为空" }
        require(word.meaning.trim().isNotEmpty()) { "单词释义不能为空" }
    }
}
