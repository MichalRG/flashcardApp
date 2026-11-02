package com.krzyzowski.vocabboost.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.krzyzowski.vocabboost.VocabBoostApp
import com.krzyzowski.vocabboost.data.DailySeconds
import com.krzyzowski.vocabboost.data.SettingsStore
import com.krzyzowski.vocabboost.data.Streak
import com.krzyzowski.vocabboost.schedule.Workload
import com.krzyzowski.vocabboost.session.TestMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as VocabBoostApp).container
    private val today = MutableStateFlow(container.clock.today())

    val day: StateFlow<LocalDate> = today.asStateFlow()

    val dailySeconds: StateFlow<DailySeconds> = container.screenTimeLog.dailySeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DailySeconds.NONE)

    val streak: StateFlow<Streak> = container.screenTimeLog.streak
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Streak.NONE)

    val goalMinutes: StateFlow<Int> = container.settings.dailyGoalMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsStore.DEFAULT_DAILY_GOAL_MINUTES)

    val workload: StateFlow<Workload?> =
        combine(container.progressStore.deck, container.settings.newWordsPerDay, today) { deck, newWordsPerDay, day ->
            Workload.of(container.words, deck, day, newWordsPerDay)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val notificationsAsked: StateFlow<Boolean?> = container.settings.notificationsAsked
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val testMode: StateFlow<TestMode?> = container.settings.testMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun refreshDay() {
        today.value = container.clock.today()
    }

    fun markNotificationsAsked() {
        viewModelScope.launch { container.settings.markNotificationsAsked() }
    }

    fun setTestMode(mode: TestMode) {
        viewModelScope.launch { container.settings.setTestMode(mode) }
    }
}
