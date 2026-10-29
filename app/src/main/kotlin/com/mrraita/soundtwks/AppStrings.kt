package com.mrraita.soundtwks

import android.content.Context
import androidx.annotation.StringRes
import com.mrraita.soundtwks.core.AppLocale

/**
 * Access to localized strings from anywhere (composables, the ViewModel, core code) without threading a Context
 * through every call. Changing the language recreates the activity, so reading the string at call time is enough.
 */
object AppStrings {
    @Volatile
    private var ctx: Context? = null

    /** Call when the app starts and whenever the language changes. */
    fun refresh(context: Context) {
        ctx = AppLocale.wrap(context.applicationContext ?: context)
    }

    fun get(@StringRes id: Int, vararg args: Any): String {
        val c = ctx ?: return ""
        return if (args.isEmpty()) c.getString(id) else c.getString(id, *args)
    }
}

/** Short alias used throughout the code: `txt(R.string.x, arg)` */
fun txt(@StringRes id: Int, vararg args: Any): String = AppStrings.get(id, *args)
