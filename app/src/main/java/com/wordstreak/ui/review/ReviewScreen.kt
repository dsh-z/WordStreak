package com.wordstreak.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wordstreak.di.AppContainer
import com.wordstreak.ui.components.ScreenScaffold
import com.wordstreak.ui.study.StudyCard

/**
 * 复习页面：待复习单词卡片（复用 StudyCard：拼写→翻面显示音标/词性/释义/例句）、
 * AGAIN/HARD/GOOD/EASY 评分、待复习与已完成计数、空状态。
 */
@Composable
fun ReviewScreen(onBack: () -> Unit) {
    val factory = remember {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ReviewViewModel(AppContainer.reviewRepository) as T
        }
    }
    val vm: ReviewViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsState()
    val word = state.currentWord

    ScreenScaffold(
        title = "复习",
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "返回") } }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("待复习：${state.remaining}", style = MaterialTheme.typography.titleMedium)
                Text("已完成：${state.completedCount} / ${state.total}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            when {
                state.isLoading -> {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text("正在加载待复习单词…")
                    }
                }
                state.empty -> {
                    Text("暂时没有需要复习的单词。去「词库」添加单词或「学习」后再来吧。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                state.finished && word == null -> {
                    Text("本轮复习完成！共复习 ${state.completedCount} 个单词。",
                        style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { vm.load() }) { Text("重新加载待复习") }
                }
                word != null -> {
                    StudyCard(word = word, flipped = state.flipped, onFlip = vm::flip)
                    Text("点击卡片翻面查看释义", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.rate("AGAIN") }, enabled = !state.isSaving, modifier = Modifier.weight(1f)) { Text("Again") }
                        OutlinedButton(onClick = { vm.rate("HARD") }, enabled = !state.isSaving, modifier = Modifier.weight(1f)) { Text("Hard") }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.rate("GOOD") }, enabled = !state.isSaving, modifier = Modifier.weight(1f)) { Text("Good") }
                        Button(onClick = { vm.rate("EASY") }, enabled = !state.isSaving, modifier = Modifier.weight(1f)) { Text("Easy") }
                    }
                    if (state.isSaving) Text("正在保存…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
