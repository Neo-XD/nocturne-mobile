package com.nocturne.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nocturne.music.core.model.Lyrics
import com.nocturne.music.ui.theme.NocturnePurple
import com.nocturne.music.ui.theme.TextDisabled
import com.nocturne.music.ui.theme.TextPrimary
import com.nocturne.music.ui.theme.TextSecondary

@Composable
fun SyncedLyricsView(
    lyrics: Lyrics?,
    currentPositionMs: Long,
    onLineClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (lyrics == null || (!lyrics.isSynced && lyrics.plain == null)) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = "No lyrics available",
                style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondary)
            )
        }
        return
    }

    if (!lyrics.isSynced && lyrics.plain != null) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            item {
                Text(
                    text = lyrics.plain.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        lineHeight = 28.sp,
                        color = TextPrimary
                    )
                )
            }
        }
        return
    }

    val listState = rememberLazyListState()
    val timedLines = lyrics.timed

    // Find current active lyric line
    val activeIndex = remember(currentPositionMs, timedLines) {
        val idx = timedLines.indexOfLast { it.timeMs <= currentPositionMs }
        if (idx >= 0) idx else 0
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex in timedLines.indices) {
            listState.animateScrollToItem(
                index = (activeIndex - 2).coerceAtLeast(0)
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        itemsIndexed(timedLines) { index, line ->
            val isActive = index == activeIndex
            val isPast = index < activeIndex

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLineClick(line.timeMs) }
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = line.text,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = if (isActive) 22.sp else 18.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            isActive -> TextPrimary
                            isPast -> TextSecondary
                            else -> TextDisabled
                        }
                    )
                )

                line.translation?.let { trans ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = trans,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isActive) NocturnePurple else TextDisabled,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    }
}
