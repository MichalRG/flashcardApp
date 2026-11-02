package com.krzyzowski.vocabboost.data

import com.krzyzowski.vocabboost.schedule.DirectionalDifficulty
import com.krzyzowski.vocabboost.schedule.DirectionalProgress
import com.krzyzowski.vocabboost.schedule.WordDifficulty
import com.krzyzowski.vocabboost.schedule.WordProgress
import com.krzyzowski.vocabboost.session.CardDirection

// Existing single-direction persistence fixtures exercise EN -> PL.
internal suspend fun ProgressStore.recordAnswer(id: String, knew: Boolean) =
    recordAnswer(id, CardDirection.ENGLISH_FIRST, knew)

internal fun DirectionalProgress.of(id: String): WordProgress? = of(id, CardDirection.ENGLISH_FIRST)

internal fun DirectionalDifficulty.of(id: String): WordDifficulty? = of(id, CardDirection.ENGLISH_FIRST)

internal fun DirectionalDifficulty.isDifficult(id: String): Boolean = isDifficult(id, CardDirection.ENGLISH_FIRST)
