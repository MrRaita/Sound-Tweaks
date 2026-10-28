plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.soundmod.nothing"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.soundmod.nothing"
        // The LSPosed/Vector base is Android 8.1 (API 27); we require Android 9+
        minSdk = 28
        targetSdk = 36
        // Uses the CI run number so it increases with every build
        versionCode = (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1)
        versionName = "0.12.0"
        // The verbose event log exists only in the pre-build (SOUNDMOD_VERBOSE=true); not in stable or local builds
        buildConfigField("boolean", "VERBOSE_LOG", (System.getenv("SOUNDMOD_VERBOSE") == "true").toString())
    }

    // Release signing: pre-builds and stable releases are signed with the same key. The key is supplied in CI through
    // environment variables (SIGNING_KEYSTORE_FILE, SIGNING_KEYSTORE_PASSWORD, SIGNING_KEY_ALIAS, SIGNING_KEY_PASSWORD).
    // If they are not defined (local build, fork) the debug key is used.
    val releaseKeystore = System.getenv("SIGNING_KEYSTORE_FILE")?.takeIf { it.isNotBlank() && file(it).exists() }
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("SIGNING_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            // Shrinking is off so R8 does not break the hook classes
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName(if (releaseKeystore != null) "release" else "debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        // If lint finds an Error the "Android Lint" job fails; warnings do not break it
        abortOnError = true
        checkReleaseBuilds = false
        // The hooks deliberately reach hidden system classes through reflection; the KTX suggestion is a style preference
        disable += "PrivateApi"
        disable += "UseKtx"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    // libxposed: the framework (Vector) provides the API at runtime; it is not packaged into the APK
    compileOnly("io.github.libxposed:api:101.0.1")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.12.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    // Compose 1.12.x (needs compileSdk 37 + AGP 9). The Material3 Expressive pre-release is used for the Expressive components.
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material3:material3:1.5.0-alpha29")
}
