package dev.soundmod.nothing.core

import android.media.AudioManager

enum class SlotType {
    /** The system_server / SystemUI hook reads <key>.<extension> */
    HOOK_FILE,
    /** The Settings.Global key holds a file path */
    SETTINGS_GLOBAL
}

data class SoundSlot(
    val id: String,
    val title: String,
    val desc: String,
    val group: String,
    val type: SlotType,
    /** HOOK_FILE: stock file name (no extension); SETTINGS_GLOBAL: setting key */
    val key: String,
    /** Reload the sound effects after a change */
    val reload: Boolean = false,
    /** AudioManager.FX_* constant that can be played to preview the sound */
    val fx: Int? = null,
    /** The stock mechanism loads this sound once when SystemUI starts: refreshing SystemUI is recommended after a change */
    val sysUiReload: Boolean = false
)

object SoundCatalog {
    const val G_KEYS = "Keyboard"
    const val G_TOUCH = "Touch and screen"
    const val G_LOCK = "Lock screen"
    const val G_POWER = "Charging and battery"
    const val G_DOCK = "Dock"

    val groups = listOf(G_KEYS, G_TOUCH, G_LOCK, G_POWER, G_DOCK)

    val slots: List<SoundSlot> = listOf(
        SoundSlot("key_std", "Key press", "Letter, number and punctuation keys", G_KEYS, SlotType.HOOK_FILE,
            "KeypressStandard", true, AudioManager.FX_KEYPRESS_STANDARD),
        SoundSlot("key_space", "Space key", "Space bar", G_KEYS, SlotType.HOOK_FILE,
            "KeypressSpacebar", true, AudioManager.FX_KEYPRESS_SPACEBAR),
        SoundSlot("key_del", "Delete key", "Backspace key", G_KEYS, SlotType.HOOK_FILE,
            "KeypressDelete", true, AudioManager.FX_KEYPRESS_DELETE),
        SoundSlot("key_ret", "Enter key", "Enter / send key", G_KEYS, SlotType.HOOK_FILE,
            "KeypressReturn", true, AudioManager.FX_KEYPRESS_RETURN),
        SoundSlot("key_inv", "Invalid key", "Key that cannot be pressed (most keyboards do not use it)", G_KEYS, SlotType.HOOK_FILE,
            "KeypressInvalid", true, AudioManager.FX_KEYPRESS_INVALID),

        SoundSlot("tick", "Touch click", "Screen touch, focus and back gesture", G_TOUCH, SlotType.HOOK_FILE,
            "Effect_Tick", true, AudioManager.FX_KEY_CLICK),
        SoundSlot("screenshot", "Screenshot", "When a screenshot is taken", G_TOUCH, SlotType.HOOK_FILE,
            "screenshot"),

        // Stock mechanism: Settings.Global path. In the optional "force play" mode SoundMod plays the same file itself.
        SoundSlot("lock", "Lock", "When the screen locks", G_LOCK, SlotType.SETTINGS_GLOBAL,
            "lock_sound", sysUiReload = true),
        SoundSlot("unlock", "Unlock", "When the device is unlocked", G_LOCK, SlotType.SETTINGS_GLOBAL,
            "unlock_sound", sysUiReload = true),
        SoundSlot("trusted", "Trusted device", "When a trusted device unlocks it", G_LOCK, SlotType.SETTINGS_GLOBAL,
            "trusted_sound", sysUiReload = true),

        SoundSlot("lowbat", "Low battery", "When the battery runs low", G_POWER, SlotType.SETTINGS_GLOBAL, "low_battery_sound"),
        SoundSlot("charge", "Charging started", "When a charging cable is plugged in", G_POWER, SlotType.SETTINGS_GLOBAL,
            "charging_started_sound"),
        SoundSlot("wcharge", "Wireless charging", "When wireless charging starts", G_POWER, SlotType.SETTINGS_GLOBAL,
            "wireless_charging_started_sound"),

        SoundSlot("dock", "Desk dock", "When placed in a desk dock", G_DOCK, SlotType.SETTINGS_GLOBAL, "desk_dock_sound"),
        SoundSlot("cardock", "Car dock", "When placed in a car dock", G_DOCK, SlotType.SETTINGS_GLOBAL, "car_dock_sound"),
        SoundSlot("undock", "Desk undock", "When removed from a desk dock", G_DOCK, SlotType.SETTINGS_GLOBAL,
            "desk_undock_sound"),
        SoundSlot("carundock", "Car undock", "When removed from a car dock", G_DOCK, SlotType.SETTINGS_GLOBAL,
            "car_undock_sound")
    )
}
