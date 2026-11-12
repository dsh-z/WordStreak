package com.wordstreak.domain.usecase

import com.wordstreak.data.local.entity.StudyRecord

/** 单日学习汇总（来自真实 study_records 计算）。 */
data class DailySummary(
    val wordCount: Int,
    val recordCount: Int,
    val correctCount: Int,
    val incorrectCount: Int
) {
    val hasData: Boolean get() = recordCount > 0
}

/** 每日统计计算（纯函数，便于单测）。 */
object StudyStats {

    fun summarize(records: List<StudyRecord>): DailySummary {
        if (records.isEmpty()) return DailySummary(0, 0, 0, 0)
        // 学习单词数：按 wordId 去重；单词已删除时用快照拼写替代。
        val wordKeys = records.map { it.wordId ?: it.wordSpelling ?: it.id }.distinct()
        val correct = records.count { it.isCorrect }
        return DailySummary(
            wordCount = wordKeys.size,
            recordCount = records.size,
            correctCount = correct,
            incorrectCount = records.size - correct
        )
    }
}
