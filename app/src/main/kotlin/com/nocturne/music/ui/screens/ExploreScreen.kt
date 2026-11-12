/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nocturne.music.ui.screens

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.nocturne.music.ui.screens.modern.ModernExploreScreen
import com.nocturne.music.viewmodels.ChartsViewModel
import com.nocturne.music.viewmodels.ExploreViewModel

@Composable
fun ExploreScreen(
    navController: NavController,
    exploreViewModel: ExploreViewModel = hiltViewModel(),
    chartsViewModel: ChartsViewModel = hiltViewModel(),
) {
    ModernExploreScreen(
        navController = navController,
        exploreViewModel = exploreViewModel,
        chartsViewModel = chartsViewModel,
    )
}
