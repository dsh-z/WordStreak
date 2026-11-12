package com.wordstreak.domain.usecase

import java.time.LocalDate

/** 连续打卡计算（纯函数，便于单测）。日期均为 ISO yyyy-MM-dd 字符串。 */
object StudyStreak {

    /** 历史最长连续天数。 */
    fun longestStreak(dates: Collection<String>): Int {
        val days = parse(dates)
        if (days.isEmpty()) return 0
        val sorted = days.sorted()
        var best = 1
        var current = 1
        for (i in 1 until sorted.size) {
            current = when {
                sorted[i] == sorted[i - 1].plusDays(1) -> current + 1
                sorted[i] == sorted[i - 1] -> current
                else -> 1
            }
            if (current > best) best = current
        }
        return best
    }

    /**
     * 当前连续天数：从 `today` 向前回溯连续打卡；
     * 若今天尚未打卡但昨天已打卡，则从昨天起算（连续仍在延续）；否则为 0。
     */
    fun currentStreak(dates: Collection<String>, today: String): Int {
        val set = parse(dates)
        if (set.isEmpty()) return 0
        val todayDate = runCatching { LocalDate.parse(today) }.getOrNull() ?: return 0
        var anchor = when {
            set.contains(todayDate) -> todayDate
            set.contains(todayDate.minusDays(1)) -> todayDate.minusDays(1)
            else -> return 0
        }
        var count = 0
        while (set.contains(anchor)) {
            count++
            anchor = anchor.minusDays(1)
        }
        return count
    }

    private fun parse(dates: Collection<String>): Set<LocalDate> =
        dates.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
}
