package com.wordstreak.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wordstreak.ui.components.ScreenScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    ScreenScaffold(title = "设置") { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            ListItem(
                headlineContent = { Text("每日目标") },
                supportingContent = { Text("阶段 5 开放自定义每日学习目标") },
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            ListItem(
                headlineContent = { Text("词库与数据") },
                supportingContent = { Text("阶段 3 支持 CSV 导入与导出") },
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            ListItem(
                headlineContent = { Text("关于 WordStreak") },
                supportingContent = { Text("版本 0.1.0 · 离线优先") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
