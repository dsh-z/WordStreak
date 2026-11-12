package com.wordstreak

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wordstreak.data.local.WordStreakDatabase
import com.wordstreak.data.local.entity.StudyRecord
import com.wordstreak.data.local.entity.StudySession
import com.wordstreak.data.local.entity.Word
import com.wordstreak.data.repository.StudyRepository
import com.wordstreak.data.repository.WordBookRepository
import com.wordstreak.data.repository.WordRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WordStreakStudyTest {
    private lateinit var database: WordStreakDatabase
    private lateinit var books: WordBookRepository
    private lateinit var words: WordRepository
    private lateinit var study: StudyRepository

    @Before
    fun setUp() = runBlocking<Unit> {
        database = WordStreakDatabase.createInMemory(ApplicationProvider.getApplicationContext())
        books = WordBookRepository(database.wordBookDao())
        words = WordRepository(database.wordDao())
        study = StudyRepository(database.studyDao(), database.reviewDao(), database.wordDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun sessionStoresRatingAndWordSnapshot() = runBlocking<Unit> {
        val bookId = books.create("学习测试")
        val now = System.currentTimeMillis()
        val wordId = words.addWord(
            Word(
                bookId = bookId,
                spelling = "apple",
                meaning = "苹果",
                phonetic = "/ˈæp.əl/",
                partOfSpeech = "noun",
                example = "I eat an apple.",
                exampleTranslation = "我吃一个苹果。",
                createdAt = now,
                updatedAt = now
            )
        )
        val sessionId = study.startSession(StudySession(date = "2026-11-12", startedAt = now))

        study.addRecord(
            StudyRecord(
                sessionId = sessionId,
                wordId = wordId,
                result = "GOOD",
                isCorrect = true,
                createdAt = now
            )
        )
        val updatedSession = StudySession(
            id = sessionId,
            date = "2026-11-12",
            startedAt = now,
            endedAt = now + 1,
            newCount = 1,
            reviewCount = 0,
            completed = true
        )
        study.finishSession(updatedSession)

        val record = study.recordsBySession(sessionId).single()
        assertEquals("GOOD", record.result)
        assertTrue(record.isCorrect)
        assertEquals("apple", record.wordSpelling)
        assertEquals("苹果", record.wordMeaning)
        assertEquals(wordId, record.wordId)
        val savedSession = study.getSession(sessionId)
        assertNotNull(savedSession)
        assertEquals(1, savedSession?.newCount)
        assertTrue(savedSession?.completed == true)
    }

    @Test
    fun unfinishedSessionKeepsSavedRecordsAndRemainsIncomplete() = runBlocking<Unit> {
        val bookId = books.create("中途退出测试")
        val now = System.currentTimeMillis()
        val wordId = words.addWord(
            Word(bookId = bookId, spelling = "book", meaning = "书", createdAt = now, updatedAt = now)
        )
        val sessionId = study.startSession(StudySession(date = "2026-11-12", startedAt = now))
        study.addRecord(
            StudyRecord(
                sessionId = sessionId,
                wordId = wordId,
                result = "AGAIN",
                isCorrect = false,
                createdAt = now
            )
        )
        study.finishSession(
            StudySession(
                id = sessionId,
                date = "2026-11-12",
                startedAt = now,
                endedAt = now + 1,
                newCount = 1,
                reviewCount = 0,
                completed = false
            )
        )

        assertEquals(1, study.recordsBySession(sessionId).size)
        assertFalse(study.getSession(sessionId)?.completed ?: true)
        assertEquals(1, study.getSession(sessionId)?.newCount)
    }

    @Test
    fun onlySupportedRatingValuesAreAccepted() = runBlocking<Unit> {
        val bookId = books.create("评分校验")
        val now = System.currentTimeMillis()
        val wordId = words.addWord(
            Word(bookId = bookId, spelling = "test", meaning = "测试", createdAt = now, updatedAt = now)
        )
        val sessionId = study.startSession(StudySession(date = "2026-11-12", startedAt = now))
        val error = runCatching {
            study.addRecord(
                StudyRecord(
                    sessionId = sessionId,
                    wordId = wordId,
                    result = "UNKNOWN",
                    isCorrect = false,
                    createdAt = now
                )
            )
        }.exceptionOrNull()
        assertNotNull(error)
        assertNull(study.recordsBySession(sessionId).firstOrNull())
        assertEquals(setOf("AGAIN", "HARD", "GOOD", "EASY"), StudyRepository.RESULTS)
    }

    @Test
    fun ratingInsertRollsBackWhenSessionUpdateFails() = runBlocking<Unit> {
        val bookId = books.create("事务回滚测试")
        val now = System.currentTimeMillis()
        val wordId = words.addWord(
            Word(bookId = bookId, spelling = "atomic", meaning = "原子性", createdAt = now, updatedAt = now)
        )
        val sessionId = study.startSession(StudySession(date = "2026-11-12", startedAt = now))

        val error = runCatching {
            study.saveRatingAndSession(
                StudyRecord(
                    sessionId = sessionId,
                    wordId = wordId,
                    result = "GOOD",
                    isCorrect = true,
                    createdAt = now
                ),
                StudySession(
                    id = sessionId + 999,
                    date = "2026-11-12",
                    startedAt = now,
                    endedAt = now + 1,
                    newCount = 1,
                    reviewCount = 0,
                    completed = false
                )
            )
        }.exceptionOrNull()

        assertNotNull(error)
        assertTrue(study.recordsBySession(sessionId).isEmpty())
        val savedSession = study.getSession(sessionId)
        assertNotNull(savedSession)
        assertEquals(0, savedSession?.newCount)
        assertFalse(savedSession?.completed ?: true)
    }

}
