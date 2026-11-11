package com.wordstreak

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.room.Room
import androidx.room.withTransaction
import com.wordstreak.data.local.WordStreakDatabase
import com.wordstreak.data.local.entity.DailyStat
import com.wordstreak.data.local.entity.ReviewState
import com.wordstreak.data.local.entity.StudyRecord
import com.wordstreak.data.local.entity.StudySession
import com.wordstreak.data.local.entity.Word
import com.wordstreak.data.repository.CheckInRepository
import com.wordstreak.data.repository.SettingsRepository
import com.wordstreak.data.repository.StatisticsRepository
import com.wordstreak.data.repository.StudyRepository
import com.wordstreak.data.repository.WordBookRepository
import com.wordstreak.data.repository.WordRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Debug-only 数据库自检：在真实设备（真实 Android SQLite）上运行全套 CRUD / 约束 /
 * 历史保护 / 事务回滚 / 统计一致性用例。结果写入 filesDir/db_selftest.txt 并打印 logcat(DB_SELFTEST)。
 * 仅 debug 构建包含；标准/CI 环境改由 instrumented JUnit（WordStreakDatabaseTest）在 connectedAndroidTest 运行。
 */
class DbSelfTestActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        setContentView(tv)
        val report = try {
            runTests()
        } catch (t: Throwable) {
            "HARNESS ERROR: ${t.stackTraceToString()}"
        }
        File(filesDir, "db_selftest.txt").writeText(report)
        tv.text = report
        Log.i("DB_SELFTEST", report)
    }

    private fun runTests(): String {
        val r = StringBuilder()
        var pass = 0
        var fail = 0

        val db = WordStreakDatabase.createInMemory(this)
        val bookRepo = WordBookRepository(db.wordBookDao())
        val wordRepo = WordRepository(db.wordDao())
        val studyRepo = StudyRepository(db.studyDao(), db.reviewDao(), db.wordDao())
        val checkRepo = CheckInRepository(db.checkInDao())
        val statRepo = StatisticsRepository(db.dailyStatDao())
        val setRepo = SettingsRepository(db.settingsDao())

        runBlocking {
            suspend fun check(name: String, block: suspend () -> Unit) {
                try {
                    block(); pass++; r.appendLine("PASS  $name")
                } catch (t: Throwable) {
                    fail++; r.appendLine("FAIL  $name -> ${t.message}")
                }
            }

            check("1_createAndQueryWordBook") {
                val id = bookRepo.create("四级核心", "CET4")
                eq("四级核心", bookRepo.getById(id)!!.name)
            }
            check("2_updateWordBook") {
                val id = bookRepo.create("旧名")
                val b = bookRepo.getById(id)!!
                bookRepo.update(b.copy(name = "新名"))
                eq("新名", bookRepo.getById(id)!!.name)
            }
            check("3_deleteWordBook") {
                val id = bookRepo.create("待删")
                bookRepo.deleteById(id)
                isNull(bookRepo.getById(id))
            }
            check("4_5_6_7_wordCrudAndSearch") {
                val b = bookRepo.create("B1")
                val w1 = wordRepo.addWord(Word(bookId = b, spelling = "apple", meaning = "苹果", createdAt = 1, updatedAt = 1))
                wordRepo.updateWord(wordRepo.getById(w1)!!.copy(meaning = "苹果2"))
                eq("苹果2", wordRepo.getById(w1)!!.meaning)
                wordRepo.addWord(Word(bookId = b, spelling = "application", meaning = "应用", createdAt = 1, updatedAt = 1))
                eq(2, wordRepo.search(b, "app").first().size)
                eq(1, wordRepo.search(b, "应用").first().size)
                eq(2, wordRepo.wordCount(b))
                wordRepo.deleteWord(wordRepo.getById(w1)!!)
                isNull(wordRepo.getById(w1))
            }
            check("S_duplicateWordRejected") {
                val b = bookRepo.create("B2")
                wordRepo.addWord(Word(bookId = b, spelling = "dup", meaning = "x", createdAt = 1, updatedAt = 1))
                try {
                    wordRepo.addWord(Word(bookId = b, spelling = "dup", meaning = "y", createdAt = 1, updatedAt = 1))
                    throw AssertionError("应拒绝同词库重复单词")
                } catch (e: IllegalStateException) { }
            }
            check("S_caseInsensitiveDuplicateRejected") {
                val b = bookRepo.create("B2c")
                wordRepo.addWord(Word(bookId = b, spelling = "Apple", meaning = "苹果", createdAt = 1, updatedAt = 1))
                try {
                    wordRepo.addWord(Word(bookId = b, spelling = "apple", meaning = "苹果", createdAt = 1, updatedAt = 1))
                    throw AssertionError("忽略大小写的重复应被拒绝")
                } catch (e: IllegalStateException) { }
                eq(1, wordRepo.wordCount(b))
            }
            check("S_blankFieldsRejected") {
                val b = bookRepo.create("B3")
                try {
                    wordRepo.addWord(Word(bookId = b, spelling = "  ", meaning = "x", createdAt = 1, updatedAt = 1))
                    throw AssertionError("应拒绝空单词")
                } catch (e: IllegalArgumentException) { }
                try {
                    wordRepo.addWord(Word(bookId = b, spelling = "w", meaning = "   ", createdAt = 1, updatedAt = 1))
                    throw AssertionError("应拒绝空释义")
                } catch (e: IllegalArgumentException) { }
            }
            check("8_studySessionAndRecords") {
                val b = bookRepo.create("B4")
                val w = wordRepo.addWord(Word(bookId = b, spelling = "s", meaning = "m", createdAt = 1, updatedAt = 1))
                val sid = studyRepo.startSession(StudySession(date = "2026-01-01", startedAt = 1))
                studyRepo.addRecord(StudyRecord(sessionId = sid, wordId = w, result = "GOOD", isCorrect = true, createdAt = 2))
                studyRepo.addRecord(StudyRecord(sessionId = sid, wordId = w, result = "AGAIN", isCorrect = false, createdAt = 3))
                eq(2, studyRepo.recordsBySession(sid).size)
                eq(2, studyRepo.dailyRecords("2026-01-01").size)
            }
            check("S_invalidStudyResultRejected") {
                val b = bookRepo.create("B5")
                val w = wordRepo.addWord(Word(bookId = b, spelling = "s", meaning = "m", createdAt = 1, updatedAt = 1))
                val sid = studyRepo.startSession(StudySession(date = "2026-01-01", startedAt = 1))
                try {
                    studyRepo.addRecord(StudyRecord(sessionId = sid, wordId = w, result = "BAD", isCorrect = false, createdAt = 1))
                    throw AssertionError("应拒绝非法 result")
                } catch (e: IllegalArgumentException) { }
            }
            check("11_saveReviewState") {
                val b = bookRepo.create("B6")
                val w = wordRepo.addWord(Word(bookId = b, spelling = "s", meaning = "m", createdAt = 1, updatedAt = 1))
                studyRepo.saveReviewState(ReviewState(wordId = w, dueDate = "2026-01-05", intervalDays = 3, reviewCount = 1))
                eq("2026-01-05", studyRepo.reviewState(w)!!.dueDate)
                eq(1, studyRepo.dueWords("2026-01-10").size)
                studyRepo.saveReviewState(studyRepo.reviewState(w)!!.copy(dueDate = "2026-01-20", reviewCount = 2))
                eq(2, studyRepo.reviewState(w)!!.reviewCount)
            }
            check("12_saveDailyStat") {
                statRepo.upsert(DailyStat(date = "2026-01-01", newWords = 5, reviewedWords = 3, correctCount = 7, incorrectCount = 1, totalStudySeconds = 600))
                eq(5, statRepo.getByDate("2026-01-01")!!.newWords)
                statRepo.upsert(statRepo.getByDate("2026-01-01")!!.copy(newWords = 8))
                eq(8, statRepo.getByDate("2026-01-01")!!.newWords)
            }
            check("13_duplicateCheckInSameDay") {
                isTrue(checkRepo.checkIn("2026-01-01", goalSnapshot = 20, completedCount = 20))
                isTrue(!checkRepo.checkIn("2026-01-01", goalSnapshot = 20, completedCount = 25))
                eq(1, checkRepo.count())
                eq(20, checkRepo.getByDate("2026-01-01")!!.goalSnapshot)
            }
            check("14_saveSettings") {
                setRepo.set(SettingsRepository.Keys.DAILY_GOAL, "30")
                eq("30", setRepo.get(SettingsRepository.Keys.DAILY_GOAL))
                setRepo.set(SettingsRepository.Keys.DAILY_GOAL, "50")
                eq("50", setRepo.get(SettingsRepository.Keys.DAILY_GOAL))
            }
            check("S_invalidDateRejected") {
                try {
                    checkRepo.checkIn("2026-13-40", goalSnapshot = 10, completedCount = 10)
                    throw AssertionError("应拒绝非法日期")
                } catch (e: IllegalArgumentException) { }
                try {
                    statRepo.upsert(DailyStat(date = "not-a-date"))
                    throw AssertionError("应拒绝非法日期")
                } catch (e: IllegalArgumentException) { }
            }
            check("S_goalSnapshotIndependentOfSettings") {
                setRepo.set(SettingsRepository.Keys.DAILY_GOAL, "50")
                checkRepo.checkIn("2026-04-03", goalSnapshot = 20, completedCount = 20)
                setRepo.set(SettingsRepository.Keys.DAILY_GOAL, "99")
                eq(20, checkRepo.getByDate("2026-04-03")!!.goalSnapshot)
            }
            check("S_dailyStatNoDoubleCount") {
                statRepo.upsert(DailyStat(date = "2026-04-02", newWords = 5))
                statRepo.upsert(DailyStat(date = "2026-04-02", newWords = 5))
                eq(5, statRepo.getByDate("2026-04-02")!!.newWords)
            }
            check("S_statisticsConsistencyOnBookDelete") {
                statRepo.upsert(DailyStat(date = "2026-04-01", newWords = 3, reviewedWords = 2, correctCount = 4, incorrectCount = 1, totalStudySeconds = 300))
                checkRepo.checkIn("2026-04-01", goalSnapshot = 10, completedCount = 10)
                val b = bookRepo.create("BS")
                wordRepo.addWord(Word(bookId = b, spelling = "x", meaning = "y", createdAt = 1, updatedAt = 1))
                bookRepo.deleteById(b)
                eq(3, statRepo.getByDate("2026-04-01")!!.newWords)
                eq(10, checkRepo.getByDate("2026-04-01")!!.goalSnapshot)
            }
            check("S_transactionRollback") {
                val before = bookRepo.count()
                try {
                    db.withTransaction {
                        bookRepo.create("TX")
                        throw RuntimeException("boom")
                    }
                    throw AssertionError("应抛异常")
                } catch (e: RuntimeException) { }
                eq(before, bookRepo.count())
            }
            check("16_historyPreservedOnBookDelete_noOrphans") {
                val b = bookRepo.create("B7")
                val w = wordRepo.addWord(Word(bookId = b, spelling = "s", meaning = "m", createdAt = 1, updatedAt = 1))
                val sid = studyRepo.startSession(StudySession(date = "2026-02-02", startedAt = 1))
                studyRepo.addRecord(StudyRecord(sessionId = sid, wordId = w, result = "EASY", isCorrect = true, createdAt = 1))
                studyRepo.saveReviewState(ReviewState(wordId = w, dueDate = "2026-02-03"))
                bookRepo.deleteById(b)
                isNull(wordRepo.getById(w))
                val recs = studyRepo.recordsBySession(sid)
                eq(1, recs.size)                 // 历史学习记录保留
                isNull(recs[0].wordId)           // 外键 SET NULL，无孤立
                eq("s", recs[0].wordSpelling)    // 快照可追溯
                eq("m", recs[0].wordMeaning)
                isNull(studyRepo.reviewState(w)) // 复习调度随单词删除
            }
            check("17_historyPreservedOnWordDelete") {
                val b = bookRepo.create("B8")
                val w = wordRepo.addWord(Word(bookId = b, spelling = "hist", meaning = "历史", createdAt = 1, updatedAt = 1))
                val sid = studyRepo.startSession(StudySession(date = "2026-02-03", startedAt = 1))
                studyRepo.addRecord(StudyRecord(sessionId = sid, wordId = w, result = "GOOD", isCorrect = true, createdAt = 1))
                wordRepo.deleteWord(wordRepo.getById(w)!!)
                isNull(wordRepo.getById(w))
                val recs = studyRepo.recordsBySession(sid)
                eq(1, recs.size)
                isNull(recs[0].wordId)
                eq("hist", recs[0].wordSpelling)
                eq("历史", recs[0].wordMeaning)
            }
            check("15_dataPersistsAcrossRestart") {
                val name = "selftest_persist.db"
                deleteDatabase(name)
                var p = Room.databaseBuilder(this@DbSelfTestActivity, WordStreakDatabase::class.java, name).build()
                val id = WordBookRepository(p.wordBookDao()).create("持久化词库")
                val wid = WordRepository(p.wordDao())
                    .addWord(Word(bookId = id, spelling = "persist", meaning = "持久", createdAt = 1, updatedAt = 1))
                p.close()
                p = Room.databaseBuilder(this@DbSelfTestActivity, WordStreakDatabase::class.java, name).build()
                eq("持久化词库", WordBookRepository(p.wordBookDao()).getById(id)!!.name)
                eq("persist", WordRepository(p.wordDao()).getById(wid)!!.spelling)
                p.close()
                deleteDatabase(name)
            }
            check("M_migrationV1ToV2_preservesDataAndSchema") {
                migrateV1ToV2AndVerify()
            }
            check("S_timezoneCalendarDayStoredAsGiven") {
                // 规则：日期以写入时的设备本地日历日(yyyy-MM-dd)固化存储，读取不随时区漂移
                val d = com.wordstreak.data.local.converter.DateValidator.today()
                com.wordstreak.data.local.converter.DateValidator.requireValid(d)
                checkRepo.checkIn(d, goalSnapshot = 1, completedCount = 1)
                eq(d, checkRepo.getByDate(d)!!.date)
            }
        }

        db.close()
        r.appendLine()
        r.appendLine("SUMMARY: PASS=$pass FAIL=$fail TOTAL=${pass + fail}")
        return r.toString()
    }

    /** 真实 v1→v2 迁移测试：手工构造 v1 结构+数据，用 Room+MIGRATION_1_2 打开并校验保留/结构/外键。 */
    private fun migrateV1ToV2AndVerify() {
        val dbName = "selftest_migration.db"
        deleteDatabase(dbName)
        // 1) 构造与 schemas/1.json 一致的 v1 结构 + 历史数据
        val v1 = openOrCreateDatabase(dbName, MODE_PRIVATE, null)
        v1.execSQL("CREATE TABLE IF NOT EXISTS `word_books` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL);")
        v1.execSQL("CREATE TABLE IF NOT EXISTS `words` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `bookId` INTEGER NOT NULL, `spelling` TEXT NOT NULL, `phonetic` TEXT, `meaning` TEXT NOT NULL, `partOfSpeech` TEXT, `example` TEXT, `exampleTranslation` TEXT, `favorite` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`bookId`) REFERENCES `word_books`(`id`) ON UPDATE CASCADE ON DELETE CASCADE );")
        v1.execSQL("CREATE TABLE IF NOT EXISTS `study_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `startedAt` INTEGER NOT NULL, `endedAt` INTEGER, `newCount` INTEGER NOT NULL, `reviewCount` INTEGER NOT NULL, `completed` INTEGER NOT NULL);")
        v1.execSQL("CREATE TABLE IF NOT EXISTS `study_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `wordId` INTEGER NOT NULL, `result` TEXT NOT NULL, `isCorrect` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`sessionId`) REFERENCES `study_sessions`(`id`) ON UPDATE CASCADE ON DELETE CASCADE , FOREIGN KEY(`wordId`) REFERENCES `words`(`id`) ON UPDATE CASCADE ON DELETE CASCADE );")
        v1.execSQL("CREATE TABLE IF NOT EXISTS `review_states` (`wordId` INTEGER NOT NULL, `dueDate` TEXT NOT NULL, `difficulty` REAL, `stability` REAL, `intervalDays` INTEGER NOT NULL, `lastReviewedAt` INTEGER, `reviewCount` INTEGER NOT NULL, PRIMARY KEY(`wordId`), FOREIGN KEY(`wordId`) REFERENCES `words`(`id`) ON UPDATE CASCADE ON DELETE CASCADE );")
        v1.execSQL("CREATE TABLE IF NOT EXISTS `daily_stats` (`date` TEXT NOT NULL, `newWords` INTEGER NOT NULL, `reviewedWords` INTEGER NOT NULL, `correctCount` INTEGER NOT NULL, `incorrectCount` INTEGER NOT NULL, `totalStudySeconds` INTEGER NOT NULL, PRIMARY KEY(`date`));")
        v1.execSQL("CREATE TABLE IF NOT EXISTS `check_ins` (`date` TEXT NOT NULL, `completed` INTEGER NOT NULL, `goalSnapshot` INTEGER NOT NULL, `completedCount` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`date`));")
        v1.execSQL("CREATE TABLE IF NOT EXISTS `user_settings` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`));")
        // v1 索引（真实 v1 库由 Room 建立，必须一致，否则迁移后全库结构校验会失败）
        v1.execSQL("CREATE INDEX IF NOT EXISTS `index_words_bookId` ON `words` (`bookId`);")
        v1.execSQL("CREATE INDEX IF NOT EXISTS `index_words_spelling` ON `words` (`spelling`);")
        v1.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_words_bookId_spelling` ON `words` (`bookId`, `spelling`);")
        v1.execSQL("CREATE INDEX IF NOT EXISTS `index_study_sessions_date` ON `study_sessions` (`date`);")
        v1.execSQL("CREATE INDEX IF NOT EXISTS `index_study_records_sessionId` ON `study_records` (`sessionId`);")
        v1.execSQL("CREATE INDEX IF NOT EXISTS `index_study_records_wordId` ON `study_records` (`wordId`);")
        v1.execSQL("CREATE INDEX IF NOT EXISTS `index_study_records_createdAt` ON `study_records` (`createdAt`);")
        v1.execSQL("CREATE INDEX IF NOT EXISTS `index_review_states_dueDate` ON `review_states` (`dueDate`);")
        v1.execSQL("INSERT INTO word_books VALUES(1,'旧词库','desc',100,100)")
        v1.execSQL("INSERT INTO words VALUES(1,1,'apple',NULL,'苹果',NULL,NULL,NULL,0,100,100)")
        v1.execSQL("INSERT INTO words VALUES(2,1,'banana',NULL,'香蕉',NULL,NULL,NULL,0,100,100)")
        v1.execSQL("INSERT INTO study_sessions VALUES(1,'2026-05-01',1000,2000,2,0,1)")
        v1.execSQL("INSERT INTO study_records VALUES(1,1,1,'GOOD',1,1100)")
        v1.execSQL("INSERT INTO study_records VALUES(2,1,2,'AGAIN',0,1200)")
        v1.execSQL("INSERT INTO review_states VALUES(1,'2026-05-02',NULL,NULL,3,0,1)")
        v1.execSQL("INSERT INTO daily_stats VALUES('2026-05-01',2,0,1,1,120)")
        v1.execSQL("INSERT INTO check_ins VALUES('2026-05-01',1,10,10,123)")
        v1.execSQL("INSERT INTO user_settings VALUES('daily_goal','10')")
        v1.execSQL("PRAGMA user_version = 1")
        v1.close()

        // 2) Room v2 + MIGRATION_1_2 打开，触发真实迁移
        val migrated = Room.databaseBuilder(this, WordStreakDatabase::class.java, dbName)
            .addMigrations(WordStreakDatabase.MIGRATION_1_2).build()
        val wbCount = runBlocking { WordBookRepository(migrated.wordBookDao()).getAll().size }
        eq(1, wbCount)

        // 3) 数据保留校验
        runBlocking {
            eq("旧词库", WordBookRepository(migrated.wordBookDao()).getById(1)!!.name)
            eq(2, WordRepository(migrated.wordDao()).wordCount(1))
            val sr = StudyRepository(migrated.studyDao(), migrated.reviewDao(), migrated.wordDao())
            val recs = sr.recordsBySession(1)
            eq(2, recs.size)
            eq("apple", recs.first { it.id == 1L }.wordSpelling)   // 快照回填
            eq("香蕉", recs.first { it.id == 2L }.wordMeaning)
            eq(2, StatisticsRepository(migrated.dailyStatDao()).getByDate("2026-05-01")!!.newWords)
            eq(10, CheckInRepository(migrated.checkInDao()).getByDate("2026-05-01")!!.goalSnapshot)
            eq("10", SettingsRepository(migrated.settingsDao()).get("daily_goal"))
            eq(1, sr.reviewState(1)!!.reviewCount)
        }

        // 4) 表结构 / 外键 校验
        val support = migrated.openHelper.readableDatabase
        var hasSpell = false; var hasMeaning = false; var wordIdNullable = false
        support.query("PRAGMA table_info(`study_records`)").use { c ->
            while (c.moveToNext()) {
                val name = c.getString(c.getColumnIndex("name"))
                val notnull = c.getInt(c.getColumnIndex("notnull"))
                if (name == "wordSpelling") hasSpell = true
                if (name == "wordMeaning") hasMeaning = true
                if (name == "wordId" && notnull == 0) wordIdNullable = true
            }
        }
        isTrue(hasSpell); isTrue(hasMeaning); isTrue(wordIdNullable)
        var fkSetNull = false
        support.query("PRAGMA foreign_key_list(`study_records`)").use { c ->
            while (c.moveToNext()) {
                val from = c.getString(c.getColumnIndex("from"))
                val onDel = c.getString(c.getColumnIndex("on_delete"))
                if (from == "wordId" && onDel.equals("SET NULL", ignoreCase = true)) fkSetNull = true
            }
        }
        isTrue(fkSetNull)

        // 5) 删除单词后历史记录保留 + 外键置空 + 快照可追溯
        runBlocking {
            val wordRepo = WordRepository(migrated.wordDao())
            wordRepo.deleteWord(wordRepo.getById(2)!!)
            isNull(wordRepo.getById(2))
            val sr = StudyRepository(migrated.studyDao(), migrated.reviewDao(), migrated.wordDao())
            val recs = sr.recordsBySession(1)
            eq(2, recs.size)
            isNull(recs.first { it.id == 2L }.wordId)
            eq("banana", recs.first { it.id == 2L }.wordSpelling)
        }
        migrated.close()
        deleteDatabase(dbName)
    }

    private fun eq(expected: Any?, actual: Any?) {
        if (expected != actual) throw AssertionError("expected=$expected actual=$actual")
    }

    private fun isNull(actual: Any?) {
        if (actual != null) throw AssertionError("expected null but was $actual")
    }

    private fun isTrue(v: Boolean) {
        if (!v) throw AssertionError("expected true")
    }
}
