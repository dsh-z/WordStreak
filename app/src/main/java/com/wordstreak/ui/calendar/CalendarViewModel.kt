package com.wordstreak.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordstreak.data.repository.CheckInRepository
import com.wordstreak.data.repository.StudyRepository
import com.wordstreak.domain.usecase.DailySummary
import com.wordstreak.domain.usecase.StudyStats
import com.wordstreak.domain.usecase.StudyStreak
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

/**
 * 学习日历与统计 ViewModel：数据全部来自真实数据库（study_records / check_ins），
 * 通过 Flow 在数据库变化后实时刷新。
 */
class CalendarViewModel(
    private val studyRepository: StudyRepository,
    private val checkInRepository: CheckInRepository
) : ViewModel() {

    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month

    private val _selectedDate = MutableStateFlow(LocalDate.now().toString())
    val selectedDate: StateFlow<String> = _selectedDate

    /** 当前月内有学习记录的日期集合（用于日历打点）。 */
    val activeDates: StateFlow<Set<String>> = _month
        .flatMapLatest { ym ->
            studyRepository.observeActiveDatesBetween(
                ym.atDay(1).toString(),
                ym.atEndOfMonth().toString()
            )
        }
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** 选中日期的学习汇总。 */
    val dailySummary: StateFlow<DailySummary> = _selectedDate
        .flatMapLatest { date -> studyRepository.observeDailyRecords(date) }
        .map { StudyStats.summarize(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DailySummary(0, 0, 0, 0))

    private val completedDates: StateFlow<List<String>> =
        checkInRepository.observeCompletedDates()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentStreak: StateFlow<Int> = completedDates
        .map { StudyStreak.currentStreak(it, LocalDate.now().toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val longestStreak: StateFlow<Int> = completedDates
        .map { StudyStreak.longestStreak(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun previousMonth() { _month.value = _month.value.minusMonths(1) }
    fun nextMonth() { _month.value = _month.value.plusMonths(1) }
    fun selectDate(date: String) { _selectedDate.value = date }
}
