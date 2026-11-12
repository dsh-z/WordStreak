package com.wordstreak.ui.study

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StudyResult(state: StudyUiState, onFinish: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("本次学习完成", style = MaterialTheme.typography.headlineSmall)
        Text(state.selectedBook?.book?.name ?: "词库")
        Text("完成 " + state.completedCount + " / " + state.totalCount + " 个单词")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("AGAIN", "HARD", "GOOD", "EASY").forEach { rating ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(rating)
                        Text("" + (state.counts[rating] ?: 0))
                    }
                }
            }
        }
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("完成") }
    }
}
