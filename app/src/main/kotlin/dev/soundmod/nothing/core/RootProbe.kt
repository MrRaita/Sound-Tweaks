package dev.soundmod.nothing.core

import dev.soundmod.nothing.BuildConfig

/** Short info about the root manager and the system (the Root card in Settings) */
data class RootInfo(
    val manager: String = "",
    val managerVersion: String = "",
    val suVersion: String = "",
    val kernel: String = "",
    val kernelMode: String = "",
    val selinux: String = "",
    val context: String = ""
)

/** Everything collected with a single `su` call at startup */
data class Probe(
    val granted: Boolean,
    val info: RootInfo,
    val hookStatus: String?,
    val debugLog: Boolean,
    val forceLock: Boolean,
    val noHpWarning: Boolean,
    /** Root granted AND the sound store (dirs, owner, mode, SELinux context) was prepared successfully */
    val storeOk: Boolean = false
)

/**
 * Collects the root queries at app startup in a single `su` process (spawning a process per query
 * would be slow and wasteful). The same call also prepares the sound store.
 */
object RootProbe {

    fun run(): Probe {
        val b = Root.q(SoundPaths.BASE)
        val script = """
            echo "uid=${'$'}(id -u)"
            if [ "${'$'}(id -u)" = "0" ]; then
              ${if (BuildConfig.VERBOSE_LOG) "" else "rm -f " + Root.q(SoundPaths.DEBUG_FLAG) + "; settings delete global " + SoundPaths.SYSUI_LAST_KEY + " >/dev/null 2>&1"}
              rm -f ${Root.q(SoundPaths.BASE + "/.status_system")} ${Root.q(SoundPaths.OLD_HP_FLAG)}
              rm -f ${Root.q(SoundPaths.OVERRIDE_DIR)}/lock.* ${Root.q(SoundPaths.OVERRIDE_DIR)}/unlock.* ${Root.q(SoundPaths.OVERRIDE_DIR)}/trusted.*
              mkdir -p ${Root.q(SoundPaths.OVERRIDE_DIR)} ${Root.q(SoundPaths.FILES_DIR)} && chown -R system:system $b && ${SoundPaths.storePermsCmd(SoundPaths.BASE)} && (chcon -R ${SoundPaths.CONTEXT} $b || restorecon -R $b) && echo "store=ok"
            fi
            echo "ctx=${'$'}(id -Z 2>/dev/null)"
            echo "kernel=${'$'}(uname -r)"
            echo "selinux=${'$'}(getenforce 2>/dev/null)"
            echo "ksud=${'$'}(ksud --version 2>/dev/null || /data/adb/ksud --version 2>/dev/null)"
            echo "magisk=${'$'}(magisk -v 2>/dev/null)"
            echo "magiskc=${'$'}(magisk -V 2>/dev/null)"
            echo "apd=${'$'}(/data/adb/ap/bin/apd --version 2>/dev/null)"
            echo "suv=${'$'}(su -v 2>/dev/null)"
            echo "lkm=${'$'}(grep -c -i kernelsu /proc/modules 2>/dev/null)"
            echo "debug=${'$'}([ -e ${Root.q(SoundPaths.DEBUG_FLAG)} ] && echo yes)"
            echo "force=${'$'}([ -e ${Root.q(SoundPaths.FORCE_FLAG)} ] && echo yes)"
            echo "nohp=${'$'}([ -f ${Root.q(SoundPaths.HP_SCRIPT)} ] && [ -f ${Root.q(SoundPaths.HP_LAUNCHER)} ] && echo yes)"
            echo "sys=${'$'}(cat ${SoundPaths.STATUS_SYSTEM} 2>/dev/null)"
            echo "sysui=${'$'}(settings get global ${SoundPaths.SYSUI_STATUS_KEY} 2>/dev/null)"
        """.trimIndent()

        val r = Root.run(script)
        val kv = HashMap<String, String>()
        for (line in r.out.lines()) {
            val i = line.indexOf('=')
            if (i > 0) kv[line.substring(0, i)] = line.substring(i + 1).trim()
        }
        val granted = kv["uid"] == "0"
        if (!granted) return Probe(false, RootInfo(), null, false, false, false)

        val ksud = kv["ksud"].orEmpty()
        val magisk = kv["magisk"].orEmpty()
        val apd = kv["apd"].orEmpty()
        val manager: String
        val version: String
        when {
            ksud.isNotBlank() -> { manager = "KernelSU"; version = ksud }
            magisk.isNotBlank() -> { manager = "Magisk"; version = (magisk + " " + kv["magiskc"].orEmpty()).trim() }
            apd.isNotBlank() -> { manager = "APatch"; version = apd }
            else -> { manager = "Unknown"; version = "" }
        }
        val lkm = (kv["lkm"]?.toIntOrNull() ?: 0) > 0
        val mode = when {
            manager == "KernelSU" && lkm -> "LKM (loadable kernel module)"
            manager == "KernelSU" -> "Built into the kernel (GKI / custom kernel)"
            else -> ""
        }
        val info = RootInfo(
            manager = manager,
            managerVersion = version,
            suVersion = kv["suv"].orEmpty(),
            kernel = kv["kernel"].orEmpty(),
            kernelMode = mode,
            selinux = kv["selinux"].orEmpty(),
            context = kv["ctx"].orEmpty()
        )
        val hook = listOfNotNull(
            kv["sys"]?.takeIf { it.isNotBlank() },
            kv["sysui"]?.takeIf { it.isNotBlank() && it != "null" }
        ).takeIf { it.isNotEmpty() }?.joinToString("\n")
        return Probe(true, info, hook, kv["debug"] == "yes", kv["force"] == "yes", kv["nohp"] == "yes", kv["store"] == "ok")
    }
}
