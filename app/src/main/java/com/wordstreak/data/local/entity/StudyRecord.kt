package com.wordstreak.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 单条学习记录。result ∈ {AGAIN, HARD, GOOD, EASY}（业务层校验）。
 *
 * 历史保护（Stage 2.1）：
 * - wordId 可空，外键 ON DELETE SET NULL：删除单词后本记录保留、外键置空，不产生孤立数据。
 * - wordSpelling / wordMeaning 为记录时的单词快照：单词（或词库）删除后，历史学习记录仍可追溯。
 * - sessionId 外键 ON DELETE CASCADE（会话为历史记录的一部分，正常不删除会话）。
 */
@Entity(
    tableName = "study_records",
    foreignKeys = [
        ForeignKey(
            entity = StudySession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Word::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("wordId"), Index("createdAt")]
)
data class StudyRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val wordId: Long?,
    val result: String,
    val isCorrect: Boolean,
    val createdAt: Long,
    val wordSpelling: String? = null,
    val wordMeaning: String? = null
)
