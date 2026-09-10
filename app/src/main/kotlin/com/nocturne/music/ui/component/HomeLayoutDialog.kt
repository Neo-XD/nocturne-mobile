/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nocturne.music.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nocturne.music.R
import com.nocturne.music.extensions.move
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

data class HomeLayoutSectionItem(
    val key: String,
    val title: String,
    val shown: Boolean = true
)

@Composable
fun HomeLayoutDialog(
    sections: List<HomeLayoutSectionItem>,
    onDismissRequest: () -> Unit,
    onSave: (order: List<String>, hidden: Set<String>) -> Unit
) {
    val items = remember(sections) {
        mutableStateListOf<HomeLayoutSectionItem>().apply {
            addAll(sections)
        }
    }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState
    ) { from, to ->
        items.move(from.index, to.index)
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 440.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = "Edit home",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Drag to reorder. Hide anything you don't want on the page.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Section List
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 380.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    itemsIndexed(items, key = { _, item -> item.key }) { index, item ->
                        ReorderableItem(
                            state = reorderableState,
                            key = item.key
                        ) { isDragging ->
                            val elevation by animateDpAsState(
                                targetValue = if (isDragging) 4.dp else 0.dp,
                                label = "elevation"
                            )
                            val rowBg = if (isDragging) {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            } else {
                                Color.Transparent
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(elevation, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(rowBg)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (item.shown) FontWeight.Medium else FontWeight.Normal,
                                    color = if (item.shown) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            items[index] = item.copy(shown = !item.shown)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                if (item.shown) R.drawable.ic_visibility else R.drawable.ic_visibility_off
                                            ),
                                            contentDescription = if (item.shown) "Hide from home" else "Show on home",
                                            tint = if (item.shown) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .draggableHandle()
                                            .size(36.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.drag_handle),
                                            contentDescription = "Drag to reorder",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Footer Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = ButtonDefaults.ContentPadding
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            val newOrder = items.map { it.key }
                            val hidden = items.filter { !it.shown }.map { it.key }.toSet()
                            onSave(newOrder, hidden)
                            onDismissRequest()
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = ButtonDefaults.ContentPadding
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
