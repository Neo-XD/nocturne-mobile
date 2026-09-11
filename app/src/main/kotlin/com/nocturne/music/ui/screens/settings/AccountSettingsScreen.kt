/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nocturne.music.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.utils.parseCookieString
import com.nocturne.music.LocalPlayerAwareWindowInsets
import com.nocturne.music.constants.*
import com.nocturne.music.ui.component.*
import com.nocturne.music.ui.utils.backToMain
import com.nocturne.music.ui.utils.safeOpenUri
import com.nocturne.music.utils.rememberPreference
import com.nocturne.music.viewmodels.AccountSettingsViewModel
import com.nocturne.music.viewmodels.HomeViewModel
import com.nocturne.music.R
private enum class AccountTab {
    RECOMMENDED,
    ALL_SERVICES
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AccountSettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val (accountNamePref, _) = rememberPreference(AccountNameKey, "")
    val (accountEmail, _) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, _) = rememberPreference(AccountChannelHandleKey, "")
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val (visitorData, _) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, _) = rememberPreference(DataSyncIdKey, "")

    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(UseLoginForBrowse, true)
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, true)

    val homeViewModel: HomeViewModel = hiltViewModel()
    val accountSettingsViewModel: AccountSettingsViewModel = hiltViewModel()
    val accountName by homeViewModel.accountName.collectAsState()
    val accountImageUrl by homeViewModel.accountImageUrl.collectAsState()

    var showToken by remember { mutableStateOf(false) }
    var showTokenEditor by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(AccountTab.RECOMMENDED) }

    val (storedAccountsRaw) = rememberPreference(StoredGoogleAccountsKey, "")
    val storedAccounts = remember(storedAccountsRaw) {
        AccountSettingsViewModel.parseStoredAccounts(storedAccountsRaw)
    }

    LaunchedEffect(isLoggedIn, innerTubeCookie, accountName) {
        if (isLoggedIn && innerTubeCookie.isNotBlank()) {
            accountSettingsViewModel.syncCurrentAccountIfMissing(
                context = context,
                cookie = innerTubeCookie,
                visitorData = visitorData,
                dataSyncId = dataSyncId,
                accountName = accountNamePref.ifBlank { accountName },
                accountEmail = accountEmail,
                accountChannelHandle = accountChannelHandle,
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
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
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Header Section
            Text(
                text = stringResource(R.string.account_services),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            if (isLoggedIn) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("account") }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!accountImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = accountImageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.person),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = accountName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.account),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        painter = painterResource(R.drawable.chevron_right_px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Google Accounts Multi-Account Card
            if (storedAccounts.isNotEmpty() || isLoggedIn) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.accounts),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(
                                onClick = { navController.navigate("login") },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.add),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.add_another_account),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        storedAccounts.forEach { acc ->
                            val isCurrent = (acc.cookie == innerTubeCookie) || (acc.isActive && isLoggedIn)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                        else Color.Transparent
                                    )
                                    .clickable(enabled = !isCurrent) {
                                        accountSettingsViewModel.switchAccount(context, acc)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isCurrent) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = acc.name.firstOrNull()?.uppercase() ?: "G",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = acc.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (acc.email.isNotBlank() || acc.channelHandle.isNotBlank()) {
                                        Text(
                                            text = acc.email.ifBlank { acc.channelHandle },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (isCurrent) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.active_account),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = { accountSettingsViewModel.switchAccount(context, acc) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.cached),
                                            contentDescription = stringResource(R.string.switch_account),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                if (!isCurrent || storedAccounts.size > 1) {
                                    IconButton(
                                        onClick = { accountSettingsViewModel.removeAccount(context, acc.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.delete),
                                            contentDescription = stringResource(R.string.remove_account),
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Switcher Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { selectedTab = AccountTab.RECOMMENDED },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == AccountTab.RECOMMENDED) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        },
                        contentColor = if (selectedTab == AccountTab.RECOMMENDED) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.tab_recommended),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Button(
                    onClick = { selectedTab = AccountTab.ALL_SERVICES },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == AccountTab.ALL_SERVICES) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        },
                        contentColor = if (selectedTab == AccountTab.ALL_SERVICES) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.tab_all_services),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Panels
            if (selectedTab == AccountTab.RECOMMENDED) {
                if (isLoggedIn) {
                    // Toggles & Log Out wrapped inside standard Material3SettingsGroup
                    Material3SettingsGroup(
                        items = listOf(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.add_circle),
                                title = { Text(stringResource(R.string.more_content)) },
                                trailingContent = {
                                    Switch(
                                        checked = useLoginForBrowse,
                                        onCheckedChange = {
                                            YouTube.useLoginForBrowse = it
                                            onUseLoginForBrowseChange(it)
                                        },
                                        thumbContent = {
                                            Icon(
                                                painter = painterResource(
                                                    id = if (useLoginForBrowse) R.drawable.check else R.drawable.close
                                                ),
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        }
                                    )
                                },
                                onClick = {
                                    val newValue = !useLoginForBrowse
                                    YouTube.useLoginForBrowse = newValue
                                    onUseLoginForBrowseChange(newValue)
                                },
                                isExpressive = true
                            ),
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.cached),
                                title = { Text(stringResource(R.string.yt_sync)) },
                                trailingContent = {
                                    Switch(
                                        checked = ytmSync,
                                        onCheckedChange = onYtmSyncChange,
                                        thumbContent = {
                                            Icon(
                                                painter = painterResource(
                                                    id = if (ytmSync) R.drawable.check else R.drawable.close
                                                ),
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        }
                                    )
                                },
                                onClick = { onYtmSyncChange(!ytmSync) },
                                isExpressive = true
                            ),
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.logout),
                                title = { Text(stringResource(R.string.action_logout)) },
                                onClick = { showLogoutDialog = true },
                                isExpressive = true
                            )
                        )
                    )
                } else {
                    // Sign In Box
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.google),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.login),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.account_sync_desc_off),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { navController.navigate("login") }
                            ) {
                                Text(
                                    text = stringResource(R.string.login),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.give_feedback),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            uriHandler.safeOpenUri(context, "https://github.com/Neo-XD/nocturne-mobile/issues")
                        }
                    )
                }
            } else {
                // All Services Tab Content
                Text(
                    text = stringResource(R.string.account_settings_tab_general),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                )

                Material3SettingsGroup(
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.token),
                            title = {
                                Text(
                                    when {
                                        !isLoggedIn -> stringResource(R.string.advanced_login)
                                        showToken -> stringResource(R.string.token_shown)
                                        else -> stringResource(R.string.token_hidden)
                                    }
                                )
                            },
                            onClick = {
                                if (!isLoggedIn) showTokenEditor = true
                                else if (!showToken) showToken = true
                                else showTokenEditor = true
                            },
                            isExpressive = true
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.integration),
                            title = { Text(stringResource(R.string.integrations)) },
                            onClick = { navController.navigate("settings/integrations") },
                            isExpressive = true
                        )
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showTokenEditor) {
            val text = """
                ***INNERTUBE COOKIE*** =$innerTubeCookie
                ***VISITOR DATA*** =$visitorData
                ***DATASYNC ID*** =$dataSyncId
                ***ACCOUNT NAME*** =$accountNamePref
                ***ACCOUNT EMAIL*** =$accountEmail
                ***ACCOUNT CHANNEL HANDLE*** =$accountChannelHandle
            """.trimIndent()

            TextFieldDialog(
                initialTextFieldValue = TextFieldValue(text),
                onDone = { data ->
                    var cookie = ""
                    var visitorDataValue = ""
                    var dataSyncIdValue = ""
                    var accountNameValue = ""
                    var accountEmailValue = ""
                    var accountChannelHandleValue = ""

                    data.split("\n").forEach {
                        when {
                            it.startsWith("***INNERTUBE COOKIE*** =") -> cookie = it.substringAfter("=")
                            it.startsWith("***VISITOR DATA*** =") -> visitorDataValue = it.substringAfter("=")
                            it.startsWith("***DATASYNC ID*** =") -> dataSyncIdValue = it.substringAfter("=")
                            it.startsWith("***ACCOUNT NAME*** =") -> accountNameValue = it.substringAfter("=")
                            it.startsWith("***ACCOUNT EMAIL*** =") -> accountEmailValue = it.substringAfter("=")
                            it.startsWith("***ACCOUNT CHANNEL HANDLE*** =") -> accountChannelHandleValue = it.substringAfter("=")
                        }
                    }
                    accountSettingsViewModel.saveTokenAndRestart(
                        context = context,
                        cookie = cookie,
                        visitorData = visitorDataValue,
                        dataSyncId = dataSyncIdValue,
                        accountName = accountNameValue,
                        accountEmail = accountEmailValue,
                        accountChannelHandle = accountChannelHandleValue,
                    )
                },
                onDismiss = { showTokenEditor = false },
                singleLine = false,
                maxLines = 20,
                isInputValid = { fullText ->
                    val cookieLine = fullText.lines()
                        .find { it.startsWith("***INNERTUBE COOKIE*** =") }
                    val cookieValue = cookieLine?.substringAfter("***INNERTUBE COOKIE*** =")?.trim() ?: ""
                    cookieValue.isNotEmpty() && "SAPISID" in parseCookieString(cookieValue)
                },
                extraContent = {
                    InfoLabel(text = stringResource(R.string.token_adv_login_description))
                }
            )
        }
        if (showLogoutDialog) {
            DefaultDialog(
                onDismiss = { showLogoutDialog = false },
                title = { Text(stringResource(R.string.logout_dialog_title)) },
                buttons = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        // Cancel button
                        ToggleButton(
                            checked = false,
                            onCheckedChange = { showLogoutDialog = false },
                            modifier = Modifier.weight(1f),
                            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                        ) {
                            Text(stringResource(android.R.string.cancel))
                        }

                        // Clear Data button
                        ToggleButton(
                            checked = false,
                            onCheckedChange = {
                                accountSettingsViewModel.logoutAndClearSyncedContent(context, onInnerTubeCookieChange)
                                showLogoutDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
                            colors = ToggleButtonDefaults.toggleButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.logout_clear_data))
                        }

                        // Keep Data button (Primary OK)
                        ToggleButton(
                            checked = true,
                            onCheckedChange = {
                                accountSettingsViewModel.logoutKeepData(context, onInnerTubeCookieChange)
                                showLogoutDialog = false
                                navController.navigateUp()
                            },
                            modifier = Modifier.weight(1f),
                            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                        ) {
                            Text(stringResource(R.string.logout_keep_data))
                        }
                    }
                }
            ) {
                Text(
                    text = stringResource(R.string.logout_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}


