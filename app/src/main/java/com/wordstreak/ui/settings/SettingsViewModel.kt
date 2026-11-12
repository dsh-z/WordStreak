package com.wordstreak.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordstreak.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 设置 ViewModel：每日目标读取与保存。 */
class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private val _dailyGoal = MutableStateFlow(SettingsRepository.DEFAULT_DAILY_GOAL)
    val dailyGoal: StateFlow<Int> = _dailyGoal.asStateFlow()

    init {
        viewModelScope.launch { _dailyGoal.value = settingsRepository.getDailyGoal() }
    }

    fun saveDailyGoal(input: String, onError: (String) -> Unit, onSaved: () -> Unit) {
        val value = input.trim().toIntOrNull()
        if (value == null || value !in 1..999) {
            onError("每日目标需为 1–999 的整数")
            return
        }
        viewModelScope.launch {
            runCatching { settingsRepository.setDailyGoal(value) }
                .onSuccess { _dailyGoal.value = value; onSaved() }
                .onFailure { onError(it.message ?: "保存每日目标失败") }
        }
    }
}
