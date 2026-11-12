package com.wordstreak

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wordstreak.data.local.WordStreakDatabase
import com.wordstreak.data.local.entity.StudyRecord
import com.wordstreak.data.local.entity.StudySession
import com.wordstreak.data.repository.CheckInRepository
import com.wordstreak.data.repository.StudyRepository
import com.wordstreak.domain.usecase.StudyStats
import com.wordstreak.domain.usecase.StudyStreak
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Stage 5：学习日历与统计的测试。
 * 覆盖：连续打卡（当前/最长）、每日汇总计算、月度活跃日期/每日记录/已完成打卡的 Flow 查询。
 */
@RunWith(AndroidJUnit4::class)
class WordStreakStatsTest {

    private lateinit var context: Context
    private lateinit var db: WordStreakDatabase
    private lateinit var studyRepo: StudyRepository
    private lateinit var checkInRepo: CheckInRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = WordStreakDatabase.createInMemory(context)
        studyRepo = StudyRepository(db.studyDao(), db.reviewDao(), db.wordDao())
        checkInRepo = CheckInRepository(db.checkInDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ---------- 纯函数：连续打卡 ----------

    @Test
    fun longestStreak_emptyIsZero() {
        assertEquals(0, StudyStreak.longestStreak(emptyList()))
    }

    @Test
    fun longestStreak_withGaps() {
        val dates = listOf("2026-01-01", "2026-01-02", "2026-01-03", "2026-01-05", "2026-01-06")
        assertEquals(3, StudyStreak.longestStreak(dates))
    }

    @Test
    fun longestStreak_unorderedAndDuplicates() {
        val dates = listOf("2026-02-03", "2026-02-01", "2026-02-02", "2026-02-02")
        assertEquals(3, StudyStreak.longestStreak(dates))
    }

    @Test
    fun currentStreak_endingToday() {
        val dates = listOf("2026-03-08", "2026-03-09", "2026-03-10")
        assertEquals(3, StudyStreak.currentStreak(dates, "2026-03-10"))
    }

    @Test
    fun currentStreak_todayMissingButYesterdayPresentContinues() {
        val dates = listOf("2026-03-08", "2026-03-09")
        // 今天(03-10)尚未打卡，但昨天(03-09)已打卡 → 连续仍为 2
        assertEquals(2, StudyStreak.currentStreak(dates, "2026-03-10"))
    }

    @Test
    fun currentStreak_brokenReturnsZero() {
        val dates = listOf("2026-03-01", "2026-03-02")
        assertEquals(0, StudyStreak.currentStreak(dates, "2026-03-10"))
    }

    @Test
    fun currentStreak_emptyIsZero() {
        assertEquals(0, StudyStreak.currentStreak(emptyList(), "2026-03-10"))
    }

    // ---------- 纯函数：每日汇总 ----------

    @Test
    fun summarize_distinctWordsAndCorrectness() {
        val records = listOf(
            StudyRecord(id = 1, sessionId = 1, wordId = 1, result = "GOOD", isCorrect = true, createdAt = 1),
            StudyRecord(id = 2, sessionId = 1, wordId = 1, result = "AGAIN", isCorrect = false, createdAt = 2),
            StudyRecord(id = 3, sessionId = 1, wordId = 2, result = "EASY", isCorrect = true, createdAt = 3),
            StudyRecord(id = 4, sessionId = 1, wordId = null, wordSpelling = "gone", result = "HARD", isCorrect = true, createdAt = 4)
        )
        val s = StudyStats.summarize(records)
        assertEquals(3, s.wordCount)     // 1, 2, "gone"
        assertEquals(4, s.recordCount)
        assertEquals(3, s.correctCount)
        assertEquals(1, s.incorrectCount)
        assertTrue(s.hasData)
    }

    @Test
    fun summarize_emptyHasNoData() {
        val s = StudyStats.summarize(emptyList())
        assertEquals(0, s.recordCount)
        assertTrue(!s.hasData)
    }

    // ---------- 数据库 Flow 查询 ----------

    @Test
    fun observeActiveDates_onlyDatesWithRecords() = runBlocking {
        // 2026-04-01 有记录
        val s1 = studyRepo.startSession(StudySession(date = "2026-04-01", startedAt = 1))
        studyRepo.addRecord(StudyRecord(sessionId = s1, wordId = null, wordSpelling = "a", result = "GOOD", isCorrect = true, createdAt = 1))
        // 2026-04-05 有记录
        val s2 = studyRepo.startSession(StudySession(date = "2026-04-05", startedAt = 1))
        studyRepo.addRecord(StudyRecord(sessionId = s2, wordId = null, wordSpelling = "b", result = "EASY", isCorrect = true, createdAt = 2))
        // 2026-04-03 仅有会话、无记录 → 不应计入
        studyRepo.startSession(StudySession(date = "2026-04-03", startedAt = 1))

        val active = studyRepo.observeActiveDatesBetween("2026-04-01", "2026-04-30").first()
        assertEquals(setOf("2026-04-01", "2026-04-05"), active.toSet())
    }

    @Test
    fun observeDailyRecords_returnsRecordsOfThatDay() = runBlocking {
        val s = studyRepo.startSession(StudySession(date = "2026-05-01", startedAt = 1))
        studyRepo.addRecord(StudyRecord(sessionId = s, wordId = null, wordSpelling = "x", result = "GOOD", isCorrect = true, createdAt = 1))
        studyRepo.addRecord(StudyRecord(sessionId = s, wordId = null, wordSpelling = "y", result = "AGAIN", isCorrect = false, createdAt = 2))
        val other = studyRepo.startSession(StudySession(date = "2026-05-02", startedAt = 1))
        studyRepo.addRecord(StudyRecord(sessionId = other, wordId = null, wordSpelling = "z", result = "EASY", isCorrect = true, createdAt = 3))

        val daily = studyRepo.observeDailyRecords("2026-05-01").first()
        assertEquals(2, daily.size)
        assertEquals(2, StudyStats.summarize(daily).recordCount)
        assertEquals(1, StudyStats.summarize(daily).incorrectCount)
    }

    @Test
    fun observeCompletedDates_onlyCompleted() = runBlocking {
        checkInRepo.checkIn("2026-06-02", goalSnapshot = 10, completedCount = 10) // completed
        checkInRepo.checkIn("2026-06-03", goalSnapshot = 10, completedCount = 3)  // not completed

        val completed = checkInRepo.observeCompletedDates().first()
        assertEquals(listOf("2026-06-02"), completed)
    }
}
