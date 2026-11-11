package com.wordstreak.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wordstreak.data.local.dao.CheckInDao
import com.wordstreak.data.local.dao.DailyStatDao
import com.wordstreak.data.local.dao.ReviewDao
import com.wordstreak.data.local.dao.SettingsDao
import com.wordstreak.data.local.dao.StudyDao
import com.wordstreak.data.local.dao.WordBookDao
import com.wordstreak.data.local.dao.WordDao
import com.wordstreak.data.local.entity.CheckIn
import com.wordstreak.data.local.entity.DailyStat
import com.wordstreak.data.local.entity.ReviewState
import com.wordstreak.data.local.entity.StudyRecord
import com.wordstreak.data.local.entity.StudySession
import com.wordstreak.data.local.entity.UserSetting
import com.wordstreak.data.local.entity.Word
import com.wordstreak.data.local.entity.WordBook

/**
 * WordStreak 本地数据库。
 *
 * 迁移策略：
 * - 固定显式版本号 VERSION，禁用破坏性迁移（未调用 fallbackToDestructiveMigration）。
 * - 版本升级必须在 ALL_MIGRATIONS 中登记 Migration，禁止无条件删库。
 * - exportSchema = true，schema 输出到 app/schemas，作为迁移测试（MigrationTestHelper）的依据。
 *
 * 注意：SQL 编译期查询验证默认启用（标准/CI 构建）。仅本机 bionic 宿主用
 * -Pwordstreak.localBionic=true 走打桩编译器兜底，不在源码层关闭验证。
 */
@Database(
    entities = [
        WordBook::class,
        Word::class,
        StudySession::class,
        StudyRecord::class,
        ReviewState::class,
        DailyStat::class,
        CheckIn::class,
        UserSetting::class
    ],
    version = WordStreakDatabase.VERSION,
    exportSchema = true
)
abstract class WordStreakDatabase : RoomDatabase() {

    abstract fun wordBookDao(): WordBookDao
    abstract fun wordDao(): WordDao
    abstract fun studyDao(): StudyDao
    abstract fun reviewDao(): ReviewDao
    abstract fun dailyStatDao(): DailyStatDao
    abstract fun checkInDao(): CheckInDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        /** v2：study_records 支持历史保护（wordId 可空 + SET NULL + 快照字段）。 */
        const val VERSION = 2
        private const val DB_NAME = "wordstreak.db"

        @Volatile
        private var INSTANCE: WordStreakDatabase? = null

        private val lock = Any()

        /**
         * v1 → v2：重建 study_records，wordId 改为可空且 ON DELETE SET NULL，
         * 新增 wordSpelling / wordMeaning 快照列，并从现有 words 回填快照。
         * 现有数据全部保留（不删除历史学习记录）。
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `study_records_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionId` INTEGER NOT NULL,
                        `wordId` INTEGER,
                        `result` TEXT NOT NULL,
                        `isCorrect` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `wordSpelling` TEXT,
                        `wordMeaning` TEXT,
                        FOREIGN KEY(`sessionId`) REFERENCES `study_sessions`(`id`) ON UPDATE CASCADE ON DELETE CASCADE,
                        FOREIGN KEY(`wordId`) REFERENCES `words`(`id`) ON UPDATE CASCADE ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `study_records_new`
                        (`id`,`sessionId`,`wordId`,`result`,`isCorrect`,`createdAt`,`wordSpelling`,`wordMeaning`)
                    SELECT r.`id`, r.`sessionId`, r.`wordId`, r.`result`, r.`isCorrect`, r.`createdAt`,
                           w.`spelling`, w.`meaning`
                    FROM `study_records` r
                    LEFT JOIN `words` w ON w.`id` = r.`wordId`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `study_records`")
                db.execSQL("ALTER TABLE `study_records_new` RENAME TO `study_records`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_records_sessionId` ON `study_records` (`sessionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_records_wordId` ON `study_records` (`wordId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_records_createdAt` ON `study_records` (`createdAt`)")
            }
        }

        /** 所有迁移在此登记。禁止 fallbackToDestructiveMigration。 */
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)

        fun getInstance(context: Context): WordStreakDatabase =
            INSTANCE ?: synchronized(lock) {
                INSTANCE ?: buildPersistent(context.applicationContext).also { INSTANCE = it }
            }

        private fun buildPersistent(context: Context): WordStreakDatabase =
            Room.databaseBuilder(context, WordStreakDatabase::class.java, DB_NAME)
                .addMigrations(*ALL_MIGRATIONS)
                // 明确不启用 fallbackToDestructiveMigration：结构变化必须走 Migration
                .build()

        /** 仅供测试：内存数据库。 */
        fun createInMemory(context: Context): WordStreakDatabase =
            Room.inMemoryDatabaseBuilder(context, WordStreakDatabase::class.java)
                .addMigrations(*ALL_MIGRATIONS)
                .build()
    }
}
