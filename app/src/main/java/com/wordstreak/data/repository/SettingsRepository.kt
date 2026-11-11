package com.wordstreak.data.repository

import com.wordstreak.data.local.dao.SettingsDao
import com.wordstreak.data.local.entity.UserSetting
import kotlinx.coroutines.flow.Flow

/** 设置仓库：键值偏好持久化。 */
class SettingsRepository(private val dao: SettingsDao) {

    suspend fun get(key: String): String? = dao.getByKey(key)?.value

    suspend fun set(key: String, value: String) {
        require(key.isNotBlank()) { "设置键不能为空" }
        dao.upsert(UserSetting(key = key, value = value))
    }

    fun observeAll(): Flow<List<UserSetting>> = dao.getAll()

    object Keys {
        const val DAILY_GOAL = "daily_goal"
        const val THEME = "theme"
    }
}
