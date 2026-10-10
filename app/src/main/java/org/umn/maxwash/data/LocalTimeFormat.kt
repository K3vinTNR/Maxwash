package org.umn.maxwash.data

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object LocalTimeFormat {
    val zone: ZoneId = ZoneId.of("Asia/Jakarta")
    private val locale = Locale.forLanguageTag("id-ID")
    fun display(timestamp: Long, includeYear: Boolean = false): String =
        DateTimeFormatter.ofPattern(if (includeYear) "d MMM yyyy '·' HH:mm 'WIB'" else "d MMM '·' HH:mm 'WIB'", locale)
            .withZone(zone).format(Instant.ofEpochMilli(timestamp))

    fun isThisMonth(timestamp: Long, now: Long = System.currentTimeMillis()) =
        YearMonth.from(Instant.ofEpochMilli(timestamp).atZone(zone)) == YearMonth.from(Instant.ofEpochMilli(now).atZone(zone))
}
