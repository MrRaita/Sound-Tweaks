plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.soundmod.nothing"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.soundmod.nothing"
        // LSPosed/Vector tabanı Android 8.1 (API 27); ByeDPI'daki tercihine uygun olarak Android 9+
        minSdk = 28
        targetSdk = 36
        // Her derlemede artsın diye CI çalışma numarasını kullanır
        versionCode = (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1)
        versionName = "0.11.4"
        // Ayrıntılı olay kaydı yalnızca ön derlemede (SOUNDMOD_VERBOSE=true); kararlı sürümde ve yerelde yok
        buildConfigField("boolean", "VERBOSE_LOG", (System.getenv("SOUNDMOD_VERBOSE") == "true").toString())
    }

    // Yayın imzası: ön derleme ve kararlı sürüm aynı anahtarla imzalanır. Anahtar CI'da ortam değişkenleriyle
    // verilir (SIGNING_KEYSTORE_FILE, SIGNING_KEYSTORE_PASSWORD, SIGNING_KEY_ALIAS, SIGNING_KEY_PASSWORD).
    // Tanımlı değilse (yerel derleme, çatal depo) debug anahtarına düşülür.
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
            // Hook sınıflarını R8 bozmasın diye küçültme kapalı
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName(if (releaseKeystore != null) "release" else "debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    // libxposed: API'yi framework (Vector) çalışma anında sağlar, APK'ya girmez
    compileOnly("io.github.libxposed:api:101.0.1")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.12.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Compose 1.12.x (compileSdk 37 + AGP 9 ister). Material3 Expressive alfa sürümü ByeDPI'dakiyle aynı.
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material3:material3:1.5.0-alpha29")
}
