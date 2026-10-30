package com.krzyzowski.vocabboost.reminder

import java.time.Instant
import java.time.ZoneId

enum class ReminderSlot(val hour: Int, val requestCode: Int) {
    MORNING(8, 8),
    EVENING(17, 17);

    fun nextTrigger(now: Instant, zone: ZoneId): Instant {
        val today = now.atZone(zone).toLocalDate()
        val todayAt = today.atTime(hour, 0).atZone(zone).toInstant()
        return if (todayAt.isAfter(now)) todayAt else today.plusDays(1).atTime(hour, 0).atZone(zone).toInstant()
    }
}
