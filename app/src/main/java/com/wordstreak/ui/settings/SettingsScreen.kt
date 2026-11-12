package com.wordstreak.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wordstreak.di.AppContainer
import com.wordstreak.ui.components.ScreenScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val factory = remember {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(AppContainer.settingsRepository) as T
        }
    }
    val vm: SettingsViewModel = viewModel(factory = factory)
    val goal by vm.dailyGoal.collectAsState()

    var showEditor by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(title = "设置") { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            ListItem(
                headlineContent = { Text("每日目标") },
                supportingContent = { Text("$goal 个单词 / 天（仅用于首页进度展示）") },
                trailingContent = {
                    TextButton(onClick = { input = goal.toString(); error = null; showEditor = true }) { Text("修改") }
                },
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            ListItem(
                headlineContent = { Text("词库与数据") },
                supportingContent = { Text("CSV 导入与导出将在后续阶段开放") },
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

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text("设置每日目标") },
            text = {
                Column {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { it2 -> input = it2.filter { ch -> ch.isDigit() } },
                        label = { Text("每日目标（1–999）") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.saveDailyGoal(input, onError = { error = it }, onSaved = { showEditor = false })
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("取消") } }
        )
    }
}
