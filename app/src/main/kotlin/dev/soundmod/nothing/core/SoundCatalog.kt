package dev.soundmod.nothing.core

import android.media.AudioManager

enum class SlotType {
    /** system_server / SystemUI hook'u <key>.<uzantı> dosyasını okur */
    HOOK_FILE,
    /** Settings.Global anahtarı bir dosya yolu tutar */
    SETTINGS_GLOBAL
}

data class SoundSlot(
    val id: String,
    val title: String,
    val desc: String,
    val group: String,
    val type: SlotType,
    /** HOOK_FILE: stok dosya adı (uzantısız); SETTINGS_GLOBAL: ayar anahtarı */
    val key: String,
    /** Değişiklikten sonra ses efektlerini yeniden yükle */
    val reload: Boolean = false,
    /** Denemek için çalınabilir AudioManager.FX_* sabiti */
    val fx: Int? = null,
    /** Ses Sistem Arayüzü açılırken bir kez yüklenir: değişince SystemUI yenilenmeli */
    val restartSysUi: Boolean = false
)

object SoundCatalog {
    const val G_KEYS = "Klavye"
    const val G_TOUCH = "Dokunma ve ekran"
    const val G_LOCK = "Kilit ekranı"
    const val G_POWER = "Şarj ve pil"
    const val G_DOCK = "Dock"

    val groups = listOf(G_KEYS, G_TOUCH, G_LOCK, G_POWER, G_DOCK)

    val slots: List<SoundSlot> = listOf(
        SoundSlot("key_std", "Tuş sesi", "Harf, rakam ve noktalama tuşları", G_KEYS, SlotType.HOOK_FILE,
            "KeypressStandard", true, AudioManager.FX_KEYPRESS_STANDARD),
        SoundSlot("key_space", "Boşluk tuşu", "Boşluk çubuğu", G_KEYS, SlotType.HOOK_FILE,
            "KeypressSpacebar", true, AudioManager.FX_KEYPRESS_SPACEBAR),
        SoundSlot("key_del", "Silme tuşu", "Geri silme tuşu", G_KEYS, SlotType.HOOK_FILE,
            "KeypressDelete", true, AudioManager.FX_KEYPRESS_DELETE),
        SoundSlot("key_ret", "Enter tuşu", "Enter / gönder tuşu", G_KEYS, SlotType.HOOK_FILE,
            "KeypressReturn", true, AudioManager.FX_KEYPRESS_RETURN),
        SoundSlot("key_inv", "Geçersiz tuş", "Basılamayan tuş (çoğu klavye kullanmaz)", G_KEYS, SlotType.HOOK_FILE,
            "KeypressInvalid", true, AudioManager.FX_KEYPRESS_INVALID),

        SoundSlot("tick", "Dokunma tıkı", "Ekrana dokunma, odak ve geri hareketi", G_TOUCH, SlotType.HOOK_FILE,
            "Effect_Tick", true, AudioManager.FX_KEY_CLICK),
        SoundSlot("screenshot", "Ekran görüntüsü", "Ekran görüntüsü alınırken", G_TOUCH, SlotType.HOOK_FILE,
            "screenshot"),

        SoundSlot("lock", "Kilitleme", "Ekran kilitlenince", G_LOCK, SlotType.SETTINGS_GLOBAL,
            "lock_sound", restartSysUi = true),
        SoundSlot("unlock", "Kilit açma", "Kilit açılınca", G_LOCK, SlotType.SETTINGS_GLOBAL,
            "unlock_sound", restartSysUi = true),
        SoundSlot("trusted", "Güvenilir cihaz", "Güvenilir cihaz kilidi açınca", G_LOCK, SlotType.SETTINGS_GLOBAL,
            "trusted_sound", restartSysUi = true),

        SoundSlot("lowbat", "Düşük pil", "Pil azalınca", G_POWER, SlotType.SETTINGS_GLOBAL, "low_battery_sound"),
        SoundSlot("charge", "Şarj başladı", "Kablolu şarj takılınca", G_POWER, SlotType.SETTINGS_GLOBAL,
            "charging_started_sound"),
        SoundSlot("wcharge", "Kablosuz şarj", "Kablosuz şarj başlayınca", G_POWER, SlotType.SETTINGS_GLOBAL,
            "wireless_charging_started_sound"),

        SoundSlot("dock", "Masa dock'u", "Masa dock'una takılınca", G_DOCK, SlotType.SETTINGS_GLOBAL, "desk_dock_sound"),
        SoundSlot("cardock", "Araç dock'u", "Araç dock'una takılınca", G_DOCK, SlotType.SETTINGS_GLOBAL, "car_dock_sound")
    )
}
