package com.wordstreak.domain.usecase

import java.time.LocalDate
import kotlin.math.ceil

/**
 * 基础复习调度（纯函数，便于单测）。复用 AGAIN/HARD/GOOD/EASY 评分。
 *
 * Stage 3.4：基于历史间隔的动态计算（基础 SM-2-lite，确定性）：
 *   AGAIN = 1 天（重置）
 *   HARD  = max(2, ceil(prev * 1.2))
 *   GOOD  = max(4, prev * 2)
 *   EASY  = max(7, prev * 3)
 *   统一上限 MAX_INTERVAL_DAYS = 365 天。
 * 首次复习（prev = 0）时结果恰为 1 / 2 / 4 / 7，与旧固定映射一致（向后兼容）。
 */
object ReviewScheduler {
    val RESULTS = setOf("AGAIN", "HARD", "GOOD", "EASY")
    const val MAX_INTERVAL_DAYS = 365

    /** 兼容旧调用：无历史间隔时按 prev = 0 计算（结果同旧固定映射）。 */
    fun nextIntervalDays(result: String): Int = nextIntervalDays(result, 0)

    /**
     * 基于历史间隔 previousIntervalDays 计算下次间隔天数。
     * @param previousIntervalDays 该单词上次的间隔（来自 ReviewState.intervalDays），首次为 0。
     */
    fun nextIntervalDays(result: String, previousIntervalDays: Int): Int {
        val prev = previousIntervalDays.coerceAtLeast(0)
        val raw = when (result) {
            "AGAIN" -> 1
            "HARD" -> maxOf(2, ceil(prev * 1.2).toInt())
            "GOOD" -> maxOf(4, prev * 2)
            "EASY" -> maxOf(7, prev * 3)
            else -> throw IllegalArgumentException("不支持的评分：$result，应为 $RESULTS")
        }
        return raw.coerceIn(1, MAX_INTERVAL_DAYS)
    }

    /** 兼容旧调用：按首次（prev = 0）计算到期日。 */
    fun nextDueDate(result: String, fromDate: String): String = nextDueDate(result, fromDate, 0)

    /** 由“评分 + 复习基准日 + 历史间隔”计算下次到期日（ISO yyyy-MM-dd）。 */
    fun nextDueDate(result: String, fromDate: String, previousIntervalDays: Int): String {
        val base = LocalDate.parse(fromDate)
        return base.plusDays(nextIntervalDays(result, previousIntervalDays).toLong()).toString()
    }
}
