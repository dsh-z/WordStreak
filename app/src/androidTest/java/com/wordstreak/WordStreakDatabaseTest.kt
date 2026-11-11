package com.wordstreak

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 数据层持久化测试（Android Instrumented Test，真实设备 SQLite）。
 * 标准/CI 环境通过 `connectedAndroidTest` / `am instrument` 运行；覆盖 CRUD、搜索、约束、
 * 历史数据保护、大小写重复、事务回滚、统计一致性、持久化。
 */
@RunWith(AndroidJUnit4::class)
class WordStreakDatabaseTest {

    private lateinit var context: Context
    private lateinit var db: WordStreakDatabase

    private lateinit var wordBookRepo: WordBookRepository
    private lateinit var wordRepo: WordRepository
    private lateinit var studyRepo: StudyRepository
    private lateinit var checkInRepo: CheckInRepository
    private lateinit var statisticsRepo: StatisticsRepository
    private lateinit var settingsRepo: SettingsRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = WordStreakDatabase.createInMemory(context)
        wordBookRepo = WordBookRepository(db.wordBookDao())
        wordRepo = WordRepository(db.wordDao())
        studyRepo = StudyRepository(db.studyDao(), db.reviewDao(), db.wordDao())
        checkInRepo = CheckInRepository(db.checkInDao())
        statisticsRepo = StatisticsRepository(db.dailyStatDao())
        settingsRepo = SettingsRepository(db.settingsDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun createAndQueryWordBook() = runBlocking<Unit> {
        val id = wordBookRepo.create("四级核心", "CET4")
        val loaded = wordBookRepo.getById(id)
        assertNotNull(loaded)
        assertEquals("四级核心", loaded!!.name)
        assertEquals(1, wordBookRepo.getAll().size)
    }

    @Test
    fun updateWordBook() = runBlocking<Unit> {
        val id = wordBookRepo.create("旧名")
        val book = wordBookRepo.getById(id)!!
        wordBookRepo.update(book.copy(name = "新名"))
        assertEquals("新名", wordBookRepo.getById(id)!!.name)
    }

    @Test
    fun deleteWordBook() = runBlocking<Unit> {
        val id = wordBookRepo.create("待删")
        wordBookRepo.deleteById(id)
        assertNull(wordBookRepo.getById(id))
        assertEquals(0, wordBookRepo.count())
    }

    @Test
    fun wordCrudAndSearch() = runBlocking<Unit> {
        val bookId = wordBookRepo.create("Book")
        val wid = wordRepo.addWord(newWord(bookId, "apple", "苹果"))
        assertEquals("apple", wordRepo.getById(wid)!!.spelling)
        wordRepo.updateWord(wordRepo.getById(wid)!!.copy(meaning = "苹果(水果)"))
        assertEquals("苹果(水果)", wordRepo.getById(wid)!!.meaning)
        wordRepo.addWord(newWord(bookId, "banana", "香蕉"))
        wordRepo.addWord(newWord(bookId, "application", "应用"))
        assertEquals(2, wordRepo.search(bookId, "app").first().size)
        assertEquals(1, wordRepo.search(bookId, "香蕉").first().size)
        assertEquals(3, wordRepo.wordCount(bookId))
        wordRepo.deleteWord(wordRepo.getById(wid)!!)
        assertNull(wordRepo.getById(wid))
        assertEquals(2, wordRepo.wordCount(bookId))
    }

    @Test
    fun duplicateWordRejected() = runBlocking<Unit> {
        val bookId = wordBookRepo.create("Book")
        wordRepo.addWord(newWord(bookId, "apple", "苹果"))
        try {
            wordRepo.addWord(newWord(bookId, "apple", "另一个苹果"))
            fail("应拒绝同词库重复单词")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("已存在"))
        }
        assertEquals(1, wordRepo.wordCount(bookId))
    }

    /** 新增：大小写不敏感重复。 */
    @Test
    fun caseInsensitiveDuplicateRejected() = runBlocking<Unit> {
        val bookId = wordBookRepo.create("Book")
        wordRepo.addWord(newWord(bookId, "Apple", "苹果"))
        try {
            wordRepo.addWord(newWord(bookId, "apple", "苹果"))
            fail("忽略大小写的重复应被拒绝")
        } catch (e: IllegalStateException) { /* ok */ }
        assertEquals(1, wordRepo.wordCount(bookId))
    }

    @Test
    fun blankFieldsRejected() = runBlocking<Unit> {
        val bookId = wordBookRepo.create("Book")
        try {
            wordRepo.addWord(newWord(bookId, "  ", "释义"))
            fail("空单词应被拒绝")
        } catch (e: IllegalArgumentException) { /* ok */ }
        try {
            wordRepo.addWord(newWord(bookId, "word", "   "))
            fail("空释义应被拒绝")
        } catch (e: IllegalArgumentException) { /* ok */ }
        assertEquals(0, wordRepo.wordCount(bookId))
    }

    @Test
    fun studySessionAndRecords() = runBlocking<Unit> {
        val bookId = wordBookRepo.create("Book")
        val wid = wordRepo.addWord(newWord(bookId, "apple", "苹果"))
        val sid = studyRepo.startSession(StudySession(date = "2026-01-01", startedAt = 1L, newCount = 1, reviewCount = 0))
        studyRepo.addRecord(StudyRecord(sessionId = sid, wordId = wid, result = "GOOD", isCorrect = true, createdAt = 2L))
        studyRepo.addRecord(StudyRecord(sessionId = sid, wordId = wid, result = "AGAIN", isCorrect = false, createdAt = 3L))
        assertEquals(2, studyRepo.recordsBySession(sid).size)
        assertEquals(2, studyRepo.dailyRecords("2026-01-01").size)
    }

    @Test
    fun invalidStudyResultRejected() = runBlocking<Unit> {
        val bookId = wordBookRepo.create("Book")
        val wid = wordRepo.addWord(newWord(bookId, "apple", "苹果"))
        val sid = studyRepo.startSession(StudySession(date = "2026-01-01", startedAt = 1L))
        try {
            studyRepo.addRecord(StudyRecord(sessionId = sid, wordId = wid, result = "BAD", isCorrect = false, createdAt = 1L))
            fail("非法 result 应被拒绝")
        } catch (e: IllegalArgumentException) { /* ok */ }
        assertEquals(0, studyRepo.recordsBySession(sid).size)
    }

    @Test
    fun saveReviewState() = runBlocking<Unit> {
        val bookId = wordBookRepo.create("Book")
        val wid = wordRepo.addWord(newWord(bookId, "apple", "苹果"))
        studyRepo.saveReviewState(ReviewState(wordId = wid, dueDate = "2026-01-05", intervalDays = 3, reviewCount = 1))
        assertEquals("2026-01-05", studyRepo.reviewState(wid)!!.dueDate)
        assertEquals(1, studyRepo.dueWords("2026-01-10").size)
        studyRepo.saveReviewState(studyRepo.reviewState(wid)!!.copy(dueDate = "2026-01-20", reviewCount = 2))
        assertEquals(2, studyRepo.reviewState(wid)!!.reviewCount)
    }

    @Test
    fun saveDailyStat() = runBlocking<Unit> {
        statisticsRepo.upsert(DailyStat(date = "2026-01-01", newWords = 5, reviewedWords = 3, correctCount = 7, incorrectCount = 1, totalStudySeconds = 600))
        assertEquals(5, statisticsRepo.getByDate("2026-01-01")!!.newWords)
        statisticsRepo.upsert(statisticsRepo.getByDate("2026-01-01")!!.copy(newWords = 8))
        assertEquals(8, statisticsRepo.getByDate("2026-01-01")!!.newWords)
        assertEquals(1, statisticsRepo.observeAll().first().size)
    }

    @Test
    fun duplicateCheckInSameDay() = runBlocking<Unit> {
        assertTrue(checkInRepo.checkIn("2026-01-01", goalSnapshot = 20, completedCount = 20))
        assertTrue(!checkInRepo.checkIn("2026-01-01", goalSnapshot = 20, completedCount = 25))
        assertEquals(1, checkInRepo.count())
        assertEquals(20, checkInRepo.getByDate("2026-01-01")!!.goalSnapshot)
    }

    @Test
    fun saveSettings() = runBlocking<Unit> {
        settingsRepo.set(SettingsRepository.Keys.DAILY_GOAL, "30")
        assertEquals("30", settingsRepo.get(SettingsRepository.Keys.DAILY_GOAL))
        settingsRepo.set(SettingsRepository.Keys.DAILY_GOAL, "50")
        assertEquals("50", settingsRepo.get(SettingsRepository.Keys.DAILY_GOAL))
        assertNull(settingsRepo.get("不存在"))
    }

    @Test
    fun invalidDateRejected() = runBlocking<Unit> {
        try {
            checkInRepo.checkIn("2026-13-40", goalSnapshot = 10, completedCount = 10)
            fail("非法日期应被拒绝")
        } catch (e: IllegalArgumentException) { /* ok */ }
        try {
            statisticsRepo.upsert(DailyStat(date = "not-a-date"))
            fail("非法日期应被拒绝")
        } catch (e: IllegalArgumentException) { /* ok */ }
        assertEquals(0, checkInRepo.count())
    }

    /** 新增：目标快照独立于当前设置。 */
    @Test
    fun goalSnapshotIndependentOfSettings() = runBlocking<Unit> {
        settingsRepo.set(SettingsRepository.Keys.DAILY_GOAL, "50")
        checkInRepo.checkIn("2026-04-03", goalSnapshot = 20, completedCount = 20)
        settingsRepo.set(SettingsRepository.Keys.DAILY_GOAL, "99")
        assertEquals(20, checkInRepo.getByDate("2026-04-03")!!.goalSnapshot)
    }

    /** 新增：DailyStat upsert 覆盖而非累加。 */
    @Test
    fun dailyStatNoDoubleCount() = runBlocking<Unit> {
        statisticsRepo.upsert(DailyStat(date = "2026-04-02", newWords = 5))
        statisticsRepo.upsert(DailyStat(date = "2026-04-02", newWords = 5))
        assertEquals(5, statisticsRepo.getByDate("2026-04-02")!!.newWords)
    }

    /** 新增：删除词库后历史统计/打卡一致。 */
    @Test
    fun statisticsConsistencyOnBookDelete() = runBlocking<Unit> {
        statisticsRepo.upsert(DailyStat(date = "2026-04-01", newWords = 3, reviewedWords = 2, correctCount = 4, incorrectCount = 1, totalStudySeconds = 300))
        checkInRepo.checkIn("2026-04-01", goalSnapshot = 10, completedCount = 10)
        val b = wordBookRepo.create("BS")
        wordRepo.addWord(newWord(b, "x", "y"))
        wordBookRepo.deleteById(b)
        assertEquals(3, statisticsRepo.getByDate("2026-04-01")!!.newWords)
        assertEquals(10, checkInRepo.getByDate("2026-04-01")!!.goalSnapshot)
    }

    /** 新增：事务失败回滚。 */
    @Test
    fun transactionRollback() = runBlocking<Unit> {
        val before = wordBookRepo.count()
        try {
            db.withTransaction {
                wordBookRepo.create("TX")
                throw RuntimeException("boom")
            }
            fail("应抛异常")
        } catch (e: RuntimeException) { /* ok */ }
        assertEquals(before, wordBookRepo.count())
    }

    /** 更新：删除词库时历史学习记录保留（wordId 置空 + 快照），不产生孤立数据。 */
    @Test
    fun historyPreservedOnBookDeleteNoOrphans() = runBlocking<Unit> {
        val bookId = wordBookRepo.create("Book")
        val wid = wordRepo.addWord(newWord(bookId, "hist", "历史"))
        val sid = studyRepo.startSession(StudySession(date = "2026-02-02", startedAt = 1L))
        studyRepo.addRecord(StudyRecord(sessionId = sid, wordId = wid, result = "EASY", isCorrect = true, createdAt = 1L))
        studyRepo.saveReviewState(ReviewState(wordId = wid, dueDate = "2026-02-03"))

        wordBookRepo.deleteById(bookId)

        assertNull(wordRepo.getById(wid))
        val recs = studyRepo.recordsBySession(sid)
        assertEquals(1, recs.size)
        assertNull(recs[0].wordId)
        assertEquals("hist", recs[0].wordSpelling)
        assertEquals("历史", recs[0].wordMeaning)
        assertNull(studyRepo.reviewState(wid))
    }

    /** 新增：删除单词后历史学习记录仍可追溯。 */
    @Test
    fun historyPreservedOnWordDelete() = runBlocking<Unit> {
        val bookId = wordBookRepo.create("Book")
        val wid = wordRepo.addWord(newWord(bookId, "trace", "追踪"))
        val sid = studyRepo.startSession(StudySession(date = "2026-02-03", startedAt = 1L))
        studyRepo.addRecord(StudyRecord(sessionId = sid, wordId = wid, result = "GOOD", isCorrect = true, createdAt = 1L))
        wordRepo.deleteWord(wordRepo.getById(wid)!!)
        assertNull(wordRepo.getById(wid))
        val recs = studyRepo.recordsBySession(sid)
        assertEquals(1, recs.size)
        assertNull(recs[0].wordId)
        assertEquals("trace", recs[0].wordSpelling)
        assertEquals("追踪", recs[0].wordMeaning)
    }

    @Test
    fun dataPersistsAcrossRestart() = runBlocking<Unit> {
        val dbName = "persistence_test.db"
        context.deleteDatabase(dbName)

        var persistent = Room.databaseBuilder(context, WordStreakDatabase::class.java, dbName).build()
        val id = WordBookRepository(persistent.wordBookDao()).create("持久化词库")
        val wordId = WordRepository(persistent.wordDao()).addWord(
            Word(bookId = id, spelling = "persist", meaning = "持久", createdAt = 1L, updatedAt = 1L)
        )
        persistent.close()

        persistent = Room.databaseBuilder(context, WordStreakDatabase::class.java, dbName).build()
        assertNotNull(WordBookRepository(persistent.wordBookDao()).getById(id))
        assertEquals("持久化词库", WordBookRepository(persistent.wordBookDao()).getById(id)!!.name)
        assertEquals("persist", WordRepository(persistent.wordDao()).getById(wordId)!!.spelling)
        persistent.close()
        context.deleteDatabase(dbName)
    }

    private fun newWord(bookId: Long, spelling: String, meaning: String): Word =
        Word(
            bookId = bookId,
            spelling = spelling,
            meaning = meaning,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
}
