package com.mrraita.soundtwks.core

import android.media.AudioManager
import androidx.annotation.StringRes
import com.mrraita.soundtwks.R

enum class SlotType {
    /** The system_server / SystemUI hook reads <key>.<extension> */
    HOOK_FILE,
    /** The Settings.Global key holds a file path */
    SETTINGS_GLOBAL
}

data class SoundSlot(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    /** String resource id of the group title; also the grouping key */
    @StringRes val group: Int,
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
    val G_KEYS = R.string.grp_keys
    val G_TOUCH = R.string.grp_touch
    val G_LOCK = R.string.grp_lock
    val G_POWER = R.string.grp_power
    val G_DOCK = R.string.grp_dock

    val groups = listOf(G_KEYS, G_TOUCH, G_LOCK, G_POWER, G_DOCK)

    val slots: List<SoundSlot> = listOf(
        SoundSlot("key_std", R.string.slot_key_std, R.string.slot_key_std_desc, G_KEYS, SlotType.HOOK_FILE,
            "KeypressStandard", true, AudioManager.FX_KEYPRESS_STANDARD),
        SoundSlot("key_space", R.string.slot_key_space, R.string.slot_key_space_desc, G_KEYS, SlotType.HOOK_FILE,
            "KeypressSpacebar", true, AudioManager.FX_KEYPRESS_SPACEBAR),
        SoundSlot("key_del", R.string.slot_key_del, R.string.slot_key_del_desc, G_KEYS, SlotType.HOOK_FILE,
            "KeypressDelete", true, AudioManager.FX_KEYPRESS_DELETE),
        SoundSlot("key_ret", R.string.slot_key_ret, R.string.slot_key_ret_desc, G_KEYS, SlotType.HOOK_FILE,
            "KeypressReturn", true, AudioManager.FX_KEYPRESS_RETURN),
        SoundSlot("key_inv", R.string.slot_key_inv, R.string.slot_key_inv_desc, G_KEYS, SlotType.HOOK_FILE,
            "KeypressInvalid", true, AudioManager.FX_KEYPRESS_INVALID),

        SoundSlot("tick", R.string.slot_tick, R.string.slot_tick_desc, G_TOUCH, SlotType.HOOK_FILE,
            "Effect_Tick", true, AudioManager.FX_KEY_CLICK),
        SoundSlot("screenshot", R.string.slot_screenshot, R.string.slot_screenshot_desc, G_TOUCH, SlotType.HOOK_FILE,
            "screenshot"),

        // Stock mechanism: Settings.Global path. In the optional "force play" mode Sound Tweaks plays the same file itself.
        SoundSlot("lock", R.string.slot_lock, R.string.slot_lock_desc, G_LOCK, SlotType.SETTINGS_GLOBAL,
            "lock_sound", sysUiReload = true),
        SoundSlot("unlock", R.string.slot_unlock, R.string.slot_unlock_desc, G_LOCK, SlotType.SETTINGS_GLOBAL,
            "unlock_sound", sysUiReload = true),
        SoundSlot("trusted", R.string.slot_trusted, R.string.slot_trusted_desc, G_LOCK, SlotType.SETTINGS_GLOBAL,
            "trusted_sound", sysUiReload = true),

        SoundSlot("lowbat", R.string.slot_lowbat, R.string.slot_lowbat_desc, G_POWER, SlotType.SETTINGS_GLOBAL, "low_battery_sound"),
        SoundSlot("charge", R.string.slot_charge, R.string.slot_charge_desc, G_POWER, SlotType.SETTINGS_GLOBAL,
            "charging_started_sound"),
        SoundSlot("wcharge", R.string.slot_wcharge, R.string.slot_wcharge_desc, G_POWER, SlotType.SETTINGS_GLOBAL,
            "wireless_charging_started_sound"),

        SoundSlot("dock", R.string.slot_dock, R.string.slot_dock_desc, G_DOCK, SlotType.SETTINGS_GLOBAL, "desk_dock_sound"),
        SoundSlot("cardock", R.string.slot_cardock, R.string.slot_cardock_desc, G_DOCK, SlotType.SETTINGS_GLOBAL, "car_dock_sound"),
        SoundSlot("undock", R.string.slot_undock, R.string.slot_undock_desc, G_DOCK, SlotType.SETTINGS_GLOBAL,
            "desk_undock_sound"),
        SoundSlot("carundock", R.string.slot_carundock, R.string.slot_carundock_desc, G_DOCK, SlotType.SETTINGS_GLOBAL,
            "car_undock_sound")
    )
}
