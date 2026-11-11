package com.wordstreak.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wordstreak.data.local.entity.UserSetting
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM user_settings WHERE `key` = :key")
    suspend fun getByKey(key: String): UserSetting?

    @Upsert
    suspend fun upsert(setting: UserSetting)

    @Query("SELECT * FROM user_settings ORDER BY `key` ASC")
    fun getAll(): Flow<List<UserSetting>>
}
