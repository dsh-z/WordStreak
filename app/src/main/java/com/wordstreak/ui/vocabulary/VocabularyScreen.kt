package com.wordstreak.ui.vocabulary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wordstreak.data.local.entity.WordBook
import com.wordstreak.di.AppContainer
import com.wordstreak.ui.components.ScreenScaffold

@Composable
fun VocabularyScreen(onOpenBook: (Long) -> Unit = {}) {
    val factory = remember {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                VocabularyViewModel(AppContainer.wordBookRepository) as T
        }
    }
    val vm: VocabularyViewModel = viewModel(factory = factory)
    val books by vm.books.collectAsState()
    var editing by remember { mutableStateOf<WordBook?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<WordBook?>(null) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(title = "词库") { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                editing = null; name = ""; description = ""; error = null; showEditor = true
            }, modifier = Modifier.fillMaxWidth()) { Text("新建词库") }
            if (books.isEmpty()) {
                Text("还没有词库，创建一个词库开始整理单词。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(books, key = { it.book.id }) { item ->
                        Card(Modifier.fillMaxWidth().clickable { onOpenBook(item.book.id) }) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(item.book.name, style = MaterialTheme.typography.titleMedium)
                                Text(item.book.description.ifBlank { "暂无描述" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${item.wordCount} 个单词", style = MaterialTheme.typography.labelMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = {
                                        editing = item.book; name = item.book.name
                                        description = item.book.description; error = null; showEditor = true
                                    }) { Text("编辑") }
                                    TextButton(onClick = { deleting = item.book }) { Text("删除") }
                                    TextButton(onClick = { onOpenBook(item.book.id) }) { Text("查看单词") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editing == null) "新建词库" else "编辑词库") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("词库名称") }, singleLine = true)
                    OutlinedTextField(description, { description = it }, label = { Text("描述（可选）") })
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.save(editing, name, description, onError = { error = it }, onSaved = { showEditor = false })
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("取消") } }
        )
    }

    deleting?.let { book ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除词库？") },
            text = { Text("删除「${book.name}」会同时删除其中的单词及其 ReviewState。已有学习历史记录及拼写、释义快照会保留。此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = { vm.delete(book) { error = it }; deleting = null }) { Text("确认删除") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } }
        )
    }
}
