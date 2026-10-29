package com.mrraita.soundtwks.core

import com.mrraita.soundtwks.R
import com.mrraita.soundtwks.txt

import android.content.Context
import android.net.Uri
import java.io.File

/** Result info: level measurement (if any) */
data class ApplyInfo(val stats: AudioConverter.Stats?, val normalized: Boolean) {
    fun summary(): String = stats?.summary(normalized) ?: ""
}

/** Deploys sound files through root and manages Settings.Global paths. */
class SoundRepository(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("soundtweaks", Context.MODE_PRIVATE)
    private val srcDir = File(ctx.filesDir, "src")
    private val previewDir = File(ctx.filesDir, "preview")

    fun label(slot: SoundSlot): String? = prefs.getString("label_${slot.id}", null)

    private fun setLabel(slot: SoundSlot, v: String?) {
        prefs.edit().apply { if (v == null) remove("label_${slot.id}") else putString("label_${slot.id}", v) }.apply()
    }

    /** File the app can read and play for previews (the custom/selected sound) */
    fun previewPath(slot: SoundSlot): String? =
        prefs.getString("prev_${slot.id}", null)?.takeIf { File(it).isFile }

    /** Whether a source exists that can be reprocessed */
    fun hasSource(slot: SoundSlot): Boolean =
        label(slot) != null && prefs.getString("srcpath_${slot.id}", null)?.let { File(it).isFile } == true

    /** Stock file to play when no sound is selected */
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

    /** The store (directories, ownership, SELinux context) is prepared once during the probe. */
    @Volatile private var storeReady = false

    fun probe(): Probe {
        val p = RootProbe.run()
        // Root alone does not mean the store is usable; only a successful prepare chain marks it ready
        storeReady = p.granted && p.storeOk
        return p
    }

    private fun ensureStore(): Root.Result {
        val b = Root.q(SoundPaths.BASE)
        val r = Root.run(
            "mkdir -p ${Root.q(SoundPaths.OVERRIDE_DIR)} ${Root.q(SoundPaths.FILES_DIR)} && chown -R system:system $b && ${SoundPaths.storePermsCmd(SoundPaths.BASE)} && (chcon -R ${SoundPaths.CONTEXT} $b || restorecon -R $b)"
        )
        if (r.ok) storeReady = true
        return r
    }

    /** "Force play" mode for lock sounds (flag file). Off by default = stock mechanism. */
    fun setForceLock(on: Boolean): Root.Result {
        val f = Root.q(SoundPaths.FORCE_FLAG)
        return Root.run(
            if (on) "touch $f && (chcon ${SoundPaths.CONTEXT} $f || true)" else "rm -f $f"
        )
    }

    /**
     * Turns the headphone volume warning off/on: writes a tiny script under /data/adb/soundtweaks that sets only three
     * system properties at boot and is called from post-fs-data.d. No hook is installed. When enabled the properties
     * are also set immediately; when disabled the script is deleted and stock behavior returns on the next boot.
     */
    fun setNoHpWarning(on: Boolean): Root.Result {
        if (!on) return Root.run("rm -f ${Root.q(SoundPaths.HP_LAUNCHER)} ${Root.q(SoundPaths.HP_SCRIPT)}; rmdir ${Root.q(SoundPaths.HP_DIR)} 2>/dev/null; true")
        val script = "#!/system/bin/sh\n# Sound Tweaks: disables the headphone high-volume warning (runs at boot)\nP=setprop\ncommand -v resetprop >/dev/null 2>&1 && P=resetprop\n\$P audio.safemedia.bypass true\n\$P audio.safemedia.force false\n\$P audio.safemedia.csd.force false\n"
        val launcher = "#!/system/bin/sh\n[ -f ${SoundPaths.HP_SCRIPT} ] && sh ${SoundPaths.HP_SCRIPT}\n"
        return Root.run(
            "mkdir -p ${Root.q(SoundPaths.HP_DIR)} /data/adb/post-fs-data.d && printf '%b' ${Root.q(script)} > ${Root.q(SoundPaths.HP_SCRIPT)} && printf '%b' ${Root.q(launcher)} > ${Root.q(SoundPaths.HP_LAUNCHER)} && chmod 755 ${Root.q(SoundPaths.HP_SCRIPT)} ${Root.q(SoundPaths.HP_LAUNCHER)} && sh ${Root.q(SoundPaths.HP_SCRIPT)}"
        )
    }

    /** Forgets stale slot records left over from older versions (no root needed). */
    fun forget(slot: SoundSlot) {
        prefs.edit().remove("label_${slot.id}").remove("prev_${slot.id}").remove("srcpath_${slot.id}").apply()
        File(srcDir, slot.id).delete()
        File(previewDir, "${slot.id}.wav").delete()
    }

    /** Verbose event log: while the flag file exists the SystemUI hook writes its last event (off by default). */
    fun setDebug(on: Boolean): Root.Result {
        val f = Root.q(SoundPaths.DEBUG_FLAG)
        return Root.run(
            if (on) "touch $f && (chcon ${SoundPaths.CONTEXT} $f || true)"
            else "rm -f $f; settings delete global ${SoundPaths.SYSUI_LAST_KEY}"
        )
    }

    /** Restarts SystemUI by exact process name (never a substring match) and reports whether it worked. */
    fun restartSystemUi(): Root.Result =
        Root.run("pids=\$(pidof com.android.systemui) && [ -n \"\$pids\" ] && kill \$pids")

    // ---- Applying ----

    fun applySystem(slot: SoundSlot, sound: SystemSound, opt: AudioConverter.Options): Result<ApplyInfo> =
        applyFrom(slot, File(sound.path), "sys:${sound.name}", opt)

    fun applyCustom(slot: SoundSlot, uri: Uri, displayName: String, opt: AudioConverter.Options): Result<ApplyInfo> {
        return try {
            srcDir.mkdirs()
            clearSourceCopies(slot)
            // Keep the picked file's real extension so it is never redistributed under a wrong one
            val ext = extensionOf(uri, displayName)
            val copy = File(srcDir, "${slot.id}.$ext")
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                copy.outputStream().use { input.copyTo(it) }
            } ?: return Result.failure(IllegalStateException(txt(R.string.err_open_file)))
            applyFrom(slot, copy, "file:$displayName", opt)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Mutes the selected sound (installs a short silent file). */
    fun applySilence(slot: SoundSlot): Result<ApplyInfo> {
        return try {
            previewDir.mkdirs()
            val wav = File(previewDir, "${slot.id}.wav")
            AudioConverter.writeSilence(wav)
            val r = deploy(slot, wav, "wav", direct = false)
            if (r.isFailure) return Result.failure(r.exceptionOrNull()!!)
            remember(slot, "silent", wav.absolutePath, wav.absolutePath)
            Result.success(ApplyInfo(null, false))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Re-applies the saved source with the current normalization settings */
    fun reprocess(slot: SoundSlot, opt: AudioConverter.Options): Result<ApplyInfo> {
        val label = label(slot) ?: return Result.failure(IllegalStateException(txt(R.string.err_no_custom)))
        val src = prefs.getString("srcpath_${slot.id}", null)?.let { File(it) }
        if (src == null || !src.isFile) return Result.failure(IllegalStateException(txt(R.string.err_source_missing)))
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
                Root.run("$c && rm -f ${Root.q(SoundPaths.FILES_DIR)}/${slot.key}.*")
            }
        }
        if (!r.ok) return Result.failure(IllegalStateException(r.out))
        setLabel(slot, null)
        prefs.edit().remove("prev_${slot.id}").remove("srcpath_${slot.id}").apply()
        clearSourceCopies(slot)
        File(previewDir, "${slot.id}.wav").delete()
        return Result.success(Unit)
    }

    // ---- Internal ----

    /**
     * Puts the source file into the store (converted first if normalization is on).
     * With normalization off the file is copied as is; for Settings slots, system files are
     * referenced directly by path without copying.
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

    /** Removes the app's private copies of a slot's source file (any extension). */
    private fun clearSourceCopies(slot: SoundSlot) {
        srcDir.listFiles()?.filter { it.name == slot.id || it.name.startsWith("${slot.id}.") }?.forEach { it.delete() }
    }

    /** Extension from the display name, else from the MIME type; unknown types fall back to "ogg". */
    private fun extensionOf(uri: Uri, displayName: String): String {
        val fromName = displayName.substringAfterLast('.', "").lowercase()
        if (fromName in SoundPaths.EXTENSIONS) return fromName
        val mime = ctx.contentResolver.getType(uri)?.lowercase().orEmpty()
        return when {
            "wav" in mime -> "wav"
            "mpeg" in mime || "mp3" in mime -> "mp3"
            "mp4" in mime || "m4a" in mime -> "m4a"
            "flac" in mime -> "flac"
            "opus" in mime -> "opus"
            "aac" in mime -> "aac"
            else -> "ogg"
        }
    }

    private fun remember(slot: SoundSlot, label: String, previewPath: String, srcPath: String) {
        setLabel(slot, label)
        prefs.edit().putString("prev_${slot.id}", previewPath).putString("srcpath_${slot.id}", srcPath).apply()
    }

    private fun deploy(slot: SoundSlot, src: File, ext: String, direct: Boolean): Result<Unit> {
        if (!storeReady) {
            val st = ensureStore()
            if (!st.ok) return Result.failure(IllegalStateException(txt(R.string.err_prepare_store, st.out)))
        }

        val fix = { dest: String ->
            "chown system:system ${Root.q(dest)} && chmod 644 ${Root.q(dest)} && (chcon ${SoundPaths.CONTEXT} ${Root.q(dest)} || restorecon ${Root.q(dest)})"
        }

        val r = when (slot.type) {
            SlotType.HOOK_FILE -> {
                val dest = "${SoundPaths.OVERRIDE_DIR}/${slot.key}.$ext"
                Root.run(
                    "rm -f ${Root.q(SoundPaths.OVERRIDE_DIR)}/${slot.key}.*; cp ${Root.q(src.absolutePath)} ${Root.q(dest)} && ${fix(dest)}"
                )
            }
            SlotType.SETTINGS_GLOBAL -> {
                rememberOriginal(slot.key)
                if (direct) {
                    Root.run("settings put global ${slot.key} ${Root.q(src.absolutePath)}")
                } else {
                    val dest = "${SoundPaths.FILES_DIR}/${slot.key}.$ext"
                    Root.run(
                        "rm -f ${Root.q(SoundPaths.FILES_DIR)}/${slot.key}.*; cp ${Root.q(src.absolutePath)} ${Root.q(dest)} && ${fix(dest)} && settings put global ${slot.key} ${Root.q(dest)}"
                    )
                }
            }
        }
        if (!r.ok) return Result.failure(IllegalStateException(r.out.ifBlank { txt(R.string.err_command_failed, r.code) }))
        return Result.success(Unit)
    }

    private fun rememberOriginal(key: String) {
        val k = "orig_$key"
        if (prefs.contains(k)) return
        val v = Root.run("settings get global $key")
        if (v.ok) prefs.edit().putString(k, v.out.trim()).apply()
    }
}
