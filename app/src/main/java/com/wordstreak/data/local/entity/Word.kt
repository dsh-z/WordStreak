package com.wordstreak.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 单词。
 * - bookId 外键指向词库，词库删除时级联删除单词。
 * - (bookId, spelling) 唯一索引：同一词库内不允许重复单词；跨词库允许同拼写。
 * - spelling 单列索引：加速搜索/排序。
 */
@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = WordBook::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("bookId"),
        Index("spelling"),
        Index(value = ["bookId", "spelling"], unique = true)
    ]
)
data class Word(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val spelling: String,
    val phonetic: String? = null,
    val meaning: String,
    val partOfSpeech: String? = null,
    val example: String? = null,
    val exampleTranslation: String? = null,
    val favorite: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
