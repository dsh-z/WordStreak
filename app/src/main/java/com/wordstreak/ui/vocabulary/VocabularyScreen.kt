package com.wordstreak.ui.vocabulary

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wordstreak.ui.components.EmptyState
import com.wordstreak.ui.components.ScreenScaffold

@Composable
fun VocabularyScreen() {
    ScreenScaffold(title = "词库") { innerPadding ->
        EmptyState(
            icon = Icons.Filled.List,
            title = "还没有词库",
            subtitle = "阶段 3 将支持创建、编辑、删除词库，以及单词增删改查、搜索、收藏与 CSV 导入导出。",
            modifier = Modifier.padding(innerPadding)
        )
    }
}
