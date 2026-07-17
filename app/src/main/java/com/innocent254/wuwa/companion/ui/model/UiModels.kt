package com.innocent254.wuwa.companion.ui.model

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
    @StringRes val titleRes: Int,
    val count: Int,
    val glyph: String,
)

data class LibraryEntryUi(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val typeRes: Int,
    @StringRes val detailRes: Int,
    val accentLabel: String,
    val imageAvailable: Boolean,
    val imageModel: Any? = null,
)

data class CompanionUiState(
    val databaseVersion: String,
    val appVersion: String,
    val databaseReady: Boolean,
    val imageAssetsAvailable: Boolean,
    val isOffline: Boolean,
    val categories: List<CategoryUi>,
    val entries: List<LibraryEntryUi>,
)

object DemoUiStateFactory {
    fun create(): CompanionUiState = CompanionUiState(
        databaseVersion = "0.1.8",
        appVersion = "0.1.0",
        databaseReady = true,
        imageAssetsAvailable = true,
        isOffline = true,
        categories = listOf(
            CategoryUi(R.string.category_resonators, 57, "R"),
            CategoryUi(R.string.category_weapons, 120, "W"),
            CategoryUi(R.string.category_echoes, 180, "E"),
            CategoryUi(R.string.category_materials, 131, "M"),
        ),
        entries = listOf(
            LibraryEntryUi(
                id = "resonator-spotlight",
                nameRes = R.string.sample_resonator_name,
                typeRes = R.string.category_resonators,
                detailRes = R.string.sample_resonator_detail,
                accentLabel = "AERO",
                imageAvailable = true,
            ),
            LibraryEntryUi(
                id = "weapon-spotlight",
                nameRes = R.string.sample_weapon_name,
                typeRes = R.string.category_weapons,
                detailRes = R.string.sample_weapon_detail,
                accentLabel = "5★",
                imageAvailable = true,
            ),
            LibraryEntryUi(
                id = "echo-spotlight",
                nameRes = R.string.sample_echo_name,
                typeRes = R.string.category_echoes,
                detailRes = R.string.sample_echo_detail,
                accentLabel = "COST 4",
                imageAvailable = true,
            ),
            LibraryEntryUi(
                id = "material-spotlight",
                nameRes = R.string.sample_material_name,
                typeRes = R.string.category_materials,
                detailRes = R.string.sample_material_detail,
                accentLabel = "ASCENSION",
                imageAvailable = false,
            ),
        ),
    )
}
