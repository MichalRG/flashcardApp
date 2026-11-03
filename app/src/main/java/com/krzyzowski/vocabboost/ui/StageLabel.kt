package com.krzyzowski.vocabboost.ui

import androidx.annotation.StringRes
import com.krzyzowski.vocabboost.R
import com.krzyzowski.vocabboost.schedule.Stage

object StageLabel {
    @StringRes
    fun of(stage: Stage?): Int = when (stage) {
        null -> R.string.stage_not_started
        Stage.S1 -> R.string.stage_1_day
        Stage.S3 -> R.string.stage_3_days
        Stage.S7 -> R.string.stage_7_days
        Stage.S14 -> R.string.stage_14_days
        Stage.S30 -> R.string.stage_30_days
        Stage.C60 -> R.string.stage_60_day_check
        Stage.C90 -> R.string.stage_90_day_check
        Stage.C180 -> R.string.stage_180_day_check
        Stage.C365 -> R.string.stage_365_day_check
        Stage.MAINTENANCE -> R.string.stage_maintenance
    }
}
