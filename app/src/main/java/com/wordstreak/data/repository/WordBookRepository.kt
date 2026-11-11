package com.wordstreak.data.repository

import com.wordstreak.data.local.dao.WordBookDao
import com.wordstreak.data.local.entity.WordBook
import kotlinx.coroutines.flow.Flow

/** 词库仓库。删除词库依赖外键 CASCADE，自动级联清理其单词及关联的学习记录/复习状态。 */
class WordBookRepository(private val dao: WordBookDao) {

    fun observeAll(): Flow<List<WordBook>> = dao.getAll()

    suspend fun getAll(): List<WordBook> = dao.getAllOnce()

    suspend fun getById(id: Long): WordBook? = dao.getById(id)

    suspend fun create(name: String, description: String = ""): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "词库名不能为空" }
        val now = System.currentTimeMillis()
        return dao.insert(
            WordBook(name = trimmed, description = description.trim(), createdAt = now, updatedAt = now)
        )
    }

    suspend fun update(book: WordBook) {
        require(book.name.trim().isNotEmpty()) { "词库名不能为空" }
        dao.update(book.copy(name = book.name.trim(), updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(book: WordBook) = dao.delete(book)

    suspend fun deleteById(id: Long) {
        dao.getById(id)?.let { dao.delete(it) }
    }

    suspend fun count(): Int = dao.count()
}
