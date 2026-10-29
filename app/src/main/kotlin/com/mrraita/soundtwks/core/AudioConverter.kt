package com.mrraita.soundtwks.core

import com.mrraita.soundtwks.R
import com.mrraita.soundtwks.txt

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Decodes any audio file (trimming and normalizing if requested) and writes a PCM16 WAV.
 *
 * The normalization metric is the same as ffmpeg `volumedetect`'s "mean_volume"
 * (RMS of the whole file, in dBFS). If the gain needed to reach the target pushes peaks over full scale,
 * a soft limiter is applied instead of hard clipping.
 */
object AudioConverter {
    data class Options(val normalize: Boolean, val targetDb: Int, val maxSeconds: Int = 6)

    data class Stats(
        val beforeDb: Double,
        val afterDb: Double,
        val peakDb: Double,
        val gainDb: Double,
        val limited: Boolean
    ) {
        fun summary(normalized: Boolean): String {
            if (!normalized) return txt(R.string.conv_level_off, fmt(beforeDb))
            val change = if (abs(gainDb) < 0.15) txt(R.string.conv_unchanged) else "${fmt(beforeDb)} → ${fmt(afterDb)} dBFS"
            return txt(R.string.conv_level, change, fmt(peakDb)) + if (limited) txt(R.string.conv_peaks_softened) else ""
        }

        private fun fmt(v: Double) = String.format(java.util.Locale.US, "%.1f", v)
    }

    private const val LIMIT_START = 0.72
    private const val LIMIT_CEIL = 0.97

    private const val DECODE_TIMEOUT_MS = 30_000L

    fun toWav(ctx: Context, uri: Uri, out: File, opt: Options): Stats {
        val ex = MediaExtractor()
        var codec: MediaCodec? = null
        val pcm = ByteArrayOutputStream()
        var sampleRate = 0
        var channels = 0
        var maxBytes = 0L
        try {
            ex.setDataSource(ctx, uri, null)
            var track = -1
            var fmt: MediaFormat? = null
            for (i in 0 until ex.trackCount) {
                val f = ex.getTrackFormat(i)
                if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    track = i
                    fmt = f
                    break
                }
            }
            val format = fmt ?: throw IllegalArgumentException(txt(R.string.err_no_audio_track))
            ex.selectTrack(track)

            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            maxBytes = sampleRate.toLong() * channels * 2L * opt.maxSeconds
            val dec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            codec = dec
            dec.configure(format, null, null, 0)
            dec.start()

            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            val deadline = android.os.SystemClock.elapsedRealtime() + DECODE_TIMEOUT_MS
            while (!outputDone) {
                // A broken file or decoder that never reports end-of-stream must not hang the app
                if (android.os.SystemClock.elapsedRealtime() > deadline) {
                    throw IllegalStateException(txt(R.string.err_decode_timeout))
                }
                if (!inputDone) {
                    val idx = dec.dequeueInputBuffer(10_000)
                    if (idx >= 0) {
                        val buf = dec.getInputBuffer(idx)!!
                        val n = ex.readSampleData(buf, 0)
                        if (n < 0) {
                            dec.queueInputBuffer(idx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            dec.queueInputBuffer(idx, 0, n, ex.sampleTime, 0)
                            ex.advance()
                        }
                    }
                }
                val oi = dec.dequeueOutputBuffer(info, 10_000)
                if (oi >= 0) {
                    val ob = dec.getOutputBuffer(oi)!!
                    val chunk = ByteArray(info.size)
                    ob.position(info.offset)
                    ob.get(chunk)
                    pcm.write(chunk)
                    dec.releaseOutputBuffer(oi, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    if (pcm.size() >= maxBytes) outputDone = true
                } else if (oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val nf = dec.outputFormat
                    sampleRate = nf.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = nf.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    maxBytes = sampleRate.toLong() * channels * 2L * opt.maxSeconds
                }
            }
        } finally {
            codec?.let {
                runCatching { it.stop() }
                runCatching { it.release() }
            }
            runCatching { ex.release() }
        }

        val raw = pcm.toByteArray()
        val usable = minOf(raw.size.toLong(), maxBytes).toInt() and 1.inv()
        val samples = ShortArray(usable / 2)
        ByteBuffer.wrap(raw, 0, usable).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples)
        require(samples.isNotEmpty()) { txt(R.string.err_decoded_empty) }

        val before = meanDb(samples)
        var gainDb = 0.0
        var limited = false
        if (opt.normalize && before > -90.0) {
            gainDb = (opt.targetDb - before).coerceIn(-30.0, 24.0)
            val gain = 10.0.pow(gainDb / 20.0)
            for (i in samples.indices) {
                var x = samples[i] / 32768.0 * gain
                val a = abs(x)
                if (a > LIMIT_START) {
                    limited = true
                    val y = LIMIT_START + (LIMIT_CEIL - LIMIT_START) * tanh((a - LIMIT_START) / (LIMIT_CEIL - LIMIT_START))
                    x = if (x < 0) -y else y
                }
                samples[i] = (x * 32767.0).roundToInt().coerceIn(-32768, 32767).toShort()
            }
        }
        val stats = Stats(before, meanDb(samples), peakDb(samples), gainDb, limited)
        writeWav(out, samples, sampleRate, channels)
        return stats
    }

    /** Writes a short silent WAV, used to mute a sound. */
    fun writeSilence(out: File, ms: Int = 80) {
        writeWav(out, ShortArray(44100 * ms / 1000), 44100, 1)
    }

    private fun meanDb(s: ShortArray): Double {
        var sumSq = 0.0
        for (v in s) {
            val d = v / 32768.0
            sumSq += d * d
        }
        val rms = sqrt(sumSq / s.size)
        return if (rms < 1e-6) -120.0 else 20.0 * log10(rms)
    }

    private fun peakDb(s: ShortArray): Double {
        var p = 0
        for (v in s) if (abs(v.toInt()) > p) p = abs(v.toInt())
        return if (p == 0) -120.0 else 20.0 * log10(p / 32768.0)
    }

    private fun writeWav(f: File, s: ShortArray, rate: Int, ch: Int) {
        f.parentFile?.mkdirs()
        if (f.exists()) f.delete()
        val dataLen = s.size * 2
        val bb = ByteBuffer.allocate(44 + dataLen).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray()); bb.putInt(36 + dataLen); bb.put("WAVE".toByteArray())
        bb.put("fmt ".toByteArray()); bb.putInt(16); bb.putShort(1); bb.putShort(ch.toShort())
        bb.putInt(rate); bb.putInt(rate * ch * 2); bb.putShort((ch * 2).toShort()); bb.putShort(16)
        bb.put("data".toByteArray()); bb.putInt(dataLen)
        for (v in s) bb.putShort(v)
        RandomAccessFile(f, "rw").use { it.write(bb.array()) }
    }
}
