package com.nocturne.music.data.remote.innertube

import com.nocturne.music.core.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.*

class InnerTubeService(
    private val httpClient: HttpClient,
    private val json: Json
) {
    private val baseUrl = "https://music.youtube.com/youtubei/v1"

    suspend fun searchAll(query: String): Result<SearchResults> = runCatching {
        val client = YouTubeClients.WEB_REMIX
        val contextJson = buildInnerTubeContextJson(client)
        val payload = buildJsonObject {
            put("context", contextJson)
            put("query", query)
        }

        val response = httpClient.post("$baseUrl/search?prettyPrint=false") {
            contentType(ContentType.Application.Json)
            headers {
                append(HttpHeaders.UserAgent, client.userAgent)
                append("X-YouTube-Client-Name", client.clientId ?: "67")
                append("X-YouTube-Client-Version", client.clientVersion)
                append(HttpHeaders.Origin, "https://music.youtube.com")
                append(HttpHeaders.Referrer, "https://music.youtube.com")
            }
            setBody(payload.toString())
        }

        val rawJson = json.parseToJsonElement(response.body<String>())
        InnerTubeParser.parseSearchAll(rawJson)
    }

    suspend fun getHome(params: String? = null): Result<HomePage> = runCatching {
        val client = YouTubeClients.WEB_REMIX
        val contextJson = buildInnerTubeContextJson(client)
        val payload = buildJsonObject {
            put("context", contextJson)
            put("browseId", "FEmusic_home")
            params?.let { put("params", it) }
        }

        val response = httpClient.post("$baseUrl/browse?prettyPrint=false") {
            contentType(ContentType.Application.Json)
            headers {
                append(HttpHeaders.UserAgent, client.userAgent)
                append("X-YouTube-Client-Name", client.clientId ?: "67")
                append("X-YouTube-Client-Version", client.clientVersion)
                append(HttpHeaders.Origin, "https://music.youtube.com")
                append(HttpHeaders.Referrer, "https://music.youtube.com")
            }
            setBody(payload.toString())
        }

        val rawJson = json.parseToJsonElement(response.body<String>())
        InnerTubeParser.parseHome(rawJson)
    }

    suspend fun getAlbum(browseId: String): Result<AlbumPage> = runCatching {
        val client = YouTubeClients.WEB_REMIX
        val contextJson = buildInnerTubeContextJson(client)
        val payload = buildJsonObject {
            put("context", contextJson)
            put("browseId", browseId)
        }

        val response = httpClient.post("$baseUrl/browse?prettyPrint=false") {
            contentType(ContentType.Application.Json)
            headers {
                append(HttpHeaders.UserAgent, client.userAgent)
                append("X-YouTube-Client-Name", client.clientId ?: "67")
                append("X-YouTube-Client-Version", client.clientVersion)
            }
            setBody(payload.toString())
        }

        val rawJson = json.parseToJsonElement(response.body<String>())
        InnerTubeParser.parseAlbum(rawJson)
    }

    suspend fun getArtist(browseId: String): Result<ArtistPage> = runCatching {
        val client = YouTubeClients.WEB_REMIX
        val contextJson = buildInnerTubeContextJson(client)
        val payload = buildJsonObject {
            put("context", contextJson)
            put("browseId", browseId)
        }

        val response = httpClient.post("$baseUrl/browse?prettyPrint=false") {
            contentType(ContentType.Application.Json)
            headers {
                append(HttpHeaders.UserAgent, client.userAgent)
                append("X-YouTube-Client-Name", client.clientId ?: "67")
                append("X-YouTube-Client-Version", client.clientVersion)
            }
            setBody(payload.toString())
        }

        val rawJson = json.parseToJsonElement(response.body<String>())
        InnerTubeParser.parseArtist(rawJson)
    }

    suspend fun getPlayerResponse(
        client: ClientInfo,
        videoId: String,
        playlistId: String? = null,
        signatureTimestamp: Int? = null
    ): Result<JsonObject> = runCatching {
        val contextJson = buildInnerTubeContextJson(client, videoId = videoId)
        val payload = buildJsonObject {
            put("context", contextJson)
            put("videoId", videoId)
            playlistId?.let { put("playlistId", it) }
            put("contentCheckOk", true)
            put("racyCheckOk", true)
            signatureTimestamp?.let { sts ->
                putJsonObject("playbackContext") {
                    putJsonObject("contentPlaybackContext") {
                        put("signatureTimestamp", sts)
                    }
                }
            }
        }

        val endpoint = if (client.clientName == "WEB_REMIX") "$baseUrl/player?prettyPrint=false" else "https://www.youtube.com/youtubei/v1/player?prettyPrint=false"
        val response = httpClient.post(endpoint) {
            contentType(ContentType.Application.Json)
            headers {
                append(HttpHeaders.UserAgent, client.userAgent)
                client.clientId?.let { append("X-YouTube-Client-Name", it) }
                append("X-YouTube-Client-Version", client.clientVersion)
                if (client.clientName == "WEB_REMIX") {
                    append(HttpHeaders.Origin, "https://music.youtube.com")
                    append(HttpHeaders.Referrer, "https://music.youtube.com")
                }
            }
            setBody(payload.toString())
        }

        json.parseToJsonElement(response.body<String>()).jsonObject
    }
}
