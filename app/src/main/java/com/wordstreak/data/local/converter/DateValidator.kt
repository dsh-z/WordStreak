package com.wordstreak.data.local.converter

import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * 日期统一采用 ISO LocalDate 字符串（yyyy-MM-dd）。
 * 数据库层不做类型转换（字段为 String），这里提供业务层的日期校验，杜绝非法日期入库。
 */
object DateValidator {

    fun isValid(isoDate: String): Boolean =
        try {
            LocalDate.parse(isoDate)
            true
        } catch (e: DateTimeParseException) {
            false
        }

    /** 非法日期直接抛出 IllegalArgumentException。 */
    fun requireValid(isoDate: String, field: String = "date"): String {
        require(isValid(isoDate)) { "非法的日期格式($field): $isoDate，应为 yyyy-MM-dd" }
        return isoDate
    }

    fun today(): String = LocalDate.now().toString()
}
