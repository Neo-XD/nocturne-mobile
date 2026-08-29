package com.nocturne.music.data.remote.innertube

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.util.concurrent.TimeUnit

class NewPipeDownloader private constructor(
    private val client: OkHttpClient
) : Downloader() {

    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = okhttp3.Request.Builder()
            .url(url)

        headers.forEach { (name, values) ->
            values.forEach { value ->
                requestBuilder.addHeader(name, value)
            }
        }

        val requestBody = dataToSend?.toRequestBody(null)

        when (httpMethod) {
            "GET" -> requestBuilder.get()
            "POST" -> requestBuilder.post(requestBody ?: ByteArray(0).toRequestBody(null))
            "HEAD" -> requestBuilder.head()
            else -> requestBuilder.method(httpMethod, requestBody)
        }

        val response = client.newCall(requestBuilder.build()).execute()
        val responseBody = response.body?.string() ?: ""
        val responseHeaders = response.headers.toMultimap()

        return Response(
            response.code,
            response.message,
            responseHeaders,
            responseBody,
            response.request.url.toString()
        )
    }

    companion object {
        private var instance: NewPipeDownloader? = null

        fun init(): NewPipeDownloader {
            if (instance == null) {
                val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()
                instance = NewPipeDownloader(okHttpClient)
            }
            return instance!!
        }
    }
}
