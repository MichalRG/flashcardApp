package com.krzyzowski.vocabboost.data

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DailySeconds private constructor(private val seconds: Map<LocalDate, Long>) {

    fun secondsOn(day: LocalDate): Long = seconds[day] ?: 0

    fun add(from: Instant, to: Instant, zone: ZoneId): DailySeconds {
        if (!to.isAfter(from)) return this
        val updated = seconds.toMutableMap()
        var start = from
        while (start.isBefore(to)) {
            val day = start.atZone(zone).toLocalDate()
            val end = minOf(to, day.plusDays(1).atStartOfDay(zone).toInstant())
            updated[day] = (updated[day] ?: 0) + Duration.between(start, end).plusMillis(500).seconds
            start = end
        }
        val oldestKept = to.atZone(zone).toLocalDate().minusDays(RETENTION_DAYS - 1)
        return DailySeconds(updated.filterKeys { !it.isBefore(oldestKept) })
    }

    fun encode(): String = seconds.entries
        .sortedBy { it.key }
        .joinToString(",") { "${it.key}=${it.value}" }

    companion object {
        const val RETENTION_DAYS = 7L

        val NONE = DailySeconds(emptyMap())

        fun decode(encoded: String): DailySeconds = DailySeconds(
            encoded.split(',')
                .filter { it.isNotBlank() }
                .mapNotNull { entry ->
                    runCatching {
                        val (day, seconds) = entry.split('=')
                        LocalDate.parse(day) to seconds.toLong()
                    }.getOrNull()
                }
                .toMap(),
        )
    }
}
