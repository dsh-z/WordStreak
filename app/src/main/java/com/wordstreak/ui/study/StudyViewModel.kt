package com.wordstreak.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordstreak.data.local.converter.DateValidator
import com.wordstreak.data.local.entity.BookWithWordCount
import com.wordstreak.data.local.entity.StudyRecord
import com.wordstreak.data.local.entity.StudySession
import com.wordstreak.data.local.entity.Word
import com.wordstreak.data.repository.CheckInRepository
import com.wordstreak.data.repository.SettingsRepository
import com.wordstreak.data.repository.StudyRepository
import com.wordstreak.data.repository.WordBookRepository
import com.wordstreak.data.repository.WordRepository
import com.wordstreak.domain.usecase.StudyStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 基础学习流程：选择词库、按现有词库顺序学习、保存评分与会话结果。
 * 本阶段不执行新词/到期复习筛选，也不实现 FSRS。
 */
class StudyViewModel(
    private val booksRepository: WordBookRepository,
    private val wordsRepository: WordRepository,
    private val studyRepository: StudyRepository,
    private val checkInRepository: CheckInRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val books: StateFlow<List<BookWithWordCount>> = booksRepository.observeWithWordCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(StudyUiState())
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    private var currentSession: StudySession? = null

    fun startStudy(bookId: Long) {
        if (_uiState.value.isLoading || _uiState.value.sessionId != null) return
        viewModelScope.launch {
            _uiState.value = StudyUiState(isLoading = true)
            runCatching {
                val book = booksRepository.getById(bookId)
                    ?: error("找不到所选词库，请刷新后重试")
                val words = wordsRepository.getByBook(bookId)
                require(words.isNotEmpty()) { "这个词库还没有单词，请先添加单词" }

                val now = System.currentTimeMillis()
                val session = StudySession(
                    date = DateValidator.today(),
                    startedAt = now,
                    newCount = 0,
                    reviewCount = 0
                )
                val sessionId = studyRepository.startSession(session)
                Triple(book, words, session.copy(id = sessionId))
            }.onSuccess { (book, words, session) ->
                currentSession = session
                val bookWithCount = books.value.firstOrNull { it.book.id == book.id }
                    ?: BookWithWordCount(book, words.size)
                _uiState.value = StudyUiState(
                    selectedBook = bookWithCount,
                    words = words,
                    sessionId = session.id
                )
            }.onFailure { error ->
                _uiState.value = StudyUiState(error = error.message ?: "开始学习失败")
            }
        }
    }

    fun flipCard() {
        val state = _uiState.value
        if (state.sessionId != null && !state.isSaving && !state.completed) {
            _uiState.value = state.copy(isFlipped = !state.isFlipped)
        }
    }

    fun rate(result: String) {
        require(result in StudyRepository.RESULTS) { "不支持的评分：$result" }
        val state = _uiState.value
        val word = state.currentWord ?: return
        val session = currentSession ?: return
        if (state.isSaving || state.completed) return

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            try {
                val now = System.currentTimeMillis()
                val updatedCounts = state.counts.toMutableMap().apply {
                    this[result] = (this[result] ?: 0) + 1
                }
                val completedCount = updatedCounts.values.sum()
                val isLastWord = state.currentIndex == state.words.lastIndex
                val updatedSession = session.copy(
                    newCount = completedCount,
                    reviewCount = 0,
                    endedAt = if (isLastWord) now else null,
                    completed = isLastWord
                )
                studyRepository.saveRatingAndSession(
                    StudyRecord(
                        sessionId = session.id,
                        wordId = word.id,
                        result = result,
                        isCorrect = result != "AGAIN",
                        createdAt = now
                    ),
                    updatedSession
                )
                currentSession = updatedSession
                _uiState.value = state.copy(
                    currentIndex = if (updatedSession.completed) state.currentIndex else state.currentIndex + 1,
                    counts = updatedCounts,
                    isFlipped = false,
                    isSaving = false,
                    completed = updatedSession.completed
                )
                // 学习记录保存成功后同步当日打卡（幂等；打卡失败不影响学习保存）
                runCatching { syncDailyCheckIn() }
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = error.message ?: "保存学习记录失败，请重试"
                )
            }
        }
    }

    /**
     * 一天一次打卡：以“当日已学去重单词数”记录进度，completed 由“当天是否有学习”决定。
     * 每次评分成功后调用，保证中途退出也能及时打卡；同一天多次调用是幂等更新。
     */
    private suspend fun syncDailyCheckIn() {
        val today = DateValidator.today()
        val goal = settingsRepository.getDailyGoal()
        val learnedToday = StudyStats.summarize(studyRepository.dailyRecords(today)).wordCount
        checkInRepository.recordProgress(today, goal, learnedToday)
    }

    /** 中途离开：保留已保存的评分记录，并将会话标记为未完成。 */
    fun exitStudy(onFinished: () -> Unit = {}) {
        val session = currentSession
        if (session == null || session.completed) {
            currentSession = null
            _uiState.value = StudyUiState()
            onFinished()
            return
        }
        if (_uiState.value.isSaving) return

        viewModelScope.launch {
            runCatching {
                val updated = session.copy(
                    newCount = _uiState.value.completedCount,
                    reviewCount = 0,
                    endedAt = System.currentTimeMillis(),
                    completed = false
                )
                studyRepository.finishSession(updated)
                updated
            }.onSuccess {
                currentSession = null
                _uiState.value = StudyUiState()
                onFinished()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message ?: "退出学习失败")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class StudyUiState(
    val selectedBook: BookWithWordCount? = null,
    val words: List<Word> = emptyList(),
    val currentIndex: Int = 0,
    val sessionId: Long? = null,
    val counts: Map<String, Int> = emptyMap(),
    val isFlipped: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val completed: Boolean = false,
    val error: String? = null
) {
    val currentWord: Word? get() = words.getOrNull(currentIndex)
    val completedCount: Int get() = counts.values.sum()
    val totalCount: Int get() = words.size
}
