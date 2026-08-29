package com.nocturne.music.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import com.nocturne.music.ui.components.BottomNavBar
import com.nocturne.music.ui.components.MiniPlayer
import com.nocturne.music.ui.components.NavigationTab
import com.nocturne.music.ui.viewmodel.HomeViewModel
import com.nocturne.music.ui.viewmodel.PlayerViewModel
import com.nocturne.music.ui.viewmodel.SearchViewModel
import com.nocturne.music.ui.viewmodel.SyncViewModel
import org.koin.androidx.compose.koinViewModel

@UnstableApi
@Composable
fun MainScreen(
    homeViewModel: HomeViewModel = koinViewModel(),
    searchViewModel: SearchViewModel = koinViewModel(),
    playerViewModel: PlayerViewModel = koinViewModel(),
    syncViewModel: SyncViewModel = koinViewModel()
) {
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
    val isPlayerSheetVisible by playerViewModel.isPlayerSheetVisible.collectAsState()
    val playbackState by playerViewModel.playbackState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                Column {
                    // Mini-player bar when a track is loaded
                    if (playbackState.currentTrack != null) {
                        MiniPlayer(
                            playbackState = playbackState,
                            onPlayPauseClick = { playerViewModel.togglePlayPause() },
                            onNextClick = { playerViewModel.next() },
                            onClick = { playerViewModel.showPlayerSheet() }
                        )
                    }

                    BottomNavBar(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    NavigationTab.HOME -> HomeScreen(
                        viewModel = homeViewModel,
                        onPlayTrack = { track -> playerViewModel.playTrack(track) }
                    )
                    NavigationTab.SEARCH -> SearchScreen(
                        viewModel = searchViewModel,
                        onPlayTrack = { track -> playerViewModel.playTrack(track) }
                    )
                    NavigationTab.SETTINGS -> SettingsScreen()
                }
            }
        }

        // Full-screen Player Sheet
        AnimatedVisibility(
            visible = isPlayerSheetVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            PlayerScreen(
                viewModel = playerViewModel,
                onDismiss = { playerViewModel.hidePlayerSheet() }
            )
        }
    }
}
