plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val resolvedVersionName = providers.gradleProperty("VERSION_NAME").orNull ?: "0.2.0"
val resolvedVersionCode = providers.gradleProperty("VERSION_CODE").orNull ?: "2"
val resolvedSupportsImages = providers.gradleProperty("SUPPORTS_IMAGES")
    .orNull
    ?.toBooleanStrictOrNull()
    ?: true
val resolvedBuildProfile = if (resolvedSupportsImages) "images" else "minimal"

android {
    namespace = "com.innocent254.wuwa.companion"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.innocent254.wuwa.companion"
        minSdk = 26
        targetSdk = 35
        versionCode = resolvedVersionCode.toInt()
        versionName = resolvedVersionName

        buildConfigField(
            "String",
            "DATABASE_MANIFEST_URL",
            "\"https://raw.githubusercontent.com/Innocent254/wuwa-database-server/main/public/latest/version.json\"",
        )
        buildConfigField(
            "String",
            "APP_MANIFEST_URL",
            "\"https://raw.githubusercontent.com/Innocent254/wuwa-companion-unofficial/main/updates/app-update-$resolvedBuildProfile.json\"",
        )
        buildConfigField("boolean", "SUPPORTS_IMAGES", resolvedSupportsImages.toString())
        buildConfigField("String", "BUILD_PROFILE", "\"$resolvedBuildProfile\"")
        buildConfigField(
            "String",
            "CURRENT_RELEASE_NOTES_URL",
            "\"https://github.com/Innocent254/wuwa-companion-unofficial/releases/tag/app-$resolvedBuildProfile-v$resolvedVersionName\"",
        )
    }

    signingConfigs {
        create("release") {
            val storePath = System.getenv("ANDROID_KEYSTORE_PATH")
            if (!storePath.isNullOrBlank()) {
                storeFile = file(storePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (!System.getenv("ANDROID_KEYSTORE_PATH").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)

    debugImplementation(libs.compose.ui.tooling)
}
