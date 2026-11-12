package com.wordstreak.ui.vocabulary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordstreak.data.local.entity.BookWithWordCount
import com.wordstreak.data.local.entity.WordBook
import com.wordstreak.data.repository.WordBookRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VocabularyViewModel(private val repository: WordBookRepository) : ViewModel() {
    val books: StateFlow<List<BookWithWordCount>> = repository.observeWithWordCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(book: WordBook?, name: String, description: String, onError: (String) -> Unit, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                if (book == null) repository.create(name, description)
                else repository.update(book.copy(name = name, description = description))
            }.onSuccess { onSaved() }.onFailure { onError(it.message ?: "保存词库失败") }
        }
    }

    fun delete(book: WordBook, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.delete(book) }
                .onFailure { onError(it.message ?: "删除词库失败") }
        }
    }
}
