package com.wordstreak

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wordstreak.data.local.WordStreakDatabase
import com.wordstreak.data.local.entity.Word
import com.wordstreak.data.repository.ReviewRepository
import com.wordstreak.data.repository.WordBookRepository
import com.wordstreak.data.repository.WordRepository
import com.wordstreak.domain.usecase.ReviewScheduler
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Stage 3.4 复习算法优化测试：
 * - 动态间隔（首次默认、GOOD/EASY/HARD 增长、AGAIN 重置、365 上限、单调性）
 * - 动态到期日边界（跨年/闰日）
 * - 数据库持久化：连续 GOOD 复习 intervalDays 递增(4→8→16)、reviewCount 累加并持久化
 * 既有 WordStreakReviewTest（首次默认映射等）保持不变并仍应通过。
 */
@RunWith(AndroidJUnit4::class)
class WordStreakReviewScheduleTest {

    private lateinit var context: Context
    private lateinit var db: WordStreakDatabase
    private lateinit var bookRepo: WordBookRepository
    private lateinit var wordRepo: WordRepository
    private lateinit var reviewRepo: ReviewRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = WordStreakDatabase.createInMemory(context)
        bookRepo = WordBookRepository(db.wordBookDao())
        wordRepo = WordRepository(db.wordDao())
        reviewRepo = ReviewRepository(db.reviewDao())
    }

    @After
    fun tearDown() { db.close() }

    // ---------- 纯函数：动态间隔 ----------

    @Test
    fun firstReviewDefaults_matchOldMapping() {
        assertEquals(1, ReviewScheduler.nextIntervalDays("AGAIN", 0))
        assertEquals(2, ReviewScheduler.nextIntervalDays("HARD", 0))
        assertEquals(4, ReviewScheduler.nextIntervalDays("GOOD", 0))
        assertEquals(7, ReviewScheduler.nextIntervalDays("EASY", 0))
        // 旧 1 参方法等价（prev=0）
        assertEquals(4, ReviewScheduler.nextIntervalDays("GOOD"))
    }

    @Test
    fun goodGrowth_doubles() {
        assertEquals(4, ReviewScheduler.nextIntervalDays("GOOD", 0))
        assertEquals(8, ReviewScheduler.nextIntervalDays("GOOD", 4))
        assertEquals(16, ReviewScheduler.nextIntervalDays("GOOD", 8))
        assertEquals(32, ReviewScheduler.nextIntervalDays("GOOD", 16))
    }

    @Test
    fun easyGrowth_triples() {
        assertEquals(7, ReviewScheduler.nextIntervalDays("EASY", 0))
        assertEquals(21, ReviewScheduler.nextIntervalDays("EASY", 7))
        assertEquals(63, ReviewScheduler.nextIntervalDays("EASY", 21))
    }

    @Test
    fun hardGrowth_ceilAndIncrement() {
        assertEquals(2, ReviewScheduler.nextIntervalDays("HARD", 0))
        assertEquals(3, ReviewScheduler.nextIntervalDays("HARD", 2)) // ceil(2.4)=3
        assertEquals(4, ReviewScheduler.nextIntervalDays("HARD", 3)) // ceil(3.6)=4
        assertEquals(5, ReviewScheduler.nextIntervalDays("HARD", 4)) // ceil(4.8)=5
    }

    @Test
    fun againResetsToOne() {
        assertEquals(1, ReviewScheduler.nextIntervalDays("AGAIN", 0))
        assertEquals(1, ReviewScheduler.nextIntervalDays("AGAIN", 5))
        assertEquals(1, ReviewScheduler.nextIntervalDays("AGAIN", 100))
    }

    @Test
    fun capAt365() {
        assertEquals(365, ReviewScheduler.nextIntervalDays("GOOD", 300)) // 600 -> 365
        assertEquals(365, ReviewScheduler.nextIntervalDays("EASY", 200)) // 600 -> 365
        assertEquals(365, ReviewScheduler.nextIntervalDays("HARD", 400)) // ceil(480) -> 365
    }

    @Test
    fun monotonicNonDecreasing() {
        val prevs = listOf(0, 1, 2, 4, 8, 16, 50, 200)
        for (r in listOf("HARD", "GOOD", "EASY")) {
            var last = 0
            for (p in prevs) {
                val v = ReviewScheduler.nextIntervalDays(r, p)
                if (v < last) throw AssertionError("$r 在 prev=$p 处非单调：$v < $last")
                last = v
            }
        }
    }

    @Test
    fun invalidResultThrows2arg() {
        try {
            ReviewScheduler.nextIntervalDays("BAD", 3)
            throw AssertionError("非法评分应抛异常")
        } catch (e: IllegalArgumentException) { /* ok */ }
    }

    @Test
    fun nextDueDate_withPreviousIntervalBoundaries() {
        // prev=4 GOOD -> +8 天：2026-06-15 -> 2026-06-23
        assertEquals("2026-06-23", ReviewScheduler.nextDueDate("GOOD", "2026-06-15", 4))
        // prev=10 EASY -> max(7,30)=30 天，跨年：2026-12-30 -> 2027-01-29
        assertEquals("2027-01-29", ReviewScheduler.nextDueDate("EASY", "2026-12-30", 10))
        // prev=0 GOOD -> 4 天，闰年 2028-02-28 -> 2028-03-03
        assertEquals("2028-03-03", ReviewScheduler.nextDueDate("GOOD", "2028-02-28", 0))
    }

    // ---------- 数据库持久化 ----------

    @Test
    fun repeatedGood_growsIntervalAndPersists() = runBlocking<Unit> {
        val bookId = bookRepo.create("Book")
        val wid = wordRepo.addWord(Word(bookId = bookId, spelling = "apple", meaning = "苹果", createdAt = 1, updatedAt = 1))
        val word = wordRepo.getById(wid)!!
        val today = "2026-06-15"

        val r1 = reviewRepo.review(word, "GOOD", today)   // prev=0 -> 4
        assertEquals(4, r1.intervalDays)
        assertEquals("2026-06-19", r1.dueDate)
        assertEquals(1, r1.reviewCount)

        val r2 = reviewRepo.review(word, "GOOD", today)   // prev=4 -> 8
        assertEquals(8, r2.intervalDays)
        assertEquals("2026-06-23", r2.dueDate)
        assertEquals(2, r2.reviewCount)

        val r3 = reviewRepo.review(word, "GOOD", today)   // prev=8 -> 16
        assertEquals(16, r3.intervalDays)
        assertEquals("2026-07-01", r3.dueDate)
        assertEquals(3, r3.reviewCount)

        // 持久化：读取回来 intervalDays / reviewCount 一致
        val persisted = reviewRepo.getReviewState(wid)!!
        assertEquals(16, persisted.intervalDays)
        assertEquals(3, persisted.reviewCount)
        assertEquals("2026-07-01", persisted.dueDate)

        // 到期日未来 → 今天不待复习
        assertFalse(reviewRepo.loadDueWords(today).any { it.id == wid })
    }

    @Test
    fun againAfterGrowth_resetsIntervalButKeepsCount() = runBlocking<Unit> {
        val bookId = bookRepo.create("Book")
        val wid = wordRepo.addWord(Word(bookId = bookId, spelling = "banana", meaning = "香蕉", createdAt = 1, updatedAt = 1))
        val word = wordRepo.getById(wid)!!
        reviewRepo.review(word, "EASY", "2026-06-15")  // prev0 -> 7
        reviewRepo.review(word, "EASY", "2026-06-15")  // prev7 -> 21
        val r = reviewRepo.review(word, "AGAIN", "2026-06-15") // 重置 -> 1
        assertEquals(1, r.intervalDays)
        assertEquals("2026-06-16", r.dueDate)
        assertEquals(3, r.reviewCount)
    }
}
