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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
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
import androidx.compose.ui.unit.Dp
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

private val SuccessGreen = Color(0xFF2E7D32)

private enum class ResponsiveClass {
    NARROW,
    COMPACT,
    MEDIUM,
    EXPANDED,
}

private data class ResponsiveMetrics(
    val responsiveClass: ResponsiveClass,
    val horizontalPadding: Dp,
    val topPadding: Dp,
    val sectionSpacing: Dp,
    val itemSpacing: Dp,
    val cardPadding: Dp,
    val cardRadius: Dp,
    val navigationBarHeight: Dp,
    val navigationRailWidth: Dp,
    val navigationIconSize: Dp,
    val categoryIconSize: Dp,
    val categoryCardWidth: Dp,
    val categoryCardMinHeight: Dp,
    val artworkSize: Dp,
    val contentMaxWidth: Dp,
    val gridMinWidth: Dp,
    val categoryColumns: Int,
    val useNavigationRail: Boolean,
    val useGrid: Boolean,
    val useTwoColumnCards: Boolean,
    val showNavigationLabels: Boolean,
)

private val LocalResponsiveMetrics = staticCompositionLocalOf {
    ResponsiveMetrics(
        responsiveClass = ResponsiveClass.COMPACT,
        horizontalPadding = 18.dp,
        topPadding = 20.dp,
        sectionSpacing = 18.dp,
        itemSpacing = 12.dp,
        cardPadding = 16.dp,
        cardRadius = 22.dp,
        navigationBarHeight = 70.dp,
        navigationRailWidth = 92.dp,
        navigationIconSize = 34.dp,
        categoryIconSize = 38.dp,
        categoryCardWidth = 148.dp,
        categoryCardMinHeight = 126.dp,
        artworkSize = 84.dp,
        contentMaxWidth = 600.dp,
        gridMinWidth = 260.dp,
        categoryColumns = 1,
        useNavigationRail = false,
        useGrid = false,
        useTwoColumnCards = false,
        showNavigationLabels = true,
    )
}

private fun resolveResponsiveMetrics(width: Dp, height: Dp): ResponsiveMetrics {
    val landscape = width > height
    val shortHeight = height < 480.dp

    val base = when {
        width < 340.dp -> ResponsiveMetrics(
            responsiveClass = ResponsiveClass.NARROW,
            horizontalPadding = 12.dp,
            topPadding = if (shortHeight) 10.dp else 14.dp,
            sectionSpacing = 14.dp,
            itemSpacing = 10.dp,
            cardPadding = 14.dp,
            cardRadius = 18.dp,
            navigationBarHeight = 62.dp,
            navigationRailWidth = 82.dp,
            navigationIconSize = 30.dp,
            categoryIconSize = 34.dp,
            categoryCardWidth = 132.dp,
            categoryCardMinHeight = 116.dp,
            artworkSize = 70.dp,
            contentMaxWidth = 560.dp,
            gridMinWidth = 240.dp,
            categoryColumns = 1,
            useNavigationRail = false,
            useGrid = false,
            useTwoColumnCards = false,
            showNavigationLabels = false,
        )

        width < 600.dp -> ResponsiveMetrics(
            responsiveClass = ResponsiveClass.COMPACT,
            horizontalPadding = 18.dp,
            topPadding = if (shortHeight) 12.dp else 20.dp,
            sectionSpacing = 18.dp,
            itemSpacing = 12.dp,
            cardPadding = 16.dp,
            cardRadius = 22.dp,
            navigationBarHeight = 70.dp,
            navigationRailWidth = 92.dp,
            navigationIconSize = 34.dp,
            categoryIconSize = 38.dp,
            categoryCardWidth = 148.dp,
            categoryCardMinHeight = 126.dp,
            artworkSize = 84.dp,
            contentMaxWidth = 600.dp,
            gridMinWidth = 260.dp,
            categoryColumns = 1,
            useNavigationRail = false,
            useGrid = false,
            useTwoColumnCards = false,
            showNavigationLabels = true,
        )

        width < 840.dp -> ResponsiveMetrics(
            responsiveClass = ResponsiveClass.MEDIUM,
            horizontalPadding = 24.dp,
            topPadding = if (shortHeight) 14.dp else 24.dp,
            sectionSpacing = 20.dp,
            itemSpacing = 14.dp,
            cardPadding = 20.dp,
            cardRadius = 24.dp,
            navigationBarHeight = 72.dp,
            navigationRailWidth = 100.dp,
            navigationIconSize = 38.dp,
            categoryIconSize = 42.dp,
            categoryCardWidth = 180.dp,
            categoryCardMinHeight = 136.dp,
            artworkSize = 96.dp,
            contentMaxWidth = 920.dp,
            gridMinWidth = 270.dp,
            categoryColumns = 2,
            useNavigationRail = width >= 720.dp || landscape,
            useGrid = true,
            useTwoColumnCards = true,
            showNavigationLabels = true,
        )

        else -> ResponsiveMetrics(
            responsiveClass = ResponsiveClass.EXPANDED,
            horizontalPadding = 32.dp,
            topPadding = if (shortHeight) 16.dp else 28.dp,
            sectionSpacing = 24.dp,
            itemSpacing = 16.dp,
            cardPadding = 24.dp,
            cardRadius = 28.dp,
            navigationBarHeight = 76.dp,
            navigationRailWidth = 112.dp,
            navigationIconSize = 42.dp,
            categoryIconSize = 46.dp,
            categoryCardWidth = 200.dp,
            categoryCardMinHeight = 144.dp,
            artworkSize = 108.dp,
            contentMaxWidth = 1200.dp,
            gridMinWidth = 300.dp,
            categoryColumns = 4,
            useNavigationRail = true,
            useGrid = true,
            useTwoColumnCards = true,
            showNavigationLabels = true,
        )
    }

    return if (landscape && width >= 600.dp && !base.useNavigationRail) {
        base.copy(useNavigationRail = true)
    } else {
        base
    }
}

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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val metrics = remember(maxWidth, maxHeight) {
                resolveResponsiveMetrics(maxWidth, maxHeight)
            }

            CompositionLocalProvider(LocalResponsiveMetrics provides metrics) {
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
                } else {
                    MainCompanionShell(
                        uiState = uiState,
                        updateState = updateState,
                        preferences = preferences,
                        onDatabaseAction = updateViewModel::onDatabaseAction,
                        onAppAction = updateViewModel::onAppAction,
                    )

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
                }
            }
        }
    }
}

@Composable
private fun MainCompanionShell(
    uiState: CompanionUiState,
    updateState: UpdateCenterUiState,
    preferences: UiPreferences,
    onDatabaseAction: () -> Unit,
    onAppAction: () -> Unit,
) {
    val metrics = LocalResponsiveMetrics.current
    var selectedDestination by rememberSaveable { mutableStateOf(AppDestination.HOME.name) }
    val destination = AppDestination.entries.firstOrNull { it.name == selectedDestination }
        ?: AppDestination.HOME

    BackHandler(enabled = destination != AppDestination.HOME) {
        selectedDestination = AppDestination.HOME.name
    }

    if (metrics.useNavigationRail) {
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
                onDatabaseAction = onDatabaseAction,
                onAppAction = onAppAction,
                onBack = { selectedDestination = AppDestination.HOME.name },
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
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
                onDatabaseAction = onDatabaseAction,
                onAppAction = onAppAction,
                onBack = { selectedDestination = AppDestination.HOME.name },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
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
    val metrics = LocalResponsiveMetrics.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = metrics.horizontalPadding,
                vertical = metrics.topPadding,
            ),
            verticalArrangement = Arrangement.Center,
        ) {
            item {
                Text(
                    text = stringResource(R.string.onboarding_title),
                    style = when (metrics.responsiveClass) {
                        ResponsiveClass.NARROW -> MaterialTheme.typography.headlineMedium
                        ResponsiveClass.COMPACT -> MaterialTheme.typography.headlineLarge
                        ResponsiveClass.MEDIUM,
                        ResponsiveClass.EXPANDED -> MaterialTheme.typography.displaySmall
                    },
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = metrics.sectionSpacing),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = metrics.itemSpacing),
                )
            }

            item {
                TooltipButton(
                    label = stringResource(R.string.onboarding_continue),
                    tooltip = stringResource(R.string.tooltip_finish_setup),
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = metrics.sectionSpacing),
                )
            }
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
    val metrics = LocalResponsiveMetrics.current
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(metrics.navigationBarHeight),
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
    val metrics = LocalResponsiveMetrics.current
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(metrics.navigationRailWidth)
            .padding(horizontal = 8.dp, vertical = metrics.itemSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.app_short_name),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(vertical = metrics.itemSpacing),
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
    val metrics = LocalResponsiveMetrics.current
    val label = stringResource(item.labelRes)

    TooltipAction(
        tooltip = label,
        onClick = onClick,
        modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = if (metrics.showNavigationLabels) 6.dp else 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(metrics.navigationIconSize)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.glyph,
                    style = when (metrics.responsiveClass) {
                        ResponsiveClass.NARROW -> MaterialTheme.typography.labelLarge
                        ResponsiveClass.COMPACT -> MaterialTheme.typography.titleSmall
                        ResponsiveClass.MEDIUM,
                        ResponsiveClass.EXPANDED -> MaterialTheme.typography.titleMedium
                    },
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Black,
                )
            }

            if (metrics.showNavigationLabels) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
    modifier: Modifier = Modifier,
) {
    when (destination) {
        AppDestination.HOME -> HomeScreen(
            uiState = uiState,
            installedDatabaseVersion = updateState.database.installedVersion,
            onCategorySelected = { onDestinationSelected(AppDestination.LIBRARY) },
            modifier = modifier,
        )

        AppDestination.LIBRARY -> LibraryScreen(
            uiState = uiState,
            onBack = onBack,
            modifier = modifier,
        )

        AppDestination.UPDATES -> UpdatesScreen(
            updateState = updateState,
            onDatabaseAction = onDatabaseAction,
            onAppAction = onAppAction,
            onBack = onBack,
            modifier = modifier,
        )

        AppDestination.SETTINGS -> SettingsScreen(
            installedDatabaseVersion = updateState.database.installedVersion,
            preferences = preferences,
            onBack = onBack,
            modifier = modifier,
        )
    }
}

@Composable
private fun ResponsiveContentFrame(
    modifier: Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val metrics = LocalResponsiveMetrics.current
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        content(
            Modifier
                .widthIn(max = metrics.contentMaxWidth)
                .fillMaxSize(),
        )
    }
}

@Composable
private fun HomeScreen(
    uiState: CompanionUiState,
    installedDatabaseVersion: String,
    onCategorySelected: (CategoryUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = LocalResponsiveMetrics.current
    val showImages = BuildConfig.SUPPORTS_IMAGES && uiState.imageAssetsAvailable

    ResponsiveContentFrame(modifier) { contentModifier ->
        LazyColumn(
            modifier = contentModifier,
            contentPadding = PaddingValues(
                start = metrics.horizontalPadding,
                end = metrics.horizontalPadding,
                top = metrics.topPadding,
                bottom = metrics.sectionSpacing,
            ),
            verticalArrangement = Arrangement.spacedBy(metrics.sectionSpacing),
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
                ResponsiveCategoryCollection(
                    categories = uiState.categories,
                    onCategorySelected = onCategorySelected,
                )
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

            item {
                ResponsiveEntryCollection(
                    entries = uiState.entries.take(4),
                    showImages = showImages,
                )
            }
        }
    }
}

@Composable
private fun ResponsiveCategoryCollection(
    categories: List<CategoryUi>,
    onCategorySelected: (CategoryUi) -> Unit,
) {
    val metrics = LocalResponsiveMetrics.current

    if (metrics.categoryColumns <= 1) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(metrics.itemSpacing)) {
            items(categories) { category ->
                CategoryButton(
                    category = category,
                    onClick = { onCategorySelected(category) },
                    modifier = Modifier.width(metrics.categoryCardWidth),
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(metrics.itemSpacing)) {
            categories.chunked(metrics.categoryColumns).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(metrics.itemSpacing),
                ) {
                    rowItems.forEach { category ->
                        CategoryButton(
                            category = category,
                            onClick = { onCategorySelected(category) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(metrics.categoryColumns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ResponsiveEntryCollection(
    entries: List<LibraryEntryUi>,
    showImages: Boolean,
) {
    val metrics = LocalResponsiveMetrics.current

    if (metrics.useTwoColumnCards) {
        Column(verticalArrangement = Arrangement.spacedBy(metrics.itemSpacing)) {
            entries.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(metrics.itemSpacing),
                ) {
                    rowItems.forEach { entry ->
                        LibraryEntryCard(
                            entry = entry,
                            showImages = showImages,
                            forceStacked = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(metrics.itemSpacing)) {
            entries.forEach { entry ->
                LibraryEntryCard(
                    entry = entry,
                    showImages = showImages,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    uiState: CompanionUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = LocalResponsiveMetrics.current
    var query by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val showImages = BuildConfig.SUPPORTS_IMAGES && uiState.imageAssetsAvailable
    val visibleEntries = uiState.entries.filter { entry ->
        query.isBlank() ||
            context.getString(entry.nameRes).contains(query, ignoreCase = true) ||
            context.getString(entry.typeRes).contains(query, ignoreCase = true)
    }

    ResponsiveContentFrame(modifier) { contentModifier ->
        Column(
            modifier = contentModifier.padding(top = metrics.topPadding),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = metrics.horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(metrics.itemSpacing),
            ) {
                ScreenHeader(
                    title = stringResource(R.string.library_title),
                    subtitle = stringResource(R.string.library_subtitle),
                    onBack = onBack,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.search_library_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(metrics.cardRadius.coerceAtMost(20.dp)),
                )
            }

            if (metrics.useGrid) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = metrics.gridMinWidth),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = metrics.itemSpacing),
                    contentPadding = PaddingValues(
                        start = metrics.horizontalPadding,
                        end = metrics.horizontalPadding,
                        bottom = metrics.sectionSpacing,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(metrics.itemSpacing),
                    verticalArrangement = Arrangement.spacedBy(metrics.itemSpacing),
                ) {
                    items(visibleEntries, key = { it.id }) { entry ->
                        LibraryEntryCard(
                            entry = entry,
                            showImages = showImages,
                            forceStacked = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = metrics.itemSpacing),
                    contentPadding = PaddingValues(
                        start = metrics.horizontalPadding,
                        end = metrics.horizontalPadding,
                        bottom = metrics.sectionSpacing,
                    ),
                    verticalArrangement = Arrangement.spacedBy(metrics.itemSpacing),
                ) {
                    items(visibleEntries, key = { it.id }) { entry ->
                        LibraryEntryCard(
                            entry = entry,
                            showImages = showImages,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdatesScreen(
    updateState: UpdateCenterUiState,
    onDatabaseAction: () -> Unit,
    onAppAction: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = LocalResponsiveMetrics.current

    ResponsiveContentFrame(modifier) { contentModifier ->
        LazyColumn(
            modifier = contentModifier,
            contentPadding = PaddingValues(
                start = metrics.horizontalPadding,
                end = metrics.horizontalPadding,
                top = metrics.topPadding,
                bottom = metrics.sectionSpacing,
            ),
            verticalArrangement = Arrangement.spacedBy(metrics.sectionSpacing),
        ) {
            item {
                ScreenHeader(
                    title = stringResource(R.string.updates_title),
                    subtitle = stringResource(R.string.updates_subtitle_new),
                    onBack = onBack,
                )
            }
            item {
                ResponsivePair(
                    first = {
                        DatabaseUpdateCard(
                            state = updateState.database,
                            supportsImages = updateState.supportsImages,
                            assetVersion = updateState.assetVersion,
                            onAction = onDatabaseAction,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    second = {
                        AppUpdateCard(
                            state = updateState.app,
                            onAction = onAppAction,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    installedDatabaseVersion: String,
    preferences: UiPreferences,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = LocalResponsiveMetrics.current
    val currentLanguage = remember {
        Locale.getDefault().getDisplayName(Locale.getDefault())
    }

    ResponsiveContentFrame(modifier) { contentModifier ->
        LazyColumn(
            modifier = contentModifier,
            contentPadding = PaddingValues(
                start = metrics.horizontalPadding,
                end = metrics.horizontalPadding,
                top = metrics.topPadding,
                bottom = metrics.sectionSpacing,
            ),
            verticalArrangement = Arrangement.spacedBy(metrics.itemSpacing),
        ) {
            item {
                ScreenHeader(
                    title = stringResource(R.string.settings_title),
                    subtitle = stringResource(R.string.settings_subtitle_new),
                    onBack = onBack,
                )
            }
            item {
                ResponsivePair(
                    first = {
                        SettingsPanel(
                            title = stringResource(R.string.theme_setting_title),
                            description = stringResource(R.string.theme_setting_description),
                            modifier = Modifier.fillMaxWidth(),
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
                    },
                    second = {
                        BuildProfileCard(
                            supportsImages = BuildConfig.SUPPORTS_IMAGES,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                )
            }
            item {
                ResponsivePair(
                    first = {
                        SettingsPanel(
                            title = stringResource(R.string.language_setting_title),
                            description = stringResource(R.string.language_setting_description),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.language_current, currentLanguage),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    },
                    second = {
                        SettingsPanel(
                            title = stringResource(R.string.about_title),
                            description = stringResource(R.string.unofficial_notice),
                            modifier = Modifier.fillMaxWidth(),
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
                    },
                )
            }
        }
    }
}

@Composable
private fun ResponsivePair(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
) {
    val metrics = LocalResponsiveMetrics.current

    if (metrics.useTwoColumnCards) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(metrics.itemSpacing),
            verticalAlignment = Alignment.Top,
        ) {
            Box(modifier = Modifier.weight(1f)) { first() }
            Box(modifier = Modifier.weight(1f)) { second() }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(metrics.itemSpacing),
        ) {
            first()
            second()
        }
    }
}

@Composable
private fun DatabaseUpdateCard(
    state: UpdateItemUiState,
    supportsImages: Boolean,
    assetVersion: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    UpdateCardFrame(
        title = stringResource(R.string.database_package_title),
        installedVersion = state.installedVersion,
        statusContent = { UpdateStatus(state) },
        modifier = modifier,
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
    modifier: Modifier = Modifier,
) {
    UpdateCardFrame(
        title = stringResource(R.string.app_package_title),
        installedVersion = state.installedVersion,
        statusContent = { UpdateStatus(state) },
        modifier = modifier,
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
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val metrics = LocalResponsiveMetrics.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(metrics.cardRadius),
    ) {
        BoxWithConstraints {
            val stackHeader = maxWidth < 390.dp
            Column(
                modifier = Modifier.padding(metrics.cardPadding),
                verticalArrangement = Arrangement.spacedBy(metrics.itemSpacing),
            ) {
                if (stackHeader) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        UpdateCardTitle(title, installedVersion)
                        statusContent()
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            UpdateCardTitle(title, installedVersion)
                        }
                        Spacer(modifier = Modifier.width(metrics.itemSpacing))
                        statusContent()
                    }
                }
                content()
            }
        }
    }
}

@Composable
private fun UpdateCardTitle(title: String, installedVersion: String) {
    Column {
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
}

@Composable
private fun UpdateStatus(state: UpdateItemUiState) {
    when (state.phase) {
        UpdatePhase.CHECKING -> AssistChip(
            onClick = {},
            label = { Text(stringResource(R.string.status_checking), maxLines = 1) },
        )

        UpdatePhase.UP_TO_DATE -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.titleMedium,
                color = SuccessGreen,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = if (state.justUpdated) {
                    stringResource(R.string.status_updated)
                } else {
                    stringResource(R.string.status_up_to_date)
                },
                color = SuccessGreen,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
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
                    maxLines = 2,
                )
            },
        )

        UpdatePhase.DOWNLOADING -> AssistChip(
            onClick = {},
            label = { Text(stringResource(R.string.status_downloading), maxLines = 1) },
        )

        UpdatePhase.READY_TO_INSTALL -> AssistChip(
            onClick = {},
            label = { Text(stringResource(R.string.status_install_ready), maxLines = 2) },
        )

        UpdatePhase.NOT_AVAILABLE -> AssistChip(
            onClick = {},
            label = { Text(stringResource(R.string.status_not_published), maxLines = 2) },
        )

        UpdatePhase.ERROR -> AssistChip(
            onClick = {},
            label = {
                Text(
                    state.message ?: stringResource(R.string.status_check_failed),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            },
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
    val metrics = LocalResponsiveMetrics.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        if (onBack != null) {
            val backSize = metrics.navigationIconSize.coerceAtLeast(40.dp)
            TooltipAction(
                tooltip = stringResource(R.string.action_back),
                onClick = onBack,
                modifier = Modifier.sizeIn(
                    minWidth = 48.dp,
                    minHeight = 48.dp,
                    maxWidth = backSize + 10.dp,
                    maxHeight = backSize + 10.dp,
                ),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "‹",
                        style = when (metrics.responsiveClass) {
                            ResponsiveClass.NARROW,
                            ResponsiveClass.COMPACT -> MaterialTheme.typography.headlineMedium
                            ResponsiveClass.MEDIUM,
                            ResponsiveClass.EXPANDED -> MaterialTheme.typography.headlineLarge
                        },
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = when (metrics.responsiveClass) {
                    ResponsiveClass.NARROW -> MaterialTheme.typography.headlineSmall
                    ResponsiveClass.COMPACT -> MaterialTheme.typography.headlineMedium
                    ResponsiveClass.MEDIUM,
                    ResponsiveClass.EXPANDED -> MaterialTheme.typography.headlineLarge
                },
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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
    val metrics = LocalResponsiveMetrics.current
    val gradient = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
        ),
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(metrics.cardRadius + 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(metrics.cardPadding + 4.dp),
        ) {
            Text(
                text = stringResource(R.string.hero_eyebrow),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.82f),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.hero_title),
                style = when (metrics.responsiveClass) {
                    ResponsiveClass.NARROW -> MaterialTheme.typography.titleLarge
                    ResponsiveClass.COMPACT -> MaterialTheme.typography.headlineSmall
                    ResponsiveClass.MEDIUM,
                    ResponsiveClass.EXPANDED -> MaterialTheme.typography.headlineMedium
                },
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
private fun CategoryButton(
    category: CategoryUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = LocalResponsiveMetrics.current
    val label = stringResource(category.titleRes)
    TooltipAction(
        tooltip = label,
        onClick = onClick,
        modifier = modifier,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = metrics.categoryCardMinHeight),
            shape = RoundedCornerShape(metrics.cardRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(metrics.cardPadding),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(metrics.categoryIconSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = category.glyph,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = metrics.itemSpacing),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
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
private fun LibraryEntryCard(
    entry: LibraryEntryUi,
    showImages: Boolean,
    modifier: Modifier = Modifier,
    forceStacked: Boolean = false,
) {
    val metrics = LocalResponsiveMetrics.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(metrics.cardRadius),
    ) {
        BoxWithConstraints {
            val stacked = forceStacked || (showImages && maxWidth < 390.dp)
            if (stacked) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (showImages) {
                        EntryArtwork(
                            entry = entry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f),
                        )
                    }
                    EntryTextContent(
                        entry = entry,
                        modifier = Modifier.padding(metrics.cardPadding),
                        showFallbackIcon = !showImages,
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(metrics.cardPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showImages) {
                        EntryArtwork(
                            entry = entry,
                            modifier = Modifier
                                .size(metrics.artworkSize)
                                .clip(RoundedCornerShape(metrics.cardRadius.coerceAtMost(18.dp))),
                        )
                        Spacer(modifier = Modifier.width(metrics.itemSpacing))
                    } else {
                        EntryFallbackIcon(entry)
                        Spacer(modifier = Modifier.width(metrics.itemSpacing))
                    }
                    EntryTextContent(
                        entry = entry,
                        modifier = Modifier.weight(1f),
                        showFallbackIcon = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryTextContent(
    entry: LibraryEntryUi,
    modifier: Modifier = Modifier,
    showFallbackIcon: Boolean,
) {
    val metrics = LocalResponsiveMetrics.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        if (showFallbackIcon) {
            EntryFallbackIcon(entry)
            Spacer(modifier = Modifier.width(metrics.itemSpacing))
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(entry.detailRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (metrics.useGrid) 3 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun EntryFallbackIcon(entry: LibraryEntryUi) {
    val metrics = LocalResponsiveMetrics.current
    Box(
        modifier = Modifier
            .size(metrics.categoryIconSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = entry.accentLabel.take(2),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
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
                color = MaterialTheme.colorScheme.onPrimaryContainer,
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
    val metrics = LocalResponsiveMetrics.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(metrics.cardRadius),
    ) {
        Column(
            modifier = Modifier.padding(metrics.cardPadding),
            verticalArrangement = Arrangement.spacedBy(metrics.itemSpacing),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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
    val metrics = LocalResponsiveMetrics.current
    TooltipAction(
        tooltip = tooltip,
        onClick = onClick,
        modifier = modifier.sizeIn(minHeight = 48.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(metrics.cardRadius.coerceAtMost(16.dp)),
            color = if (primary) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (primary) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(
                    horizontal = metrics.cardPadding,
                    vertical = 13.dp,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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
    val metrics = LocalResponsiveMetrics.current
    var tooltipVisible by remember { mutableStateOf(false) }
    val tooltipOffset = with(LocalDensity.current) {
        -(metrics.navigationIconSize + 20.dp).roundToPx()
    }

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
                    modifier = Modifier.widthIn(max = 280.dp),
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
