package com.nocturne.music.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nocturne.music.core.model.BrowseItem
import com.nocturne.music.core.model.Track
import com.nocturne.music.ui.theme.*
import com.nocturne.music.ui.viewmodel.HomeUiState
import com.nocturne.music.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPlayTrack: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedChip by viewModel.selectedChipParam.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        Text(
            text = "Nocturne",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = NocturnePurple
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NocturnePurple)
                }
            }
            is HomeUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.message, color = TextSecondary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.loadHome() },
                            colors = ButtonDefaults.buttonColors(containerColor = NocturnePurple)
                        ) {
                            Text(text = "Retry")
                        }
                    }
                }
            }
            is HomeUiState.Success -> {
                val homePage = state.homePage
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    // Mood & Genre Filter Chips
                    if (homePage.chips.isNotEmpty()) {
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                items(homePage.chips) { chip ->
                                    val isSelected = selectedChip == chip.params
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (isSelected) viewModel.loadHome(null)
                                            else viewModel.loadHome(chip.params)
                                        },
                                        label = { Text(chip.title) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NocturnePurple,
                                            selectedLabelColor = TextPrimary,
                                            containerColor = NocturneDarkSurfaceVariant,
                                            labelColor = TextSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Carousel sections
                    items(homePage.sections) { section ->
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(section.items) { item ->
                                    HomeCardItem(
                                        item = item,
                                        onClick = {
                                            if (item.kind == "song") {
                                                onPlayTrack(item.toTrack())
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeCardItem(
    item: BrowseItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = item.thumbnail,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        item.subtitle?.let { sub ->
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 11.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
