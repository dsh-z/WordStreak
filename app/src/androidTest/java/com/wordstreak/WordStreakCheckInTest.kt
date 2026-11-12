package com.wordstreak

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wordstreak.data.local.WordStreakDatabase
import com.wordstreak.data.repository.CheckInRepository
import com.wordstreak.data.repository.SettingsRepository
import com.wordstreak.domain.usecase.StudyStreak
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Stage 3.5 P0 测试：
 * - 每日进度打卡：同日幂等（一条记录，重复调用为更新）、completedCount>0 即打卡、跨日多条
 * - 连续打卡由已完成日期驱动
 * - 每日目标默认 20、1–999 边界校验
 */
@RunWith(AndroidJUnit4::class)
class WordStreakCheckInTest {

    private lateinit var context: Context
    private lateinit var db: WordStreakDatabase
    private lateinit var checkInRepo: CheckInRepository
    private lateinit var settingsRepo: SettingsRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = WordStreakDatabase.createInMemory(context)
        checkInRepo = CheckInRepository(db.checkInDao())
        settingsRepo = SettingsRepository(db.settingsDao())
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun recordProgress_sameDayIsIdempotent_andUpdates() = runBlocking<Unit> {
        val day = "2026-06-15"
        checkInRepo.recordProgress(day, goalSnapshot = 20, completedCount = 3)
        checkInRepo.recordProgress(day, goalSnapshot = 20, completedCount = 8)

        assertEquals("同一天只应有一条打卡记录", 1, checkInRepo.count())
        val row = checkInRepo.getByDate(day)!!
        assertEquals(8, row.completedCount)
        assertEquals(20, row.goalSnapshot)
        assertTrue("当天有学习即打卡", row.completed)
    }

    @Test
    fun recordProgress_zeroCountIsNotCompleted() = runBlocking<Unit> {
        val day = "2026-06-16"
        checkInRepo.recordProgress(day, goalSnapshot = 20, completedCount = 0)
        val row = checkInRepo.getByDate(day)!!
        assertFalse("没有学习不应算打卡", row.completed)
    }

    @Test
    fun recordProgress_differentDaysAreDistinctRows() = runBlocking<Unit> {
        checkInRepo.recordProgress("2026-06-15", 20, 1)
        checkInRepo.recordProgress("2026-06-16", 20, 2)
        assertEquals(2, checkInRepo.count())
    }

    @Test
    fun completedDates_driveStreak() = runBlocking<Unit> {
        checkInRepo.recordProgress("2026-06-15", 20, 5)
        checkInRepo.recordProgress("2026-06-16", 20, 2)
        val days = checkInRepo.observeCompletedDates().first()
        assertTrue(days.contains("2026-06-15"))
        assertTrue(days.contains("2026-06-16"))
        assertEquals(2, StudyStreak.currentStreak(days, "2026-06-16"))
    }

    @Test
    fun dailyGoal_defaultAndBounds() = runBlocking<Unit> {
        assertEquals("默认每日目标应为 20", 20, settingsRepo.getDailyGoal())
        settingsRepo.setDailyGoal(35)
        assertEquals(35, settingsRepo.getDailyGoal())
        try { settingsRepo.setDailyGoal(0); fail("应拒绝 0") } catch (e: IllegalArgumentException) { }
        try { settingsRepo.setDailyGoal(1000); fail("应拒绝 1000") } catch (e: IllegalArgumentException) { }
    }
}
