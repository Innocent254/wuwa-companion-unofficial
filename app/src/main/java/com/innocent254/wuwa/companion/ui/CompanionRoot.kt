@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.innocent254.wuwa.companion.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import com.innocent254.wuwa.companion.BuildConfig
import com.innocent254.wuwa.companion.R
import com.innocent254.wuwa.companion.core.update.UpdateCenterUiState
import com.innocent254.wuwa.companion.core.update.UpdateEvent
import com.innocent254.wuwa.companion.core.update.UpdateItemUiState
import com.innocent254.wuwa.companion.core.update.UpdatePhase
import com.innocent254.wuwa.companion.core.update.UpdateViewModel
import com.innocent254.wuwa.companion.ui.model.AppDestination
import com.innocent254.wuwa.companion.ui.model.CategoryUi
import com.innocent254.wuwa.companion.ui.model.CompanionUiState
import com.innocent254.wuwa.companion.ui.model.LibraryEntryUi
import com.innocent254.wuwa.companion.ui.preferences.ThemeMode
import com.innocent254.wuwa.companion.ui.preferences.UiPreferences
import java.io.File
import java.util.Locale
import kotlinx.coroutines.delay

private val PhonePadding = 18.dp
private val WidePadding = 30.dp
private val SuccessGreen = Color(0xFF2E7D32)

@Composable
fun CompanionRoot(
    uiState: CompanionUiState,
    preferences: UiPreferences,
    updateViewModel: UpdateViewModel,
    onInstallApk: (File) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val context = LocalContext.current
    val updateState by updateViewModel.uiState.collectAsState()

    LaunchedEffect(updateViewModel) {
        updateViewModel.events.collect { event ->
            when (event) {
                is UpdateEvent.ToastMessage -> {
                    Toast.makeText(context, event.text, Toast.LENGTH_LONG).show()
                }

                is UpdateEvent.InstallApk -> onInstallApk(File(event.absolutePath))
            }
        }
    }

    if (!preferences.onboardingComplete) {
        FirstInstallScreen(
            selectedTheme = preferences.themeMode,
            supportsImages = BuildConfig.SUPPORTS_IMAGES,
            onThemeSelected = preferences::setThemeMode,
            onContinue = {
                preferences.completeOnboarding(
                    currentVersionCode = BuildConfig.VERSION_CODE,
                    currentVersionName = BuildConfig.VERSION_NAME,
                )
            },
        )
        return
    }

    if (preferences.shouldShowVersionInformation(BuildConfig.VERSION_CODE)) {
        VersionInstalledDialog(
            previousVersion = preferences.previousVersionName,
            currentVersion = BuildConfig.VERSION_NAME,
            onDismiss = {
                preferences.acknowledgeVersion(
                    currentVersionCode = BuildConfig.VERSION_CODE,
                    currentVersionName = BuildConfig.VERSION_NAME,
                )
            },
            onOpenReleaseNotes = {
                onOpenUrl(BuildConfig.CURRENT_RELEASE_NOTES_URL)
            },
        )
    }

    var selectedDestination by rememberSaveable { mutableStateOf(AppDestination.HOME.name) }
    val destination = AppDestination.entries.firstOrNull { it.name == selectedDestination }
        ?: AppDestination.HOME

    BackHandler(enabled = destination != AppDestination.HOME) {
        selectedDestination = AppDestination.HOME.name
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val expanded = maxWidth >= 840.dp

        if (expanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                CompanionNavigationRail(
                    destination = destination,
                    onDestinationSelected = { selectedDestination = it.name },
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                DestinationContent(
                    destination = destination,
                    uiState = uiState,
                    updateState = updateState,
                    preferences = preferences,
                    onDestinationSelected = { selectedDestination = it.name },
                    onDatabaseAction = updateViewModel::onDatabaseAction,
                    onAppAction = updateViewModel::onAppAction,
                    onBack = { selectedDestination = AppDestination.HOME.name },
                    horizontalPadding = WidePadding,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    CompanionNavigationBar(
                        destination = destination,
                        onDestinationSelected = { selectedDestination = it.name },
                    )
                },
            ) { innerPadding ->
                DestinationContent(
                    destination = destination,
                    uiState = uiState,
                    updateState = updateState,
                    preferences = preferences,
                    onDestinationSelected = { selectedDestination = it.name },
                    onDatabaseAction = updateViewModel::onDatabaseAction,
                    onAppAction = updateViewModel::onAppAction,
                    onBack = { selectedDestination = AppDestination.HOME.name },
                    horizontalPadding = PhonePadding,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun FirstInstallScreen(
    selectedTheme: ThemeMode,
    supportsImages: Boolean,
    onThemeSelected: (ThemeMode) -> Unit,
    onContinue: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        item {
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        item {
            SettingsPanel(
                title = stringResource(R.string.theme_setting_title),
                description = stringResource(R.string.onboarding_theme_description),
                modifier = Modifier.padding(top = 24.dp),
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(ThemeMode.entries) { mode ->
                        FilterChip(
                            selected = selectedTheme == mode,
                            onClick = { onThemeSelected(mode) },
                            label = { Text(themeLabel(mode)) },
                        )
                    }
                }
            }
        }

        item {
            BuildProfileCard(
                supportsImages = supportsImages,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        item {
            TooltipButton(
                label = stringResource(R.string.onboarding_continue),
                tooltip = stringResource(R.string.tooltip_finish_setup),
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            )
        }
    }
}

@Composable
private fun VersionInstalledDialog(
    previousVersion: String?,
    currentVersion: String,
    onDismiss: () -> Unit,
    onOpenReleaseNotes: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.updated_dialog_title, currentVersion))
        },
        text = {
            Text(
                if (previousVersion.isNullOrBlank()) {
                    stringResource(R.string.updated_dialog_body_generic)
                } else {
                    stringResource(
                        R.string.updated_dialog_body_versions,
                        previousVersion,
                        currentVersion,
                    )
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onOpenReleaseNotes) {
                Text(stringResource(R.string.action_release_notes))
            }
        },
    )
}

@Composable
private fun CompanionNavigationBar(
    destination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppDestination.entries.forEach { item ->
                NavigationButton(
                    item = item,
                    selected = item == destination,
                    onClick = { onDestinationSelected(item) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CompanionNavigationRail(
    destination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(104.dp)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.app_short_name),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        AppDestination.entries.forEach { item ->
            NavigationButton(
                item = item,
                selected = item == destination,
                onClick = { onDestinationSelected(item) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun NavigationButton(
    item: AppDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(item.labelRes)
    TooltipAction(
        tooltip = label,
        onClick = onClick,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.glyph,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Black,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DestinationContent(
    destination: AppDestination,
    uiState: CompanionUiState,
    updateState: UpdateCenterUiState,
    preferences: UiPreferences,
    onDestinationSelected: (AppDestination) -> Unit,
    onDatabaseAction: () -> Unit,
    onAppAction: () -> Unit,
    onBack: () -> Unit,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    when (destination) {
        AppDestination.HOME -> HomeScreen(
            uiState = uiState,
            installedDatabaseVersion = updateState.database.installedVersion,
            onCategorySelected = { onDestinationSelected(AppDestination.LIBRARY) },
            horizontalPadding = horizontalPadding,
            modifier = modifier,
        )

        AppDestination.LIBRARY -> LibraryScreen(
            uiState = uiState,
            horizontalPadding = horizontalPadding,
            onBack = onBack,
            modifier = modifier,
        )

        AppDestination.UPDATES -> UpdatesScreen(
            updateState = updateState,
            onDatabaseAction = onDatabaseAction,
            onAppAction = onAppAction,
            horizontalPadding = horizontalPadding,
            onBack = onBack,
            modifier = modifier,
        )

        AppDestination.SETTINGS -> SettingsScreen(
            installedDatabaseVersion = updateState.database.installedVersion,
            preferences = preferences,
            horizontalPadding = horizontalPadding,
            onBack = onBack,
            modifier = modifier,
        )
    }
}

@Composable
private fun HomeScreen(
    uiState: CompanionUiState,
    installedDatabaseVersion: String,
    onCategorySelected: (CategoryUi) -> Unit,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val showImages = BuildConfig.SUPPORTS_IMAGES && uiState.imageAssetsAvailable

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = horizontalPadding,
            end = horizontalPadding,
            top = 24.dp,
            bottom = 30.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.home_title),
                subtitle = stringResource(R.string.home_subtitle),
            )
        }

        item {
            HeroCard(
                databaseVersion = installedDatabaseVersion,
                supportsImages = BuildConfig.SUPPORTS_IMAGES,
            )
        }

        item {
            SectionTitle(
                title = stringResource(R.string.section_browse_categories),
                subtitle = stringResource(R.string.section_browse_categories_subtitle),
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(uiState.categories) { category ->
                    CategoryButton(
                        category = category,
                        onClick = { onCategorySelected(category) },
                    )
                }
            }
        }

        item {
            SectionTitle(
                title = stringResource(R.string.section_featured),
                subtitle = if (showImages) {
                    stringResource(R.string.image_layout_enabled)
                } else {
                    stringResource(R.string.text_layout_enabled)
                },
            )
        }

        items(uiState.entries.take(4), key = { it.id }) { entry ->
            LibraryEntryCard(entry = entry, showImages = showImages)
        }
    }
}

@Composable
private fun LibraryScreen(
    uiState: CompanionUiState,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val showImages = BuildConfig.SUPPORTS_IMAGES && uiState.imageAssetsAvailable
    val visibleEntries = uiState.entries.filter { entry ->
        query.isBlank() ||
            context.getString(entry.nameRes).contains(query, ignoreCase = true) ||
            context.getString(entry.typeRes).contains(query, ignoreCase = true)
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = horizontalPadding,
            end = horizontalPadding,
            top = 20.dp,
            bottom = 30.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.library_title),
                subtitle = stringResource(R.string.library_subtitle),
                onBack = onBack,
            )
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_library_label)) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
            )
        }
        items(visibleEntries, key = { it.id }) { entry ->
            LibraryEntryCard(entry = entry, showImages = showImages)
        }
    }
}

@Composable
private fun UpdatesScreen(
    updateState: UpdateCenterUiState,
    onDatabaseAction: () -> Unit,
    onAppAction: () -> Unit,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = horizontalPadding,
            end = horizontalPadding,
            top = 20.dp,
            bottom = 30.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.updates_title),
                subtitle = stringResource(R.string.updates_subtitle_new),
                onBack = onBack,
            )
        }
        item {
            DatabaseUpdateCard(
                state = updateState.database,
                supportsImages = updateState.supportsImages,
                assetVersion = updateState.assetVersion,
                onAction = onDatabaseAction,
            )
        }
        item {
            AppUpdateCard(
                state = updateState.app,
                onAction = onAppAction,
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    installedDatabaseVersion: String,
    preferences: UiPreferences,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentLanguage = remember {
        Locale.getDefault().getDisplayName(Locale.getDefault())
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = horizontalPadding,
            end = horizontalPadding,
            top = 20.dp,
            bottom = 30.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.settings_title),
                subtitle = stringResource(R.string.settings_subtitle_new),
                onBack = onBack,
            )
        }
        item {
            SettingsPanel(
                title = stringResource(R.string.theme_setting_title),
                description = stringResource(R.string.theme_setting_description),
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(ThemeMode.entries) { mode ->
                        FilterChip(
                            selected = preferences.themeMode == mode,
                            onClick = { preferences.setThemeMode(mode) },
                            label = { Text(themeLabel(mode)) },
                        )
                    }
                }
            }
        }
        item {
            BuildProfileCard(supportsImages = BuildConfig.SUPPORTS_IMAGES)
        }
        item {
            SettingsPanel(
                title = stringResource(R.string.language_setting_title),
                description = stringResource(R.string.language_setting_description),
            ) {
                Text(
                    text = stringResource(R.string.language_current, currentLanguage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        item {
            SettingsPanel(
                title = stringResource(R.string.about_title),
                description = stringResource(R.string.unofficial_notice),
            ) {
                Text(
                    text = stringResource(
                        R.string.installed_versions,
                        BuildConfig.VERSION_NAME,
                        installedDatabaseVersion,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun DatabaseUpdateCard(
    state: UpdateItemUiState,
    supportsImages: Boolean,
    assetVersion: String,
    onAction: () -> Unit,
) {
    UpdateCardFrame(
        title = stringResource(R.string.database_package_title),
        installedVersion = state.installedVersion,
        statusContent = { UpdateStatus(state) },
    ) {
        Text(
            text = stringResource(R.string.database_package_description_new),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (supportsImages) {
                stringResource(R.string.database_images_build_note, assetVersion)
            } else {
                stringResource(R.string.database_minimal_build_note)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when (state.phase) {
            UpdatePhase.CHECKING -> IndeterminateUpdateProgress(
                label = stringResource(R.string.status_checking),
            )

            UpdatePhase.DOWNLOADING -> DownloadProgress(
                progress = state.progress,
                label = state.message ?: stringResource(R.string.status_downloading),
            )

            UpdatePhase.AVAILABLE -> TooltipButton(
                label = stringResource(R.string.action_update_database),
                tooltip = stringResource(R.string.tooltip_update_database),
                onClick = onAction,
            )

            UpdatePhase.UP_TO_DATE -> {
                if (!state.justUpdated) {
                    TooltipButton(
                        label = stringResource(R.string.action_check_database),
                        tooltip = stringResource(R.string.tooltip_check_database),
                        onClick = onAction,
                        primary = false,
                    )
                }
            }

            UpdatePhase.NOT_AVAILABLE,
            UpdatePhase.ERROR -> TooltipButton(
                label = stringResource(R.string.action_retry_database),
                tooltip = stringResource(R.string.tooltip_retry_database),
                onClick = onAction,
                primary = false,
            )

            UpdatePhase.READY_TO_INSTALL -> Unit
        }
    }
}

@Composable
private fun AppUpdateCard(
    state: UpdateItemUiState,
    onAction: () -> Unit,
) {
    UpdateCardFrame(
        title = stringResource(R.string.app_package_title),
        installedVersion = state.installedVersion,
        statusContent = { UpdateStatus(state) },
    ) {
        Text(
            text = stringResource(R.string.app_package_description_new),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when (state.phase) {
            UpdatePhase.CHECKING -> IndeterminateUpdateProgress(
                label = stringResource(R.string.status_checking),
            )

            UpdatePhase.DOWNLOADING -> DownloadProgress(
                progress = state.progress,
                label = stringResource(R.string.status_downloading_app),
            )

            UpdatePhase.AVAILABLE -> TooltipButton(
                label = stringResource(
                    R.string.action_download_app_version,
                    state.availableVersion ?: "",
                ),
                tooltip = stringResource(R.string.tooltip_download_app),
                onClick = onAction,
            )

            UpdatePhase.READY_TO_INSTALL -> TooltipButton(
                label = stringResource(R.string.action_install_app_update),
                tooltip = stringResource(R.string.tooltip_install_app),
                onClick = onAction,
            )

            UpdatePhase.UP_TO_DATE,
            UpdatePhase.ERROR,
            UpdatePhase.NOT_AVAILABLE -> Unit
        }
    }
}

@Composable
private fun UpdateCardFrame(
    title: String,
    installedVersion: String,
    statusContent: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.version_label, installedVersion),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                statusContent()
            }
            content()
        }
    }
}

@Composable
private fun UpdateStatus(state: UpdateItemUiState) {
    when (state.phase) {
        UpdatePhase.CHECKING -> AssistChip(
            onClick = {},
            label = { Text(stringResource(R.string.status_checking)) },
        )

        UpdatePhase.UP_TO_DATE -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("✓", color = SuccessGreen, fontWeight = FontWeight.Black)
            Text(
                text = if (state.justUpdated) {
                    stringResource(R.string.status_updated)
                } else {
                    stringResource(R.string.status_up_to_date)
                },
                color = SuccessGreen,
                fontWeight = FontWeight.Bold,
            )
        }

        UpdatePhase.AVAILABLE -> AssistChip(
            onClick = {},
            label = {
                Text(
                    stringResource(
                        R.string.status_version_available,
                        state.availableVersion ?: "",
                    ),
                )
            },
        )

        UpdatePhase.DOWNLOADING -> AssistChip(
            onClick = {},
            label = { Text(stringResource(R.string.status_downloading)) },
        )

        UpdatePhase.READY_TO_INSTALL -> AssistChip(
            onClick = {},
            label = { Text(stringResource(R.string.status_install_ready)) },
        )

        UpdatePhase.NOT_AVAILABLE -> AssistChip(
            onClick = {},
            label = { Text(stringResource(R.string.status_not_published)) },
        )

        UpdatePhase.ERROR -> AssistChip(
            onClick = {},
            label = { Text(state.message ?: stringResource(R.string.status_check_failed)) },
        )
    }
}

@Composable
private fun IndeterminateUpdateProgress(label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DownloadProgress(progress: Float, label: String) {
    val safeProgress = progress.coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearProgressIndicator(
            progress = { safeProgress },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(
                R.string.download_progress_label,
                label,
                (safeProgress * 100).toInt(),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BuildProfileCard(
    supportsImages: Boolean,
    modifier: Modifier = Modifier,
) {
    SettingsPanel(
        title = stringResource(R.string.build_profile_title),
        description = if (supportsImages) {
            stringResource(R.string.build_profile_images_description)
        } else {
            stringResource(R.string.build_profile_minimal_description)
        },
        modifier = modifier,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Text(
                text = if (supportsImages) {
                    stringResource(R.string.build_profile_images)
                } else {
                    stringResource(R.string.build_profile_minimal)
                },
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
        Text(
            text = stringResource(R.string.build_profile_locked_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ScreenHeader(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        if (onBack != null) {
            TooltipAction(
                tooltip = stringResource(R.string.action_back),
                onClick = onBack,
                modifier = Modifier.size(48.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "‹",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
    }
}

@Composable
private fun HeroCard(databaseVersion: String, supportsImages: Boolean) {
    val gradient = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
        ),
    )
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.hero_eyebrow),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.82f),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.hero_title),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(
                    R.string.hero_status,
                    databaseVersion,
                    if (supportsImages) {
                        stringResource(R.string.build_profile_images)
                    } else {
                        stringResource(R.string.build_profile_minimal)
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun CategoryButton(category: CategoryUi, onClick: () -> Unit) {
    val label = stringResource(category.titleRes)
    TooltipAction(
        tooltip = label,
        onClick = onClick,
        modifier = Modifier.width(150.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = category.glyph,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Text(
                    text = stringResource(R.string.entry_count, category.count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun LibraryEntryCard(entry: LibraryEntryUi, showImages: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showImages) {
                EntryArtwork(
                    entry = entry,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(18.dp)),
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(entry.typeRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(entry.nameRes),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(entry.detailRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun EntryArtwork(entry: LibraryEntryUi, modifier: Modifier = Modifier) {
    if (entry.imageModel != null) {
        AsyncImage(
            model = entry.imageModel,
            contentDescription = stringResource(
                R.string.entry_image_description,
                stringResource(entry.nameRes),
            ),
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = entry.accentLabel.take(2),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun SettingsPanel(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

@Composable
private fun TooltipButton(
    label: String,
    tooltip: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
) {
    TooltipAction(
        tooltip = tooltip,
        onClick = onClick,
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = if (primary) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (primary) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
            )
        }
    }
}

@Composable
private fun TooltipAction(
    tooltip: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    var tooltipVisible by remember { mutableStateOf(false) }
    val tooltipOffset = with(LocalDensity.current) { (-52).dp.roundToPx() }

    LaunchedEffect(tooltipVisible) {
        if (tooltipVisible) {
            delay(1800)
            tooltipVisible = false
        }
    }

    Box(
        modifier = modifier.combinedClickable(
            role = Role.Button,
            onClick = onClick,
            onLongClick = { tooltipVisible = true },
        ),
    ) {
        content()
        if (tooltipVisible) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, tooltipOffset),
                onDismissRequest = { tooltipVisible = false },
                properties = PopupProperties(focusable = false),
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shadowElevation = 6.dp,
                ) {
                    Text(
                        text = tooltip,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
    ThemeMode.DARK -> stringResource(R.string.theme_dark)
}
