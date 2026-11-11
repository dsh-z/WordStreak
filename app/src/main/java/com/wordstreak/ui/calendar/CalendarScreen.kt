package com.wordstreak.ui.calendar

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wordstreak.ui.components.EmptyState
import com.wordstreak.ui.components.ScreenScaffold

@Composable
fun CalendarScreen() {
    ScreenScaffold(title = "日历与统计") { innerPadding ->
        EmptyState(
            icon = Icons.Filled.DateRange,
            title = "暂无学习数据",
            subtitle = "完成学习后，月度日历、每日统计与连续打卡记录会显示在这里（阶段 5）。",
            modifier = Modifier.padding(innerPadding)
        )
    }
}
