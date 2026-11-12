package com.wordstreak.domain.usecase

import java.time.LocalDate

/**
 * 基础复习调度（纯函数，便于单测）。复用 AGAIN/HARD/GOOD/EASY 评分。
 * 规则（基础版，固定间隔，非 FSRS）：
 *   AGAIN -> 1 天；HARD -> 2 天；GOOD -> 4 天；EASY -> 7 天。
 */
object ReviewScheduler {
    val RESULTS = setOf("AGAIN", "HARD", "GOOD", "EASY")

    fun nextIntervalDays(result: String): Int = when (result) {
        "AGAIN" -> 1
        "HARD" -> 2
        "GOOD" -> 4
        "EASY" -> 7
        else -> throw IllegalArgumentException("不支持的评分：$result，应为 $RESULTS")
    }

    /** 由“评分 + 复习基准日”计算下次到期日（ISO yyyy-MM-dd）。 */
    fun nextDueDate(result: String, fromDate: String): String {
        val base = LocalDate.parse(fromDate)
        return base.plusDays(nextIntervalDays(result).toLong()).toString()
    }
}
