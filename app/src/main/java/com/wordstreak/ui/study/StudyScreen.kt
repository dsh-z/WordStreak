package com.wordstreak.ui.study

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wordstreak.di.AppContainer
import com.wordstreak.ui.components.ScreenScaffold

@Composable
fun StudyScreen(onOpenReview: () -> Unit = {}) {
    val factory = remember {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                StudyViewModel(AppContainer.wordBookRepository, AppContainer.wordRepository, AppContainer.studyRepository) as T
        }
    }
    val vm: StudyViewModel = viewModel(factory = factory)
    val books by vm.books.collectAsState()
    val state by vm.uiState.collectAsState()

    ScreenScaffold(title = "学习") { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when {
                state.isLoading -> {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text("正在准备学习内容…")
                    }
                }
                state.sessionId == null -> {
                    Button(onClick = onOpenReview, modifier = Modifier.fillMaxWidth()) { Text("单词复习（按到期筛选）") }
                    Text("选择一个词库开始学习", style = MaterialTheme.typography.titleMedium)
                    if (books.isEmpty()) {
                        Text("还没有词库，请先到「词库」页面创建词库并添加单词。")
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(books, key = { it.book.id }) { item ->
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(item.book.name, style = MaterialTheme.typography.titleMedium)
                                        Text(item.book.description.ifBlank { "暂无描述" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("单词数量：" + item.wordCount)
                                        Button(onClick = { vm.startStudy(item.book.id) }, enabled = item.wordCount > 0) {
                                            Text(if (item.wordCount > 0) "开始学习" else "暂时没有单词")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                state.completed -> StudyResult(state = state, onFinish = { vm.exitStudy() })
                else -> {
                    val word = state.currentWord
                    if (word != null) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(state.selectedBook?.book?.name ?: "学习中", style = MaterialTheme.typography.titleMedium)
                            Text("" + (state.currentIndex + 1) + " / " + state.totalCount)
                        }
                        StudyCard(word = word, flipped = state.isFlipped, onFlip = vm::flipCard)
                        Text("已完成 " + state.completedCount + " / " + state.totalCount, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { vm.rate("AGAIN") }, enabled = !state.isSaving, modifier = Modifier.weight(1f)) { Text("Again") }
                            OutlinedButton(onClick = { vm.rate("HARD") }, enabled = !state.isSaving, modifier = Modifier.weight(1f)) { Text("Hard") }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.rate("GOOD") }, enabled = !state.isSaving, modifier = Modifier.weight(1f)) { Text("Good") }
                            Button(onClick = { vm.rate("EASY") }, enabled = !state.isSaving, modifier = Modifier.weight(1f)) { Text("Easy") }
                        }
                        TextButton(onClick = { vm.exitStudy() }, enabled = !state.isSaving) { Text("退出本次学习") }
                        if (state.isSaving) Text("正在保存…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
