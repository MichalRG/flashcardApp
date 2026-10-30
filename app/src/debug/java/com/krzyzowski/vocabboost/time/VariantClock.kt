package com.krzyzowski.vocabboost.time

import android.content.Context
import com.krzyzowski.vocabboost.debug.DebugTimeOffset

object VariantClock {
    fun create(context: Context): AppClock = DeviceClock(DebugTimeOffset(context)::current)
}
