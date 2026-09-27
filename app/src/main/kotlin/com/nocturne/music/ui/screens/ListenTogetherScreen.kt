/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nocturne.music.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.nocturne.music.nocturne.AudioDeviceBottomSheet
import com.nocturne.music.ui.component.DefaultDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton as MaterialIconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import coil3.compose.AsyncImage
import com.nocturne.music.constants.ListenTogetherServerUrlKey
import com.nocturne.music.listentogether.TrackInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.nocturne.music.LocalListenTogetherManager
import com.nocturne.music.LocalPlayerAwareWindowInsets
import com.nocturne.music.R
import com.nocturne.music.constants.AppBarHeight
import com.nocturne.music.constants.ListenTogetherInTopBarKey
import com.nocturne.music.constants.ListenTogetherUsernameKey
import com.nocturne.music.listentogether.ConnectionState
import com.nocturne.music.listentogether.JoinRequestPayload
import com.nocturne.music.listentogether.ListenTogetherEvent
import com.nocturne.music.listentogether.SuggestionReceivedPayload
import com.nocturne.music.listentogether.UserInfo
import com.nocturne.music.ui.component.IconButton
import com.nocturne.music.ui.utils.backToMain
import com.nocturne.music.utils.rememberPreference
import kotlinx.coroutines.launch

object ListenTogetherInvite {
    fun makeInvite(server: String, code: String): String {
        val cleanServer = server
            .removePrefix("wss://")
            .removePrefix("ws://")
            .removeSuffix("/ws")
        return "${code.uppercase()}@$cleanServer"
    }

    fun parseInvite(raw: String): Pair<String?, String> {
        val s = raw.trim()
        if (s.startsWith("LMSC~")) {
            try {
                val decoded = String(android.util.Base64.decode(s.removePrefix("LMSC~"), android.util.Base64.DEFAULT))
                val parts = decoded.split('|')
                if (parts.size >= 2) {
                    return Pair(parts[0], parts[1].uppercase())
                }
            } catch (_: Exception) {}
        }
        val at = s.lastIndexOf('@')
        if (at < 0) {
            return Pair(null, s.uppercase())
        }
        var server = s.substring(at + 1)
        if (!server.startsWith("ws://") && !server.startsWith("wss://")) {
            server = "wss://$server"
        }
        val pathPart = server.removePrefix("wss://").removePrefix("ws://")
        if (!pathPart.contains('/')) {
            server += "/ws"
        }
        val code = s.substring(0, at).uppercase()
        return Pair(server, code)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ListenTogetherScreen(
    navController: NavController,
    showTopBar: Boolean = false
) {
    val context = LocalContext.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val windowInsets = LocalPlayerAwareWindowInsets.current

    if (listenTogetherManager == null) {
        NotConfiguredContent()
        return
    }

    val connectionState by listenTogetherManager.connectionState.collectAsState()
    val roomState by listenTogetherManager.roomState.collectAsState()
    val userId by listenTogetherManager.userId.collectAsState()
    val pendingJoinRequests by listenTogetherManager.pendingJoinRequests.collectAsState()
    val pendingSuggestions by listenTogetherManager.pendingSuggestions.collectAsState()

    val (listenTogetherInTopBar) = rememberPreference(ListenTogetherInTopBarKey, defaultValue = true)
    val shouldShowTopBar = showTopBar || listenTogetherInTopBar
    
    var savedUsername by rememberPreference(ListenTogetherUsernameKey, "")
    var savedServerUrl by rememberPreference(ListenTogetherServerUrlKey, listenTogetherManager.getServerUrl())
    var mode by rememberSaveable { mutableStateOf("join") }
    var inviteInput by rememberSaveable { mutableStateOf("") }
    var serverUrlInput by rememberSaveable { mutableStateOf(savedServerUrl) }
    var usernameInput by rememberSaveable { mutableStateOf(savedUsername) }

    var isCreatingRoom by rememberSaveable { mutableStateOf(false) }
    var isJoiningRoom by rememberSaveable { mutableStateOf(false) }
    var joinErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    var selectedUserForMenu by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedUsername by rememberSaveable { mutableStateOf<String?>(null) }

    val waitingForApprovalText = stringResource(R.string.waiting_for_approval)
    val invalidRoomCodeText = stringResource(R.string.invalid_room_code)
    val joinRequestDeniedText = stringResource(R.string.join_request_denied)

    LaunchedEffect(savedUsername) {
        if (usernameInput.isBlank() && savedUsername.isNotBlank()) {
            usernameInput = savedUsername
        }
    }
    LaunchedEffect(savedServerUrl) {
        if (serverUrlInput.isBlank() && savedServerUrl.isNotBlank()) {
            serverUrlInput = savedServerUrl
        }
    }

    LaunchedEffect(listenTogetherManager) {
        listenTogetherManager.events.collect { event ->
            when (event) {
                is ListenTogetherEvent.JoinRejected -> {
                    val reason = event.reason
                    joinErrorMessage = when {
                        reason.isNullOrBlank() -> joinRequestDeniedText
                        reason.contains("invalid", ignoreCase = true) -> invalidRoomCodeText
                        else -> "$joinRequestDeniedText: $reason"
                    }
                    isJoiningRoom = false
                    isCreatingRoom = false
                }
                is ListenTogetherEvent.JoinApproved -> {
                    isJoiningRoom = false
                    joinErrorMessage = null
                }
                is ListenTogetherEvent.RoomCreated -> {
                    isCreatingRoom = false
                    val server = listenTogetherManager.getServerUrl()
                    val invite = ListenTogetherInvite.makeInvite(server, event.roomCode)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("ListenTogetherInvite", invite)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Invite copied: $invite", Toast.LENGTH_SHORT).show()
                }
                is ListenTogetherEvent.ConnectionError -> {
                    if (isJoiningRoom || isCreatingRoom) {
                        isJoiningRoom = false
                        isCreatingRoom = false
                        joinErrorMessage = event.error
                    }
                }
                else -> {}
            }
        }
    }

    val isInRoom = listenTogetherManager.isInRoom
    val isHost = roomState?.hostId == userId

    // User action menu dialog
    if (selectedUserForMenu != null && selectedUsername != null) {
        UserActionDialog(
            username = selectedUsername ?: "",
            onKick = {
                selectedUserForMenu?.let {
                    listenTogetherManager.kickUser(it, "Removed by host")
                }
                selectedUserForMenu = null
                selectedUsername = null
            },
            onPermanentKick = {
                selectedUserForMenu?.let { userId ->
                    selectedUsername?.let { username ->
                        listenTogetherManager.blockUser(username)
                        listenTogetherManager.kickUser(userId, R.string.user_blocked_by_host.toString())
                    }
                }
                selectedUserForMenu = null
                selectedUsername = null
            },
            onTransferOwnership = {
                selectedUserForMenu?.let {
                    listenTogetherManager.transferHost(it)
                }
                selectedUserForMenu = null
                selectedUsername = null
            },
            onDismiss = {
                selectedUserForMenu = null
                selectedUsername = null
            }
        )
    }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var showAudioDeviceBottomSheet by remember { mutableStateOf(false) }
    
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    
    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = windowInsets.asPaddingValues().calculateTopPadding() + 16.dp,
            bottom = windowInsets.asPaddingValues().calculateBottomPadding() + 16.dp + AppBarHeight
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            HeaderSection(isInRoom = isInRoom)
        }

        // Connection status card
        item {
            ConnectionStatusCard(
                connectionState = connectionState,
                onConnect = { listenTogetherManager.connect() },
                onDisconnect = { listenTogetherManager.disconnect() },
                onReconnect = { listenTogetherManager.forceReconnect() }
            )
        }

        if (connectionState == ConnectionState.CONNECTED && !isInRoom) {
            item {
                Text(
                    text = stringResource(R.string.listen_together_background_disconnect_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (isInRoom) {
            // Room status card
            roomState?.let { room ->
                item {
                    RoomStatusCard(
                        roomCode = room.roomCode,
                        serverUrl = listenTogetherManager.getServerUrl(),
                        currentTrack = room.currentTrack,
                        isHost = isHost,
                        connectionState = connectionState,
                        context = context,
                        navController = navController,
                        onAudioOutputClick = { showAudioDeviceBottomSheet = true }
                    )
                }

                // Connected users
                val connectedUsers = room.users.filter { it.isConnected }
                val currentUserIdValue = userId ?: ""
                item {
                    ConnectedUsersSection(
                        users = connectedUsers,
                        isHost = isHost,
                        currentUserId = currentUserIdValue,
                        onUserClick = { clickedUserId, username ->
                            if (isHost && clickedUserId != currentUserIdValue) {
                                selectedUserForMenu = clickedUserId
                                selectedUsername = username
                            }
                        }
                    )
                }

                // Pending join requests (host only)
                if (isHost && pendingJoinRequests.isNotEmpty()) {
                    item {
                        PendingJoinRequestsSection(
                            requests = pendingJoinRequests,
                            onApprove = { listenTogetherManager.approveJoin(it) },
                            onReject = { listenTogetherManager.rejectJoin(it, "Rejected by host") }
                        )
                    }
                }

                // Pending suggestions (host only)
                if (isHost && pendingSuggestions.isNotEmpty()) {
                    item {
                        PendingSuggestionsSection(
                            suggestions = pendingSuggestions,
                            onApprove = { listenTogetherManager.approveSuggestion(it) },
                            onReject = { listenTogetherManager.rejectSuggestion(it, "Rejected by host") }
                        )
                    }
                }

                // Footer actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!isHost) {
                            OutlinedButton(
                                onClick = {
                                    listenTogetherManager.requestSync()
                                    Toast.makeText(context, "Sync requested", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.sync),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Re-sync")
                            }
                        }
                        Button(
                            onClick = { listenTogetherManager.leaveRoom() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.logout),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isHost) "End Session" else stringResource(R.string.leave_room),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        } else if (isJoiningRoom) {
            // Waiting for approval (PC-aligned waiting screen)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = if (connectionState == ConnectionState.CONNECTING) "Connecting…" else waitingForApprovalText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(
                            onClick = {
                                isJoiningRoom = false
                                listenTogetherManager.disconnect()
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(android.R.string.cancel))
                        }
                    }
                }
            }
        } else {
            // Join / Host tabbed section
            item {
                JoinCreateRoomSection(
                    mode = mode,
                    onModeChange = { mode = it },
                    usernameInput = usernameInput,
                    onUsernameChange = { usernameInput = it },
                    inviteInput = inviteInput,
                    onInviteChange = { inviteInput = it },
                    serverUrlInput = serverUrlInput,
                    onServerUrlChange = { serverUrlInput = it },
                    joinErrorMessage = joinErrorMessage,
                    bringIntoViewRequester = bringIntoViewRequester,
                    onJoinSession = {
                        val name = usernameInput.trim()
                        if (name.isEmpty()) {
                            Toast.makeText(context, "Enter a name first", Toast.LENGTH_SHORT).show()
                            return@JoinCreateRoomSection
                        }
                        val parsed = ListenTogetherInvite.parseInvite(inviteInput)
                        if (parsed.second.isEmpty()) {
                            Toast.makeText(context, "Paste the invite code your friend sent", Toast.LENGTH_SHORT).show()
                            return@JoinCreateRoomSection
                        }
                        savedUsername = name
                        val targetServer = parsed.first ?: listenTogetherManager.getServerUrl()
                        if (parsed.first != null) {
                            listenTogetherManager.setServerUrl(targetServer)
                            savedServerUrl = targetServer
                        }
                        isJoiningRoom = true
                        isCreatingRoom = false
                        joinErrorMessage = null
                        listenTogetherManager.connect(targetServer)
                        listenTogetherManager.joinRoom(parsed.second, name)
                    },
                    onStartSession = {
                        val name = usernameInput.trim()
                        if (name.isEmpty()) {
                            Toast.makeText(context, "Enter a name first", Toast.LENGTH_SHORT).show()
                            return@JoinCreateRoomSection
                        }
                        val sUrl = serverUrlInput.trim()
                        if (sUrl.isEmpty()) {
                            Toast.makeText(context, "Enter your sync server URL", Toast.LENGTH_SHORT).show()
                            return@JoinCreateRoomSection
                        }
                        savedUsername = name
                        savedServerUrl = sUrl
                        listenTogetherManager.setServerUrl(sUrl)
                        isCreatingRoom = true
                        isJoiningRoom = false
                        joinErrorMessage = null
                        listenTogetherManager.connect(sUrl)
                        listenTogetherManager.createRoom(name)
                    },
                    onFieldFocused = {
                        coroutineScope.launch {
                            bringIntoViewRequester.bringIntoView()
                        }
                    }
                )
            }
        }

        // Settings link
        item {
            SettingsLinkCard(
                onClick = { navController.navigate("settings/integrations/listen_together") }
            )
        }
    }

    if (shouldShowTopBar) {
        TopAppBar(
            title = { Text(stringResource(R.string.together)) },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = null
                    )
                }
            }
        )
    }

    if (showAudioDeviceBottomSheet) {
        AudioDeviceBottomSheet(onDismiss = { showAudioDeviceBottomSheet = false })
    }
}

@Composable
private fun NotConfiguredContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.group),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.listen_together),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.listen_together_not_configured),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HeaderSection(isInRoom: Boolean = false) {
    if (isInRoom) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: large icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.group_outlined),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Right side: title + subtitle
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.listen_together),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.listen_together_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    connectionState: ConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onReconnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> MaterialTheme.colorScheme.secondaryContainer
                ConnectionState.ERROR -> MaterialTheme.colorScheme.errorContainer
                ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.surfaceContainerHigh
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            color = when (connectionState) {
                                ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
                                ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> MaterialTheme.colorScheme.tertiary
                                ConnectionState.ERROR -> MaterialTheme.colorScheme.error
                                ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.outline
                            }
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = when (connectionState) {
                        ConnectionState.CONNECTED -> stringResource(R.string.listen_together_connected)
                        ConnectionState.CONNECTING -> stringResource(R.string.listen_together_connecting)
                        ConnectionState.RECONNECTING -> stringResource(R.string.listen_together_reconnecting)
                        ConnectionState.ERROR -> stringResource(R.string.listen_together_error)
                        ConnectionState.DISCONNECTED -> stringResource(R.string.listen_together_disconnected)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (connectionState) {
                        ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
                        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> MaterialTheme.colorScheme.tertiary
                        ConnectionState.ERROR -> MaterialTheme.colorScheme.error
                        ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            if (connectionState == ConnectionState.CONNECTING || connectionState == ConnectionState.RECONNECTING) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (connectionState == ConnectionState.DISCONNECTED || connectionState == ConnectionState.ERROR) {
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.link),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.connect), fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = onDisconnect,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(R.string.disconnect), fontWeight = FontWeight.SemiBold)
                    }
                    FilledTonalButton(
                        onClick = onReconnect,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reconnect", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomStatusCard(
    roomCode: String,
    serverUrl: String,
    currentTrack: TrackInfo?,
    isHost: Boolean,
    connectionState: ConnectionState,
    context: Context,
    navController: NavController,
    onAudioOutputClick: () -> Unit
) {
    val invite = remember(serverUrl, roomCode) {
        ListenTogetherInvite.makeInvite(serverUrl, roomCode)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Role & status
            Text(
                text = "${if (isHost) "Hosting" else "Listening"} · ${connectionState.name.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )

            // Invite code display
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ROOM INVITE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = invite,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Copy invite button
            FilledTonalButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Listen Together Invite", invite)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Invite copied ($invite)", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(R.drawable.content_copy),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy invite")
            }

            // Now playing track (if any)
            if (currentTrack != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!currentTrack.thumbnail.isNullOrEmpty()) {
                            AsyncImage(
                                model = currentTrack.thumbnail,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.album),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "NOW PLAYING",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentTrack.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!currentTrack.artist.isNullOrEmpty()) {
                                Text(
                                    text = currentTrack.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Quick actions: Chat & Audio devices
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { navController.navigate("listen_together/chat") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.chat_msg),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.comments),
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onAudioOutputClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.speaker_apple),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.audio_devices),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectedUsersSection(
    users: List<UserInfo>,
    isHost: Boolean,
    currentUserId: String,
    onUserClick: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "${stringResource(R.string.connected_users)} (${users.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                users.forEach { user ->
                    UserAvatar(
                        user = user,
                        isCurrentUser = user.userId == currentUserId,
                        isClickable = isHost && user.userId != currentUserId,
                        onClick = { onUserClick(user.userId, user.username) }
                    )
                }
            }
        }
    }
}

@Composable
private fun UserAvatar(
    user: UserInfo,
    isCurrentUser: Boolean,
    isClickable: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(enabled = isClickable, onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = when {
                    user.isHost -> MaterialTheme.colorScheme.primary
                    isCurrentUser -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = user.username.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            user.isHost -> MaterialTheme.colorScheme.onPrimary
                            isCurrentUser -> MaterialTheme.colorScheme.onSecondary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            if (user.isHost || isCurrentUser) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(20.dp),
                    shape = CircleShape,
                    color = if (user.isHost) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            painter = painterResource(
                                if (user.isHost) R.drawable.crown else R.drawable.person
                            ),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = user.username,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
            color = if (user.isHost) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        if (user.isHost) {
            Text(
                text = stringResource(R.string.host_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        } else if (isCurrentUser) {
            Text(
                text = stringResource(R.string.you_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun PendingJoinRequestsSection(
    requests: List<JoinRequestPayload>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.listen_together_join_requests),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            requests.forEach { request ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = request.username.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = request.username,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    MaterialIconButton(onClick = { onApprove(request.userId) }) {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = stringResource(R.string.approve),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    MaterialIconButton(onClick = { onReject(request.userId) }) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = stringResource(R.string.reject),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingSuggestionsSection(
    suggestions: List<SuggestionReceivedPayload>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.pending_suggestions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            suggestions.forEach { suggestion ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = suggestion.trackInfo.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = suggestion.fromUsername,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    MaterialIconButton(onClick = { onApprove(suggestion.suggestionId) }) {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = stringResource(R.string.approve),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    MaterialIconButton(onClick = { onReject(suggestion.suggestionId) }) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = stringResource(R.string.reject),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JoinCreateRoomSection(
    mode: String,
    onModeChange: (String) -> Unit,
    usernameInput: String,
    onUsernameChange: (String) -> Unit,
    inviteInput: String,
    onInviteChange: (String) -> Unit,
    serverUrlInput: String,
    onServerUrlChange: (String) -> Unit,
    joinErrorMessage: String?,
    bringIntoViewRequester: BringIntoViewRequester,
    onJoinSession: () -> Unit,
    onStartSession: () -> Unit,
    onFieldFocused: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode toggle: Join / Host
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isJoinSelected = mode == "join"
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onModeChange("join") },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isJoinSelected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent
                    ) {
                        Text(
                            text = "Join",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isJoinSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isJoinSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val isHostSelected = mode == "host"
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onModeChange("host") },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isHostSelected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent
                    ) {
                        Text(
                            text = "Host",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isHostSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isHostSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (mode == "join") {
                // Join mode: Invite code input
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = inviteInput,
                        onValueChange = onInviteChange,
                        label = { Text("Invite code") },
                        placeholder = { Text("Paste the invite your friend sent") },
                        leadingIcon = {
                            Icon(
                                painterResource(R.drawable.group),
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (inviteInput.isNotBlank()) {
                                MaterialIconButton(onClick = { onInviteChange("") }) {
                                    Icon(painterResource(R.drawable.close), null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .onFocusChanged { if (it.isFocused) onFieldFocused() }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "The invite carries the server address, nothing else to set up.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Username input
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = onUsernameChange,
                    label = { Text(stringResource(R.string.username)) },
                    placeholder = { Text(stringResource(R.string.enter_username)) },
                    leadingIcon = {
                        Icon(
                            painterResource(R.drawable.person),
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (usernameInput.isNotBlank()) {
                            MaterialIconButton(onClick = { onUsernameChange("") }) {
                                Icon(painterResource(R.drawable.close), null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) onFieldFocused() }
                )

                // Error message
                AnimatedVisibility(
                    visible = joinErrorMessage != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Icon(
                                painterResource(R.drawable.error),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = joinErrorMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Join session button
                Button(
                    onClick = onJoinSession,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.login),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Join session", fontWeight = FontWeight.SemiBold)
                }
            } else {
                // Host mode: Sync server URL input
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = serverUrlInput,
                        onValueChange = onServerUrlChange,
                        label = { Text("Sync server") },
                        placeholder = { Text("wss://your-machine.ts.net/ws") },
                        leadingIcon = {
                            Icon(
                                painterResource(R.drawable.link),
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (serverUrlInput.isNotBlank()) {
                                MaterialIconButton(onClick = { onServerUrlChange("") }) {
                                    Icon(painterResource(R.drawable.close), null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .onFocusChanged { if (it.isFocused) onFieldFocused() }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your self-hosted server (e.g. Tailscale Funnel URL). Saved for next time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Username input
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = onUsernameChange,
                    label = { Text(stringResource(R.string.username)) },
                    placeholder = { Text(stringResource(R.string.enter_username)) },
                    leadingIcon = {
                        Icon(
                            painterResource(R.drawable.person),
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (usernameInput.isNotBlank()) {
                            MaterialIconButton(onClick = { onUsernameChange("") }) {
                                Icon(painterResource(R.drawable.close), null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) onFieldFocused() }
                )

                // Error message
                AnimatedVisibility(
                    visible = joinErrorMessage != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Icon(
                                painterResource(R.drawable.error),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = joinErrorMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Start session button
                Button(
                    onClick = onStartSession,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Start a session", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SettingsLinkCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.settings),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.listen_together_settings_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun UserActionDialog(
    username: String,
    onKick: () -> Unit,
    onPermanentKick: () -> Unit,
    onTransferOwnership: () -> Unit,
    onDismiss: () -> Unit
) {
    DefaultDialog(
        onDismiss = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.group),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.manage_user),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = username,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Kick button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onKick),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.kick_user),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = stringResource(R.string.kick_user_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Permanently kick button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPermanentKick),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.permanently_kick_user),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.permanently_kick_user_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Transfer ownership button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTransferOwnership),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.crown),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.transfer_ownership),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.transfer_ownership_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}


