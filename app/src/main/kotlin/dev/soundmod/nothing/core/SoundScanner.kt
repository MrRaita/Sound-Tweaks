package dev.soundmod.nothing.core

import java.io.File

data class SystemSound(
    val path: String,
    val name: String,
    val category: String,
    val partition: String
)

object SoundScanner {
    /** Bilinen sistem ses dizinleri (Nothing OS: yalnızca /product dolu; diğerleri başka ROM'lar için) */
    private val ROOTS = listOf(
        "/system", "/system_ext", "/product", "/odm", "/vendor",
        "/my_product", "/my_region", "/my_stock", "/my_heytap"
    ).map { it to "$it/media/audio" }

    private val AUDIO_EXT = setOf("ogg", "wav", "mp3", "m4a", "flac", "opus", "aac")

    fun scan(): List<SystemSound> {
        val out = ArrayList<SystemSound>()
        for ((part, dir) in ROOTS) {
            val root = File(dir)
            if (!root.isDirectory) continue
            root.walkTopDown().maxDepth(3).filter { it.isFile }.forEach { f ->
                if (f.extension.lowercase() in AUDIO_EXT) {
                    out += SystemSound(f.absolutePath, f.nameWithoutExtension, categoryOf(f), part)
                }
            }
        }
        return out.sortedWith(compareBy({ it.category }, { it.name.lowercase() }))
    }

    private fun categoryOf(f: File): String = when (f.parentFile?.name?.lowercase()) {
        "ui" -> "UI"
        "ringtones" -> "Zil sesleri"
        "notifications" -> "Bildirimler"
        "alarms" -> "Alarmlar"
        else -> "Diğer"
    }
}
