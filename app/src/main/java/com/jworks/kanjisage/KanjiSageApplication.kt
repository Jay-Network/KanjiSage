package com.jworks.kanjisage

import android.app.Application
import android.util.Log
import com.jworks.kanjisage.data.nlp.KuromojiTokenizer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KanjiSageApplication : Application() {

    @Inject
    lateinit var kuromojiTokenizer: KuromojiTokenizer

    override fun onCreate() {
        super.onCreate()

        // Global crash handler — logs unhandled exceptions before the process dies
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("KanjiSage", "Uncaught exception on ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Initialize Kuromoji on background thread (takes ~1-3 seconds)
        Thread { kuromojiTokenizer.initialize() }.start()
    }
}
