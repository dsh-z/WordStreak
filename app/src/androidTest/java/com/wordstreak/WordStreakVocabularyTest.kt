package com.wordstreak

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wordstreak.data.local.WordStreakDatabase
import com.wordstreak.data.local.entity.Word
import com.wordstreak.data.local.entity.WordBook
import com.wordstreak.data.repository.WordBookRepository
import com.wordstreak.data.repository.WordRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WordStreakVocabularyTest {
    private lateinit var database: WordStreakDatabase
    private lateinit var books: WordBookRepository
    private lateinit var words: WordRepository

    @Before
    fun setUp() = runBlocking<Unit> {
        database = WordStreakDatabase.createInMemory(ApplicationProvider.getApplicationContext())
        books = WordBookRepository(database.wordBookDao())
        words = WordRepository(database.wordDao())
    }

    @After
    fun tearDown() { database.close() }

    @Test
    fun updateWordAllowsItsOwnSpellingButRejectsAnotherWordIgnoringCase() = runBlocking<Unit> {
        val bookId = books.create("英语")
        val now = System.currentTimeMillis()
        val appleId = words.addWord(Word(bookId = bookId, spelling = "Apple", meaning = "苹果", createdAt = now, updatedAt = now))
        val bananaId = words.addWord(Word(bookId = bookId, spelling = "Banana", meaning = "香蕉", createdAt = now, updatedAt = now))

        val apple = words.getById(appleId)!!
        words.updateWord(apple.copy(spelling = "APPLE"))
        assertEquals("APPLE", words.getById(appleId)!!.spelling)

        val banana = words.getById(bananaId)!!
        assertThrows(IllegalStateException::class.java) {
            runBlocking { words.updateWord(banana.copy(spelling = "apple")) }
        }
    }

    @Test
    fun wordBookCountFlowReflectsWordInsertAndDelete() = runBlocking<Unit> {
        val bookId = books.create("测试词库")
        assertEquals(0, books.observeWithWordCount().first().single().wordCount)
        val now = System.currentTimeMillis()
        val wordId = words.addWord(Word(bookId = bookId, spelling = "flow", meaning = "流", createdAt = now, updatedAt = now))
        assertEquals(1, books.observeWithWordCount().first().single().wordCount)
        words.deleteWord(words.getById(wordId)!!)
        assertEquals(0, books.observeWithWordCount().first().single().wordCount)
    }

    @Test
    fun searchMatchesSpellingAndMeaning() = runBlocking<Unit> {
        val bookId = books.create("搜索")
        val now = System.currentTimeMillis()
        words.addWord(Word(bookId = bookId, spelling = "apple", meaning = "苹果", createdAt = now, updatedAt = now))
        words.addWord(Word(bookId = bookId, spelling = "orange", meaning = "橙子", createdAt = now, updatedAt = now))
        assertEquals("apple", words.search(bookId, "app").first().single().spelling)
        assertEquals("orange", words.search(bookId, "橙").first().single().spelling)
    }
}
