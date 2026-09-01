package com.vcapp.voicechanger.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder

/**
 * Decodes any audio file Android can read (MP3, M4A, AAC, OGG, WAV, FLAC...)
 * into a mono float PCM array resampled to the engine sample rate, so clips
 * can be mixed sample-by-sample into the live call stream.
 */
object AudioDecoder {

    private const val MAX_SECONDS = 300 // safety cap: 5 minutes per clip

    fun decodeToMonoFloat(context: Context, uri: Uri, targetSampleRate: Int): FloatArray? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)

            var trackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    trackIndex = i
                    format = f
                    break
                }
            }
            if (trackIndex < 0 || format == null) return null

            extractor.selectTrack(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)!!
            val srcRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val out = ArrayList<FloatArray>()
            var totalSamples = 0
            val maxSamples = MAX_SECONDS * srcRate
            val info = MediaCodec.BufferInfo()
            var sawInputEos = false
            var sawOutputEos = false

            while (!sawOutputEos && totalSamples < maxSamples) {
                if (!sawInputEos) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val inBuf = codec.getInputBuffer(inIndex)!!
                        val size = extractor.readSampleData(inBuf, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(
                                inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            sawInputEos = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(info, 10_000)
                if (outIndex >= 0) {
                    if (info.size > 0) {
                        val buf = codec.getOutputBuffer(outIndex)!!
                        buf.position(info.offset)
                        buf.limit(info.offset + info.size)
                        val shorts = buf.order(ByteOrder.nativeOrder()).asShortBuffer()
                        val frames = shorts.remaining() / channels
                        val chunk = FloatArray(frames)
                        for (i in 0 until frames) {
                            var acc = 0f
                            for (c in 0 until channels) acc += shorts.get() / 32768f
                            chunk[i] = acc / channels
                        }
                        out.add(chunk)
                        totalSamples += frames
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) sawOutputEos = true
                }
            }

            if (totalSamples == 0) return null
            val mono = FloatArray(totalSamples)
            var pos = 0
            for (chunk in out) {
                System.arraycopy(chunk, 0, mono, pos, chunk.size)
                pos += chunk.size
            }
            return if (srcRate == targetSampleRate) mono else resample(mono, srcRate, targetSampleRate)
        } catch (t: Throwable) {
            return null
        } finally {
            try { codec?.stop() } catch (_: Throwable) {}
            try { codec?.release() } catch (_: Throwable) {}
            try { extractor.release() } catch (_: Throwable) {}
        }
    }

    private fun resample(input: FloatArray, from: Int, to: Int): FloatArray {
        val ratio = to.toDouble() / from.toDouble()
        val outLen = (input.size * ratio).toInt().coerceAtLeast(1)
        val out = FloatArray(outLen)
        for (i in 0 until outLen) {
            val srcPos = i / ratio
            val idx = srcPos.toInt()
            val frac = (srcPos - idx).toFloat()
            val s0 = input[idx.coerceIn(0, input.size - 1)]
            val s1 = input[(idx + 1).coerceIn(0, input.size - 1)]
            out[i] = s0 + (s1 - s0) * frac
        }
        return out
    }
}
