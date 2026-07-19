package com.innocent254.wuwa.companion.core.catalog

import android.content.Context
import com.innocent254.wuwa.companion.BuildConfig
import com.innocent254.wuwa.companion.R
import com.innocent254.wuwa.companion.core.security.SecureAssetStore
import com.innocent254.wuwa.companion.ui.model.CategoryUi
import com.innocent254.wuwa.companion.ui.model.CompanionUiState
import com.innocent254.wuwa.companion.ui.model.LibraryEntryUi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class CatalogRepository(context: Context) {
    private val appContext = context.applicationContext
    private val store = SecureAssetStore(appContext)
    private val json = Json { ignoreUnknownKeys = true }
    private val versionPreferences = appContext.getSharedPreferences(
        VERSION_PREFERENCES,
        Context.MODE_PRIVATE,
    )

    fun load(): CompanionUiState {
        val databaseVersion = installedDatabaseVersion()
        if (!store.contains(CATALOG_ASSET_ID)) {
            return emptyState(databaseVersion = databaseVersion)
        }

        val catalog = store.open(CATALOG_ASSET_ID).bufferedReader().use { reader ->
            json.decodeFromString<CatalogPayload>(reader.readText())
        }

        val entries = buildList {
            addAll(catalog.resonators.map(::toUiEntry))
            addAll(catalog.weapons.map(::toUiEntry))
            addAll(catalog.echoes.map(::toUiEntry))
            addAll(catalog.materials.map(::toUiEntry))
        }.sortedBy { it.nameText?.lowercase() }

        val categories = CATEGORY_DEFINITIONS.map { definition ->
            CategoryUi(
                id = definition.id,
                titleRes = definition.titleRes,
                count = entries.count { it.categoryId == definition.id },
                glyph = definition.glyph,
            )
        }

        return CompanionUiState(
            databaseVersion = databaseVersion,
            appVersion = BuildConfig.VERSION_NAME,
            databaseReady = true,
            imageAssetsAvailable = entries.any(LibraryEntryUi::imageAvailable),
            isOffline = true,
            categories = categories,
            entries = entries,
        )
    }

    fun emptyState(databaseVersion: String = installedDatabaseVersion(), error: String? = null) =
        CompanionUiState(
            databaseVersion = databaseVersion,
            appVersion = BuildConfig.VERSION_NAME,
            databaseReady = false,
            imageAssetsAvailable = false,
            isOffline = true,
            categories = CATEGORY_DEFINITIONS.map { definition ->
                CategoryUi(
                    id = definition.id,
                    titleRes = definition.titleRes,
                    count = 0,
                    glyph = definition.glyph,
                )
            },
            entries = emptyList(),
            catalogError = error,
        )

    private fun installedDatabaseVersion(): String =
        versionPreferences.getString(KEY_DATABASE_VERSION, DEFAULT_VERSION) ?: DEFAULT_VERSION

    private fun toUiEntry(record: CatalogEntity): LibraryEntryUi {
        val category = CATEGORY_DEFINITIONS.firstOrNull { it.id == record.entityType }
            ?: CategoryDefinition(record.entityType, R.string.library_title, "?")
        val imageAvailable = record.imagePath?.let(store::contains) == true
        val usefulCategory = record.categories.firstOrNull {
            it.isNotBlank() && it.length <= 24 && !it.contains("page", ignoreCase = true)
        }

        return LibraryEntryUi(
            id = record.id,
            categoryId = category.id,
            nameText = record.name,
            typeText = appContext.getString(category.titleRes),
            detailText = record.summary.ifBlank {
                appContext.getString(R.string.catalog_summary_unavailable)
            },
            searchTerms = record.categories,
            accentLabel = usefulCategory ?: category.glyph,
            imageAvailable = imageAvailable,
            imageModel = null,
        )
    }

    private data class CategoryDefinition(
        val id: String,
        @androidx.annotation.StringRes val titleRes: Int,
        val glyph: String,
    )

    private companion object {
        const val CATALOG_ASSET_ID = "catalog.json"
        const val VERSION_PREFERENCES = "update_versions"
        const val KEY_DATABASE_VERSION = "database_version"
        const val DEFAULT_VERSION = "0.0.0"

        val CATEGORY_DEFINITIONS = listOf(
            CategoryDefinition("resonator", R.string.category_resonators, "R"),
            CategoryDefinition("weapon", R.string.category_weapons, "W"),
            CategoryDefinition("echo", R.string.category_echoes, "E"),
            CategoryDefinition("material", R.string.category_materials, "M"),
        )
    }
}

@Serializable
private data class CatalogPayload(
    @SerialName("schema_version") val schemaVersion: Int = 0,
    val resonators: List<CatalogEntity> = emptyList(),
    val weapons: List<CatalogEntity> = emptyList(),
    val echoes: List<CatalogEntity> = emptyList(),
    val materials: List<CatalogEntity> = emptyList(),
)

@Serializable
private data class CatalogEntity(
    val id: String,
    val name: String,
    @SerialName("entity_type") val entityType: String,
    val summary: String = "",
    val categories: List<String> = emptyList(),
    @SerialName("image_path") val imagePath: String? = null,
)
