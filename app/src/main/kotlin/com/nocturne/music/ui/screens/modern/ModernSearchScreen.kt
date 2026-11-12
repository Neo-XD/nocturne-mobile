package com.nocturne.music.ui.screens.modern

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NorthWest
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.nocturne.music.LocalDatabase
import com.nocturne.music.LocalPlayerAwareWindowInsets
import com.nocturne.music.LocalPlayerConnection
import com.nocturne.music.R
import com.nocturne.music.db.entities.SearchHistory
import com.nocturne.music.ui.component.ContinuousCornerShape
import com.nocturne.music.ui.component.PillSegmentedControl
import com.nocturne.music.ui.component.SearchField
import com.nocturne.music.ui.screens.search.VibeCategoryPicker
import com.nocturne.music.viewmodels.OnlineSearchSuggestionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLEncoder

private val PAGE_GUTTER = 20.dp

@Composable
fun ModernSearchScreen(
    navController: NavController,
    pureBlack: Boolean = false,
    viewModel: OnlineSearchSuggestionViewModel = hiltViewModel(),
) {
    var queryText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val bottomInset = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    val listState = rememberLazyListState()

    val viewState by viewModel.viewState.collectAsState()

    val onPerformSearch: (String) -> Unit = { query ->
        if (query.isNotBlank()) {
            focusManager.clearFocus()
            keyboardController?.hide()
            coroutineScope.launch(Dispatchers.IO) {
                database.query {
                    insert(SearchHistory(query = query))
                }
            }
            navController.navigate("search/${URLEncoder.encode(query, "UTF-8")}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        // Search Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.search),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp)
            )

            // BitChord Liquid Search Field
            SearchField(
                query = queryText,
                onQueryChange = {
                    queryText = it
                    viewModel.query.value = it
                },
                onSubmit = { onPerformSearch(queryText) },
                focusRequester = focusRequester,
                placeholder = stringResource(R.string.search),
                modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 6.dp)
            )
        }

        // Search Content
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = bottomInset + 32.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            // If typing, show suggestions
            if (queryText.isNotBlank()) {
                val suggestions = viewState.suggestions
                if (suggestions.isNotEmpty()) {
                    items(suggestions, key = { "sugg_$it" }) { suggestion ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clickable { onPerformSearch(suggestion) }
                                .padding(horizontal = PAGE_GUTTER)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                queryText = suggestion
                                viewModel.query.value = suggestion
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.NorthWest,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // If query is empty, show recent searches
                val history = viewState.history
                if (history.isNotEmpty()) {
                    item(key = "history_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.history),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.clear),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        database.query {
                                            clearSearchHistory()
                                        }
                                    }
                                }
                            )
                        }
                    }

                    items(history.take(6), key = { "hist_${it.id}" }) { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clickable { onPerformSearch(item.query) }
                                .padding(horizontal = PAGE_GUTTER)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = item.query,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    database.query {
                                        delete(item)
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Vibe / Mood picker tiles below
                item(key = "vibes") {
                    Column(Modifier.padding(top = 16.dp)) {
                        Text(
                            text = stringResource(R.string.explore),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp)
                        )
                        VibeCategoryPicker(
                            navController = navController,
                            onVibeSelect = { onPerformSearch(it) }
                        )
                    }
                }
            }
        }
    }
}
