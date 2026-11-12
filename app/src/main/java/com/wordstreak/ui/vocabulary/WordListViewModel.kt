package com.wordstreak.ui.vocabulary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordstreak.data.local.entity.Word
import com.wordstreak.data.local.entity.WordBook
import com.wordstreak.data.repository.WordBookRepository
import com.wordstreak.data.repository.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WordListViewModel(
    private val bookId: Long,
    private val books: WordBookRepository,
    private val wordsRepository: WordRepository
) : ViewModel() {
    private val query = MutableStateFlow("")
    val searchQuery: StateFlow<String> = query
    val words: StateFlow<List<Word>> = query.flatMapLatest { value ->
        if (value.isBlank()) wordsRepository.observeByBook(bookId)
        else wordsRepository.search(bookId, value.trim())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _book = MutableStateFlow<WordBook?>(null)
    val book: StateFlow<WordBook?> = _book

    init { viewModelScope.launch { _book.value = books.getById(bookId) } }

    fun setQuery(value: String) { query.value = value }

    fun save(word: Word?, spelling: String, meaning: String, phonetic: String, partOfSpeech: String,
             example: String, exampleTranslation: String, onError: (String) -> Unit, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                if (word == null) {
                    wordsRepository.addWord(Word(bookId = bookId, spelling = spelling, meaning = meaning,
                        phonetic = phonetic.ifBlank { null }, partOfSpeech = partOfSpeech.ifBlank { null },
                        example = example.ifBlank { null }, exampleTranslation = exampleTranslation.ifBlank { null },
                        createdAt = now, updatedAt = now))
                } else {
                    wordsRepository.updateWord(word.copy(spelling = spelling, meaning = meaning,
                        phonetic = phonetic.ifBlank { null }, partOfSpeech = partOfSpeech.ifBlank { null },
                        example = example.ifBlank { null }, exampleTranslation = exampleTranslation.ifBlank { null }))
                }
            }.onSuccess { onSaved() }.onFailure { onError(it.message ?: "保存单词失败") }
        }
    }

    fun delete(word: Word, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { wordsRepository.deleteWord(word) }
                .onFailure { onError(it.message ?: "删除单词失败") }
        }
    }
}
