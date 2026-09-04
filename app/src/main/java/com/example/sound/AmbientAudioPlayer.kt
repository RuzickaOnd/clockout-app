package com.example.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class AmbientAudioPlayer {

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private var isPlaying = false
    private var currentVolume = 0f
    private val scope = CoroutineScope(Dispatchers.Default)

    fun startAmbient(enabled: Boolean = true) {
        if (!enabled) return
        stopAmbient()

        isPlaying = true
        currentVolume = 0f

        playbackJob = scope.launch {
            val sampleRate = 44100
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(sampleRate / 4)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            track.play()

            // Frequencies: 216 Hz (warm root), 432 Hz (calm fundamental), 648 Hz (warm fifth)
            val baseFreq1 = 216.0
            val baseFreq2 = 432.0
            val baseFreq3 = 648.0
            var sampleIndex = 0L

            val buffer = ShortArray(1024)

            // Fade in over 1.5 seconds (about 66150 samples)
            val fadeInSamples = (sampleRate * 1.5).toLong()

            while (isActive && isPlaying) {
                for (i in buffer.indices) {
                    val t = sampleIndex.toDouble() / sampleRate
                    // Very slow ambient undulating modulation (8 second breathing wave)
                    val breathingLfo = 0.85 + 0.15 * sin(2.0 * PI * t / 8.0)

                    val sample1 = sin(2.0 * PI * baseFreq1 * t) * 0.4
                    val sample2 = sin(2.0 * PI * baseFreq2 * t) * 0.35
                    val sample3 = sin(2.0 * PI * baseFreq3 * t) * 0.15

                    val mixed = (sample1 + sample2 + sample3) * breathingLfo

                    // Fade in calculation
                    val envelope = if (sampleIndex < fadeInSamples) {
                        sampleIndex.toFloat() / fadeInSamples
                    } else {
                        currentVolume
                    }
                    val finalSample = (mixed * envelope * 0.28 * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

                    buffer[i] = finalSample.toShort()
                    sampleIndex++
                }

                if (sampleIndex >= fadeInSamples) {
                    currentVolume = 1f
                }

                track.write(buffer, 0, buffer.size)
            }

            try {
                track.stop()
                track.release()
            } catch (_: Exception) {}
        }
    }

    fun stopAmbient() {
        if (!isPlaying) return
        isPlaying = false
        scope.launch {
            // Smooth fade out
            var vol = currentVolume
            while (vol > 0.05f) {
                vol -= 0.15f
                currentVolume = vol
                delay(80)
            }
            playbackJob?.cancel()
            playbackJob = null
            try {
                audioTrack?.stop()
                audioTrack?.release()
                audioTrack = null
            } catch (_: Exception) {}
        }
    }
}
