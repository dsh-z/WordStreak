package com.wordstreak.ui.study

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wordstreak.ui.components.EmptyState
import com.wordstreak.ui.components.ScreenScaffold

@Composable
fun StudyScreen() {
    ScreenScaffold(title = "学习") { innerPadding ->
        EmptyState(
            icon = Icons.Filled.PlayArrow,
            title = "开始一次学习",
            subtitle = "阶段 4 将实现翻转卡片、音标、释义、例句与认识程度评分（AGAIN / HARD / GOOD / EASY）。",
            modifier = Modifier.padding(innerPadding)
        )
    }
}
