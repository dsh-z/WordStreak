package com.wordstreak.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 复习状态（每个单词一条）。当前仅保存基础结构，FSRS 算法后续实现。
 * dueDate 为 LocalDate ISO 字符串，便于按到期日检索。
 */
@Entity(
    tableName = "review_states",
    foreignKeys = [
        ForeignKey(
            entity = Word::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("dueDate")]
)
data class ReviewState(
    @PrimaryKey val wordId: Long,
    val dueDate: String,
    val difficulty: Float? = null,
    val stability: Float? = null,
    val intervalDays: Int = 0,
    val lastReviewedAt: Long? = null,
    val reviewCount: Int = 0
)
