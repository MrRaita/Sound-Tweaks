package com.mrraita.soundtwks.core

/** Minimal root runner: `su -c <command>` with a timeout and an output cap. */
object Root {
    data class Result(val code: Int, val out: String) {
        val ok: Boolean get() = code == 0
    }

    private const val MAX_OUTPUT = 256 * 1024
    private const val DEFAULT_TIMEOUT_SEC = 60L

    /** Absolute path of the `su` binary: PATH and known locations are searched, with a default as fallback. */
    private val suPath: String by lazy {
        val dirs = (System.getenv("PATH") ?: "").split(':').filter { it.isNotBlank() } +
            listOf("/system/bin", "/system/xbin", "/sbin", "/debug_ramdisk", "/data/adb/ksu/bin", "/data/adb/ap/bin")
        dirs.map { java.io.File(it, "su") }.firstOrNull { it.isFile && it.canExecute() }?.path ?: "/system/bin/su"
    }

    fun run(cmd: String, timeoutSec: Long = DEFAULT_TIMEOUT_SEC): Result {
        return try {
            val p = ProcessBuilder(suPath, "-c", cmd).redirectErrorStream(true).start()
            val buf = StringBuilder()
            // Drain output on a separate thread so a chatty command can never block the timeout; the cap bounds memory.
            val reader = Thread {
                runCatching {
                    p.inputStream.bufferedReader().use { r ->
                        val chunk = CharArray(4096)
                        while (true) {
                            val n = r.read(chunk)
                            if (n < 0) break
                            val room = MAX_OUTPUT - buf.length
                            if (room > 0) buf.append(chunk, 0, minOf(n, room))
                        }
                    }
                }
            }.apply { isDaemon = true; start() }
            if (!p.waitFor(timeoutSec, java.util.concurrent.TimeUnit.SECONDS)) {
                p.destroyForcibly()
                reader.join(1000)
                return Result(-2, "Command timed out after ${timeoutSec}s")
            }
            reader.join(2000)
            Result(p.exitValue(), buf.toString().trim())
        } catch (e: Exception) {
            Result(-1, e.message ?: "Could not run su")
        }
    }

    /** Single-quote escaping for the shell. */
    fun q(s: String): String = "'" + s.replace("'", "'\\''") + "'"
}
