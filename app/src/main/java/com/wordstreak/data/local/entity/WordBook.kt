package com.wordstreak.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 词库 */
@Entity(tableName = "word_books")
data class WordBook(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long,
    val updatedAt: Long
)
