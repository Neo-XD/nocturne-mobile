package com.nocturne.music

import android.app.Application
import com.nocturne.music.core.di.appModules
import com.nocturne.music.data.remote.innertube.NewPipeDownloader
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.schabi.newpipe.extractor.NewPipe

class NocturneApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize NewPipe Extractor
        try {
            NewPipe.init(NewPipeDownloader.init())
        } catch (_: Exception) {}

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@NocturneApp)
            modules(appModules)
        }
    }
}
