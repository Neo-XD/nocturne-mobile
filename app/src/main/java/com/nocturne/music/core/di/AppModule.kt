package com.nocturne.music.core.di

import androidx.media3.common.util.UnstableApi
import com.nocturne.music.data.remote.innertube.InnerTubeService
import com.nocturne.music.data.remote.innertube.StreamResolver
import com.nocturne.music.data.remote.lyrics.LyricsService
import com.nocturne.music.data.repository.MusicRepository
import com.nocturne.music.playback.AudioPlayerEngine
import com.nocturne.music.sync.SyncManager
import com.nocturne.music.ui.viewmodel.HomeViewModel
import com.nocturne.music.ui.viewmodel.PlayerViewModel
import com.nocturne.music.ui.viewmodel.SearchViewModel
import com.nocturne.music.ui.viewmodel.SyncViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val networkModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }

    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(get())
            }
            install(WebSockets)
        }
    }
}

@OptIn(UnstableApi::class)
val serviceModule = module {
    single { InnerTubeService(httpClient = get(), json = get()) }
    single { StreamResolver(innerTubeService = get()) }
    single { LyricsService(httpClient = get(), json = get()) }
    single { MusicRepository(innerTubeService = get(), streamResolver = get(), lyricsService = get()) }
    single { AudioPlayerEngine(context = get(), musicRepository = get()) }
    single { SyncManager(httpClient = get(), json = get(), audioPlayerEngine = get()) }
}

@OptIn(UnstableApi::class)
val viewModelModule = module {
    viewModel { HomeViewModel(musicRepository = get()) }
    viewModel { SearchViewModel(musicRepository = get()) }
    viewModel { PlayerViewModel(audioPlayerEngine = get(), musicRepository = get()) }
    viewModel { SyncViewModel(syncManager = get()) }
}

val appModules = listOf(
    networkModule,
    serviceModule,
    viewModelModule
)
