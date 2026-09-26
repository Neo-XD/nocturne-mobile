/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nocturne.music.ui.screens.search

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.nocturne.music.ui.component.NavigationTitle
import com.nocturne.music.ui.component.shimmer.ListItemPlaceHolder
import com.nocturne.music.ui.component.shimmer.ShimmerHost
import com.nocturne.music.ui.screens.MoodAndGenresButtonHeight
import com.nocturne.music.viewmodels.MoodAndGenresViewModel

private data class VibeItem(
    val emoji: String,
    val title: String,
    val description: String,
    val query: String,
    val gradientColors: List<Color>
)

private val CuratedVibes = listOf(
    VibeItem(
        emoji = "\uD83C\uDF19",
        title = "Late Night",
        description = "Midnight chill & ambient",
        query = "Late Night Vibe",
        gradientColors = listOf(Color(0xFF1A1F38), Color(0xFF2E1A47))
    ),
    VibeItem(
        emoji = "☕",
        title = "Chill & Lo-Fi",
        description = "Mellow beats to relax",
        query = "Lofi Chill Beats",
        gradientColors = listOf(Color(0xFF3E2723), Color(0xFF4E342E))
    ),
    VibeItem(
        emoji = "\uD83D\uDCAA",
        title = "Workout",
        description = "High energy & motivation",
        query = "Workout Music",
        gradientColors = listOf(Color(0xFFBF360C), Color(0xFFE65100))
    ),
    VibeItem(
        emoji = "\uD83C\uDFAF",
        title = "Focus",
        description = "Deep work & concentration",
        query = "Focus Music",
        gradientColors = listOf(Color(0xFF004D40), Color(0xFF00695C))
    ),
    VibeItem(
        emoji = "\uD83D\uDE97",
        title = "Night Drive",
        description = "Synthwave & cinematic",
        query = "Night Drive Songs",
        gradientColors = listOf(Color(0xFF1A237E), Color(0xFF311B92))
    ),
    VibeItem(
        emoji = "⚡",
        title = "Party & EDM",
        description = "Club tracks & dance",
        query = "Party Dance Songs",
        gradientColors = listOf(Color(0xFF4A148C), Color(0xFF880E4F))
    ),
    VibeItem(
        emoji = "\uD83C\uDF27\uFE0F",
        title = "Rainy Day",
        description = "Acoustic & soft indie",
        query = "Rainy Day Acoustic",
        gradientColors = listOf(Color(0xFF263238), Color(0xFF37474F))
    ),
    VibeItem(
        emoji = "\uD83D\uDC96",
        title = "Romance",
        description = "Slow love & soul",
        query = "Romantic Love Songs",
        gradientColors = listOf(Color(0xFF880E4F), Color(0xFFAD1457))
    ),
    VibeItem(
        emoji = "\uD83C\uDFAE",
        title = "Gaming",
        description = "Drift phonk & bass",
        query = "Gaming Phonk Music",
        gradientColors = listOf(Color(0xFF212121), Color(0xFF37474F))
    ),
    VibeItem(
        emoji = "\uD83C\uDFB8",
        title = "Rock & Indie",
        description = "Guitars & raw energy",
        query = "Indie Rock Songs",
        gradientColors = listOf(Color(0xFF3E2723), Color(0xFF5D4037))
    ),
    VibeItem(
        emoji = "\uD83C\uDF3F",
        title = "Peaceful",
        description = "Piano & calm ambient",
        query = "Peaceful Piano Music",
        gradientColors = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32))
    ),
    VibeItem(
        emoji = "☀️",
        title = "Feel Good",
        description = "Bright & uplifting",
        query = "Feel Good Happy Songs",
        gradientColors = listOf(Color(0xFFE65100), Color(0xFFF57F17))
    )
)

@Composable
fun VibeCategoryPicker(
    navController: NavController,
    onVibeSelect: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: MoodAndGenresViewModel = hiltViewModel()
) {
    val localConfiguration = LocalConfiguration.current
    val itemsPerRow = if (localConfiguration.orientation == ORIENTATION_LANDSCAPE) 3 else 2
    val moodAndGenresList by viewModel.moodAndGenres.collectAsState()

    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize()
    ) {
        // Section: Curated Vibes Carousel
        item(key = "curated_vibes_header") {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)) {
                Text(
                    text = "Pick Your Vibe",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tap a vibe to immediately explore matching music",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item(key = "curated_vibes_carousel") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(CuratedVibes, key = { it.title }) { vibe ->
                    VibeCard(vibe = vibe, onClick = { onVibeSelect(vibe.query) })
                }
            }
        }

        // Section: YouTube Music Moods & Categories
        item(key = "browse_categories_header") {
            Spacer(modifier = Modifier.height(8.dp))
            NavigationTitle(
                title = "Browse Categories",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        if (moodAndGenresList == null) {
            item(key = "mood_and_genres_shimmer") {
                ShimmerHost(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    repeat(6) {
                        ListItemPlaceHolder()
                    }
                }
            }
        } else {
            moodAndGenresList?.forEachIndexed { index, moodAndGenres ->
                item(key = "vibe_section_$index") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = moodAndGenres.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                        moodAndGenres.items.chunked(itemsPerRow).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { item ->
                                    CategoryButton(
                                        title = item.title,
                                        onClick = {
                                            navController.navigate("youtube_browse/${item.endpoint.browseId}?params=${item.endpoint.params}")
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 4.dp)
                                    )
                                }
                                repeat(itemsPerRow - row.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun VibeCard(
    vibe: VibeItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(150.dp)
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(vibe.gradientColors))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = vibe.emoji,
                fontSize = 26.sp
            )
            Column {
                Text(
                    text = vibe.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = vibe.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CategoryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .height(MoodAndGenresButtonHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
