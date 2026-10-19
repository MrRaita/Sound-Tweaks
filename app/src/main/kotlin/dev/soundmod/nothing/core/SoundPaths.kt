package dev.soundmod.nothing.core

/** Hook (system_server / SystemUI) ve uygulama arasında paylaşılan sabitler. */
object SoundPaths {
    /** Root'un yönettiği, herkesin okuyabildiği (system_file bağlamlı) ses deposu */
    const val BASE = "/data/system/soundmod"
    /** Hook'un baktığı dizin: <stokAdı>.<uzantı> (ör. KeypressStandard.wav) */
    const val OVERRIDE_DIR = "$BASE/override"
    /** Settings.Global yolları için özel dosyalar */
    const val FILES_DIR = "$BASE/files"
    /** system_server hook durumu (depo dışında, system_server kendisi yazar) */
    const val STATUS_SYSTEM = "/data/system/soundmod_status"
    /** SystemUI hook durumu Settings.Global anahtarına yazılır */
    const val SYSUI_STATUS_KEY = "soundmod_sysui_status"
    /** SystemUI hook'unun son olayı (tanılama için) */
    const val SYSUI_LAST_KEY = "soundmod_sysui_last"
    /** Bu dosya varsa SystemUI hook'u son olayını Settings.Global'a yazar (yalnızca tanılama için) */
    const val DEBUG_FLAG = "$BASE/.debug"
    /**
     * Bu dosya varsa kilit ekranı sesleri (kilitleme, kilit açma, güvenilir cihaz) sistemin kısıtlamalarını
     * aşıp medya ses seviyesiyle SoundMod tarafından çalınır. Yoksa stok mekanizma kullanılır.
     */
    const val FORCE_FLAG = "$BASE/.force_lock"
    /** Kulaklık uyarısı betiğinin kendi klasörü (kök yöneticisinin okuduğu /data/adb altında) */
    const val HP_DIR = "/data/adb/soundmod"
    const val HP_SCRIPT = "$HP_DIR/hp_warning.sh"
    /** Magisk / KernelSU / APatch'in sistem başlamadan önce çalıştırdığı dizin; içine yalnızca 1 satırlık çağrı konur */
    const val HP_LAUNCHER = "/data/adb/post-fs-data.d/soundmod_hp_warning.sh"
    /** Eski sürümlerden kalan bayrak dosyası (temizlenir) */
    const val OLD_HP_FLAG = "$BASE/.no_hp_warning"
    /** Herkesin (SystemUI dahil) okuyabildiği SELinux tipi */
    const val CONTEXT = "u:object_r:system_file:s0"

    val EXTENSIONS = listOf("wav", "ogg", "mp3", "m4a", "flac", "opus", "aac")
}
