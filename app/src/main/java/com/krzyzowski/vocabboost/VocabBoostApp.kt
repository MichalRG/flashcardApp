package com.krzyzowski.vocabboost

import android.app.Application

class VocabBoostApp : Application() {
    val container by lazy { AppContainer(this) }
}
