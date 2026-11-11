package com.wordstreak.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 每日打卡记录。date 作为主键 → 同一自然日最多一条，杜绝重复打卡。
 * goalSnapshot 保存打卡时的每日目标快照，之后修改目标不影响历史记录。
 */
@Entity(tableName = "check_ins")
data class CheckIn(
    @PrimaryKey val date: String,
    val completed: Boolean,
    val goalSnapshot: Int,
    val completedCount: Int,
    val createdAt: Long
)
