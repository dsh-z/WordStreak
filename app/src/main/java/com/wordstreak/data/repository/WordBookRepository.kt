package com.wordstreak.data.repository

import com.wordstreak.data.local.dao.WordBookDao
import com.wordstreak.data.local.entity.BookWithWordCount
import com.wordstreak.data.local.entity.WordBook
import kotlinx.coroutines.flow.Flow

/** 词库仓库。删除词库会级联删除单词和复习状态；学习记录快照由 SET NULL 保留。 */
class WordBookRepository(private val dao: WordBookDao) {
    fun observeAll(): Flow<List<WordBook>> = dao.getAll()
    fun observeWithWordCount(): Flow<List<BookWithWordCount>> = dao.getAllWithWordCount()
    suspend fun getAll(): List<WordBook> = dao.getAllOnce()
    suspend fun getById(id: Long): WordBook? = dao.getById(id)

    suspend fun create(name: String, description: String = ""): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "词库名不能为空" }
        val now = System.currentTimeMillis()
        return dao.insert(WordBook(name = trimmed, description = description.trim(), createdAt = now, updatedAt = now))
    }

    suspend fun update(book: WordBook) {
        val name = book.name.trim()
        require(name.isNotEmpty()) { "词库名不能为空" }
        dao.update(book.copy(name = name, description = book.description.trim(), updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(book: WordBook) = dao.delete(book)
    suspend fun deleteById(id: Long) { dao.getById(id)?.let { dao.delete(it) } }
    suspend fun count(): Int = dao.count()
}
