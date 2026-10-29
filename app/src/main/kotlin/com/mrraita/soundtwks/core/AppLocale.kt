package com.mrraita.soundtwks.core

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * In-app language choice: "system" follows the device (and Android 13+ per-app language setting), "en" and "tr"
 * force that language. The choice is stored in the app's own preferences so it can be read before any UI exists.
 */
object AppLocale {
    const val PREFS = "soundtweaks_ui"
    const val KEY = "language"
    const val SYSTEM = "system"
    const val EN = "en"
    const val TR = "tr"

    fun saved(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, SYSTEM) ?: SYSTEM

    fun save(ctx: Context, lang: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, lang).apply()
    }

    /** Returns a context whose resources use the saved language (or [base] itself when following the system). */
    fun wrap(base: Context): Context {
        val lang = saved(base)
        if (lang == SYSTEM) return base
        val cfg = Configuration(base.resources.configuration)
        cfg.setLocale(Locale.forLanguageTag(lang))
        return base.createConfigurationContext(cfg)
    }
}
