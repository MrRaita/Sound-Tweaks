package dev.soundmod.nothing.core

import java.io.File

data class SystemSound(
    val path: String,
    val name: String,
    val category: String,
    val partition: String
)

object SoundScanner {
    /** Kök dizin okunamazsa kullanılan bilinen bölümler (Nothing OS: yalnızca /product dolu) */
    private val KNOWN = listOf(
        "system", "system_ext", "product", "odm", "vendor",
        "my_product", "my_region", "my_stock", "my_heytap"
    )

    /** Kök dizinde bulunmayacak, ses deposu olmayan girdiler */
    private val SKIP = setOf("proc", "sys", "dev", "data", "storage", "sdcard", "mnt", "acct", "config", "apex")

    /**
     * `/<bölüm>/media/audio` dizinlerini üreticiye göre tahmin etmeden keşfeder: kökteki her bölüm denenir (One UI,
     * HyperOS, ColorOS vb. ek bölümleri de kapsar). Aynı yere çözülen bağlantılar (ör. /system/product) tek sayılır.
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

    private val AUDIO_EXT = setOf("ogg", "wav", "mp3", "m4a", "flac", "opus", "aac")

    fun scan(): List<SystemSound> {
        val out = ArrayList<SystemSound>()
        for ((part, root) in roots()) {
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
