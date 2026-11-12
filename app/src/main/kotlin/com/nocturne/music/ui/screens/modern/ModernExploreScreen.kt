package com.nocturne.music.ui.screens.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.models.AlbumItem
import com.music.innertube.pages.MoodAndGenres
import com.nocturne.music.LocalPlayerAwareWindowInsets
import com.nocturne.music.LocalPlayerConnection
import com.nocturne.music.R
import com.nocturne.music.models.toMediaMetadata
import com.nocturne.music.ui.component.ContinuousCornerShape
import com.nocturne.music.ui.component.shimmer.ShimmerHost
import com.nocturne.music.ui.component.shimmer.TextPlaceholder
import com.nocturne.music.viewmodels.ChartsViewModel
import com.nocturne.music.viewmodels.ExploreViewModel
import kotlin.math.abs

private val PAGE_GUTTER = 20.dp
private const val MOOD_STRIPE_FRACTION = 0.22f
private val MOOD_CARD_SHAPE = ContinuousCornerShape(16.dp)
private val MOOD_SPACING = 12.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernExploreScreen(
    navController: NavController,
    exploreViewModel: ExploreViewModel = hiltViewModel(),
    chartsViewModel: ChartsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val explorePage by exploreViewModel.explorePage.collectAsState()
    val chartsPage by chartsViewModel.chartsPage.collectAsState()
    val isChartsLoading by chartsViewModel.isLoading.collectAsState()
    val playerConnection = LocalPlayerConnection.current
    val listState = rememberLazyListState()
    val bottomInset = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()

    LaunchedEffect(Unit) {
        if (chartsPage == null) {
            chartsViewModel.loadCharts()
        }
    }

    PullToRefreshBox(
        isRefreshing = explorePage == null || isChartsLoading,
        onRefresh = {
            chartsViewModel.loadCharts()
        },
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = bottomInset + 32.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // DisplayLarge Apple Music style "Explore" title
            item(key = "title") {
                Text(
                    text = stringResource(R.string.explore),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(
                        start = PAGE_GUTTER,
                        end = PAGE_GUTTER,
                        top = 16.dp,
                        bottom = 12.dp,
                    ),
                )
            }

            // New Releases Shelf
            val newReleases = explorePage?.newReleaseAlbums
            if (!newReleases.isNullOrEmpty()) {
                item(key = "new_releases_header") {
                    Text(
                        text = stringResource(R.string.new_release_albums),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(
                            start = PAGE_GUTTER,
                            end = PAGE_GUTTER,
                            top = 8.dp,
                            bottom = 12.dp,
                        ),
                    )
                }
                item(key = "new_releases_row") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                    ) {
                        items(newReleases.take(10), key = { "nr_${it.id}" }) { album ->
                            ModernExploreAlbumCard(
                                album = album,
                                onClick = { navController.navigate("album/${album.id}") },
                            )
                        }
                    }
                }
            }

            // Mood and Genres Title
            val moods = explorePage?.moodAndGenres
            if (!moods.isNullOrEmpty()) {
                item(key = "moods_header") {
                    Text(
                        text = stringResource(R.string.mood_and_genres),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(
                            start = PAGE_GUTTER,
                            end = PAGE_GUTTER,
                            top = 8.dp,
                            bottom = 12.dp,
                        ),
                    )
                }

                // 2-column BitChord Duotone Mood & Genre Sleeves
                val chunkedMoods = moods.chunked(2)
                items(chunkedMoods, key = { row -> row.first().title }) { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MOOD_SPACING),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PAGE_GUTTER)
                            .padding(bottom = MOOD_SPACING),
                    ) {
                        row.forEach { item ->
                            ModernMoodGenreCard(
                                item = item,
                                onClick = {
                                    val endpoint = item.endpoint
                                    navController.navigate("youtube_browse/${endpoint.browseId}?params=${endpoint.params ?: ""}")
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size < 2) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernMoodGenreCard(
    item: MoodAndGenres.Item,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tone = remember(item.stripeColor, item.title) {
        val baseColor = item.stripeColor?.let { Color(it.toInt()) } ?: fallbackMoodColor(item.title)
        val stripe = lerp(baseColor, Color.Black, 0.08f)
        val shadow = lerp(baseColor, Color.Black, 0.68f)
        val highlight = lerp(baseColor, Color.White, 0.32f)
        val placeholder = lerp(shadow, highlight, 0.45f)
        val filter = duotoneFilter(shadow, highlight)
        Triple(stripe, placeholder, filter)
    }

    Box(
        modifier = modifier
            .aspectRatio(1.85f)
            .clip(MOOD_CARD_SHAPE)
            .background(tone.first)
            .clickable(onClick = onClick),
    ) {
        // Duotone image container
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(1f - MOOD_STRIPE_FRACTION)
                .background(tone.second),
        )

        // Soft gradient overlay for text readability
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.28f), 0.65f to Color.Transparent)),
        )

        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium.copy(
                shadow = Shadow(Color.Black.copy(alpha = 0.4f), offset = Offset(0f, 1f), blurRadius = 6f),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            ),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun ModernExploreAlbumCard(
    album: AlbumItem,
    onClick: () -> Unit,
) {
    val cardShape = ContinuousCornerShape(18.dp)
    Column(
        modifier = Modifier
            .width(148.dp)
            .clickable { onClick() },
    ) {
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        ) {
            AsyncImage(
                model = album.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = album.artists.orEmpty().joinToString { it.name },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun duotoneFilter(shadow: Color, highlight: Color): ColorFilter {
    fun channel(from: Float, to: Float): FloatArray {
        val span = to - from
        return floatArrayOf(span * 0.299f, span * 0.587f, span * 0.114f, 0f, from * 255f)
    }
    return ColorFilter.colorMatrix(
        ColorMatrix(
            channel(shadow.red, highlight.red) +
                channel(shadow.green, highlight.green) +
                channel(shadow.blue, highlight.blue) +
                floatArrayOf(0f, 0f, 0f, 1f, 0f),
        ),
    )
}

private fun fallbackMoodColor(title: String): Color {
    val colors = listOf(
        Color(0xFFE57373),
        Color(0xFF81C784),
        Color(0xFF64B5F6),
        Color(0xFFFFB74D),
        Color(0xFFBA68C8),
        Color(0xFF4DB6AC),
        Color(0xFFFF8A65),
        Color(0xFFAED581),
    )
    return colors[abs(title.hashCode()) % colors.size]
}
