package com.wordstreak.data.local.entity

import androidx.room.Embedded

/** 词库列表投影：词库字段与实时单词数量。 */
data class BookWithWordCount(
    @Embedded val book: WordBook,
    val wordCount: Int
)
