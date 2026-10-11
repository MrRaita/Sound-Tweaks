package dev.soundmod.nothing.core

/** Hook (system_server / SystemUI) ve uygulama arasında paylaşılan sabitler. */
object SoundPaths {
    const val BASE = "/data/system/soundmod"
    /** Hook'un baktığı dizin: <stokAdı>.<uzantı> (ör. KeypressStandard.wav) */
    const val OVERRIDE_DIR = "$BASE/override"
    /** Settings.Global yolları için özel dosyalar */
    const val FILES_DIR = "$BASE/files"
    const val STATUS_SYSTEM = "$BASE/.status_system"
    const val STATUS_SYSUI = "$BASE/.status_sysui"

    val EXTENSIONS = listOf("wav", "ogg", "mp3", "m4a", "flac", "opus", "aac")
}
