/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nocturne.music.ui.screens.search

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.nocturne.music.ui.screens.modern.ModernSearchScreen

@Composable
fun SearchScreen(
    navController: NavController,
    pureBlack: Boolean = false,
) {
    ModernSearchScreen(
        navController = navController,
        pureBlack = pureBlack,
    )
}
