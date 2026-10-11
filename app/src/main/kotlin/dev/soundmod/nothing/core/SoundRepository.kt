package dev.soundmod.nothing.core

import android.content.Context
import android.net.Uri
import java.io.File

/** Root ile ses dosyalarını dağıtır ve Settings.Global yollarını yönetir. */
class SoundRepository(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("soundmod", Context.MODE_PRIVATE)

    fun label(slot: SoundSlot): String? = prefs.getString("label_${slot.id}", null)
    private fun setLabel(slot: SoundSlot, v: String?) =
        prefs.edit().apply { if (v == null) remove("label_${slot.id}") else putString("label_${slot.id}", v) }.apply()

    fun ensureStore(): Root.Result {
        val b = Root.q(SoundPaths.BASE)
        return Root.run(
            "mkdir -p ${Root.q(SoundPaths.OVERRIDE_DIR)} ${Root.q(SoundPaths.FILES_DIR)} && " +
                "chown -R system:system $b && chmod -R 755 $b && restorecon -R $b"
        )
    }

    fun readStatus(): String? {
        val a = Root.run("cat ${SoundPaths.STATUS_SYSTEM}")
        val b = Root.run("cat ${SoundPaths.STATUS_SYSUI}")
        val parts = listOfNotNull(
            a.takeIf { it.ok && it.out.isNotBlank() }?.out,
            b.takeIf { it.ok && it.out.isNotBlank() }?.out
        )
        return parts.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }

    // ---- Uygulama ----

    fun applySystem(slot: SoundSlot, sound: SystemSound): Result<Unit> {
        val ext = sound.path.substringAfterLast('.', "ogg").lowercase()
        return deploy(slot, File(sound.path), ext, "Sistem: ${sound.name}", direct = slot.type == SlotType.SETTINGS_GLOBAL)
    }

    fun applyCustom(slot: SoundSlot, uri: Uri, displayName: String, opt: AudioConverter.Options): Result<Unit> {
        val tmp = File(ctx.cacheDir, "${slot.key}.wav")
        return try {
            AudioConverter.toWav(ctx, uri, tmp, opt)
            deploy(slot, tmp, "wav", "Dosya: $displayName", direct = false)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            tmp.delete()
        }
    }

    fun reset(slot: SoundSlot): Result<Unit> {
        val r = when (slot.type) {
            SlotType.HOOK_FILE -> Root.run("rm -f ${Root.q(SoundPaths.OVERRIDE_DIR)}/${slot.key}.*")
            SlotType.SETTINGS_GLOBAL -> {
                val orig = prefs.getString("orig_${slot.key}", null)
                val c = if (orig == null || orig == "null" || orig.isBlank())
                    "settings delete global ${slot.key}"
                else "settings put global ${slot.key} ${Root.q(orig)}"
                Root.run("$c; rm -f ${Root.q(SoundPaths.FILES_DIR)}/${slot.key}.*")
            }
        }
        if (!r.ok) return Result.failure(IllegalStateException(r.out))
        setLabel(slot, null)
        return Result.success(Unit)
    }

    /**
     * @param direct true ise kaynak dosyayı kopyalamadan yolunu doğrudan Settings'e yazar
     *               (yalnızca sistem dosyaları için; herkesin okuyabildiği yollar)
     */
    private fun deploy(slot: SoundSlot, src: File, ext: String, label: String, direct: Boolean): Result<Unit> {
        val st = ensureStore()
        if (!st.ok) return Result.failure(IllegalStateException("Depo hazırlanamadı: ${st.out}"))

        val r = when (slot.type) {
            SlotType.HOOK_FILE -> {
                val dest = "${SoundPaths.OVERRIDE_DIR}/${slot.key}.$ext"
                Root.run(
                    "rm -f ${Root.q(SoundPaths.OVERRIDE_DIR)}/${slot.key}.*; " +
                        "cp ${Root.q(src.absolutePath)} ${Root.q(dest)} && chown system:system ${Root.q(dest)} && " +
                        "chmod 644 ${Root.q(dest)} && restorecon ${Root.q(dest)}"
                )
            }
            SlotType.SETTINGS_GLOBAL -> {
                rememberOriginal(slot.key)
                if (direct) {
                    Root.run("settings put global ${slot.key} ${Root.q(src.absolutePath)}")
                } else {
                    val dest = "${SoundPaths.FILES_DIR}/${slot.key}.$ext"
                    Root.run(
                        "rm -f ${Root.q(SoundPaths.FILES_DIR)}/${slot.key}.*; " +
                            "cp ${Root.q(src.absolutePath)} ${Root.q(dest)} && chown system:system ${Root.q(dest)} && " +
                            "chmod 644 ${Root.q(dest)} && restorecon ${Root.q(dest)} && " +
                            "settings put global ${slot.key} ${Root.q(dest)}"
                    )
                }
            }
        }
        if (!r.ok) return Result.failure(IllegalStateException(r.out.ifBlank { "Komut başarısız (${r.code})" }))
        setLabel(slot, label)
        return Result.success(Unit)
    }

    private fun rememberOriginal(key: String) {
        val k = "orig_$key"
        if (prefs.contains(k)) return
        val v = Root.run("settings get global $key")
        if (v.ok) prefs.edit().putString(k, v.out.trim()).apply()
    }
}
