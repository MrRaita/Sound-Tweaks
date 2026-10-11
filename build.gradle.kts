// AGP 9: Kotlin desteği AGP'nin içinde (built-in Kotlin), ayrıca kotlin-android eklentisi YOK.
// Compose derleyici eklentisi Kotlin sürümüyle aynı olmalı.
plugins {
    id("com.android.application") version "9.3.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}
