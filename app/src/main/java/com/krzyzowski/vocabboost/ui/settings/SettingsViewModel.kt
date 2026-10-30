package com.krzyzowski.vocabboost.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.krzyzowski.vocabboost.VocabBoostApp
import com.krzyzowski.vocabboost.data.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = (application as VocabBoostApp).container.settings

    val newWordsPerDay: StateFlow<Int> = settings.newWordsPerDay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsStore.DEFAULT_NEW_WORDS_PER_DAY)

    val dailyGoalMinutes: StateFlow<Int> = settings.dailyGoalMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsStore.DEFAULT_DAILY_GOAL_MINUTES)

    fun setNewWordsPerDay(count: Int) {
        viewModelScope.launch { settings.setNewWordsPerDay(count) }
    }

    fun setDailyGoalMinutes(minutes: Int) {
        viewModelScope.launch { settings.setDailyGoalMinutes(minutes) }
    }
}
