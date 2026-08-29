package com.nocturne.music.data.remote.innertube

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class ClientInfo(
    val clientName: String,
    val clientVersion: String,
    val clientId: String? = null,
    val userAgent: String,
    val osName: String? = null,
    val osVersion: String? = null,
    val deviceMake: String? = null,
    val deviceModel: String? = null,
    val androidSdkVersion: Int? = null,
    val hl: String = "en",
    val gl: String = "US"
)

object YouTubeClients {
    val WEB_REMIX = ClientInfo(
        clientName = "WEB_REMIX",
        clientVersion = "1.20260213.01.00",
        clientId = "67",
        userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
    )

    val ANDROID_VR = ClientInfo(
        clientName = "ANDROID_VR",
        clientVersion = "1.43.32",
        clientId = "28",
        userAgent = "com.google.android.apps.youtube.vr.oculus/1.43.32 (Linux; U; Android 12; en_US; Quest 3; Build/SQ3A.220605.009.A1; Cronet/107.0.5284.2)",
        osName = "Android",
        osVersion = "12",
        deviceMake = "Oculus",
        deviceModel = "Quest 3",
        androidSdkVersion = 32
    )

    val ANDROID_MUSIC = ClientInfo(
        clientName = "ANDROID_MUSIC",
        clientVersion = "6.42.52",
        clientId = "21",
        userAgent = "com.google.android.apps.youtube.music/6.42.52 (Linux; U; Android 14; en_US; Pixel 8 Pro; Build/UQ1A.240205.004; Cronet/121.0.6167.101)",
        osName = "Android",
        osVersion = "14",
        deviceMake = "Google",
        deviceModel = "Pixel 8 Pro",
        androidSdkVersion = 34
    )

    val VISIONOS = ClientInfo(
        clientName = "VISIONOS",
        clientVersion = "0.1",
        clientId = "101",
        userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15",
        osName = "visionOS",
        osVersion = "1.3.21O771",
        deviceMake = "Apple",
        deviceModel = "RealityDevice14,1"
    )

    val IOS_MUSIC = ClientInfo(
        clientName = "IOS_MUSIC",
        clientVersion = "7.01.05",
        clientId = "26",
        userAgent = "com.google.ios.youtubemusic/7.01.05 (iPhone16,2; U; CPU iOS 18_2 like Mac OS X;)",
        osName = "iOS",
        osVersion = "18.2.22C152",
        deviceMake = "Apple",
        deviceModel = "iPhone16,2"
    )
}

fun buildInnerTubeContextJson(client: ClientInfo, videoId: String? = null, visitorData: String? = null): JsonObject = buildJsonObject {
    putJsonObject("client") {
        put("clientName", client.clientName)
        put("clientVersion", client.clientVersion)
        put("hl", client.hl)
        put("gl", client.gl)
        client.osName?.let { put("osName", it) }
        client.osVersion?.let { put("osVersion", it) }
        client.deviceMake?.let { put("deviceMake", it) }
        client.deviceModel?.let { put("deviceModel", it) }
        client.androidSdkVersion?.let { put("androidSdkVersion", it) }
        visitorData?.let { put("visitorData", it) }
    }
    if (videoId != null && (client.clientName == "ANDROID_VR" || client.clientName == "VISIONOS")) {
        putJsonObject("thirdParty") {
            put("embedUrl", "https://www.youtube.com/watch?v=$videoId")
        }
    }
}
