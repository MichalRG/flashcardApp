package com.krzyzowski.vocabboost.debug

import android.content.Context
import androidx.core.content.edit
import java.time.Duration

class DebugTimeOffset(context: Context) {
    private val prefs = context.getSharedPreferences("debug_clock", Context.MODE_PRIVATE)

    fun current(): Duration = Duration.ofMinutes(prefs.getLong(OFFSET_MINUTES, 0))

    fun set(minutes: Long) {
        prefs.edit(commit = true) { putLong(OFFSET_MINUTES, minutes) }
    }

    private companion object {
        const val OFFSET_MINUTES = "offset_minutes"
    }
}
