package com.wordstreak.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 通用键值设置，用于持久化每日目标、主题等偏好。 */
@Entity(tableName = "user_settings")
data class UserSetting(
    @PrimaryKey val key: String,
    val value: String
)
