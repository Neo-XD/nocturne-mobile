package com.nocturne.music.ui.screens.modern

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.nocturne.music.LocalPlayerAwareWindowInsets
import com.nocturne.music.R
import com.nocturne.music.constants.LibraryFilter
import com.nocturne.music.db.entities.Playlist
import com.nocturne.music.ui.component.CreatePlaylistDialog
import com.nocturne.music.ui.component.ContinuousCornerShape
import com.nocturne.music.ui.screens.library.LibraryAlbumsScreen
import com.nocturne.music.ui.screens.library.LibraryArtistsScreen
import com.nocturne.music.ui.screens.library.LibraryPlaylistsScreen
import com.nocturne.music.ui.screens.library.LibrarySongsScreen
import com.nocturne.music.viewmodels.LibraryMixViewModel

private val PAGE_GUTTER = 20.dp
private val LINK_ICON_SIZE = 24.dp
private val LINK_ICON_GAP = 16.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernLibraryScreen(
    navController: NavController,
    viewModel: LibraryMixViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    var activeFilter by remember { mutableStateOf<LibraryFilter?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = activeFilter != null) {
        activeFilter = null
    }

    AnimatedContent(
        targetState = activeFilter,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "ModernLibraryContent",
        modifier = modifier.fillMaxSize(),
    ) { filter ->
        when (filter) {
            LibraryFilter.PLAYLISTS -> {
                Column(Modifier.fillMaxSize()) {
                    ModernLibrarySubHeader(
                        title = stringResource(R.string.filter_playlists),
                        onBack = { activeFilter = null }
                    )
                    LibraryPlaylistsScreen(
                        navController = navController,
                        filterContent = {}
                    )
                }
            }
            LibraryFilter.SONGS -> {
                Column(Modifier.fillMaxSize()) {
                    ModernLibrarySubHeader(
                        title = stringResource(R.string.filter_songs),
                        onBack = { activeFilter = null }
                    )
                    LibrarySongsScreen(
                        navController = navController,
                        onDeselect = { activeFilter = null }
                    )
                }
            }
            LibraryFilter.ALBUMS -> {
                Column(Modifier.fillMaxSize()) {
                    ModernLibrarySubHeader(
                        title = stringResource(R.string.filter_albums),
                        onBack = { activeFilter = null }
                    )
                    LibraryAlbumsScreen(
                        navController = navController,
                        onDeselect = { activeFilter = null }
                    )
                }
            }
            LibraryFilter.ARTISTS -> {
                Column(Modifier.fillMaxSize()) {
                    ModernLibrarySubHeader(
                        title = stringResource(R.string.filter_artists),
                        onBack = { activeFilter = null }
                    )
                    LibraryArtistsScreen(
                        navController = navController,
                        onDeselect = { activeFilter = null }
                    )
                }
            }
            else -> {
                ModernLibraryRoot(
                    navController = navController,
                    viewModel = viewModel,
                    onNavigateFilter = { activeFilter = it },
                    onNewPlaylist = { showCreatePlaylistDialog = true }
                )
            }
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false }
        )
    }
}

@Composable
private fun ModernLibrarySubHeader(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernLibraryRoot(
    navController: NavController,
    viewModel: LibraryMixViewModel,
    onNavigateFilter: (LibraryFilter) -> Unit,
    onNewPlaylist: () -> Unit,
) {
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val listState = rememberLazyListState()
    val bottomInset = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = bottomInset + 32.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Big Apple Music Title
            item(key = "title") {
                Text(
                    text = stringResource(R.string.filter_library),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(
                        start = PAGE_GUTTER,
                        end = PAGE_GUTTER,
                        top = 16.dp,
                        bottom = 12.dp
                    )
                )
            }

            // Quick Folder Navigation Links
            item(key = "links") {
                ModernLibraryLinkList(
                    onSelect = onNavigateFilter
                )
            }

            // Playlists Section
            if (playlists.isNotEmpty()) {
                item(key = "playlists_shelf") {
                    ModernLibraryShelf(
                        title = stringResource(R.string.filter_playlists),
                        onShowAll = { onNavigateFilter(LibraryFilter.PLAYLISTS) }
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // New Playlist Card
                            item(key = "new_playlist") {
                                ModernNewPlaylistCard(onClick = onNewPlaylist)
                            }
                            items(playlists.take(8), key = { "pl_${it.id}" }) { playlist ->
                                ModernPlaylistCard(
                                    playlist = playlist,
                                    onClick = { navController.navigate("local_playlist/${playlist.id}") }
                                )
                            }
                        }
                    }
                }
            }

            // Recently Added Albums Section
            if (albums.isNotEmpty()) {
                item(key = "albums_shelf") {
                    ModernLibraryShelf(
                        title = stringResource(R.string.albums),
                        onShowAll = { onNavigateFilter(LibraryFilter.ALBUMS) }
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(albums.take(8), key = { "alb_${it.id}" }) { album ->
                                ModernAlbumCard(
                                    title = album.title,
                                    subtitle = album.artists.joinToString { it.name },
                                    thumbnailUrl = album.thumbnailUrl,
                                    onClick = { navController.navigate("album/${album.id}") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class LinkItem(
    val filter: LibraryFilter,
    val titleRes: Int,
    val icon: ImageVector,
)

@Composable
private fun ModernLibraryLinkList(
    onSelect: (LibraryFilter) -> Unit,
) {
    val links = remember {
        listOf(
            LinkItem(LibraryFilter.PLAYLISTS, R.string.filter_playlists, Icons.AutoMirrored.Rounded.QueueMusic),
            LinkItem(LibraryFilter.SONGS, R.string.filter_songs, Icons.Rounded.MusicNote),
            LinkItem(LibraryFilter.ALBUMS, R.string.filter_albums, Icons.Rounded.Album),
            LinkItem(LibraryFilter.ARTISTS, R.string.filter_artists, Icons.Rounded.Person),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 24.dp)
    ) {
        links.forEachIndexed { index, link ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clickable { onSelect(link.filter) }
                    .padding(horizontal = PAGE_GUTTER)
            ) {
                Icon(
                    imageVector = link.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(LINK_ICON_SIZE)
                )
                Spacer(Modifier.width(LINK_ICON_GAP))
                Text(
                    text = stringResource(link.titleRes),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
            if (index < links.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = PAGE_GUTTER + LINK_ICON_SIZE + LINK_ICON_GAP),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            }
        }
    }
}

@Composable
private fun ModernLibraryShelf(
    title: String,
    onShowAll: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 26.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.more_label),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onShowAll() }
            )
        }
        content()
    }
}

@Composable
private fun ModernNewPlaylistCard(
    onClick: () -> Unit,
) {
    val cardShape = ContinuousCornerShape(18.dp)
    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.create_playlist),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ModernPlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
) {
    val cardShape = ContinuousCornerShape(18.dp)
    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            AsyncImage(
                model = playlist.thumbnails.firstOrNull(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = playlist.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${playlist.songCount} songs",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ModernAlbumCard(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    onClick: () -> Unit,
) {
    val cardShape = ContinuousCornerShape(18.dp)
    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
