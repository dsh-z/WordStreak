package com.wordstreak.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordstreak.data.local.converter.DateValidator
import com.wordstreak.data.repository.CheckInRepository
import com.wordstreak.data.repository.ReviewRepository
import com.wordstreak.data.repository.SettingsRepository
import com.wordstreak.data.repository.StudyRepository
import com.wordstreak.domain.usecase.DailySummary
import com.wordstreak.domain.usecase.StudyStats
import com.wordstreak.domain.usecase.StudyStreak
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * 首页真实数据：今日学习进度、连续打卡天数、每日目标、待复习数。
 * 全部来自真实数据库（study_records / check_ins / user_settings / words+review_states）。
 */
class HomeViewModel(
    studyRepository: StudyRepository,
    checkInRepository: CheckInRepository,
    settingsRepository: SettingsRepository,
    reviewRepository: ReviewRepository
) : ViewModel() {

    private val today: String = DateValidator.today()

    /** 今日学习汇总（wordCount 为当日去重单词数，用于进度分子）。 */
    val dailySummary: StateFlow<DailySummary> =
        studyRepository.observeDailyRecords(today)
            .map { StudyStats.summarize(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DailySummary(0, 0, 0, 0))

    /** 每日目标（默认 20，1–999）。 */
    val dailyGoal: StateFlow<Int> =
        settingsRepository.observeAll()
            .map { list ->
                list.firstOrNull { it.key == SettingsRepository.Keys.DAILY_GOAL }
                    ?.value?.toIntOrNull()?.coerceIn(1, 999)
                    ?: SettingsRepository.DEFAULT_DAILY_GOAL
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_DAILY_GOAL)

    /** 当前连续打卡天数（当天有学习即打卡）。 */
    val currentStreak: StateFlow<Int> =
        checkInRepository.observeCompletedDates()
            .map { StudyStreak.currentStreak(it, today) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** 待复习单词数（新词 + 到期），随复习状态实时刷新。 */
    val dueCount: StateFlow<Int> =
        reviewRepository.observeDueCount(today)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
