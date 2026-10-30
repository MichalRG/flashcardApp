package com.krzyzowski.vocabboost.ui.testmode

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.krzyzowski.vocabboost.VocabBoostApp
import com.krzyzowski.vocabboost.schedule.Workload
import com.krzyzowski.vocabboost.session.TestSession
import com.krzyzowski.vocabboost.session.TestTimer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

class TestModeViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as VocabBoostApp).container
    private val clock = container.clock
    private val _session = MutableStateFlow<TestSession?>(null)
    private val _timer = MutableStateFlow(TestTimer())
    private val _now = MutableStateFlow(clock.now())
    private var saving: Job? = null

    val session: StateFlow<TestSession?> = _session.asStateFlow()
    val timer: StateFlow<TestTimer> = _timer.asStateFlow()
    val now: StateFlow<Instant> = _now.asStateFlow()

    var isOpen = false
        private set

    fun start() {
        isOpen = true
        _session.value = null
        _timer.value = TestTimer()
        viewModelScope.launch {
            val deck = container.progressStore.deck.first()
            val workload = Workload.of(container.words, deck, clock.today(), container.settings.newWordsPerDay.first())
            _session.value = TestSession(container.testDeckBuilder.build(workload, deck))
        }
    }

    fun shown() {
        _timer.update { it.shown(clock.now()) }
        tick()
    }

    fun hidden() {
        _timer.update { it.hidden(clock.now()) }
    }

    fun tick() {
        _now.value = clock.now()
    }

    fun answer(knew: Boolean) {
        if (saving?.isActive == true) return
        val session = _session.value ?: return
        val card = session.current ?: return
        saving = viewModelScope.launch {
            container.progressStore.recordAnswer(card.word.id, knew)
            _session.value = session.answer(knew, timeUp = _timer.value.timeUp(clock.now()))
        }
    }

    fun end() {
        _session.update { it?.end() }
    }

    fun close() {
        isOpen = false
        _session.value = null
    }
}
