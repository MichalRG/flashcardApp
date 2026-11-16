package com.krzyzowski.vocabboost.ui.testmode

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.krzyzowski.vocabboost.VocabBoostApp
import com.krzyzowski.vocabboost.schedule.DirectionalDifficulty
import com.krzyzowski.vocabboost.schedule.DirectionalWorkload
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
    private val _state = MutableStateFlow(TestModeState())
    private val _timer = MutableStateFlow(TestTimer())
    private val _now = MutableStateFlow(clock.now())
    private var saving: Job? = null

    /** Bumped on every start()/close() so a save from an earlier session never publishes into a later one. */
    private var generation = 0

    /**
     * The session together with the difficulty as of its last recorded answer. Both live in one StateFlow so the
     * screen never sees a new difficulty with an old card (or the reverse) and gates typed recall consistently.
     * difficulty is null until loaded, so typed-recall gating is never guessed.
     */
    val state: StateFlow<TestModeState> = _state.asStateFlow()
    val timer: StateFlow<TestTimer> = _timer.asStateFlow()
    val now: StateFlow<Instant> = _now.asStateFlow()

    var isOpen = false
        private set

    fun start() {
        isOpen = true
        val startGeneration = ++generation
        val pendingSave = saving
        _state.value = TestModeState()
        _timer.value = TestTimer()
        viewModelScope.launch {
            // Let an answer from a previous session finish writing first, so this session reads fresh data.
            pendingSave?.join()
            val deck = container.progressStore.deck.first()
            val difficulty = container.progressStore.difficulty.first()
            val mode = container.settings.testMode.first()
            val workload = DirectionalWorkload.of(
                container.words, deck, clock.today(), container.settings.newWordsPerDay.first(), mode,
            )
            if (isOpen && generation == startGeneration) {
                _state.value = TestModeState(TestSession.start(workload.cards), difficulty)
            }
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
        val session = _state.value.session ?: return
        if (session.awaitingTimeChoice) return
        val card = session.current ?: return
        val answerGeneration = generation
        saving = viewModelScope.launch {
            container.progressStore.recordAnswer(card.word.id, card.direction, knew)
            val difficulty = container.progressStore.difficulty.first()
            val answered = session.answer(knew, timeUp = _timer.value.timeUp(clock.now()))
            _state.update { latest ->
                val current = latest.session
                when {
                    !isOpen || generation != answerGeneration -> latest
                    current === session -> TestModeState(answered, difficulty)
                    current?.stopped == true && current.cards === session.cards ->
                        TestModeState(answered.end(), difficulty)
                    else -> latest
                }
            }
        }
    }

    fun end() {
        _state.update { it.copy(session = it.session?.end()) }
    }

    fun continueLearning() {
        _state.update { it.copy(session = it.session?.continueLearning()) }
    }

    fun close() {
        isOpen = false
        generation++
        _state.value = TestModeState()
    }
}

/** What the test screen renders: the session and the difficulty its typed-recall gating is based on. */
data class TestModeState(
    val session: TestSession? = null,
    val difficulty: DirectionalDifficulty? = null,
)
