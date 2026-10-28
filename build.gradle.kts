// AGP 9: Kotlin support is built into AGP (built-in Kotlin); there is NO separate kotlin-android plugin.
// The Compose compiler plugin version must match the Kotlin version.
plugins {
    id("com.android.application") version "9.3.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}
