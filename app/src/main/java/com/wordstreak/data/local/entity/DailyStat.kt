package com.wordstreak.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 每日学习统计。date 为自然日（LocalDate ISO 字符串），一天一条。 */
@Entity(tableName = "daily_stats")
data class DailyStat(
    @PrimaryKey val date: String,
    val newWords: Int = 0,
    val reviewedWords: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val totalStudySeconds: Long = 0
)
