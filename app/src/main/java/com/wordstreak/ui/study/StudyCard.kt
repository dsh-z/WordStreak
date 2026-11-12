package com.wordstreak.ui.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wordstreak.data.local.entity.Word

@Composable
fun StudyCard(word: Word, flipped: Boolean, onFlip: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onFlip)) {
        Column(Modifier.fillMaxWidth().heightIn(min = 260.dp).padding(22.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            if (!flipped) {
                Text(word.spelling, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                Text("点击卡片查看释义", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                word.phonetic?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                word.partOfSpeech?.takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text(word.meaning, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                word.example?.takeIf { it.isNotBlank() }?.let { Text(it, Modifier.padding(top = 14.dp), textAlign = TextAlign.Center) }
                word.exampleTranslation?.takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) }
                Text("点击卡片返回单词", Modifier.padding(top = 14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
