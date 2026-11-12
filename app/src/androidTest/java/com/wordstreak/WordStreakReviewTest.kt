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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Stage 3.3 复习功能测试：
 * - 复习调度纯函数（AGAIN/HARD/GOOD/EASY 间隔、跨年/跨月/闰日边界、非法评分）
 * - 数据库更新：新词入待复习、评分后写入下次到期日/间隔/次数、到期筛选边界、再次复习累加
 */
@RunWith(AndroidJUnit4::class)
class WordStreakReviewTest {

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

    // ---------- 纯函数：复习调度 ----------

    @Test
    fun intervals_followBasicMapping() {
        assertEquals(1, ReviewScheduler.nextIntervalDays("AGAIN"))
        assertEquals(2, ReviewScheduler.nextIntervalDays("HARD"))
        assertEquals(4, ReviewScheduler.nextIntervalDays("GOOD"))
        assertEquals(7, ReviewScheduler.nextIntervalDays("EASY"))
    }

    @Test
    fun invalidResultThrows() {
        try {
            ReviewScheduler.nextIntervalDays("BAD")
            throw AssertionError("非法评分应抛异常")
        } catch (e: IllegalArgumentException) { /* ok */ }
    }

    @Test
    fun nextDueDate_crossMonthYearAndLeap() {
        assertEquals("2027-01-03", ReviewScheduler.nextDueDate("GOOD", "2026-12-30")) // 跨年
        assertEquals("2026-02-02", ReviewScheduler.nextDueDate("HARD", "2026-01-31")) // 跨月
        assertEquals("2028-03-06", ReviewScheduler.nextDueDate("EASY", "2028-02-28")) // 闰年(2028)2月
        assertEquals("2026-06-16", ReviewScheduler.nextDueDate("AGAIN", "2026-06-15")) // +1 天
    }

    // ---------- 数据库更新 ----------

    @Test
    fun newWordIsDue_andReviewSchedulesNext() = runBlocking<Unit> {
        val bookId = bookRepo.create("Book")
        val wid = wordRepo.addWord(Word(bookId = bookId, spelling = "apple", meaning = "苹果", createdAt = 1, updatedAt = 1))
        val word = wordRepo.getById(wid)!!
        val today = "2026-06-15"

        // 新词（无复习状态）应出现在待复习、计数为 1
        assertEquals(1, reviewRepo.loadDueWords(today).size)
        assertEquals(1, reviewRepo.countDueWords(today))

        // GOOD → 下次到期 = +4 天，间隔 4，次数 1
        val st = reviewRepo.review(word, "GOOD", today)
        assertEquals("2026-06-19", st.dueDate)
        assertEquals(4, st.intervalDays)
        assertEquals(1, st.reviewCount)
        assertEquals("2026-06-19", reviewRepo.getReviewState(wid)!!.dueDate)

        // 到期日未来 → 今天不再待复习
        assertEquals(0, reviewRepo.countDueWords(today))
        assertFalse(reviewRepo.loadDueWords(today).any { it.id == wid })

        // 再次复习 → 次数累加、间隔按新评分
        val st2 = reviewRepo.review(word, "AGAIN", today)
        assertEquals(2, st2.reviewCount)
        assertEquals(1, st2.intervalDays)
        assertEquals("2026-06-16", st2.dueDate)
    }

    @Test
    fun dueFilter_respectsDueDateBoundary() = runBlocking<Unit> {
        val bookId = bookRepo.create("Book")
        val wid = wordRepo.addWord(Word(bookId = bookId, spelling = "banana", meaning = "香蕉", createdAt = 1, updatedAt = 1))
        val word = wordRepo.getById(wid)!!
        // 复习 @2026-06-01 GOOD -> dueDate 2026-06-05
        reviewRepo.review(word, "GOOD", "2026-06-01")

        // 到期前(06-04)不出现；到期当日(06-05)与之后出现
        assertFalse(reviewRepo.loadDueWords("2026-06-04").any { it.id == wid })
        assertTrue(reviewRepo.loadDueWords("2026-06-05").any { it.id == wid })
        assertTrue(reviewRepo.loadDueWords("2026-06-10").any { it.id == wid })
    }

    @Test
    fun emptyDueWhenNoWords() = runBlocking<Unit> {
        assertEquals(0, reviewRepo.countDueWords("2026-06-15"))
        assertTrue(reviewRepo.loadDueWords("2026-06-15").isEmpty())
    }
}
