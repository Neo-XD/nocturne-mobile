package com.nocturne.music.data.remote.innertube

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val androidSdkVersion: String? = null,
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
        androidSdkVersion = "32"
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

@Serializable
data class InnerTubeContextPayload(
    val client: Map<String, String>,
    val user: Map<String, String> = emptyMap(),
    @SerialName("thirdParty")
    val thirdParty: Map<String, String>? = null
)

fun createInnerTubeContext(client: ClientInfo, videoId: String? = null, visitorData: String? = null): InnerTubeContextPayload {
    val clientMap = mutableMapOf(
        "clientName" to client.clientName,
        "clientVersion" to client.clientVersion,
        "hl" to client.hl,
        "gl" to client.gl,
        "userAgent" to client.userAgent
    )
    client.osName?.let { clientMap["osName"] = it }
    client.osVersion?.let { clientMap["osVersion"] = it }
    client.deviceMake?.let { clientMap["deviceMake"] = it }
    client.deviceModel?.let { clientMap["deviceModel"] = it }
    client.androidSdkVersion?.let { clientMap["androidSdkVersion"] = it }
    visitorData?.let { clientMap["visitorData"] = it }

    val thirdParty = if (videoId != null && (client.clientName == "ANDROID_VR" || client.clientName == "VISIONOS")) {
        mapOf("embedUrl" to "https://www.youtube.com/watch?v=$videoId")
    } else null

    return InnerTubeContextPayload(client = clientMap, thirdParty = thirdParty)
}
