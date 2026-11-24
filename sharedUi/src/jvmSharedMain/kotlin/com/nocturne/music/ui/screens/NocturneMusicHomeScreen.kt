package com.nocturne.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nocturne.music.data.model.CARD_ART_PX
import com.nocturne.music.data.model.HomeShelf
import com.nocturne.music.data.model.ROW_ART_PX
import com.nocturne.music.data.model.ShelfItem
import com.nocturne.music.data.model.ShelfLayout
import com.nocturne.music.data.model.Song
import com.nocturne.music.data.model.TopPickMood
import com.nocturne.music.data.model.UiState
import com.nocturne.music.data.model.artworkAt
import com.nocturne.music.sharedui.resources.Res
import com.nocturne.music.sharedui.resources.retry
import com.nocturne.music.ui.components.MessageState
import com.nocturne.music.ui.components.PAGE_GUTTER
import com.nocturne.music.ui.components.PullToRefresh
import com.nocturne.music.ui.components.SearchPlayingBars
import com.nocturne.music.ui.components.ShelfRow
import com.nocturne.music.ui.components.SignInBanner
import com.nocturne.music.ui.components.contextClick
import com.nocturne.music.ui.components.feedMoreSkeleton
import com.nocturne.music.ui.components.feedSkeleton
import com.nocturne.music.ui.components.longPressMenuClickable
import com.nocturne.music.ui.components.thumbnailBorder
import com.nocturne.music.ui.components.trackColumnWidth
import org.jetbrains.compose.resources.stringResource
import java.util.Calendar

private data class MoodChipEntry(
    val label: String,
    val mood: TopPickMood?,
)

private val NocturneMoodChips = listOf(
    MoodChipEntry("All", null),
    MoodChipEntry("Relax", TopPickMood.RELAX),
    MoodChipEntry("Energize", TopPickMood.ENERGY),
    MoodChipEntry("Workout", TopPickMood.FITNESS),
    MoodChipEntry("Focus", TopPickMood.FOCUS),
    MoodChipEntry("Feel good", TopPickMood.FEEL_GOOD),
    MoodChipEntry("Party", TopPickMood.PARTY),
    MoodChipEntry("Commute", TopPickMood.COMMUTING),
    MoodChipEntry("Sad", TopPickMood.FEELING_BLUE),
    MoodChipEntry("Sleep", TopPickMood.SLEEP),
)

/**
 * Nocturne Music styled Home screen adapted from LiMusic Android:
 * - Ambient blurred currently-playing header backdrop fading smoothly into the page
 * - Time-of-day greeting with user avatar + chevron opening account settings
 * - Horizontal mood chip strip
 * - 2-column compact Quick Shortcuts grid
 * - Centered hero carousel ("Quick Picks") with rounded 28.dp art cards & gradient overlay
 * - Clean horizontal shelves & 4-row track lists below
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NocturneMusicHomeScreen(
    state: UiState<List<HomeShelf>>,
    listState: LazyListState,
    onItemClick: (ShelfItem, String) -> Unit,
    onRetry: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pullState: PullToRefreshState,
    contentPadding: PaddingValues,
    listenerName: String? = null,
    avatarUrl: String? = null,
    onOpenAccountSettings: (() -> Unit)? = null,
    signedIn: Boolean = true,
    onSignIn: (() -> Unit)? = null,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    loadingMore: Boolean = false,
    currentSong: Song? = null,
    isPlaying: Boolean = false,
    onMoodClick: ((TopPickMood, String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val pageBackground = MaterialTheme.colorScheme.background
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 5 -> "Good night"
            hour < 12 -> "Good morning"
            hour < 18 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val firstName = remember(listenerName) {
        listenerName?.trim()?.substringBefore(' ')?.takeIf { it.isNotBlank() }
    }
    var selectedChip by remember { mutableStateOf("All") }

    PullToRefresh(
        refreshing = refreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = modifier,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 0.dp,
                bottom = contentPadding.calculateBottomPadding(),
            ),
        ) {
            // ── Ambient Header Backdrop + Greeting + Mood Chips ─────────
            item(key = "nocturne-home-header") {
                Box(modifier = Modifier.fillMaxWidth()) {
                    NocturneHomeBackdrop(
                        coverUrl = currentSong?.thumbnailUrl?.artworkAt(ROW_ART_PX),
                        pageBackground = pageBackground,
                        modifier = Modifier.matchParentSize(),
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = contentPadding.calculateTopPadding()),
                    ) {
                        // Greeting Row with Avatar + Chevron
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PAGE_GUTTER, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .then(
                                        if (onOpenAccountSettings != null) {
                                            Modifier.clickable { onOpenAccountSettings() }
                                        } else Modifier,
                                    )
                                    .padding(end = 6.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (!avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = listenerName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .thumbnailBorder(CircleShape),
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                                if (onOpenAccountSettings != null) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }

                            Spacer(Modifier.width(6.dp))

                            Text(
                                text = if (firstName != null) "$greeting, $firstName" else greeting,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        // Horizontal Mood Chips
                        if (onMoodClick != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                NocturneMoodChips.forEach { entry ->
                                    val selected = selectedChip == entry.label
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                                        },
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                width = 0.75.dp,
                                                color = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                                shape = RoundedCornerShape(12.dp),
                                            )
                                            .clickable {
                                                selectedChip = entry.label
                                                if (entry.mood != null) {
                                                    onMoodClick(entry.mood, entry.label)
                                                } else {
                                                    onRefresh()
                                                }
                                            },
                                    ) {
                                        Text(
                                            text = entry.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }

            if (!signedIn && onSignIn != null) {
                item(key = "nocturne-signin") {
                    SignInBanner(
                        onSignIn = onSignIn,
                        modifier = Modifier.padding(bottom = 12.dp),
                        motion = !listState.isScrollInProgress,
                    )
                }
            }

            when (state) {
                is UiState.Loading -> feedSkeleton()
                is UiState.Error -> item {
                    MessageState(
                        state.message,
                        actionLabel = stringResource(Res.string.retry),
                        onAction = onRetry,
                    )
                }
                is UiState.Success -> {
                    val shelves = state.data
                    // Promote Quick Picks / first track shelf to hero carousel, like LiMusic
                    val quickPicksIdx = shelves.indexOfFirst {
                        it.title.contains("quick", ignoreCase = true) ||
                            it.layout == ShelfLayout.LIST
                    }.takeIf { it >= 0 } ?: 0

                    val shortcutItems = shelves.firstOrNull()?.items?.take(6).orEmpty()
                    if (shortcutItems.size >= 2) {
                        item(key = "nocturne-shortcuts") {
                            NocturneShortcutsGrid(
                                items = shortcutItems,
                                shelfTitle = shelves.first().title,
                                onItemClick = onItemClick,
                                onItemLongPress = onItemLongPress,
                                currentSong = currentSong,
                            )
                        }
                    }

                    val heroShelf = shelves.getOrNull(quickPicksIdx)
                    if (heroShelf != null && heroShelf.items.isNotEmpty()) {
                        item(key = "nocturne-hero-carousel") {
                            NocturneHeroCarousel(
                                shelf = heroShelf,
                                onItemClick = { item -> onItemClick(item, heroShelf.title) },
                                onItemLongPress = onItemLongPress,
                            )
                        }
                    }

                    shelves.forEachIndexed { index, shelf ->
                        if (index == quickPicksIdx) return@forEachIndexed
                        item(key = "nocturne-shelf-${shelf.title}-$index") {
                            val openItem: (ShelfItem) -> Unit = { item -> onItemClick(item, shelf.title) }
                            if (shelf.layout == ShelfLayout.LIST) {
                                NocturneListShelf(
                                    shelf = shelf,
                                    onItemClick = openItem,
                                    onItemLongPress = onItemLongPress,
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                )
                            } else {
                                NocturneCardShelf(
                                    shelf = shelf,
                                    onItemClick = openItem,
                                    onItemLongPress = onItemLongPress,
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                )
                            }
                        }
                    }

                    if (loadingMore) feedMoreSkeleton()
                }
            }
        }
    }

    if (onLoadMore != null && state is UiState.Success) {
        val loadMore by rememberUpdatedState(onLoadMore)
        LaunchedEffect(listState, loadingMore) {
            snapshotFlow {
                val layout = listState.layoutInfo
                (layout.visibleItemsInfo.lastOrNull()?.index ?: -1) to layout.totalItemsCount
            }.collect { (lastVisible, total) ->
                if (!loadingMore && total > 0 && lastVisible >= total - 3) loadMore()
            }
        }
    }
}

@Composable
private fun NocturneHomeBackdrop(
    coverUrl: String?,
    pageBackground: Color,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    Box(modifier) {
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(40.dp)
                    .alpha(0.65f),
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(
                            Brush.radialGradient(
                                listOf(primary.copy(alpha = 0.22f), Color.Transparent),
                                center = Offset(size.width * 0.15f, 0f),
                                radius = size.width * 0.9f,
                            ),
                        )
                    },
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            pageBackground.copy(alpha = 0.25f),
                            pageBackground.copy(alpha = 0.72f),
                            pageBackground,
                        ),
                    ),
                ),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            pageBackground.copy(alpha = 0.55f),
                            pageBackground.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

/** 2-column compact Quick Shortcuts grid inspired by LiMusic's ShortcutsSection. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NocturneShortcutsGrid(
    items: List<ShelfItem>,
    shelfTitle: String,
    onItemClick: (ShelfItem, String) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    currentSong: Song?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pair.forEach { item ->
                    val active = currentSong != null && item.videoId != null && item.videoId == currentSong.videoId
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (active) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .longPressMenuClickable(
                                onClick = { onItemClick(item, shelfTitle) },
                                onLongClick = onItemLongPress?.let { { it(item) } },
                            )
                            .contextClick(onItemLongPress?.let { { it(item) } }),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = item.thumbnailUrl?.artworkAt(ROW_ART_PX),
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)),
                            )
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp),
                            )
                        }
                    }
                }
                if (pair.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Centered Hero Carousel ("Quick Picks") adapted from LiMusic's HomeDiscoverSection:
 * large 28.dp rounded artwork cards with smooth bottom scrim, title & artist overlay,
 * and play badge.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NocturneHeroCarousel(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
) {
    Column(modifier = Modifier.padding(top = 12.dp, bottom = 22.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = shelf.title.ifBlank { "Quick picks" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            shelf.items.firstOrNull()?.let { first ->
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onItemClick(first) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Play",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val wide = maxWidth >= 600.dp
            val cardWidth = if (wide) 320.dp else (maxWidth * 0.76f)
            val cardHeight = if (wide) 340.dp else (cardWidth * 1.06f)

            LazyRow(
                contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(shelf.items) { _, item ->
                    Box(
                        modifier = Modifier
                            .width(cardWidth)
                            .height(cardHeight)
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .longPressMenuClickable(
                                onClick = { onItemClick(item) },
                                onLongClick = onItemLongPress?.let { { it(item) } },
                            )
                            .contextClick(onItemLongPress?.let { { it(item) } }),
                    ) {
                        AsyncImage(
                            model = item.thumbnailUrl?.artworkAt(CARD_ART_PX),
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.25f to Color.Transparent,
                                        1.0f to Color.Black.copy(alpha = 0.86f),
                                    ),
                                ),
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 20.dp, end = 64.dp, bottom = 20.dp),
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (item.subtitle.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.82f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NocturneListShelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    currentSong: Song?,
    isPlaying: Boolean,
) {
    Column(Modifier.padding(bottom = 24.dp)) {
        Text(
            text = shelf.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
        )
        BoxWithConstraints {
            val columnWidth = trackColumnWidth(maxWidth)
            ShelfRow(
                contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(shelf.items.chunked(4)) { column ->
                    Column(Modifier.width(columnWidth)) {
                        column.forEach { item ->
                            val isCurrent = currentSong != null && item.videoId != null && item.videoId == currentSong.videoId
                            CompactTrackRow(
                                title = item.title,
                                subtitle = item.subtitle,
                                thumbnailUrl = item.thumbnailUrl,
                                onClick = { onItemClick(item) },
                                onLongPress = onItemLongPress?.let { { it(item) } },
                                isCurrent = isCurrent,
                                isPlaying = isCurrent && isPlaying,
                                isVideo = item.isVideo,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NocturneCardShelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    currentSong: Song?,
    isPlaying: Boolean,
) {
    Column(Modifier.padding(bottom = 24.dp)) {
        Text(
            text = shelf.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
        )
        ShelfRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(shelf.items) { item ->
                val isCurrent = currentSong != null && item.videoId != null && item.videoId == currentSong.videoId
                Column(
                    modifier = Modifier
                        .width(156.dp)
                        .longPressMenuClickable(
                            onClick = { onItemClick(item) },
                            onLongClick = onItemLongPress?.let { { it(item) } },
                        )
                        .contextClick(onItemLongPress?.let { { it(item) } }),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        AsyncImage(
                            model = item.thumbnailUrl?.artworkAt(CARD_ART_PX),
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (isCurrent && isPlaying) {
                            SearchPlayingBars(Modifier.align(Alignment.Center))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.subtitle.isNotBlank()) {
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
