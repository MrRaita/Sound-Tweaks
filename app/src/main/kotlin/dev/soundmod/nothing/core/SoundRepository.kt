package dev.soundmod.nothing.core

import android.content.Context
import android.net.Uri
import java.io.File

/** Sonuç bilgisi: seviye ölçümü (varsa) */
data class ApplyInfo(val stats: AudioConverter.Stats?, val normalized: Boolean) {
    fun summary(): String = stats?.summary(normalized) ?: ""
}

/** Root ile ses dosyalarını dağıtır ve Settings.Global yollarını yönetir. */
class SoundRepository(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("soundmod", Context.MODE_PRIVATE)
    private val srcDir = File(ctx.filesDir, "src")
    private val previewDir = File(ctx.filesDir, "preview")

    fun label(slot: SoundSlot): String? = prefs.getString("label_${slot.id}", null)

    private fun setLabel(slot: SoundSlot, v: String?) {
        prefs.edit().apply { if (v == null) remove("label_${slot.id}") else putString("label_${slot.id}", v) }.apply()
    }

    /** Uygulamanın okuyabildiği, önizleme için çalınacak dosya (özel/seçilmiş ses) */
    fun previewPath(slot: SoundSlot): String? =
        prefs.getString("prev_${slot.id}", null)?.takeIf { File(it).isFile }

    /** Yeniden işlenebilen kaynak var mı */
    fun hasSource(slot: SoundSlot): Boolean =
        label(slot) != null && prefs.getString("srcpath_${slot.id}", null)?.let { File(it).isFile } == true

    /** Ses seçilmemişken çalınacak stok dosya */
    fun stockPath(slot: SoundSlot, library: List<SystemSound>): String? = when (slot.type) {
        SlotType.HOOK_FILE ->
            library.firstOrNull { it.name.equals(slot.key, ignoreCase = true) }?.path
        SlotType.SETTINGS_GLOBAL -> {
            val orig = prefs.getString("orig_${slot.key}", null)
                ?: Root.run("settings get global ${slot.key}").takeIf { it.ok }?.out?.trim()
            orig?.takeIf { it.startsWith("/") && File(it).isFile }
        }
    }

    // ---- Depo ----

    /** Depo (dizinler, sahiplik, SELinux bağlamı) probe sırasında bir kez hazırlanır. */
    @Volatile private var storeReady = false

    fun probe(): Probe {
        val p = RootProbe.run()
        if (p.granted) storeReady = true
        return p
    }

    private fun ensureStore(): Root.Result {
        val b = Root.q(SoundPaths.BASE)
        val r = Root.run(
            "mkdir -p ${Root.q(SoundPaths.OVERRIDE_DIR)} ${Root.q(SoundPaths.FILES_DIR)} && " +
                "chown -R system:system $b && chmod -R 755 $b && " +
                "(chcon -R ${SoundPaths.CONTEXT} $b || restorecon -R $b)"
        )
        if (r.ok) storeReady = true
        return r
    }

    /** Kilit sesleri "zorla çal" modu (bayrak dosyası). Varsayılan kapalı = stok mekanizma. */
    fun setForceLock(on: Boolean): Root.Result {
        val f = Root.q(SoundPaths.FORCE_FLAG)
        return Root.run(
            if (on) "touch $f && (chcon ${SoundPaths.CONTEXT} $f || true)" else "rm -f $f"
        )
    }

    /** Eski sürümlerden kalan, artık geçersiz yuva kayıtlarını (root gerektirmeden) unutur. */
    fun forget(slot: SoundSlot) {
        prefs.edit().remove("label_${slot.id}").remove("prev_${slot.id}").remove("srcpath_${slot.id}").apply()
        File(srcDir, slot.id).delete()
        File(previewDir, "${slot.id}.wav").delete()
    }

    /** Ayrıntılı olay kaydı: bayrak dosyası varken SystemUI hook'u son olayını yazar (varsayılan kapalı). */
    fun setDebug(on: Boolean): Root.Result {
        val f = Root.q(SoundPaths.DEBUG_FLAG)
        return Root.run(
            if (on) "touch $f && (chcon ${SoundPaths.CONTEXT} $f || true)"
            else "rm -f $f; settings delete global ${SoundPaths.SYSUI_LAST_KEY}"
        )
    }

    fun restartSystemUi() {
        Root.run("pkill -f com.android.systemui || killall com.android.systemui")
    }

    // ---- Uygulama ----

    fun applySystem(slot: SoundSlot, sound: SystemSound, opt: AudioConverter.Options): Result<ApplyInfo> =
        applyFrom(slot, File(sound.path), "Sistem: ${sound.name}", opt)

    fun applyCustom(slot: SoundSlot, uri: Uri, displayName: String, opt: AudioConverter.Options): Result<ApplyInfo> {
        return try {
            srcDir.mkdirs()
            val copy = File(srcDir, slot.id)
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                copy.outputStream().use { input.copyTo(it) }
            } ?: return Result.failure(IllegalStateException("Dosya açılamadı"))
            applyFrom(slot, copy, "Dosya: $displayName", opt)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Kayıtlı kaynağı güncel normalize ayarıyla yeniden uygular */
    fun reprocess(slot: SoundSlot, opt: AudioConverter.Options): Result<ApplyInfo> {
        val label = label(slot) ?: return Result.failure(IllegalStateException("Özel ses yok"))
        val src = prefs.getString("srcpath_${slot.id}", null)?.let { File(it) }
        if (src == null || !src.isFile) return Result.failure(IllegalStateException("Kaynak dosya yok"))
        return applyFrom(slot, src, label, opt)
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
        prefs.edit().remove("prev_${slot.id}").remove("srcpath_${slot.id}").apply()
        File(srcDir, slot.id).delete()
        File(previewDir, "${slot.id}.wav").delete()
        return Result.success(Unit)
    }

    // ---- İç ----

    /**
     * Kaynak dosyayı (normalize açıksa dönüştürüp) depoya koyar.
     * Normalize kapalıysa dosya olduğu gibi kopyalanır; Settings yuvalarında sistem dosyaları
     * kopyalanmadan doğrudan yolla gösterilir.
     */
    private fun applyFrom(slot: SoundSlot, src: File, label: String, opt: AudioConverter.Options): Result<ApplyInfo> {
        return try {
            if (opt.normalize) {
                previewDir.mkdirs()
                val wav = File(previewDir, "${slot.id}.wav")
                val stats = AudioConverter.toWav(ctx, Uri.fromFile(src), wav, opt)
                val r = deploy(slot, wav, "wav", direct = false)
                if (r.isFailure) return Result.failure(r.exceptionOrNull()!!)
                remember(slot, label, wav.absolutePath, src.absolutePath)
                Result.success(ApplyInfo(stats, true))
            } else {
                val ext = src.extension.lowercase().ifBlank { "ogg" }
                val isSystem = src.absolutePath.startsWith("/system") || src.absolutePath.startsWith("/product") ||
                    src.absolutePath.startsWith("/vendor") || src.absolutePath.startsWith("/odm") ||
                    src.absolutePath.startsWith("/my_")
                val r = deploy(slot, src, ext, direct = isSystem && slot.type == SlotType.SETTINGS_GLOBAL && !slot.sysUiReload)
                if (r.isFailure) return Result.failure(r.exceptionOrNull()!!)
                remember(slot, label, src.absolutePath, src.absolutePath)
                Result.success(ApplyInfo(null, false))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun remember(slot: SoundSlot, label: String, previewPath: String, srcPath: String) {
        setLabel(slot, label)
        prefs.edit().putString("prev_${slot.id}", previewPath).putString("srcpath_${slot.id}", srcPath).apply()
    }

    private fun deploy(slot: SoundSlot, src: File, ext: String, direct: Boolean): Result<Unit> {
        if (!storeReady) {
            val st = ensureStore()
            if (!st.ok) return Result.failure(IllegalStateException("Depo hazırlanamadı: ${st.out}"))
        }

        val fix = { dest: String ->
            "chown system:system ${Root.q(dest)} && chmod 644 ${Root.q(dest)} && " +
                "(chcon ${SoundPaths.CONTEXT} ${Root.q(dest)} || restorecon ${Root.q(dest)})"
        }

        val r = when (slot.type) {
            SlotType.HOOK_FILE -> {
                val dest = "${SoundPaths.OVERRIDE_DIR}/${slot.key}.$ext"
                Root.run(
                    "rm -f ${Root.q(SoundPaths.OVERRIDE_DIR)}/${slot.key}.*; " +
                        "cp ${Root.q(src.absolutePath)} ${Root.q(dest)} && ${fix(dest)}"
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
                            "cp ${Root.q(src.absolutePath)} ${Root.q(dest)} && ${fix(dest)} && " +
                            "settings put global ${slot.key} ${Root.q(dest)}"
                    )
                }
            }
        }
        if (!r.ok) return Result.failure(IllegalStateException(r.out.ifBlank { "Komut başarısız (${r.code})" }))
        return Result.success(Unit)
    }

    private fun rememberOriginal(key: String) {
        val k = "orig_$key"
        if (prefs.contains(k)) return
        val v = Root.run("settings get global $key")
        if (v.ok) prefs.edit().putString(k, v.out.trim()).apply()
    }
}
