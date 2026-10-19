package dev.soundmod.nothing.core

/** Çok basit root çalıştırıcı: `su -c <komut>` */
object Root {
    data class Result(val code: Int, val out: String) {
        val ok: Boolean get() = code == 0
    }

    /** `su` ikilisinin mutlak yolu: PATH ve bilinen konumlar taranır, bulunamazsa varsayılan konum kullanılır. */
    private val suPath: String by lazy {
        val dirs = (System.getenv("PATH") ?: "").split(':').filter { it.isNotBlank() } +
            listOf("/system/bin", "/system/xbin", "/sbin", "/debug_ramdisk", "/data/adb/ksu/bin", "/data/adb/ap/bin")
        dirs.map { java.io.File(it, "su") }.firstOrNull { it.isFile && it.canExecute() }?.path ?: "/system/bin/su"
    }

    fun run(cmd: String): Result {
        return try {
            val p = ProcessBuilder(suPath, "-c", cmd).redirectErrorStream(true).start()
            val out = p.inputStream.bufferedReader().readText()
            Result(p.waitFor(), out.trim())
        } catch (e: Exception) {
            Result(-1, e.message ?: "su çalıştırılamadı")
        }
    }

    fun available(): Boolean = run("id -u").let { it.ok && it.out.lines().last().trim() == "0" }

    /** Kabuk için tek tırnak kaçışı */
    fun q(s: String): String = "'" + s.replace("'", "'\\''") + "'"
}
