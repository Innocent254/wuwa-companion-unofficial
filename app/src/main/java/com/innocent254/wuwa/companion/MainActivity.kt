package com.innocent254.wuwa.companion

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import com.innocent254.wuwa.companion.core.catalog.CatalogViewModel
import com.innocent254.wuwa.companion.core.update.UpdateViewModel
import com.innocent254.wuwa.companion.ui.CompanionRoot
import com.innocent254.wuwa.companion.ui.preferences.UiPreferences
import com.innocent254.wuwa.companion.ui.splash.WuWaLaunchScreen
import com.innocent254.wuwa.companion.ui.theme.WuWaTheme
import java.io.File

class MainActivity : ComponentActivity() {
    private var pendingApk: File? = null

    private val unknownSourcesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        val file = pendingApk ?: return@registerForActivityResult
        if (canInstallPackages()) {
            pendingApk = null
            launchPackageInstaller(file)
        }
    }

    private val updateViewModel: UpdateViewModel by lazy {
        ViewModelProvider(this)[UpdateViewModel::class.java]
    }

    private val catalogViewModel: CatalogViewModel by lazy {
        ViewModelProvider(this)[CatalogViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val systemSplash = installSplashScreen()
        super.onCreate(savedInstanceState)
        systemSplash.setOnExitAnimationListener { splashScreenView ->
            splashScreenView.remove()
        }
        enableEdgeToEdge()

        setContent {
            val applicationContext = LocalContext.current.applicationContext
            val preferences = remember(applicationContext) {
                UiPreferences(applicationContext)
            }
            val uiState by catalogViewModel.uiState.collectAsState()
            val updateState by updateViewModel.uiState.collectAsState()
            var showLaunchScreen by rememberSaveable {
                mutableStateOf(savedInstanceState == null)
            }

            LaunchedEffect(updateState.database.installedVersion) {
                catalogViewModel.reload()
            }

            WuWaTheme(themeMode = preferences.themeMode) {
                if (showLaunchScreen) {
                    WuWaLaunchScreen(
                        onFinished = { showLaunchScreen = false },
                    )
                } else {
                    CompanionRoot(
                        uiState = uiState,
                        preferences = preferences,
                        updateViewModel = updateViewModel,
                        onInstallApk = ::requestPackageInstall,
                        onOpenUrl = ::openExternalUrl,
                    )
                }
            }
        }
    }

    private fun requestPackageInstall(file: File) {
        if (!file.isFile) {
            Toast.makeText(this, R.string.toast_downloaded_apk_missing, Toast.LENGTH_LONG).show()
            return
        }

        if (!canInstallPackages()) {
            pendingApk = file
            Toast.makeText(this, R.string.toast_allow_unknown_sources, Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:$packageName"),
            )
            unknownSourcesLauncher.launch(intent)
            return
        }

        launchPackageInstaller(file)
    }

    private fun canInstallPackages(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            packageManager.canRequestPackageInstalls()
    }

    private fun launchPackageInstaller(file: File) {
        val uri = FileProvider.getUriForFile(
            this,
            "$packageName.files",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun openExternalUrl(url: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            Toast.makeText(this, R.string.toast_cannot_open_link, Toast.LENGTH_LONG).show()
        }
    }
}
