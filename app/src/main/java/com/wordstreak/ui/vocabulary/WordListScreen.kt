package com.wordstreak.ui.vocabulary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wordstreak.data.local.entity.Word
import com.wordstreak.di.AppContainer
import com.wordstreak.ui.components.ScreenScaffold

@Composable
fun WordListScreen(bookId: Long, onBack: () -> Unit) {
    val factory = remember(bookId) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                WordListViewModel(bookId, AppContainer.wordBookRepository, AppContainer.wordRepository) as T
        }
    }
    val vm: WordListViewModel = viewModel(key = "word-list-$bookId", factory = factory)
    val book by vm.book.collectAsState()
    val words by vm.words.collectAsState()
    val query by vm.searchQuery.collectAsState()
    var editing by remember { mutableStateOf<Word?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Word?>(null) }
    var spelling by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }
    var phonetic by remember { mutableStateOf("") }
    var partOfSpeech by remember { mutableStateOf("") }
    var example by remember { mutableStateOf("") }
    var exampleTranslation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(
        title = book?.name ?: "词库详情",
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "返回") } }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(query, vm::setQuery, modifier = Modifier.fillMaxWidth(), label = { Text("搜索拼写或释义") }, singleLine = true)
            Button(onClick = {
                editing = null; spelling = ""; meaning = ""; phonetic = ""; partOfSpeech = ""
                example = ""; exampleTranslation = ""; error = null; showEditor = true
            }, modifier = Modifier.fillMaxWidth()) { Text("添加单词") }
            if (words.isEmpty()) {
                Text(if (query.isBlank()) "这个词库还没有单词。" else "没有找到匹配的单词。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(words, key = { it.id }) { word ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(word.spelling, style = MaterialTheme.typography.titleMedium)
                                Text(word.meaning)
                                word.phonetic?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = {
                                        editing = word; spelling = word.spelling; meaning = word.meaning
                                        phonetic = word.phonetic.orEmpty(); partOfSpeech = word.partOfSpeech.orEmpty()
                                        example = word.example.orEmpty(); exampleTranslation = word.exampleTranslation.orEmpty()
                                        error = null; showEditor = true
                                    }) { Text("编辑") }
                                    TextButton(onClick = { deleting = word }) { Text("删除") }
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
            title = { Text(if (editing == null) "添加单词" else "编辑单词") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(spelling, { spelling = it }, label = { Text("拼写") }, singleLine = true)
                    OutlinedTextField(meaning, { meaning = it }, label = { Text("释义") })
                    OutlinedTextField(phonetic, { phonetic = it }, label = { Text("音标（可选）") }, singleLine = true)
                    OutlinedTextField(partOfSpeech, { partOfSpeech = it }, label = { Text("词性（可选）") }, singleLine = true)
                    OutlinedTextField(example, { example = it }, label = { Text("例句（可选）") })
                    OutlinedTextField(exampleTranslation, { exampleTranslation = it }, label = { Text("例句翻译（可选）") })
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.save(editing, spelling, meaning, phonetic, partOfSpeech, example, exampleTranslation,
                        onError = { error = it }, onSaved = { showEditor = false })
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("取消") } }
        )
    }

    deleting?.let { word ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除单词？") },
            text = { Text("删除「${word.spelling}」会清理对应 ReviewState；已有学习历史记录及单词快照会保留。") },
            confirmButton = {
                TextButton(onClick = { vm.delete(word) { error = it }; deleting = null }) { Text("确认删除") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } }
        )
    }
}
