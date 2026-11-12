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

    /** 读取每日目标，默认 20，并夹取到 1–999。 */
    suspend fun getDailyGoal(default: Int = DEFAULT_DAILY_GOAL): Int =
        get(Keys.DAILY_GOAL)?.toIntOrNull()?.coerceIn(1, 999) ?: default

    /** 保存每日目标（1–999）。 */
    suspend fun setDailyGoal(value: Int) {
        require(value in 1..999) { "每日目标需在 1–999 之间" }
        set(Keys.DAILY_GOAL, value.toString())
    }

    fun observeAll(): Flow<List<UserSetting>> = dao.getAll()

    companion object {
        const val DEFAULT_DAILY_GOAL = 20
    }

    object Keys {
        const val DAILY_GOAL = "daily_goal"
        const val THEME = "theme"
    }
}
