package com.wordstreak.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wordstreak.di.AppContainer
import com.wordstreak.domain.usecase.DailySummary
import com.wordstreak.ui.components.ScreenScaffold
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen() {
    val factory = remember {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CalendarViewModel(AppContainer.studyRepository, AppContainer.checkInRepository) as T
        }
    }
    val vm: CalendarViewModel = viewModel(factory = factory)
    val month by vm.month.collectAsState()
    val active by vm.activeDates.collectAsState()
    val selected by vm.selectedDate.collectAsState()
    val summary by vm.dailySummary.collectAsState()
    val currentStreak by vm.currentStreak.collectAsState()
    val longestStreak by vm.longestStreak.collectAsState()
    val today = remember { LocalDate.now().toString() }

    ScreenScaffold(title = "日历与统计") { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MonthHeader(
                month = month,
                onPrev = vm::previousMonth,
                onNext = vm::nextMonth
            )
            WeekdayHeader()
            MonthGrid(
                month = month,
                active = active,
                selected = selected,
                today = today,
                onSelect = vm::selectDate
            )
            Text(
                text = if (active.isEmpty()) "本月还没有学习记录，完成学习后会在这里显示。"
                else "本月学习 ${active.size} 天",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DailySummaryCard(selected, summary)
            StreakCard(currentStreak, longestStreak)
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("‹", style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.clickable { onPrev() }.padding(horizontal = 16.dp, vertical = 4.dp))
        Text(
            "${month.year}年${month.monthValue}月",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text("›", style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.clickable { onNext() }.padding(horizontal = 16.dp, vertical = 4.dp))
    }
}

@Composable
private fun WeekdayHeader() {
    Row(Modifier.fillMaxWidth()) {
        listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
            Text(
                label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    active: Set<String>,
    selected: String,
    today: String,
    onSelect: (String) -> Unit
) {
    val leading = month.atDay(1).dayOfWeek.value - 1 // 周一为 0
    val daysInMonth = month.lengthOfMonth()
    val cells = buildList<Int?> {
        repeat(leading) { add(null) }
        for (d in 1..daysInMonth) add(d)
        while (size % 7 != 0) add(null)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { day ->
                    if (day == null) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = month.atDay(day).toString()
                        DayCell(
                            modifier = Modifier.weight(1f),
                            day = day,
                            isActive = active.contains(date),
                            isToday = date == today,
                            isSelected = date == selected,
                            onClick = { onSelect(date) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    modifier: Modifier,
    day: Int,
    isActive: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else -> Color.Transparent
    }
    val borderModifier = if (isToday && !isSelected)
        Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
    else Modifier
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .then(borderModifier)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
            fontWeight = if (isActive || isToday) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun DailySummaryCard(date: String, summary: DailySummary) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("当日统计 · $date", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (!summary.hasData) {
                Text("当日没有学习记录。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("学习单词：${summary.wordCount} 个")
                Text("学习次数：${summary.recordCount} 次")
                Text("答对 ${summary.correctCount} · 答错 ${summary.incorrectCount}")
            }
        }
    }
}

@Composable
private fun StreakCard(current: Int, longest: Int) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("连续打卡", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                StreakStat("当前连续", current)
                StreakStat("历史最长", longest)
            }
        }
    }
}

@Composable
private fun StreakStat(label: String, value: Int) {
    Column {
        Text("$value 天", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
