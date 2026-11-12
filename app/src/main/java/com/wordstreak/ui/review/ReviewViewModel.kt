package com.wordstreak.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordstreak.data.local.converter.DateValidator
import com.wordstreak.data.local.entity.Word
import com.wordstreak.data.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 基础复习流程：载入待复习单词快照，逐张评分并更新下次复习时间。
 * 复习次数/到期由 ReviewRepository 写入数据库；本 VM 维护本轮进度。
 */
class ReviewViewModel(private val reviewRepository: ReviewRepository) : ViewModel() {

    private val today: String = DateValidator.today()

    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = ReviewUiState(isLoading = true)
            runCatching { reviewRepository.loadDueWords(today) }
                .onSuccess { words -> _state.value = ReviewUiState(words = words, isLoading = false) }
                .onFailure { e -> _state.value = ReviewUiState(isLoading = false, error = e.message ?: "加载待复习单词失败") }
        }
    }

    fun flip() {
        val s = _state.value
        if (!s.isSaving && !s.finished && s.currentWord != null) {
            _state.value = s.copy(flipped = !s.flipped)
        }
    }

    fun rate(result: String) {
        val s = _state.value
        val word = s.currentWord ?: return
        if (s.isSaving || s.finished) return
        viewModelScope.launch {
            _state.value = s.copy(isSaving = true, error = null)
            runCatching { reviewRepository.review(word, result, today) }
                .onSuccess {
                    val nextIndex = s.index + 1
                    val finished = nextIndex >= s.words.size
                    _state.value = s.copy(
                        index = nextIndex,
                        completedCount = s.completedCount + 1,
                        flipped = false,
                        isSaving = false,
                        finished = finished
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isSaving = false, error = e.message ?: "复习保存失败，请重试")
                }
        }
    }
}

data class ReviewUiState(
    val words: List<Word> = emptyList(),
    val index: Int = 0,
    val flipped: Boolean = false,
    val completedCount: Int = 0,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val finished: Boolean = false,
    val error: String? = null
) {
    val currentWord: Word? get() = words.getOrNull(index)
    val total: Int get() = words.size
    val remaining: Int get() = (words.size - completedCount).coerceAtLeast(0)
    val empty: Boolean get() = !isLoading && words.isEmpty()
}
