package com.innocent254.wuwa.companion.core.update

import android.app.Application
import android.content.Context
import com.innocent254.wuwa.companion.BuildConfig
import com.innocent254.wuwa.companion.R
import com.innocent254.wuwa.companion.core.security.SecureAssetImporter
import com.innocent254.wuwa.companion.core.security.SecureAssetStore
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val versionPreferences = appContext.getSharedPreferences(
        VERSION_PREFERENCES,
        Context.MODE_PRIVATE,
    )
    private val repository = UpdateRepository(appContext)
    private val importer = SecureAssetImporter(SecureAssetStore(appContext))

    private val installedDatabaseVersion: String
        get() = versionPreferences.getString(KEY_DATABASE_VERSION, DEFAULT_VERSION) ?: DEFAULT_VERSION

    private val installedAssetVersion: String
        get() = versionPreferences.getString(KEY_ASSET_VERSION, DEFAULT_VERSION) ?: DEFAULT_VERSION

    private val _uiState = MutableStateFlow(
        UpdateCenterUiState(
            database = UpdateItemUiState(
                installedVersion = installedDatabaseVersion,
                phase = UpdatePhase.CHECKING,
            ),
            app = UpdateItemUiState(
                installedVersion = BuildConfig.VERSION_NAME,
                phase = UpdatePhase.CHECKING,
            ),
            assetVersion = installedAssetVersion,
            supportsImages = BuildConfig.SUPPORTS_IMAGES,
        ),
    )
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UpdateEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    private var latestAvailability: UpdateAvailability? = null
    private var databaseJob: Job? = null
    private var appJob: Job? = null

    init {
        refreshAvailability(silent = true)
    }

    fun refreshAvailability(silent: Boolean = false) {
        if (databaseJob?.isActive == true || appJob?.isActive == true) return

        viewModelScope.launch {
            _uiState.update { current ->
                current.copy(
                    database = current.database.copy(
                        phase = UpdatePhase.CHECKING,
                        message = null,
                        justUpdated = false,
                    ),
                    app = current.app.copy(
                        phase = UpdatePhase.CHECKING,
                        message = null,
                    ),
                )
            }

            val availability = repository.check(
                currentAppVersionCode = BuildConfig.VERSION_CODE,
                currentDatabaseVersion = installedDatabaseVersion,
                currentAssetVersion = installedAssetVersion,
            )
            latestAvailability = availability
            applyAvailability(availability)

            if (!silent && availability.data != null && !availability.databaseUpdateAvailable) {
                if (availability.databaseRequiresAppUpdate) {
                    emitToast(appContext.getString(R.string.toast_database_requires_app_update))
                } else {
                    emitToast(appContext.getString(R.string.toast_database_current))
                }
            }
        }
    }

    fun onDatabaseAction() {
        if (databaseJob?.isActive == true) return

        databaseJob = viewModelScope.launch {
            var availability = latestAvailability
            if (availability?.databaseUpdateAvailable != true) {
                _uiState.update { current ->
                    current.copy(
                        database = current.database.copy(
                            phase = UpdatePhase.CHECKING,
                            message = null,
                            justUpdated = false,
                        ),
                    )
                }

                availability = repository.check(
                    currentAppVersionCode = BuildConfig.VERSION_CODE,
                    currentDatabaseVersion = installedDatabaseVersion,
                    currentAssetVersion = installedAssetVersion,
                )
                latestAvailability = availability

                if (availability.data == null) {
                    _uiState.update { current ->
                        current.copy(
                            database = current.database.copy(
                                phase = UpdatePhase.ERROR,
                                message = appContext.getString(R.string.status_check_failed),
                            ),
                        )
                    }
                    emitToast(appContext.getString(R.string.toast_database_check_failed))
                    return@launch
                }

                if (!availability.databaseUpdateAvailable) {
                    applyAvailability(availability)
                    if (availability.databaseRequiresAppUpdate) {
                        emitToast(appContext.getString(R.string.toast_database_requires_app_update))
                    } else {
                        emitToast(appContext.getString(R.string.toast_database_current))
                    }
                    return@launch
                }
            }

            installDatabaseUpdate(checkNotNull(availability))
        }
    }

    fun onAppAction() {
        if (appJob?.isActive == true) return
        val current = _uiState.value.app

        if (current.phase == UpdatePhase.READY_TO_INSTALL) {
            current.downloadedFilePath?.let { path ->
                _events.tryEmit(UpdateEvent.InstallApk(path))
            }
            return
        }

        val manifest = latestAvailability?.app ?: return
        val apkUrl = manifest.apkUrl?.takeIf { it.isNotBlank() } ?: return
        val apkSha256 = manifest.apkSha256?.takeIf { it.isNotBlank() } ?: return
        if (current.phase != UpdatePhase.AVAILABLE) return

        appJob = viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    app = state.app.copy(
                        phase = UpdatePhase.DOWNLOADING,
                        progress = 0f,
                        message = null,
                    ),
                )
            }

            runCatching {
                repository.downloadVerified(
                    url = apkUrl,
                    expectedSha256 = apkSha256,
                    fileName = "WuWaCompanion-${manifest.versionName}-${BuildConfig.BUILD_PROFILE}.apk",
                    destinationDirectory = File(appContext.cacheDir, "apk_updates"),
                    onProgress = { downloaded, total ->
                        val progress = if (total > 0L) downloaded.toFloat() / total.toFloat() else 0f
                        _uiState.update { state ->
                            state.copy(app = state.app.copy(progress = progress.coerceIn(0f, 1f)))
                        }
                    },
                )
            }.onSuccess { apk ->
                _uiState.update { state ->
                    state.copy(
                        app = state.app.copy(
                            phase = UpdatePhase.READY_TO_INSTALL,
                            progress = 1f,
                            downloadedFilePath = apk.absolutePath,
                        ),
                    )
                }
                _events.tryEmit(UpdateEvent.InstallApk(apk.absolutePath))
            }.onFailure {
                _uiState.update { state ->
                    state.copy(
                        app = state.app.copy(
                            phase = UpdatePhase.ERROR,
                            message = appContext.getString(R.string.status_download_failed),
                        ),
                    )
                }
                emitToast(appContext.getString(R.string.toast_app_download_failed))
            }
        }
    }

    private suspend fun installDatabaseUpdate(availability: UpdateAvailability) {
        val databasePackage = availability.data?.database ?: return
        val databaseUrl = databasePackage.url
        val databaseSha = databasePackage.sha256

        if (databaseUrl.isNullOrBlank() || databaseSha.isNullOrBlank()) {
            _uiState.update { current ->
                current.copy(
                    database = current.database.copy(
                        phase = UpdatePhase.ERROR,
                        message = appContext.getString(R.string.status_package_incomplete),
                    ),
                )
            }
            emitToast(appContext.getString(R.string.toast_database_update_failed))
            return
        }

        _uiState.update { current ->
            current.copy(
                database = current.database.copy(
                    phase = UpdatePhase.DOWNLOADING,
                    progress = 0f,
                    availableVersion = databasePackage.version,
                    message = appContext.getString(R.string.status_downloading_database),
                ),
            )
        }

        val databaseResult = runCatching {
            val packageFile = repository.downloadVerified(
                url = databaseUrl,
                expectedSha256 = databaseSha,
                fileName = "database-${databasePackage.version}.wupack",
                onProgress = { downloaded, total ->
                    val progress = if (total > 0L) downloaded.toFloat() / total.toFloat() else 0f
                    _uiState.update { current ->
                        current.copy(
                            database = current.database.copy(
                                progress = progress.coerceIn(0f, 1f),
                            ),
                        )
                    }
                },
            )
            importer.importPackage(packageFile)
        }

        if (databaseResult.isFailure) {
            _uiState.update { current ->
                current.copy(
                    database = current.database.copy(
                        phase = UpdatePhase.ERROR,
                        message = appContext.getString(R.string.status_download_failed),
                    ),
                )
            }
            emitToast(appContext.getString(R.string.toast_database_update_failed))
            return
        }

        versionPreferences.edit()
            .putString(KEY_DATABASE_VERSION, databasePackage.version)
            .apply()

        var assetVersion = installedAssetVersion
        if (BuildConfig.SUPPORTS_IMAGES && availability.assetUpdateAvailable) {
            val assetPackage = availability.data?.assets
            val assetUrl = assetPackage?.url
            val assetSha256 = assetPackage?.sha256
            if (assetPackage != null && assetUrl != null && assetSha256 != null) {
                _uiState.update { current ->
                    current.copy(
                        database = current.database.copy(
                            progress = 0f,
                            message = appContext.getString(R.string.status_downloading_images),
                        ),
                    )
                }

                runCatching {
                    val packageFile = repository.downloadVerified(
                        url = assetUrl,
                        expectedSha256 = assetSha256,
                        fileName = "assets-${assetPackage.version}.wupack",
                        onProgress = { downloaded, total ->
                            val progress = if (total > 0L) downloaded.toFloat() / total.toFloat() else 0f
                            _uiState.update { current ->
                                current.copy(
                                    database = current.database.copy(
                                        progress = progress.coerceIn(0f, 1f),
                                    ),
                                )
                            }
                        },
                    )
                    importer.importPackage(packageFile)
                }.onSuccess {
                    assetVersion = assetPackage.version
                    versionPreferences.edit()
                        .putString(KEY_ASSET_VERSION, assetPackage.version)
                        .apply()
                }.onFailure {
                    emitToast(appContext.getString(R.string.toast_asset_update_failed))
                }
            }
        }

        _uiState.update { current ->
            current.copy(
                database = current.database.copy(
                    installedVersion = databasePackage.version,
                    availableVersion = null,
                    phase = UpdatePhase.UP_TO_DATE,
                    progress = 1f,
                    message = null,
                    justUpdated = true,
                ),
                assetVersion = assetVersion,
            )
        }
        emitToast(
            appContext.getString(
                R.string.toast_database_updated,
                databasePackage.version,
            ),
        )
    }

    private fun applyAvailability(availability: UpdateAvailability) {
        _uiState.update { current ->
            val databaseState = when {
                availability.data == null -> current.database.copy(
                    phase = UpdatePhase.ERROR,
                    message = appContext.getString(R.string.status_check_failed),
                )

                availability.databaseRequiresAppUpdate -> current.database.copy(
                    installedVersion = installedDatabaseVersion,
                    availableVersion = availability.data?.database?.version,
                    phase = UpdatePhase.ERROR,
                    progress = 0f,
                    message = appContext.getString(R.string.status_app_update_required),
                    justUpdated = false,
                )

                availability.databaseUpdateAvailable -> current.database.copy(
                    installedVersion = installedDatabaseVersion,
                    availableVersion = availability.data?.database?.version,
                    phase = UpdatePhase.AVAILABLE,
                    progress = 0f,
                    message = null,
                    justUpdated = false,
                )

                installedDatabaseVersion == DEFAULT_VERSION && availability.data?.database?.available == false ->
                    current.database.copy(
                        installedVersion = DEFAULT_VERSION,
                        availableVersion = null,
                        phase = UpdatePhase.NOT_AVAILABLE,
                        message = appContext.getString(R.string.status_not_published),
                        justUpdated = false,
                    )

                else -> current.database.copy(
                    installedVersion = installedDatabaseVersion,
                    availableVersion = null,
                    phase = UpdatePhase.UP_TO_DATE,
                    progress = 0f,
                    message = null,
                    justUpdated = false,
                )
            }

            val appState = when {
                availability.app == null -> current.app.copy(
                    phase = UpdatePhase.ERROR,
                    message = appContext.getString(R.string.status_check_failed),
                )

                availability.appUpdateAvailable -> current.app.copy(
                    installedVersion = BuildConfig.VERSION_NAME,
                    availableVersion = availability.app?.versionName,
                    phase = UpdatePhase.AVAILABLE,
                    progress = 0f,
                    message = null,
                )

                else -> current.app.copy(
                    installedVersion = BuildConfig.VERSION_NAME,
                    availableVersion = null,
                    phase = UpdatePhase.UP_TO_DATE,
                    progress = 0f,
                    message = null,
                )
            }

            current.copy(
                database = databaseState,
                app = appState,
                assetVersion = installedAssetVersion,
            )
        }
    }

    private fun emitToast(text: String) {
        _events.tryEmit(UpdateEvent.ToastMessage(text))
    }

    private companion object {
        const val VERSION_PREFERENCES = "update_versions"
        const val KEY_DATABASE_VERSION = "database_version"
        const val KEY_ASSET_VERSION = "asset_version"
        const val DEFAULT_VERSION = "0.0.0"
    }
}
