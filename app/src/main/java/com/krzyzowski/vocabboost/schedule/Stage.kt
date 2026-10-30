package com.krzyzowski.vocabboost.schedule

enum class Stage(val code: String, val intervalDays: Long?, val reversible: Boolean) {
    S1("S1", 1, false),
    S3("S3", 3, false),
    S7("S7", 7, true),
    S14("S14", 14, true),
    S30("S30", 30, true),
    C60("C60", 60, true),
    C90("C90", 90, true),
    RETIRED("RET", null, false);

    val next: Stage
        get() = entries.getOrElse(ordinal + 1) { RETIRED }

    companion object {
        fun fromCode(code: String): Stage = entries.first { it.code == code }
    }
}
