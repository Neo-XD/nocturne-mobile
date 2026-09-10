/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nocturne.music.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import com.nocturne.music.constants.EnableFrostedGlassKey
import com.nocturne.music.utils.rememberPreference

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nocturne.music.ui.screens.Screens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import dev.chrisbanes.haze.hazeEffect
import com.nocturne.music.ui.component.LocalHazeState
import com.nocturne.music.ui.component.rememberNocturneHazeStyle
import com.nocturne.music.ui.component.rememberNocturneGlassBorderBrush

@Immutable
private data class NavItemState(
    val isSelected: Boolean,
    val iconRes: Int
)

@Stable
private fun isRouteSelected(currentRoute: String?, screenRoute: String, navigationItems: List<Screens>): Boolean {
    if (currentRoute == null) return false
    if (currentRoute == screenRoute) return true
    return navigationItems.any { it.route == screenRoute } && 
           currentRoute.startsWith("$screenRoute/")
}

@Composable
fun AppNavigationRail(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    floatingNav: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null
) {
    val (enableFrostedGlass) = rememberPreference(EnableFrostedGlassKey, defaultValue = true)
    val isGlassActive = enableFrostedGlass && !pureBlack && isGlassAllowed()
    val glassConfig = LocalGlassEffectConfig.current
    val containerColor = when {
        isGlassActive -> Color.Transparent
        pureBlack -> Color.Black
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val outlineColor = if (pureBlack) Color(0xFF222222) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current
    val glassBorderBrush = rememberNocturneGlassBorderBrush()
    
    if (floatingNav) {
        val pillShape = RoundedCornerShape(32.dp)
        Box(
            modifier = modifier
                .fillMaxHeight()
                .padding(start = 12.dp, top = 16.dp, bottom = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(
                modifier = Modifier
                    .width(72.dp)
                    .then(
                        if (isGlassActive) {
                            Modifier.liquidGlass(
                                config = glassConfig,
                                shape = pillShape,
                                applyEdgeEffects = true
                            )
                        } else {
                            Modifier
                                .shadow(elevation = 6.dp, shape = pillShape)
                                .background(containerColor, shape = pillShape)
                                .border(width = 1.dp, color = outlineColor, shape = pillShape)
                        }
                    )
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
            ) {
                navigationItems.forEach { screen ->
                    val isSelected = remember(currentRoute, screen.route) {
                        isRouteSelected(currentRoute, screen.route, navigationItems)
                    }
                    val iconRes = remember(isSelected, screen) {
                        if (isSelected) screen.iconIdActive else screen.iconIdInactive
                    }
                    
                    val isSearchItem = screen == Screens.Search && onSearchLongClick != null
                    val interactionSource = remember { MutableInteractionSource() }
                    
                    if (isSearchItem) {
                        LaunchedEffect(interactionSource) {
                            var isLongClick = false
                            interactionSource.interactions.collectLatest { interaction ->
                                when (interaction) {
                                    is PressInteraction.Press -> {
                                        isLongClick = false
                                        delay(viewConfiguration.longPressTimeoutMillis)
                                        isLongClick = true
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSearchLongClick?.invoke()
                                    }
                                    is PressInteraction.Release -> {
                                        if (!isLongClick) {
                                            onItemClick(screen, isSelected)
                                        }
                                    }
                                    is PressInteraction.Cancel -> {
                                        isLongClick = false
                                    }
                                }
                            }
                        }
                    }
                    
                    NavigationRailItem(
                        selected = isSelected,
                        onClick = { 
                            if (!isSearchItem) {
                                onItemClick(screen, isSelected)
                            }
                        },
                        interactionSource = interactionSource,
                        icon = {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = stringResource(screen.titleId)
                            )
                        }
                    )
                }
            }
        }
    } else {
        NavigationRail(
            modifier = modifier
                .then(
                    if (isGlassActive) {
                        Modifier.liquidGlass(
                            config = glassConfig,
                            shape = RoundedCornerShape(0.dp),
                            applyEdgeEffects = false
                        )
                    } else {
                        Modifier
                    }
                )
                .then(
                    if (isGlassActive) {
                        Modifier.drawBehind {
                            drawLine(
                                brush = glassBorderBrush,
                                start = Offset(size.width, 0f),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    } else {
                        Modifier
                    }
                ),
            containerColor = containerColor,
            windowInsets = WindowInsets(0.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            navigationItems.forEach { screen ->
                val isSelected = remember(currentRoute, screen.route) {
                    isRouteSelected(currentRoute, screen.route, navigationItems)
                }
                val iconRes = remember(isSelected, screen) {
                    if (isSelected) screen.iconIdActive else screen.iconIdInactive
                }
                
                val isSearchItem = screen == Screens.Search && onSearchLongClick != null
                val interactionSource = remember { MutableInteractionSource() }
                
                if (isSearchItem) {
                    LaunchedEffect(interactionSource) {
                        var isLongClick = false
                        interactionSource.interactions.collectLatest { interaction ->
                            when (interaction) {
                                is PressInteraction.Press -> {
                                    isLongClick = false
                                    delay(viewConfiguration.longPressTimeoutMillis)
                                    isLongClick = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSearchLongClick?.invoke()
                                }
                                is PressInteraction.Release -> {
                                    if (!isLongClick) {
                                        onItemClick(screen, isSelected)
                                    }
                                }
                                is PressInteraction.Cancel -> {
                                    isLongClick = false
                                }
                            }
                        }
                    }
                }
                
                NavigationRailItem(
                    selected = isSelected,
                    onClick = { 
                        if (!isSearchItem) {
                            onItemClick(screen, isSelected)
                        }
                    },
                    interactionSource = interactionSource,
                    icon = {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = stringResource(screen.titleId)
                        )
                    }
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun AppNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    slimNav: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
    floatingNav: Boolean = false,
    bottomInset: Dp = 0.dp,
    enableFrostedGlass: Boolean = true
) {
    if (floatingNav) {
        FloatingNavigationBar(
            navigationItems = navigationItems,
            currentRoute = currentRoute,
            onItemClick = onItemClick,
            modifier = modifier,
            pureBlack = pureBlack,
            slimNav = slimNav,
            onSearchLongClick = onSearchLongClick,
            bottomInset = bottomInset
        )
    } else {
        val isGlassActive = enableFrostedGlass && !pureBlack && isGlassAllowed()
        val glassConfig = LocalGlassEffectConfig.current
        val containerColor = when {
            isGlassActive -> Color.Transparent
            pureBlack -> Color.Black
            else -> MaterialTheme.colorScheme.surfaceContainer
        }
        val contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        val haptics = LocalHapticFeedback.current
        val viewConfiguration = LocalViewConfiguration.current
        val glassBorderBrush = rememberNocturneGlassBorderBrush()
        
        NavigationBar(
            modifier = modifier
                .then(
                    if (isGlassActive) {
                        Modifier.liquidGlass(
                            config = glassConfig,
                            shape = RoundedCornerShape(0.dp),
                            applyEdgeEffects = false
                        )
                    } else {
                        Modifier
                    }
                )
                .then(
                    if (isGlassActive) {
                        Modifier.drawBehind {
                            drawLine(
                                brush = glassBorderBrush,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    } else {
                        Modifier
                    }
                ),
            containerColor = containerColor,
            contentColor = contentColor,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0.dp)
        ) {

            navigationItems.forEach { screen ->
                val isSelected = remember(currentRoute, screen.route) {
                    isRouteSelected(currentRoute, screen.route, navigationItems)
                }
                val iconRes = remember(isSelected, screen) {
                    if (isSelected) screen.iconIdActive else screen.iconIdInactive
                }
                
                val isSearchItem = screen == Screens.Search && onSearchLongClick != null
                val interactionSource = remember { MutableInteractionSource() }
                
                // Long press detection using InteractionSource
                if (isSearchItem) {
                    LaunchedEffect(interactionSource) {
                        var isLongClick = false
                        interactionSource.interactions.collectLatest { interaction ->
                            when (interaction) {
                                is PressInteraction.Press -> {
                                    isLongClick = false
                                    delay(viewConfiguration.longPressTimeoutMillis)
                                    isLongClick = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSearchLongClick.invoke()
                                }
                                is PressInteraction.Release -> {
                                    if (!isLongClick) {
                                        onItemClick(screen, isSelected)
                                    }
                                }
                                is PressInteraction.Cancel -> {
                                    isLongClick = false
                                }
                            }
                        }
                    }
                }
                
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { 
                        if (!isSearchItem) {
                            onItemClick(screen, isSelected)
                        }
                        // For search item, click is handled via InteractionSource
                    },
                    interactionSource = interactionSource,
                    icon = {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = stringResource(screen.titleId)
                        )
                    },
                    label = if (!slimNav) {
                        {
                            Text(
                                text = stringResource(screen.titleId),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else null
                )
            }
        }
    }
}


