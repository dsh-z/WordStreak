package com.wordstreak.di

import android.content.Context
import com.wordstreak.data.local.WordStreakDatabase
import com.wordstreak.data.repository.CheckInRepository
import com.wordstreak.data.repository.SettingsRepository
import com.wordstreak.data.repository.StatisticsRepository
import com.wordstreak.data.repository.StudyRepository
import com.wordstreak.data.repository.WordBookRepository
import com.wordstreak.data.repository.WordRepository

/**
 * 轻量服务定位器（替代 DI 框架）：集中构建数据库单例与各 Repository。
 * UI / ViewModel 只能通过这里拿 Repository，绝不直接触碰 DAO。
 */
object AppContainer {

    @Volatile
    private var databaseRef: WordStreakDatabase? = null

    private val lock = Any()

    fun init(context: Context) {
        if (databaseRef == null) {
            synchronized(lock) {
                if (databaseRef == null) {
                    databaseRef = WordStreakDatabase.getInstance(context.applicationContext)
                }
            }
        }
    }

    private fun db(): WordStreakDatabase =
        databaseRef ?: error("AppContainer 未初始化，请先在 Application/Activity 中调用 init(context)")

    val wordBookRepository: WordBookRepository by lazy { WordBookRepository(db().wordBookDao()) }
    val wordRepository: WordRepository by lazy { WordRepository(db().wordDao()) }
    val studyRepository: StudyRepository by lazy { StudyRepository(db().studyDao(), db().reviewDao(), db().wordDao()) }
    val checkInRepository: CheckInRepository by lazy { CheckInRepository(db().checkInDao()) }
    val statisticsRepository: StatisticsRepository by lazy { StatisticsRepository(db().dailyStatDao()) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(db().settingsDao()) }
}
