package com.nocturne.music.data.remote.innertube

import com.nocturne.music.core.model.*
import kotlinx.serialization.json.*

object InnerTubeParser {

    fun parseSearchAll(root: JsonElement): SearchResults {
        val songs = mutableListOf<BrowseItem>()
        val albums = mutableListOf<BrowseItem>()
        val artists = mutableListOf<BrowseItem>()
        val playlists = mutableListOf<BrowseItem>()
        val top = mutableListOf<BrowseItem>()

        val listItems = findAllObjects(root, "musicResponsiveListItemRenderer")
        for (item in listItems) {
            val parsed = parseListItem(item) ?: continue
            when (parsed.kind) {
                "song" -> songs.add(parsed)
                "album" -> albums.add(parsed)
                "artist" -> artists.add(parsed)
                "playlist" -> playlists.add(parsed)
            }
        }

        // Two row items (cards)
        val cards = findAllObjects(root, "musicTwoRowItemRenderer")
        for (card in cards) {
            val parsed = parseCardItem(card) ?: continue
            when (parsed.kind) {
                "song" -> if (!songs.any { it.id == parsed.id }) songs.add(parsed)
                "album" -> if (!albums.any { it.id == parsed.id }) albums.add(parsed)
                "artist" -> if (!artists.any { it.id == parsed.id }) artists.add(parsed)
                "playlist" -> if (!playlists.any { it.id == parsed.id }) playlists.add(parsed)
            }
        }

        if (songs.isNotEmpty()) top.add(songs.first())
        else if (albums.isNotEmpty()) top.add(albums.first())
        else if (artists.isNotEmpty()) top.add(artists.first())

        return SearchResults(
            top = top,
            songs = songs,
            albums = albums,
            artists = artists,
            playlists = playlists
        )
    }

    fun parseHome(root: JsonElement): HomePage {
        val chips = mutableListOf<HomeChip>()
        val sections = mutableListOf<Section>()

        // Mood / Genre chips
        val chipNodes = findAllObjects(root, "chipCloudChipRenderer")
        for (chip in chipNodes) {
            val text = getRunsText(chip["text"]) ?: continue
            val params = chip["navigationEndpoint"]
                ?.jsonObject?.get("browseEndpoint")
                ?.jsonObject?.get("params")
                ?.jsonPrimitive?.contentOrNull ?: continue
            chips.add(HomeChip(title = text, params = params))
        }

        // Carousels and Shelves
        val carousels = findAllObjects(root, "musicCarouselShelfRenderer")
        for (carousel in carousels) {
            val title = getRunsText(carousel["header"]?.jsonObject?.get("musicCarouselShelfBasicHeaderRenderer")?.jsonObject?.get("title"))
                ?: getRunsText(carousel["title"])
                ?: "Featured"

            val items = mutableListOf<BrowseItem>()
            val itemNodes = findAllObjects(carousel, "musicTwoRowItemRenderer")
            for (node in itemNodes) {
                parseCardItem(node)?.let { items.add(it) }
            }
            if (items.isEmpty()) {
                val listNodes = findAllObjects(carousel, "musicResponsiveListItemRenderer")
                for (node in listNodes) {
                    parseListItem(node)?.let { items.add(it) }
                }
            }

            if (items.isNotEmpty()) {
                sections.add(Section(title = title, items = items))
            }
        }

        return HomePage(chips = chips, sections = sections)
    }

    fun parseListItem(node: JsonObject): BrowseItem? {
        val navEndpoint = node["navigationEndpoint"]?.jsonObject
            ?: node["overlay"]?.jsonObject?.get("musicItemThumbnailOverlayRenderer")?.jsonObject?.get("content")?.jsonObject?.get("musicPlayButtonRenderer")?.jsonObject?.get("playNavigationEndpoint")?.jsonObject

        val videoId = navEndpoint?.get("watchEndpoint")?.jsonObject?.get("videoId")?.jsonPrimitive?.contentOrNull
            ?: node["playlistItemData"]?.jsonObject?.get("videoId")?.jsonPrimitive?.contentOrNull

        val browseId = navEndpoint?.get("browseEndpoint")?.jsonObject?.get("browseId")?.jsonPrimitive?.contentOrNull

        val flexColumns = node["flexColumns"]?.jsonArray ?: return null
        if (flexColumns.isEmpty()) return null

        val col0Runs = flexColumns.getOrNull(0)?.jsonObject?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject?.get("text")?.jsonObject?.get("runs")?.jsonArray
        val title = col0Runs?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }?.joinToString("") ?: return null

        val col1Runs = flexColumns.getOrNull(1)?.jsonObject?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject?.get("text")?.jsonObject?.get("runs")?.jsonArray
        val subtitle = col1Runs?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }?.joinToString("")

        val artistRuns = mutableListOf<ArtistRun>()
        col1Runs?.forEach { r ->
            val text = r.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: return@forEach
            val channelId = r.jsonObject["navigationEndpoint"]?.jsonObject?.get("browseEndpoint")?.jsonObject?.get("browseId")?.jsonPrimitive?.contentOrNull
            if (channelId != null && channelId.startsWith("UC")) {
                artistRuns.add(ArtistRun(name = text, id = channelId))
            }
        }

        val thumbnail = getThumbnail(node)
        val id = videoId ?: browseId ?: return null
        val kind = when {
            videoId != null -> "song"
            browseId?.startsWith("MPRE") == true || browseId?.startsWith("FEmusic_library_album") == true -> "album"
            browseId?.startsWith("UC") == true || browseId?.startsWith("FEmusic_library_artist") == true -> "artist"
            else -> "playlist"
        }

        return BrowseItem(
            kind = kind,
            id = id,
            title = title,
            subtitle = subtitle,
            thumbnail = thumbnail,
            artistRuns = artistRuns
        )
    }

    fun parseCardItem(node: JsonObject): BrowseItem? {
        val title = getRunsText(node["title"]) ?: return null
        val subtitle = getRunsText(node["subtitle"])

        val navEndpoint = node["navigationEndpoint"]?.jsonObject
            ?: node["thumbnailOverlay"]?.jsonObject?.get("musicItemThumbnailOverlayRenderer")?.jsonObject?.get("content")?.jsonObject?.get("musicPlayButtonRenderer")?.jsonObject?.get("playNavigationEndpoint")?.jsonObject

        val videoId = navEndpoint?.get("watchEndpoint")?.jsonObject?.get("videoId")?.jsonPrimitive?.contentOrNull
        val browseId = navEndpoint?.get("browseEndpoint")?.jsonObject?.get("browseId")?.jsonPrimitive?.contentOrNull

        val id = videoId ?: browseId ?: return null
        val kind = when {
            videoId != null -> "song"
            browseId?.startsWith("MPRE") == true || browseId?.startsWith("FEmusic_library_album") == true -> "album"
            browseId?.startsWith("UC") == true || browseId?.startsWith("FEmusic_library_artist") == true -> "artist"
            else -> "playlist"
        }

        return BrowseItem(
            kind = kind,
            id = id,
            title = title,
            subtitle = subtitle,
            thumbnail = getThumbnail(node)
        )
    }

    fun parseAlbum(root: JsonElement): AlbumPage {
        val header = findFirstObject(root, "musicResponsiveHeaderRenderer")
            ?: findFirstObject(root, "musicDetailHeaderRenderer")

        val title = getRunsText(header?.get("title")) ?: "Album"
        val subtitle = getRunsText(header?.get("subtitle")) ?: ""
        val thumbnail = header?.let { getThumbnail(it) }

        val tracks = mutableListOf<Track>()
        val listItems = findAllObjects(root, "musicResponsiveListItemRenderer")
        for (item in listItems) {
            val parsed = parseListItem(item) ?: continue
            tracks.add(parsed.toTrack())
        }

        return AlbumPage(
            title = title,
            artists = subtitle,
            thumbnail = thumbnail,
            tracks = tracks,
            trackCount = tracks.size
        )
    }

    fun parseArtist(root: JsonElement): ArtistPage {
        val header = findFirstObject(root, "musicHeaderRenderer")
            ?: findFirstObject(root, "musicVisualHeaderRenderer")

        val name = getRunsText(header?.get("title")) ?: "Artist"
        val description = getRunsText(header?.get("description"))
        val thumbnail = header?.let { getThumbnail(it) }

        val topSongs = mutableListOf<Track>()
        val listItems = findAllObjects(root, "musicResponsiveListItemRenderer")
        for (item in listItems) {
            val parsed = parseListItem(item) ?: continue
            if (parsed.kind == "song") {
                topSongs.add(parsed.toTrack())
            }
        }

        val albums = mutableListOf<BrowseItem>()
        val cards = findAllObjects(root, "musicTwoRowItemRenderer")
        for (card in cards) {
            val parsed = parseCardItem(card) ?: continue
            if (parsed.kind == "album") albums.add(parsed)
        }

        return ArtistPage(
            name = name,
            description = description,
            thumbnail = thumbnail,
            topSongs = topSongs,
            albums = albums
        )
    }

    private fun getRunsText(element: JsonElement?): String? {
        val runs = element?.jsonObject?.get("runs")?.jsonArray ?: return element?.jsonPrimitive?.contentOrNull
        return runs.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }.joinToString("")
    }

    private fun getThumbnail(node: JsonObject): String? {
        val thumbs = node["thumbnail"]?.jsonObject?.get("musicThumbnailRenderer")?.jsonObject?.get("thumbnail")?.jsonObject?.get("thumbnails")?.jsonArray
            ?: node["thumbnailRenderer"]?.jsonObject?.get("musicThumbnailRenderer")?.jsonObject?.get("thumbnail")?.jsonObject?.get("thumbnails")?.jsonArray
            ?: node["thumbnails"]?.jsonArray
        val lastThumb = thumbs?.lastOrNull()?.jsonObject
        return lastThumb?.get("url")?.jsonPrimitive?.contentOrNull
    }

    private fun findAllObjects(element: JsonElement, key: String): List<JsonObject> {
        val list = mutableListOf<JsonObject>()
        fun walk(el: JsonElement) {
            when (el) {
                is JsonObject -> {
                    if (el.containsKey(key)) {
                        el[key]?.jsonObject?.let { list.add(it) }
                    }
                    el.values.forEach { walk(it) }
                }
                is JsonArray -> el.forEach { walk(it) }
                else -> Unit
            }
        }
        walk(element)
        return list
    }

    private fun findFirstObject(element: JsonElement, key: String): JsonObject? {
        when (element) {
            is JsonObject -> {
                if (element.containsKey(key)) return element[key]?.jsonObject
                for (v in element.values) {
                    val found = findFirstObject(v, key)
                    if (found != null) return found
                }
            }
            is JsonArray -> {
                for (v in element) {
                    val found = findFirstObject(v, key)
                    if (found != null) return found
                }
            }
            else -> Unit
        }
        return null
    }
}
