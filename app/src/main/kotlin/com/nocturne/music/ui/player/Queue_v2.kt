package com.nocturne.music.ui.player

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import com.nocturne.music.sync.LocalRemoteSyncManager
import com.nocturne.music.sync.PlaybackDeviceTarget
import com.nocturne.music.sync.RemoteConnectionState
import com.nocturne.music.sync.RemoteTrack
import com.nocturne.music.LocalPlayerConnection
import com.nocturne.music.R
import com.nocturne.music.constants.PlayerBackgroundStyle
import com.nocturne.music.constants.PlayerBackgroundStyleKey
import com.nocturne.music.constants.QueueEditLockKey
import com.nocturne.music.utils.rememberEnumPreference
import com.nocturne.music.utils.rememberPreference
import com.nocturne.music.extensions.move
import com.nocturne.music.extensions.toggleRepeatMode
import com.nocturne.music.ui.component.MediaMetadataListItem
import com.nocturne.music.utils.listItemShape
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import com.nocturne.music.extensions.metadata
import com.nocturne.music.ui.component.LocalMenuState
import com.nocturne.music.ui.component.LocalBottomSheetPageState
import com.nocturne.music.ui.menu.QueueMenu
import com.nocturne.music.ui.utils.ShowMediaInfo
import com.nocturne.music.ui.component.BottomSheetState
import androidx.navigation.NavController
import com.nocturne.music.ui.component.ActionPromptDialog
import com.nocturne.music.utils.makeTimeString
import kotlin.math.roundToInt
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.alpha

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QueueV2(
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    
    val queueWindows by playerConnection.queueWindows.collectAsState()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    
    val remoteSyncManager = LocalRemoteSyncManager.current
    val remoteRoomState by remoteSyncManager.remoteRoomState.collectAsState()
    val isRemoteDesktop by remoteSyncManager.isRemoteDesktop.collectAsState()
    val remoteConnState by remoteSyncManager.connectionState.collectAsState()
    val isConnectedToDesktop = remoteConnState == RemoteConnectionState.CONNECTED || isRemoteDesktop
    var selectedQueueTab by rememberSaveable { mutableStateOf(if (isConnectedToDesktop) "PC" else "MOBILE") }

    LaunchedEffect(isConnectedToDesktop) {
        if (isConnectedToDesktop) {
            selectedQueueTab = "PC"
        }
    }

    var locked by rememberPreference(QueueEditLockKey, false)

    // Sleep Timer
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var sleepTimerValue by remember { mutableFloatStateOf(30f) }
    val sleepTimerEnabled = remember(
        playerConnection.service.sleepTimer.triggerTime,
        playerConnection.service.sleepTimer.pauseWhenSongEnd
    ) {
        playerConnection.service.sleepTimer.isActive
    }
    var sleepTimerTimeLeft by remember { mutableLongStateOf(0L) }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft = if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                    playerConnection.player.duration - playerConnection.player.currentPosition
                } else {
                    playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                }
                delay(1000L)
            }
        }
    }

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )
    val adaptivePrimary = if (playerBackground == PlayerBackgroundStyle.DEFAULT) MaterialTheme.colorScheme.onSurface else Color.White
    val adaptiveSecondary = if (playerBackground == PlayerBackgroundStyle.DEFAULT) MaterialTheme.colorScheme.onSurfaceVariant else Color.White.copy(alpha = 0.7f)
    val adaptiveSurface = if (playerBackground == PlayerBackgroundStyle.DEFAULT) MaterialTheme.colorScheme.surfaceVariant else Color.White.copy(alpha = 0.2f)

    val lazyListState = rememberLazyListState()
    val mutableQueueWindows = remember { mutableStateListOf<Timeline.Window>() }
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    
    val currentPlayingUid = remember(currentWindowIndex, queueWindows) {
        if (currentWindowIndex in queueWindows.indices) {
            queueWindows[currentWindowIndex].uid
        } else null
    }

    LaunchedEffect(queueWindows) {
        mutableQueueWindows.apply {
            clear()
            addAll(queueWindows)
        }
    }
    
    val headerItems = 0

    LaunchedEffect(mutableQueueWindows.size, currentWindowIndex) {
        if (currentWindowIndex in mutableQueueWindows.indices) {
            lazyListState.scrollToItem(currentWindowIndex)
        }
    }

    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState
    ) { from, to ->
        val currentDragInfo = dragInfo
        dragInfo = if (currentDragInfo == null) {
            from.index to to.index
        } else {
            currentDragInfo.first to to.index
        }

        val safeFrom = (from.index - headerItems).coerceIn(0, mutableQueueWindows.lastIndex)
        val safeTo = (to.index - headerItems).coerceIn(0, mutableQueueWindows.lastIndex)
        mutableQueueWindows.move(safeFrom, safeTo)
    }

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            dragInfo?.let { (from, to) ->
                val safeFrom = (from - headerItems).coerceIn(0, queueWindows.lastIndex)
                val safeTo = (to - headerItems).coerceIn(0, queueWindows.lastIndex)

                if (!shuffleModeEnabled) {
                    playerConnection.player.moveMediaItem(safeFrom, safeTo)
                } else {
                    playerConnection.player.setShuffleOrder(
                        DefaultShuffleOrder(
                            queueWindows.map { it.firstPeriodIndex }
                                .toMutableList()
                                .move(safeFrom, safeTo)
                                .toIntArray(),
                            System.currentTimeMillis()
                        )
                    )
                }
                playerConnection.service.saveQueueToDisk(immediate = true)
                dragInfo = null
            }
        }
    }

    CompositionLocalProvider(LocalContentColor provides adaptivePrimary) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // Fixed Top Control Pills
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val pillShape = RoundedCornerShape(16.dp)
            val activeColor = adaptivePrimary.copy(alpha = 0.25f)
            val inactiveColor = adaptivePrimary.copy(alpha = 0.1f)
            
            // Shuffle
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(pillShape)
                    .background(if (shuffleModeEnabled) activeColor else inactiveColor)
                    .clickable { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled },
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.shuffle), contentDescription = "Shuffle", tint = adaptivePrimary, modifier = Modifier.size(24.dp))
            }
            // Repeat
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(pillShape)
                    .background(if (repeatMode != Player.REPEAT_MODE_OFF) activeColor else inactiveColor)
                    .clickable { playerConnection.player.toggleRepeatMode() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                            else -> R.drawable.repeat
                        }
                    ),
                    contentDescription = "Repeat",
                    tint = adaptivePrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            // Timer
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(pillShape)
                    .background(if (sleepTimerEnabled) activeColor else inactiveColor)
                    .clickable {
                        if (sleepTimerEnabled) {
                            playerConnection.service.sleepTimer.clear()
                        } else {
                            showSleepTimerDialog = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.bedtime), contentDescription = "Sleep Timer", tint = adaptivePrimary, modifier = Modifier.size(24.dp))
                    if (sleepTimerEnabled) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = makeTimeString(sleepTimerTimeLeft.coerceAtLeast(0L)),
                            style = MaterialTheme.typography.labelSmall,
                            color = adaptivePrimary
                        )
                    }
                }
            }
        }

        // Queue Header Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.queue),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = adaptivePrimary
            )
            IconButton(onClick = { locked = !locked }) {
                Icon(
                    painter = painterResource(if (locked) R.drawable.lock else R.drawable.lock_open),
                    contentDescription = if (locked) "Unlock Queue" else "Lock Queue",
                    tint = adaptiveSecondary
                )
            }
        }

        if (remoteConnState == RemoteConnectionState.CONNECTED || remoteRoomState != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Queue segmented pill switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(adaptiveSurface)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isMobileTab = selectedQueueTab == "MOBILE"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isMobileTab) adaptivePrimary.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { selectedQueueTab = "MOBILE" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Mobile Queue (${queueWindows.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = adaptivePrimary
                            )
                            if (!isRemoteDesktop) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }

                    val isPcTab = selectedQueueTab == "PC"
                    val pcCount = remoteRoomState?.queue?.size ?: 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isPcTab) adaptivePrimary.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { selectedQueueTab = "PC" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "PC Queue ($pcCount)",
                                style = MaterialTheme.typography.labelMedium,
                                color = adaptivePrimary
                            )
                            if (isRemoteDesktop) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                            }
                        }
                    }
                }

                // Active queue indicator banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isRemoteDesktop) Color(0xFF10B981) else MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = if (isRemoteDesktop) "Playing from PC Queue" else "Playing from Mobile Queue",
                            style = MaterialTheme.typography.labelSmall,
                            color = adaptiveSecondary
                        )
                    }
                    TextButton(
                        onClick = {
                            val target = if (isRemoteDesktop) PlaybackDeviceTarget.LOCAL else PlaybackDeviceTarget.REMOTE_DESKTOP
                            remoteSyncManager.setPlaybackTarget(target)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isRemoteDesktop) "Switch to Mobile" else "Switch to PC",
                            style = MaterialTheme.typography.labelSmall,
                            color = adaptivePrimary
                        )
                    }
                }
            }
        }

        if (selectedQueueTab == "PC") {
            val pcQueue = remoteRoomState?.queue.orEmpty()
            if (pcQueue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No songs in Nocturne PC queue",
                        style = MaterialTheme.typography.bodyMedium,
                        color = adaptiveSecondary
                    )
                }
            } else {
                val pcLazyListState = rememberLazyListState()
                val pcReorderableState = rememberReorderableLazyListState(
                    lazyListState = pcLazyListState
                ) { from, to ->
                    remoteSyncManager.moveQueueTrack(from.index, to.index)
                }

                LazyColumn(
                    state = pcLazyListState,
                    contentPadding = PaddingValues(bottom = 120.dp, top = 4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(pcQueue, key = { index, track -> "${track.id}_$index" }) { index, track ->
                        ReorderableItem(
                            state = pcReorderableState,
                            key = "${track.id}_$index"
                        ) { isDragging ->
                            val elevation by animateDpAsState(
                                targetValue = if (isDragging) 6.dp else 0.dp,
                                label = "pc_drag_elevation"
                            )
                            val isCurrent = track.id == remoteRoomState?.current_track?.id
                            val rowBg = when {
                                isDragging -> MaterialTheme.colorScheme.surfaceContainerHigh
                                isCurrent -> adaptivePrimary.copy(alpha = 0.15f)
                                else -> Color.Transparent
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(elevation, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(rowBg)
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { remoteSyncManager.playQueueTrack(track) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (isCurrent) {
                                        Icon(
                                            painter = painterResource(R.drawable.volume_up),
                                            contentDescription = "Playing",
                                            tint = adaptivePrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = adaptiveSecondary,
                                            modifier = Modifier.width(20.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    AsyncImage(
                                        model = track.thumbnail,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = adaptivePrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = track.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = adaptiveSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (track.duration_ms > 0) {
                                        Text(
                                            text = makeTimeString(track.duration_ms),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = adaptiveSecondary
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .draggableHandle()
                                        .size(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.drag_handle),
                                        contentDescription = "Drag to reorder",
                                        tint = adaptiveSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(bottom = 120.dp, top = 4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Queue List
            itemsIndexed(
                items = mutableQueueWindows,
                key = { _, item -> item.uid.hashCode() }
            ) { index, window ->
                ReorderableItem(
                    state = reorderableState,
                    key = window.uid.hashCode()
                ) {
                    val isActive = window.uid == currentPlayingUid

                    @OptIn(ExperimentalMaterial3Api::class)
                    val dismissBoxState = rememberSwipeToDismissBoxState(
                        positionalThreshold = { totalDistance -> totalDistance }
                    )
                    var processedDismiss by remember { mutableStateOf(false) }

                    LaunchedEffect(dismissBoxState.currentValue) {
                        val dv = dismissBoxState.currentValue
                        if (!processedDismiss && (dv == SwipeToDismissBoxValue.StartToEnd || dv == SwipeToDismissBoxValue.EndToStart)) {
                            processedDismiss = true
                            playerConnection.player.removeMediaItem(window.firstPeriodIndex)
                        }
                        if (dv == SwipeToDismissBoxValue.Settled) {
                            processedDismiss = false
                        }
                    }

                    val content: @Composable () -> Unit = {
                        MediaMetadataListItem(
                            mediaMetadata = window.mediaItem.metadata!!,
                            isSelected = false,
                            isActive = isActive,
                            isPlaying = isPlaying && isActive,
                            backgroundColor = Color.Transparent,
                            subtitleColor = adaptiveSecondary,
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                QueueMenu(
                                                    mediaMetadata = window.mediaItem.metadata!!,
                                                    navController = navController,
                                                    playerBottomSheetState = playerBottomSheetState,
                                                    onShowDetailsDialog = {
                                                        window.mediaItem.mediaId.let {
                                                            bottomSheetPageState.show {
                                                                ShowMediaInfo(it)
                                                            }
                                                        }
                                                    },
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = "Options"
                                        )
                                    }

                                    if (!locked) {
                                        Box(
                                            modifier = Modifier
                                                .draggableHandle()
                                                .size(40.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.drag_handle),
                                                contentDescription = "Drag to reorder",
                                                tint = adaptiveSecondary
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                                    playerConnection.player.playWhenReady = true
                                }
                        )
                    }

                    if (locked) {
                        content()
                    } else {
                        @OptIn(ExperimentalMaterial3Api::class)
                        SwipeToDismissBox(
                            state = dismissBoxState,
                            backgroundContent = {
                                val color by animateColorAsState(
                                    targetValue = when (dismissBoxState.targetValue) {
                                        SwipeToDismissBoxValue.Settled -> Color.Transparent
                                        else -> MaterialTheme.colorScheme.error
                                    }, label = ""
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 4.dp, horizontal = 16.dp)
                                        .background(color),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    val iconAlpha by animateFloatAsState(
                                        targetValue = if (dismissBoxState.targetValue != SwipeToDismissBoxValue.Settled) 1f else 0f,
                                        label = "iconAlpha"
                                    )
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        modifier = Modifier
                                            .padding(end = 16.dp)
                                            .alpha(iconAlpha),
                                        tint = MaterialTheme.colorScheme.onError
                                    )
                                }
                            },
                            content = { content() },
                            enableDismissFromStartToEnd = false
                        )
                    }
                }
            }
        }
            // end of queue
            } // end Column
    } // end CompositionLocalProvider
    }

    if (showSleepTimerDialog) {
        ActionPromptDialog(
            titleBar = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.sleep_timer),
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            },
            onDismiss = { showSleepTimerDialog = false },
            onConfirm = {
                showSleepTimerDialog = false
                playerConnection.service.sleepTimer.start(sleepTimerValue.roundToInt())
            },
            onCancel = { showSleepTimerDialog = false },
            onReset = { sleepTimerValue = 30f },
            content = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.minute,
                            sleepTimerValue.roundToInt(),
                            sleepTimerValue.roundToInt()
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(16.dp))
                    Slider(
                        value = sleepTimerValue,
                        onValueChange = { sleepTimerValue = it },
                        valueRange = 5f..120f,
                        steps = (120 - 5) / 5 - 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            showSleepTimerDialog = false
                            playerConnection.service.sleepTimer.start(-1)
                        }
                    ) {
                        Text(stringResource(R.string.end_of_song))
                    }
                }
            }
        )
    }
}

