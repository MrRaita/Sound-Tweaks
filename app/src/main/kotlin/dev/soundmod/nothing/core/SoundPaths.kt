package dev.soundmod.nothing.core

/** Constants shared between the hooks (system_server / SystemUI) and the app. */
object SoundPaths {
    /** libxposed API version the module is built against (keep in sync with module.prop and build.gradle.kts) */
    const val MODULE_API = 101
    /** Root-managed, world-readable (system_file context) sound store */
    const val BASE = "/data/system/soundmod"
    /** Directory the hook looks in: <stockName>.<extension> (e.g. KeypressStandard.wav) */
    const val OVERRIDE_DIR = "$BASE/override"
    /** Custom files for Settings.Global paths */
    const val FILES_DIR = "$BASE/files"
    /** system_server hook status (outside the store; written by system_server itself) */
    const val STATUS_SYSTEM = "/data/system/soundmod_status"
    /** SystemUI hook status is written to a Settings.Global key */
    const val SYSUI_STATUS_KEY = "soundmod_sysui_status"
    /** Last event of the SystemUI hook (for diagnostics) */
    const val SYSUI_LAST_KEY = "soundmod_sysui_last"
    /** While this file exists the SystemUI hook writes its last event to Settings.Global (diagnostics only) */
    const val DEBUG_FLAG = "$BASE/.debug"
    /**
     * While this file exists the lock screen sounds (lock, unlock, trusted device) bypass the system restrictions
     * and are played by SoundMod at the media volume. Otherwise the stock mechanism is used.
     */
    const val FORCE_FLAG = "$BASE/.force_lock"
    /** The headphone warning script's own folder (under /data/adb, which root managers read) */
    const val HP_DIR = "/data/adb/soundmod"
    const val HP_SCRIPT = "$HP_DIR/hp_warning.sh"
    /** Directory Magisk / KernelSU / APatch run before the system starts; only a one-line call is placed in it */
    const val HP_LAUNCHER = "/data/adb/post-fs-data.d/soundmod_hp_warning.sh"
    /** Flag file left over from older versions (cleaned up) */
    const val OLD_HP_FLAG = "$BASE/.no_hp_warning"
    /** SELinux type readable by everyone (including SystemUI) */
    const val CONTEXT = "u:object_r:system_file:s0"

    /** Supported audio extensions, in the order the hook looks for overrides. Single source for the scanner too. */
    val EXTENSIONS = listOf("wav", "ogg", "mp3", "m4a", "flac", "opus", "aac")

    /** Directories 755, regular files 644 (sound files must be readable by the system, never executable). */
    fun storePermsCmd(base: String): String {
        val b = Root.q(base)
        return "find $b -type d -exec chmod 755 {} \\; && find $b -type f -exec chmod 644 {} \\;"
    }
}
