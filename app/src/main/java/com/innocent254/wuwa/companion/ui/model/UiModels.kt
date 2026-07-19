package com.innocent254.wuwa.companion.ui.model

import android.content.Context
import androidx.annotation.StringRes
import com.innocent254.wuwa.companion.R

enum class AppDestination(
    @StringRes val labelRes: Int,
    val glyph: String,
) {
    HOME(R.string.navigation_home, "H"),
    LIBRARY(R.string.navigation_library, "L"),
    UPDATES(R.string.navigation_updates, "U"),
    SETTINGS(R.string.navigation_settings, "S"),
}

data class CategoryUi(
    val id: String,
    @StringRes val titleRes: Int,
    val count: Int,
    val glyph: String,
)

data class LibraryEntryUi(
    val id: String,
    val categoryId: String,
    @StringRes val nameRes: Int? = null,
    @StringRes val typeRes: Int? = null,
    @StringRes val detailRes: Int? = null,
    val nameText: String? = null,
    val typeText: String? = null,
    val detailText: String? = null,
    val metadata: List<Pair<String, String>> = emptyList(),
    val searchTerms: List<String> = emptyList(),
    val accentLabel: String,
    val imageAvailable: Boolean,
    val imageModel: Any? = null,
) {
    fun displayName(context: Context): String =
        nameText?.takeIf(String::isNotBlank)
            ?: nameRes?.let(context::getString)
            ?: id

    fun displayType(context: Context): String =
        typeText?.takeIf(String::isNotBlank)
            ?: typeRes?.let(context::getString)
            ?: categoryId

    fun displayDetail(context: Context): String =
        detailText?.takeIf(String::isNotBlank)
            ?: detailRes?.let(context::getString)
            ?: context.getString(R.string.catalog_summary_unavailable)

    fun matches(context: Context, query: String): Boolean {
        if (query.isBlank()) return true
        val needle = query.trim()
        return sequenceOf(
            displayName(context),
            displayType(context),
            displayDetail(context),
            categoryId,
            accentLabel,
            *metadata.flatMap { listOf(it.first, it.second) }.toTypedArray(),
            *searchTerms.toTypedArray(),
        ).any { it.contains(needle, ignoreCase = true) }
    }
}

data class CompanionUiState(
    val databaseVersion: String,
    val appVersion: String,
    val databaseReady: Boolean,
    val imageAssetsAvailable: Boolean,
    val isOffline: Boolean,
    val categories: List<CategoryUi>,
    val entries: List<LibraryEntryUi>,
    val catalogError: String? = null,
)

object DemoUiStateFactory {
    fun create(): CompanionUiState = CompanionUiState(
        databaseVersion = "0.0.0",
        appVersion = "0.0.0",
        databaseReady = false,
        imageAssetsAvailable = false,
        isOffline = true,
        categories = listOf(
            CategoryUi("resonator", R.string.category_resonators, 0, "R"),
            CategoryUi("weapon", R.string.category_weapons, 0, "W"),
            CategoryUi("echo", R.string.category_echoes, 0, "E"),
            CategoryUi("material", R.string.category_materials, 0, "M"),
        ),
        entries = emptyList(),
    )
}
