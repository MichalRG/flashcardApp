package com.krzyzowski.vocabboost

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.krzyzowski.vocabboost.ui.VocabBoostRoot
import com.krzyzowski.vocabboost.ui.VocabBoostTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val container by lazy { (application as VocabBoostApp).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { container.screenTimeTracker.track() }
        }
        setContent {
            VocabBoostTheme {
                VocabBoostRoot()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        container.reminderScheduler.armAll()
    }
}
