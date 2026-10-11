package dev.soundmod.nothing.core

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
import kotlin.math.sqrt

/** Herhangi bir ses dosyasını çözüp (gerekirse kırpıp/normalize edip) PCM16 WAV yazar. */
object AudioConverter {
    data class Options(val normalize: Boolean, val targetDb: Int, val maxSeconds: Int = 6)

    fun toWav(ctx: Context, uri: Uri, out: File, opt: Options) {
        val ex = MediaExtractor()
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
        val format = fmt ?: run {
            ex.release()
            throw IllegalArgumentException("Dosyada ses kanalı bulunamadı")
        }
        ex.selectTrack(track)

        var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()

        val info = MediaCodec.BufferInfo()
        val pcm = ByteArrayOutputStream()
        var inputDone = false
        var outputDone = false
        var maxBytes = sampleRate.toLong() * channels * 2L * opt.maxSeconds
        try {
            while (!outputDone) {
                if (!inputDone) {
                    val idx = codec.dequeueInputBuffer(10_000)
                    if (idx >= 0) {
                        val buf = codec.getInputBuffer(idx)!!
                        val n = ex.readSampleData(buf, 0)
                        if (n < 0) {
                            codec.queueInputBuffer(idx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(idx, 0, n, ex.sampleTime, 0)
                            ex.advance()
                        }
                    }
                }
                val oi = codec.dequeueOutputBuffer(info, 10_000)
                if (oi >= 0) {
                    val ob = codec.getOutputBuffer(oi)!!
                    val chunk = ByteArray(info.size)
                    ob.position(info.offset)
                    ob.get(chunk)
                    pcm.write(chunk)
                    codec.releaseOutputBuffer(oi, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    if (pcm.size() >= maxBytes) outputDone = true
                } else if (oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val nf = codec.outputFormat
                    sampleRate = nf.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = nf.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    maxBytes = sampleRate.toLong() * channels * 2L * opt.maxSeconds
                }
            }
        } finally {
            runCatching { codec.stop() }
            codec.release()
            ex.release()
        }

        val raw = pcm.toByteArray()
        val usable = minOf(raw.size.toLong(), maxBytes).toInt() and 1.inv()
        val samples = ShortArray(usable / 2)
        ByteBuffer.wrap(raw, 0, usable).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples)

        if (opt.normalize && samples.isNotEmpty()) applyNormalization(samples, opt.targetDb)
        writeWav(out, samples, sampleRate, channels)
    }

    private fun applyNormalization(s: ShortArray, targetDb: Int) {
        var sumSq = 0.0
        var peak = 0
        for (v in s) {
            val d = v.toDouble() / 32768.0
            sumSq += d * d
            if (abs(v.toInt()) > peak) peak = abs(v.toInt())
        }
        val rms = sqrt(sumSq / s.size)
        if (rms < 1e-6 || peak == 0) return
        val rmsDb = 20.0 * log10(rms)
        var gain = 10.0.pow((targetDb - rmsDb) / 20.0)
        // Tepe 0.95'i geçmesin
        val maxGain = 0.95 * 32768.0 / peak
        if (gain > maxGain) gain = maxGain
        for (i in s.indices) s[i] = (s[i] * gain).toInt().coerceIn(-32768, 32767).toShort()
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
