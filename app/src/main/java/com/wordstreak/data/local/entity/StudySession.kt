package com.wordstreak.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 一次学习会话。date 为 LocalDate ISO 字符串（yyyy-MM-dd）。 */
@Entity(
    tableName = "study_sessions",
    indices = [Index("date")]
)
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val newCount: Int = 0,
    val reviewCount: Int = 0,
    val completed: Boolean = false
)
