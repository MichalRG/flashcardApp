package com.krzyzowski.vocabboost.schedule

enum class Stage(val code: String, val intervalDays: Long) {
    S1("S1", 1),
    S3("S3", 3),
    S7("S7", 7),
    S14("S14", 14),
    S30("S30", 30),
    C60("C60", 60),
    C90("C90", 90),
    C180("C180", 180),
    C365("C365", 365),
    // Retain the stored code so legacy retired records become maintenance without losing progress.
    MAINTENANCE("RET", 365);

    val next: Stage
        get() = entries.getOrElse(ordinal + 1) { MAINTENANCE }

    companion object {
        fun fromCode(code: String): Stage = entries.first { it.code == code }
    }
}
