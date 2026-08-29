package com.nocturne.music.core.di

import com.nocturne.music.data.remote.innertube.InnerTubeService
import com.nocturne.music.data.remote.innertube.StreamResolver
import com.nocturne.music.data.remote.lyrics.LyricsService
import com.nocturne.music.data.repository.MusicRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
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

val serviceModule = module {
    single { InnerTubeService(httpClient = get(), json = get()) }
    single { StreamResolver(innerTubeService = get()) }
    single { LyricsService(httpClient = get(), json = get()) }
    single { MusicRepository(innerTubeService = get(), streamResolver = get(), lyricsService = get()) }
}

val appModules = listOf(
    networkModule,
    serviceModule
)
