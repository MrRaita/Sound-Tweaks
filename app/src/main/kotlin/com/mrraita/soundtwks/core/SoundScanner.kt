package com.mrraita.soundtwks.core

import androidx.annotation.StringRes
import com.mrraita.soundtwks.R
import java.io.File

data class SystemSound(
    val path: String,
    val name: String,
    /** String resource id of the category name */
    @StringRes val category: Int,
    val partition: String
)

object SoundScanner {
    /** Known partitions used when the root directory cannot be listed (Nothing OS: only /product is populated) */
    private val KNOWN = listOf(
        "system", "system_ext", "product", "odm", "vendor",
        "my_product", "my_region", "my_stock", "my_heytap"
    )

    /** Root entries that are never sound stores */
    private val SKIP = setOf("proc", "sys", "dev", "data", "storage", "sdcard", "mnt", "acct", "config", "apex")

    /**
     * Discovers `/<partition>/media/audio` directories without guessing per vendor: every partition at the root is
     * tried (this also covers extra partitions on One UI, HyperOS, ColorOS, etc.). Links resolving to the same place (e.g. /system/product) count once.
     */
    private fun roots(): List<Pair<String, File>> {
        val names = ((File("/").list()?.toList() ?: emptyList()) + KNOWN).distinct().filter { it !in SKIP }.sorted()
        val seen = HashSet<String>()
        val out = ArrayList<Pair<String, File>>()
        for (n in names) {
            val dir = File("/$n/media/audio")
            if (!dir.isDirectory) continue
            val canonical = runCatching { dir.canonicalPath }.getOrDefault(dir.path)
            if (seen.add(canonical)) out += "/$n" to dir
        }
        return out
    }

    private val AUDIO_EXT = SoundPaths.EXTENSIONS.toSet()

    fun scan(): List<SystemSound> {
        val out = ArrayList<SystemSound>()
        for ((part, root) in roots()) {
            root.walkTopDown().maxDepth(3).filter { it.isFile }.forEach { f ->
                if (f.extension.lowercase() in AUDIO_EXT) {
                    out += SystemSound(f.absolutePath, f.nameWithoutExtension, categoryOf(f), part)
                }
            }
        }
        return out.sortedWith(compareBy({ CATEGORY_ORDER.indexOf(it.category) }, { it.name.lowercase() }))
    }

    private val CATEGORY_ORDER = listOf(
        R.string.cat_alarms, R.string.cat_notifications, R.string.cat_ringtones, R.string.cat_ui, R.string.cat_other
    )

    private fun categoryOf(f: File): Int = when (f.parentFile?.name?.lowercase()) {
        "ui" -> R.string.cat_ui
        "ringtones" -> R.string.cat_ringtones
        "notifications" -> R.string.cat_notifications
        "alarms" -> R.string.cat_alarms
        else -> R.string.cat_other
    }
}
