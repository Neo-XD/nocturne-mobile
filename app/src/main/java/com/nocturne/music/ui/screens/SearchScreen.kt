package com.nocturne.music.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nocturne.music.core.model.Track
import com.nocturne.music.ui.components.TrackItem
import com.nocturne.music.ui.theme.*
import com.nocturne.music.ui.viewmodel.SearchUiState
import com.nocturne.music.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onPlayTrack: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val query by viewModel.query.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        // Search Input Bar
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.onQueryChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search songs, artists, albums...", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = NocturneDarkSurfaceVariant,
                unfocusedContainerColor = NocturneDarkSurfaceVariant,
                focusedBorderColor = NocturnePurple,
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            singleLine = true
        )

        when (val state = uiState) {
            is SearchUiState.Idle -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Search for any music on YouTube Music", color = TextMuted)
                }
            }
            is SearchUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NocturnePurple)
                }
            }
            is SearchUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = TextSecondary)
                }
            }
            is SearchUiState.Success -> {
                val results = state.results
                val songs = results.songs.map { it.toTrack() }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    if (songs.isNotEmpty()) {
                        item {
                            Text(
                                text = "Songs",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(songs) { track ->
                            TrackItem(
                                track = track,
                                onClick = { onPlayTrack(track) }
                            )
                        }
                    }
                }
            }
        }
    }
}
