/**
 * Nocturne Music Project (C) 2026
 * Modern Home Screen UI
 */

package com.nocturne.music.ui.screens.modern

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.models.YTItem
import com.music.innertube.pages.HomePage
import com.nocturne.music.LocalDatabase
import com.nocturne.music.LocalPlayerAwareWindowInsets
import com.nocturne.music.LocalPlayerConnection
import com.nocturne.music.R
import com.nocturne.music.ui.screens.Screens
import com.nocturne.music.db.entities.Album
import com.nocturne.music.db.entities.Artist
import com.nocturne.music.db.entities.LocalItem
import com.nocturne.music.db.entities.Playlist
import com.nocturne.music.db.entities.Song
import com.nocturne.music.models.MediaMetadata
import com.nocturne.music.models.SimilarRecommendation
import com.nocturne.music.models.toMediaMetadata
import com.nocturne.music.nocturne.AudioDeviceBottomSheet
import com.nocturne.music.playback.queues.YouTubeQueue
import com.nocturne.music.sync.LocalRemoteSyncManager
import com.nocturne.music.sync.RemoteConnectionState
import com.nocturne.music.ui.component.ChipsRow
import com.nocturne.music.ui.component.ContinuousCornerShape
import com.nocturne.music.ui.component.GLASS_CORNER_SMOOTHING
import com.nocturne.music.ui.component.LocalMenuState
import com.nocturne.music.ui.component.PlayingIndicatorBox
import com.nocturne.music.ui.menu.AlbumMenu
import com.nocturne.music.ui.menu.ArtistMenu
import com.nocturne.music.ui.menu.SongMenu
import com.nocturne.music.ui.menu.YouTubeAlbumMenu
import com.nocturne.music.ui.menu.YouTubeArtistMenu
import com.nocturne.music.ui.menu.YouTubePlaylistMenu
import com.nocturne.music.ui.menu.YouTubeSongMenu
import com.nocturne.music.ui.utils.resize
import com.nocturne.music.viewmodels.CommunityPlaylistItem
import com.nocturne.music.viewmodels.DailyDiscoverItem
import com.nocturne.music.viewmodels.HomeViewModel
import kotlinx.coroutines.launch

private val SHELF_CARD_WIDTH = 152.dp
private const val RECENT_TRACKS_PER_COLUMN = 4

data class ModernMoodItem(
    val name: String,
    val query: String,
    val colors: List<Color>,
)

val MODERN_CORE_MOODS = listOf(
    ModernMoodItem("Energize", "Energy", listOf(Color(0xFFFF416C), Color(0xFFFF4B2B))),
    ModernMoodItem("Relax", "Relax", listOf(Color(0xFF2193B0), Color(0xFF6DD5ED))),
    ModernMoodItem("Focus", "Focus", listOf(Color(0xFF373B44), Color(0xFF4286F4))),
    ModernMoodItem("Romance", "Romance", listOf(Color(0xFFFF758C), Color(0xFFFF7EB3))),
    ModernMoodItem("Feel Good", "Feel-good", listOf(Color(0xFFF7971E), Color(0xFFFFD200))),
    ModernMoodItem("Workout", "Workout", listOf(Color(0xFF11998E), Color(0xFF38EF7D))),
    ModernMoodItem("Sleep", "Sleep", listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernHomeScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val quickPicks by viewModel.quickPicks.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val similarRecommendations by viewModel.similarRecommendations.collectAsState()
    val accountPlaylists by viewModel.accountPlaylists.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val explorePage by viewModel.explorePage.collectAsState()
    val dailyDiscover by viewModel.dailyDiscover.collectAsState()
    val coversAndRemixes by viewModel.coversAndRemixes.collectAsState()
    val communityPlaylists by viewModel.communityPlaylists.collectAsState()
    val selectedChip by viewModel.selectedChip.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()

    val syncManager = LocalRemoteSyncManager.current
    val syncConnectionState by syncManager.connectionState.collectAsState()
    val isSyncConnected = syncConnectionState == RemoteConnectionState.CONNECTED
    var showAudioDeviceSheet by remember { mutableStateOf(false) }

    val playerAwarePadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val topPadding = playerAwarePadding.calculateTopPadding()
    val bottomPadding = playerAwarePadding.calculateBottomPadding()

    // Pagination for more YouTube home items
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                val total = listState.layoutInfo.totalItemsCount
                if (lastIndex != null && total > 0 && lastIndex >= total - 3) {
                    viewModel.loadMoreYouTubeItems(homePage?.continuation)
                }
            }
    }

    PullToRefreshBox(
        state = pullRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val availableWidth = maxWidth
            val columnWidth = minOf(availableWidth * 0.86f, 340.dp)

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = topPadding + 6.dp,
                    bottom = bottomPadding + 16.dp,
                ),
            ) {
                // 1. Modern Home Header
                item(key = "modern_header") {
                    ModernHomeHeader()
                }

                // 2. Chips Filter Row (Chill, Energy, Focus, etc.)
                if (homePage?.chips?.isNotEmpty() == true) {
                    item(key = "modern_chips") {
                        ChipsRow(
                            chips = homePage?.chips
                                ?.filter { !it.title.equals("Podcasts", ignoreCase = true) }
                                ?.map { it to it.title } ?: emptyList(),
                            currentValue = selectedChip,
                            onValueUpdate = { viewModel.toggleChip(it) },
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }

                // 3. Quick Picks (4-track column horizontal pager)
                quickPicks?.takeIf { it.isNotEmpty() }?.let { tracks ->
                    item(key = "quick_picks_shelf") {
                        Column(modifier = Modifier.padding(bottom = 22.dp)) {
                            ModernSectionHeader(
                                title = stringResource(R.string.quick_picks),
                                subtitle = "Start a station based on your favorites",
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                items(tracks.chunked(RECENT_TRACKS_PER_COLUMN)) { columnSongs ->
                                    Column(
                                        modifier = Modifier.width(columnWidth),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        columnSongs.forEach { song ->
                                            val isSongActive = song.id == mediaMetadata?.id
                                            ModernRecentTrackRow(
                                                title = song.song.title,
                                                subtitle = song.artists.joinToString { it.name },
                                                thumbnailUrl = song.song.thumbnailUrl,
                                                isActive = isSongActive,
                                                isPlaying = isSongActive && isPlaying,
                                                onClick = {
                                                    if (isSongActive) {
                                                        playerConnection.togglePlayPause()
                                                    } else {
                                                        playerConnection.playQueue(
                                                            YouTubeQueue.radio(song.toMediaMetadata())
                                                        )
                                                    }
                                                },
                                                onLongClick = {
                                                    menuState.show {
                                                        SongMenu(
                                                            originalSong = song,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. "Find Your Mood" Shelf
                item(key = "find_your_mood_shelf") {
                    Column(modifier = Modifier.padding(bottom = 22.dp)) {
                        ModernSectionHeader(
                            title = "Find Your Mood",
                            subtitle = "Curated sounds tailored to how you feel",
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            items(MODERN_CORE_MOODS, key = { it.name }) { mood ->
                                ModernMoodCard(
                                    mood = mood,
                                    onClick = {
                                        val matchingChip = homePage?.chips?.firstOrNull {
                                            it.title.contains(mood.query, ignoreCase = true) ||
                                            mood.name.contains(it.title, ignoreCase = true)
                                        }
                                        if (matchingChip != null) {
                                            viewModel.toggleChip(matchingChip)
                                        } else {
                                            navController.navigate("search?query=${mood.name}")
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                // 5. Forgotten Favorites Shelf
                forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { songs ->
                    item(key = "forgotten_favorites_shelf") {
                        Column(modifier = Modifier.padding(bottom = 22.dp)) {
                            ModernSectionHeader(
                                title = stringResource(R.string.forgotten_favorites),
                                subtitle = "Songs you loved and haven't heard in a while",
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                items(songs) { song ->
                                    val isSongActive = song.id == mediaMetadata?.id
                                    ModernShelfCard(
                                        title = song.song.title,
                                        subtitle = song.artists.joinToString { it.name },
                                        thumbnailUrl = song.song.thumbnailUrl,
                                        isActive = isSongActive,
                                        isPlaying = isSongActive && isPlaying,
                                        onClick = {
                                            if (isSongActive) {
                                                playerConnection.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    YouTubeQueue.radio(song.toMediaMetadata())
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Keep Listening / Recent Library Activity
                keepListening?.takeIf { it.isNotEmpty() }?.let { items ->
                    item(key = "keep_listening_shelf") {
                        Column(modifier = Modifier.padding(bottom = 22.dp)) {
                            ModernSectionHeader(
                                title = stringResource(R.string.keep_listening),
                                subtitle = "Pick up right where you left off",
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                items(items) { item ->
                                    when (item) {
                                        is Song -> {
                                            val isSongActive = item.id == mediaMetadata?.id
                                            ModernShelfCard(
                                                title = item.song.title,
                                                subtitle = item.artists.joinToString { it.name },
                                                thumbnailUrl = item.song.thumbnailUrl,
                                                isActive = isSongActive,
                                                isPlaying = isSongActive && isPlaying,
                                                onClick = {
                                                    if (isSongActive) {
                                                        playerConnection.togglePlayPause()
                                                    } else {
                                                        playerConnection.playQueue(
                                                            YouTubeQueue.radio(item.toMediaMetadata())
                                                        )
                                                    }
                                                },
                                                onLongClick = {
                                                    menuState.show {
                                                        SongMenu(
                                                            originalSong = item,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            )
                                        }
                                        is Album -> {
                                            val isAlbumActive = item.id == mediaMetadata?.album?.id
                                            ModernShelfCard(
                                                title = item.album.title,
                                                subtitle = item.artists.joinToString { it.name },
                                                thumbnailUrl = item.album.thumbnailUrl,
                                                isActive = isAlbumActive,
                                                isPlaying = isAlbumActive && isPlaying,
                                                onClick = { navController.navigate("album/${item.id}") },
                                                onLongClick = {
                                                    menuState.show {
                                                        AlbumMenu(
                                                            originalAlbum = item,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            )
                                        }
                                        is Artist -> {
                                            ModernShelfCard(
                                                title = item.artist.name,
                                                subtitle = "Artist",
                                                thumbnailUrl = item.artist.thumbnailUrl,
                                                onClick = { navController.navigate("artist/${item.id}") },
                                                onLongClick = {
                                                    menuState.show {
                                                        ArtistMenu(
                                                            originalArtist = item,
                                                            coroutineScope = scope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            )
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }
                }

                // 7. Daily Discover Shelf
                dailyDiscover?.takeIf { it.isNotEmpty() }?.let { items ->
                    item(key = "daily_discover_shelf") {
                        Column(modifier = Modifier.padding(bottom = 22.dp)) {
                            ModernSectionHeader(
                                title = stringResource(R.string.your_daily_discover),
                                subtitle = "Fresh tracks tuned to your music taste",
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                items(items) { discoverItem ->
                                    val ytItem = discoverItem.recommendation
                                    val isCurrent = ytItem.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id)
                                    ModernYtShelfCard(
                                        item = ytItem,
                                        isActive = isCurrent,
                                        isPlaying = isCurrent && isPlaying,
                                        navController = navController,
                                        playerConnection = playerConnection,
                                        menuState = menuState,
                                    )
                                }
                            }
                        }
                    }
                }

                // 8. From The Community Shelf
                communityPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
                    item(key = "community_playlists_shelf") {
                        Column(modifier = Modifier.padding(bottom = 22.dp)) {
                            ModernSectionHeader(
                                title = stringResource(R.string.from_the_community),
                                subtitle = "Playlists shared by listeners worldwide",
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                items(playlists) { communityItem ->
                                    val playlist = communityItem.playlist
                                    ModernShelfCard(
                                        title = playlist.title,
                                        subtitle = playlist.author?.name ?: "Playlist",
                                        thumbnailUrl = playlist.thumbnail,
                                        onClick = { navController.navigate("online_playlist/${playlist.id}") },
                                        onLongClick = {
                                            menuState.show {
                                                YouTubePlaylistMenu(
                                                    playlist = playlist,
                                                    coroutineScope = scope,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                // 9. Covers & Remixes Shelf
                coversAndRemixes?.let { mixSection ->
                    if (mixSection.items.isNotEmpty()) {
                        item(key = "covers_and_remixes_shelf") {
                            Column(modifier = Modifier.padding(bottom = 22.dp)) {
                                ModernSectionHeader(
                                    title = mixSection.title,
                                    subtitle = "Reimagined versions and alternate takes",
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    items(mixSection.items) { ytItem ->
                                        val isCurrent = ytItem.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id)
                                        ModernYtShelfCard(
                                            item = ytItem,
                                            isActive = isCurrent,
                                            isPlaying = isCurrent && isPlaying,
                                            navController = navController,
                                            playerConnection = playerConnection,
                                            menuState = menuState,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 10. YouTube Dynamic Sections (Mixed for you, Charts, New Releases, etc.)
                homePage?.sections?.forEachIndexed { index, section ->
                    if (section.items.isNotEmpty()) {
                        item(key = "home_section_$index") {
                            Column(modifier = Modifier.padding(bottom = 22.dp)) {
                                ModernSectionHeader(
                                    title = section.title,
                                    subtitle = section.label,
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    items(section.items) { ytItem ->
                                        val isCurrent = ytItem.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id)
                                        ModernYtShelfCard(
                                            item = ytItem,
                                            isActive = isCurrent,
                                            isPlaying = isCurrent && isPlaying,
                                            navController = navController,
                                            playerConnection = playerConnection,
                                            menuState = menuState,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 11. Similar Recommendations Shelves
                similarRecommendations?.forEachIndexed { index, rec ->
                    if (rec.items.isNotEmpty()) {
                        item(key = "similar_to_$index") {
                            Column(modifier = Modifier.padding(bottom = 22.dp)) {
                                ModernSectionHeader(
                                    title = "Similar to ${rec.title.title}",
                                    subtitle = "Recommendations based on your taste",
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    items(rec.items) { ytItem ->
                                        val isCurrent = ytItem.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id)
                                        ModernYtShelfCard(
                                            item = ytItem,
                                            isActive = isCurrent,
                                            isPlaying = isCurrent && isPlaying,
                                            navController = navController,
                                            playerConnection = playerConnection,
                                            menuState = menuState,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Loading Indicator at bottom
                if (isLoading) {
                    item(key = "loading_indicator") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAudioDeviceSheet) {
        AudioDeviceBottomSheet(
            onDismiss = { showAudioDeviceSheet = false }
        )
    }
}

/**
 * Top Title: "Listen Now"
 */
@Composable
fun ModernHomeHeader(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 4.dp),
    ) {
        Text(
            text = "Listen Now",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                letterSpacing = (-0.5).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/**
 * Section Header
 */
@Composable
fun ModernSectionHeader(
    title: String,
    subtitle: String? = null,
    onShowAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = (-0.3).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onShowAll != null) {
            Text(
                text = "See All",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onShowAll)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

/**
 * One track row in 4-track column horizontal pager
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModernRecentTrackRow(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val squircleShape = remember { ContinuousCornerShape(10.dp, GLASS_CORNER_SMOOTHING) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ContinuousCornerShape(12.dp, GLASS_CORNER_SMOOTHING))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(vertical = 5.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .border(0.5.dp, Color.White.copy(alpha = 0.12f), squircleShape)
                .clip(squircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailUrl?.resize(160, 160))
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            PlayingIndicatorBox(
                isActive = isActive,
                playWhenReady = isPlaying,
                color = Color.White,
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isActive) Color.Black.copy(alpha = 0.4f) else Color.Transparent)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                ),
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Square Squircle Shelf Card (152dp)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModernShelfCard(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier.width(SHELF_CARD_WIDTH),
) {
    val haptic = LocalHapticFeedback.current
    val cardShape = remember { ContinuousCornerShape(16.dp, GLASS_CORNER_SMOOTHING) }

    Column(
        modifier = modifier
            .clip(ContinuousCornerShape(18.dp, GLASS_CORNER_SMOOTHING))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .border(0.5.dp, Color.White.copy(alpha = 0.14f), cardShape)
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailUrl?.resize(400, 400))
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            PlayingIndicatorBox(
                isActive = isActive,
                playWhenReady = isPlaying,
                color = Color.White,
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isActive) Color.Black.copy(alpha = 0.4f) else Color.Transparent)
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            ),
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * YouTube Item helper for Shelf Card
 */
@Composable
fun ModernYtShelfCard(
    item: YTItem,
    isActive: Boolean,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: com.nocturne.music.playback.PlayerConnection,
    menuState: com.nocturne.music.ui.component.MenuState,
    modifier: Modifier = Modifier.width(SHELF_CARD_WIDTH),
) {
    val scope = rememberCoroutineScope()

    val title = item.title
    val subtitle = when (item) {
        is SongItem -> item.artists.joinToString { it.name }
        is AlbumItem -> item.artists?.joinToString { it.name } ?: "Album"
        is ArtistItem -> "Artist"
        is PlaylistItem -> item.author?.name ?: "Playlist"
    }

    ModernShelfCard(
        title = title,
        subtitle = subtitle,
        thumbnailUrl = item.thumbnail,
        isActive = isActive,
        isPlaying = isPlaying,
        onClick = {
            when (item) {
                is SongItem -> playerConnection.playQueue(
                    YouTubeQueue(
                        item.endpoint ?: WatchEndpoint(videoId = item.id),
                        item.toMediaMetadata(),
                    )
                )
                is AlbumItem -> navController.navigate("album/${item.id}")
                is ArtistItem -> navController.navigate("artist/${item.id}")
                is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
            }
        },
        onLongClick = {
            menuState.show {
                when (item) {
                    is SongItem -> YouTubeSongMenu(
                        song = item,
                        navController = navController,
                        onDismiss = menuState::dismiss,
                    )
                    is AlbumItem -> YouTubeAlbumMenu(
                        albumItem = item,
                        navController = navController,
                        onDismiss = menuState::dismiss,
                    )
                    is ArtistItem -> YouTubeArtistMenu(
                        artist = item,
                        onDismiss = menuState::dismiss,
                    )
                    is PlaylistItem -> YouTubePlaylistMenu(
                        playlist = item,
                        coroutineScope = scope,
                        onDismiss = menuState::dismiss,
                    )
                }
            }
        },
        modifier = modifier,
    )
}

/**
 * Find Your Mood Tile with rich gradient and Nocturne logo mark
 */
@Composable
fun ModernMoodCard(
    mood: ModernMoodItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(SHELF_CARD_WIDTH),
) {
    val cardShape = remember { ContinuousCornerShape(16.dp, GLASS_CORNER_SMOOTHING) }
    val gradientBrush = remember(mood.colors) {
        Brush.linearGradient(mood.colors)
    }

    Column(
        modifier = modifier
            .clip(ContinuousCornerShape(18.dp, GLASS_CORNER_SMOOTHING))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), cardShape)
                .clip(cardShape)
                .background(gradientBrush),
        ) {
            Image(
                painter = painterResource(R.drawable.vivi_music_small_icon),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(20.dp),
                alpha = 0.5f,
            )

            Text(
                text = mood.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    letterSpacing = (-0.3).sp,
                ),
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
            )
        }
    }
}
