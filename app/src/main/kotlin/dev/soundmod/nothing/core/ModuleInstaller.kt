package dev.soundmod.nothing.core

import android.content.Context
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** assets/ksu-module içeriğini zip'leyip root yöneticisine (KernelSU/Magisk/APatch) kurdurur. */
object ModuleInstaller {
    private const val MODULE_ID = "soundmod-selinux"
    private val FILES = listOf("module.prop", "service.sh")

    fun isInstalled(): Boolean = Root.run("[ -d /data/adb/modules/$MODULE_ID ] && echo yes").out.contains("yes")

    fun buildZip(ctx: Context): File {
        val zip = File(ctx.cacheDir, "SoundMod-SELinux.zip")
        if (zip.exists()) zip.delete()
        ZipOutputStream(zip.outputStream().buffered()).use { z ->
            for (name in FILES) {
                z.putNextEntry(ZipEntry(name))
                ctx.assets.open("ksu-module/$name").use { it.copyTo(z) }
                z.closeEntry()
            }
        }
        return zip
    }

    /** @return (başarılı mı, çıktı) */
    fun install(ctx: Context): Pair<Boolean, String> {
        val zip = buildZip(ctx)
        val p = Root.q(zip.absolutePath)
        // Kopyayı elle kurmak isteyenler için İndirilenler'e de bırak
        Root.run("cp $p /sdcard/Download/SoundMod-SELinux.zip")
        val attempts = listOf(
            "ksud module install $p",
            "/data/adb/ksud module install $p",
            "magisk --install-module $p",
            "/data/adb/ap/bin/apd module install $p"
        )
        var last = ""
        for (c in attempts) {
            val r = Root.run(c)
            last = r.out
            if (r.ok) return true to r.out
        }
        return false to last
    }

    fun reboot() {
        Root.run("svc power reboot || reboot")
    }
}
