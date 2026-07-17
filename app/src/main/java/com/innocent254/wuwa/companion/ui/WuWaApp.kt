package com.innocent254.wuwa.companion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.innocent254.wuwa.companion.R
import com.innocent254.wuwa.companion.ui.model.AppDestination
import com.innocent254.wuwa.companion.ui.model.CategoryUi
import com.innocent254.wuwa.companion.ui.model.CompanionUiState
import com.innocent254.wuwa.companion.ui.model.LibraryEntryUi
import com.innocent254.wuwa.companion.ui.preferences.ImageMode
import com.innocent254.wuwa.companion.ui.preferences.ThemeMode
import com.innocent254.wuwa.companion.ui.preferences.UiPreferences
import java.util.Locale

private val PhoneHorizontalPadding = 18.dp
private val LargeHorizontalPadding = 30.dp

@Composable
fun WuWaApp(
    uiState: CompanionUiState,
    preferences: UiPreferences,
) {
    var selectedDestination by rememberSaveable { mutableStateOf(AppDestination.HOME.name) }
    val destination = AppDestination.entries.firstOrNull { it.name == selectedDestination }
        ?: AppDestination.HOME

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val expandedNavigation = maxWidth >= 840.dp

        if (expandedNavigation) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                AppNavigationRail(
                    destination = destination,
                    onDestinationSelected = { selectedDestination = it.name },
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                AppDestinationContent(
                    destination = destination,
                    uiState = uiState,
                    preferences = preferences,
                    horizontalPadding = LargeHorizontalPadding,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    AppNavigationBar(
                        destination = destination,
                        onDestinationSelected = { selectedDestination = it.name },
                    )
                },
            ) { innerPadding ->
                AppDestinationContent(
                    destination = destination,
                    uiState = uiState,
                    preferences = preferences,
                    horizontalPadding = PhoneHorizontalPadding,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun AppNavigationBar(
    destination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit,
) {
    NavigationBar(modifier = Modifier.navigationBarsPadding()) {
        AppDestination.entries.forEach { item ->
            NavigationBarItem(
                selected = item == destination,
                onClick = { onDestinationSelected(item) },
                icon = { DestinationGlyph(item = item, selected = item == destination) },
                label = { Text(stringResource(item.labelRes)) },
            )
        }
    }
}

@Composable
private fun AppNavigationRail(
    destination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit,
) {
    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .padding(horizontal = 6.dp),
        header = {
            Text(
                text = stringResource(R.string.app_short_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(vertical = 20.dp),
            )
        },
    ) {
        AppDestination.entries.forEach { item ->
            NavigationRailItem(
                selected = item == destination,
                onClick = { onDestinationSelected(item) },
                icon = { DestinationGlyph(item = item, selected = item == destination) },
                label = { Text(stringResource(item.labelRes)) },
            )
        }
    }
}

@Composable
private fun DestinationGlyph(item: AppDestination, selected: Boolean) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = item.glyph,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun AppDestinationContent(
    destination: AppDestination,
    uiState: CompanionUiState,
    preferences: UiPreferences,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val showImages = when (preferences.imageMode) {
        ImageMode.AUTO -> uiState.imageAssetsAvailable
        ImageMode.SHOW -> true
        ImageMode.HIDE -> false
    }

    when (destination) {
        AppDestination.HOME -> HomeScreen(
            uiState = uiState,
            showImages = showImages,
            horizontalPadding = horizontalPadding,
            modifier = modifier,
        )

        AppDestination.LIBRARY -> LibraryScreen(
            uiState = uiState,
            showImages = showImages,
            horizontalPadding = horizontalPadding,
            modifier = modifier,
        )

        AppDestination.UPDATES -> UpdatesScreen(
            uiState = uiState,
            horizontalPadding = horizontalPadding,
            modifier = modifier,
        )

        AppDestination.SETTINGS -> SettingsScreen(
            uiState = uiState,
            preferences = preferences,
            horizontalPadding = horizontalPadding,
            modifier = modifier,
        )
    }
}

@Composable
private fun HomeScreen(
    uiState: CompanionUiState,
    showImages: Boolean,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }

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
            AppHeader(
                title = stringResource(R.string.home_title),
                subtitle = stringResource(R.string.home_subtitle),
                status = if (uiState.isOffline) stringResource(R.string.status_offline_ready)
                else stringResource(R.string.status_online),
            )
        }

        item {
            HeroCard(
                databaseVersion = uiState.databaseVersion,
                imageAssetsAvailable = uiState.imageAssetsAvailable,
            )
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_label)) },
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
            )
        }

        item {
            SectionTitle(
                title = stringResource(R.string.section_browse_categories),
                subtitle = stringResource(R.string.section_browse_categories_subtitle),
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = horizontalPadding),
            ) {
                items(uiState.categories) { category ->
                    CategoryCard(category)
                }
            }
        }

        item {
            SectionTitle(
                title = stringResource(R.string.section_featured),
                subtitle = if (showImages) stringResource(R.string.image_layout_enabled)
                else stringResource(R.string.text_layout_enabled),
            )
        }

        items(uiState.entries.take(3), key = { it.id }) { entry ->
            LibraryEntryCard(entry = entry, showImages = showImages)
        }
    }
}

@Composable
private fun LibraryScreen(
    uiState: CompanionUiState,
    showImages: Boolean,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 24.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = horizontalPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppHeader(
                title = stringResource(R.string.library_title),
                subtitle = stringResource(R.string.library_subtitle),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_library_label)) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
            )
        }

        LazyRow(
            modifier = Modifier.padding(top = 14.dp),
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text(stringResource(R.string.filter_all)) },
                )
            }
            items(uiState.categories) { category ->
                FilterChip(
                    selected = selectedCategory == category.titleRes,
                    onClick = { selectedCategory = category.titleRes },
                    label = { Text(stringResource(category.titleRes)) },
                )
            }
        }

        val context = LocalContext.current
        val visibleEntries = uiState.entries.filter { entry ->
            val matchesQuery = query.isBlank() ||
                context.getString(entry.nameRes).contains(query, ignoreCase = true)
            val matchesCategory = selectedCategory == null || entry.typeRes == selectedCategory
            matchesQuery && matchesCategory
        }

        if (showImages) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 174.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 10.dp),
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    bottom = 30.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(visibleEntries, key = { it.id }) { entry ->
                    LibraryGridCard(entry = entry)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 10.dp),
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    bottom = 30.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(visibleEntries, key = { it.id }) { entry ->
                    LibraryEntryCard(entry = entry, showImages = false)
                }
            }
        }
    }
}

@Composable
private fun UpdatesScreen(
    uiState: CompanionUiState,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = horizontalPadding,
            end = horizontalPadding,
            top = 24.dp,
            bottom = 30.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            AppHeader(
                title = stringResource(R.string.updates_title),
                subtitle = stringResource(R.string.updates_subtitle),
            )
        }
        item {
            UpdateCard(
                title = stringResource(R.string.database_package_title),
                version = uiState.databaseVersion,
                status = if (uiState.databaseReady) stringResource(R.string.status_ready)
                else stringResource(R.string.status_not_installed),
                description = stringResource(R.string.database_package_description),
                buttonLabel = stringResource(R.string.action_check_database),
            )
        }
        item {
            UpdateCard(
                title = stringResource(R.string.image_package_title),
                version = if (uiState.imageAssetsAvailable) stringResource(R.string.status_available)
                else stringResource(R.string.status_not_available),
                status = if (uiState.imageAssetsAvailable) stringResource(R.string.status_ready)
                else stringResource(R.string.status_text_only),
                description = stringResource(R.string.image_package_description),
                buttonLabel = stringResource(R.string.action_manage_images),
            )
        }
        item {
            UpdateCard(
                title = stringResource(R.string.app_package_title),
                version = uiState.appVersion,
                status = stringResource(R.string.status_up_to_date),
                description = stringResource(R.string.app_package_description),
                buttonLabel = stringResource(R.string.action_check_app),
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    uiState: CompanionUiState,
    preferences: UiPreferences,
    horizontalPadding: androidx.compose.ui.unit.Dp,
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
            top = 24.dp,
            bottom = 30.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            AppHeader(
                title = stringResource(R.string.settings_title),
                subtitle = stringResource(R.string.settings_subtitle),
            )
        }

        item {
            SettingsCard(
                title = stringResource(R.string.theme_setting_title),
                description = stringResource(R.string.theme_setting_description),
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(ThemeMode.entries) { mode ->
                        FilterChip(
                            selected = preferences.themeMode == mode,
                            onClick = { preferences.setThemeMode(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                                        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                        ThemeMode.DARK -> stringResource(R.string.theme_dark)
                                    },
                                )
                            },
                        )
                    }
                }
            }
        }

        item {
            SettingsCard(
                title = stringResource(R.string.image_setting_title),
                description = stringResource(R.string.image_setting_description),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ImageMode.entries.forEach { mode ->
                        PreferenceChoiceRow(
                            title = when (mode) {
                                ImageMode.AUTO -> stringResource(R.string.image_mode_auto)
                                ImageMode.SHOW -> stringResource(R.string.image_mode_show)
                                ImageMode.HIDE -> stringResource(R.string.image_mode_hide)
                            },
                            description = when (mode) {
                                ImageMode.AUTO -> stringResource(R.string.image_mode_auto_description)
                                ImageMode.SHOW -> stringResource(R.string.image_mode_show_description)
                                ImageMode.HIDE -> stringResource(R.string.image_mode_hide_description)
                            },
                            selected = preferences.imageMode == mode,
                            onSelected = { preferences.setImageMode(mode) },
                        )
                    }
                }
            }
        }

        item {
            SettingsCard(
                title = stringResource(R.string.language_setting_title),
                description = stringResource(R.string.language_setting_description),
            ) {
                Text(
                    text = stringResource(R.string.language_current, currentLanguage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.language_global_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SettingsCard(
                title = stringResource(R.string.about_title),
                description = stringResource(R.string.unofficial_notice),
            ) {
                Text(
                    text = stringResource(
                        R.string.installed_versions,
                        uiState.appVersion,
                        uiState.databaseVersion,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun AppHeader(
    title: String,
    subtitle: String,
    status: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
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
        if (status != null) {
            Spacer(modifier = Modifier.width(12.dp))
            AssistChip(
                onClick = {},
                label = { Text(status) },
            )
        }
    }
}

@Composable
private fun HeroCard(
    databaseVersion: String,
    imageAssetsAvailable: Boolean,
) {
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(24.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth(0.82f)) {
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
                        if (imageAssetsAvailable) stringResource(R.string.images_ready)
                        else stringResource(R.string.images_not_installed),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
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
private fun CategoryCard(category: CategoryUi) {
    Card(
        modifier = Modifier.width(148.dp),
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
                text = stringResource(category.titleRes),
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
                        .size(92.dp)
                        .clip(RoundedCornerShape(18.dp)),
                )
                Spacer(modifier = Modifier.width(16.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = entry.accentLabel.take(1),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Black,
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
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

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = entry.accentLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun LibraryGridCard(entry: LibraryEntryUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column {
            EntryArtwork(
                entry = entry,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.28f),
            )
            Column(modifier = Modifier.padding(15.dp)) {
                Text(
                    text = stringResource(entry.typeRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(entry.nameRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Text(
                    text = stringResource(entry.detailRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 5.dp),
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
            contentDescription = stringResource(R.string.entry_image_description, stringResource(entry.nameRes)),
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        val colors = if (entry.imageAvailable) {
            listOf(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.secondaryContainer,
            )
        } else {
            listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            )
        }
        Box(
            modifier = modifier.background(Brush.linearGradient(colors)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = entry.accentLabel.take(2),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun UpdateCard(
    title: String,
    version: String,
    status: String,
    description: String,
    buttonLabel: String,
) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = stringResource(R.string.version_label, version),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AssistChip(onClick = {}, label = { Text(status) })
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = {}) {
                Text(buttonLabel)
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
private fun PreferenceChoiceRow(
    title: String,
    description: String,
    selected: Boolean,
    onSelected: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(checked = selected, onCheckedChange = { if (it) onSelected() })
    }
}
