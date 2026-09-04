package com.example.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.domain.model.MeditationSoundStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class AmbientAudioPlayer {

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private var previewJob: Job? = null
    @Volatile
    private var isPlaying = false
    @Volatile
    private var currentStyle: MeditationSoundStyle = MeditationSoundStyle.TIBETAN_BOWL
    @Volatile
    private var currentVolume = 0f

    // Active bowl strike states
    private val strikeLock = Any()
    private var lastStrikeSampleIndex = -1000000L
    private var strikeFundamental = 280.0
    private var strikeGain = 0.7f

    private val scope = CoroutineScope(Dispatchers.Default)
    private val random = Random()

    fun startAmbient(style: MeditationSoundStyle, enabled: Boolean = true) {
        if (!enabled || style == MeditationSoundStyle.SILENT_HAPTIC) {
            stopAmbient()
            return
        }
        stopAmbient()

        currentStyle = style
        isPlaying = true
        currentVolume = 0f

        playbackJob = scope.launch {
            runAudioLoop(style, previewDurationSeconds = null)
        }
    }

    fun triggerBowlStrike(highPitch: Boolean = false) {
        synchronized(strikeLock) {
            strikeFundamental = if (highPitch) 350.0 else 280.0
            strikeGain = 0.65f
            lastStrikeSampleIndex = currentSampleIndex
        }
    }

    fun playPreview(style: MeditationSoundStyle, onFinished: (() -> Unit)? = null) {
        stopAmbient()
        if (style == MeditationSoundStyle.SILENT_HAPTIC) {
            onFinished?.invoke()
            return
        }

        currentStyle = style
        isPlaying = true
        currentVolume = 0f

        previewJob = scope.launch {
            runAudioLoop(style, previewDurationSeconds = 4)
            onFinished?.invoke()
        }
    }

    fun stopAmbient() {
        isPlaying = false
        previewJob?.cancel()
        previewJob = null
        playbackJob?.cancel()
        playbackJob = null

        scope.launch {
            try {
                audioTrack?.pause()
                audioTrack?.flush()
                audioTrack?.stop()
                audioTrack?.release()
            } catch (_: Exception) {}
            audioTrack = null
        }
    }

    @Volatile
    private var currentSampleIndex = 0L

    private suspend fun runAudioLoop(
        style: MeditationSoundStyle,
        previewDurationSeconds: Int?
    ) {
        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(sampleRate / 4)

        val track = try {
            AudioTrack.Builder()
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
        } catch (e: Exception) {
            return
        }

        audioTrack = track
        try {
            track.play()
        } catch (e: Exception) {
            track.release()
            return
        }

        currentSampleIndex = 0L
        synchronized(strikeLock) {
            lastStrikeSampleIndex = 0L
            strikeFundamental = 280.0
            strikeGain = 0.75f
        }

        val buffer = ShortArray(1024)
        val fadeInSamples = (sampleRate * 0.8).toLong()
        val totalPreviewSamples = previewDurationSeconds?.let { (it * sampleRate).toLong() }

        // Pink noise filter state registers
        var b0 = 0.0
        var b1 = 0.0
        var b2 = 0.0
        var b3 = 0.0
        var lowPassFilter = 0.0

        // Raindrop transient state
        var dropSamplesRemaining = 0
        var dropFreq = 2000.0
        var dropAmp = 0.0
        var dropPhase = 0.0

        while (scope.isActive && isPlaying) {
            if (totalPreviewSamples != null && currentSampleIndex >= totalPreviewSamples) {
                break
            }

            for (i in buffer.indices) {
                val t = currentSampleIndex.toDouble() / sampleRate
                val sample: Double = when (style) {
                    MeditationSoundStyle.TIBETAN_BOWL -> {
                        // Periodic soft chime every 8 seconds (breath cycle) plus initial/manual strikes
                        val cycleTime = t % 8.0
                        if (cycleTime < 0.001 && currentSampleIndex > sampleRate) {
                            synchronized(strikeLock) {
                                lastStrikeSampleIndex = currentSampleIndex
                                strikeFundamental = if ((t / 8.0).toInt() % 2 == 0) 280.0 else 350.0
                                strikeGain = 0.55f
                            }
                        }

                        // Synthesize Tibetan singing bowl harmonics:
                        // Fundamental + detuned overtone for acoustic beating + non-integer harmonics
                        val strikeElapsedSec = ((currentSampleIndex - lastStrikeSampleIndex).toDouble() / sampleRate)
                            .coerceAtLeast(0.0)

                        val f0 = strikeFundamental
                        val fBeat = f0 + 1.6 // creates organic 1.6 Hz beating vibration
                        val f2 = f0 * 2.76   // resonant metallic overtone
                        val f3 = f0 * 4.95   // higher chime shimmer

                        val decay1 = exp(-strikeElapsedSec / 3.8)
                        val decay2 = exp(-strikeElapsedSec / 2.2)
                        val decay3 = exp(-strikeElapsedSec / 1.1)

                        val strikeSignal = if (strikeElapsedSec < 7.0) {
                            val attack = (strikeElapsedSec / 0.012).coerceAtMost(1.0)
                            val comp1 = sin(2.0 * PI * f0 * strikeElapsedSec) * 0.45 * decay1
                            val compBeat = sin(2.0 * PI * fBeat * strikeElapsedSec) * 0.35 * decay1
                            val comp2 = sin(2.0 * PI * f2 * strikeElapsedSec) * 0.20 * decay2
                            val comp3 = sin(2.0 * PI * f3 * strikeElapsedSec) * 0.08 * decay3
                            (comp1 + compBeat + comp2 + comp3) * attack * strikeGain
                        } else {
                            0.0
                        }

                        // Very gentle warm ambient sub-hum (low-volume meditation drone)
                        val drone = sin(2.0 * PI * 140.0 * t) * 0.04
                        strikeSignal + drone
                    }

                    MeditationSoundStyle.RAIN_PINK_NOISE -> {
                        // Paul Kellet's Pink Noise algorithm
                        val white = random.nextDouble() * 2.0 - 1.0
                        b0 = 0.99886 * b0 + white * 0.0555179
                        b1 = 0.99332 * b1 + white * 0.0750759
                        b2 = 0.96900 * b2 + white * 0.1538520
                        b3 = 0.86650 * b3 + white * 0.3104856
                        val pink = b0 + b1 + b2 + b3 + white * 0.5362

                        // Soft warm low-pass filter (~450Hz cutoff)
                        lowPassFilter += 0.065 * (pink - lowPassFilter)

                        // Subtle natural raindrop patter (occasional soft click/ping)
                        if (dropSamplesRemaining <= 0) {
                            if (random.nextDouble() < 0.0004) {
                                dropSamplesRemaining = (sampleRate * 0.018).toInt()
                                dropFreq = 1600.0 + random.nextDouble() * 1200.0
                                dropAmp = 0.08 + random.nextDouble() * 0.08
                                dropPhase = 0.0
                            }
                        }

                        var dropSignal = 0.0
                        if (dropSamplesRemaining > 0) {
                            dropPhase += 2.0 * PI * dropFreq / sampleRate
                            val env = dropSamplesRemaining.toDouble() / (sampleRate * 0.018)
                            dropSignal = sin(dropPhase) * dropAmp * env * env
                            dropSamplesRemaining--
                        }

                        lowPassFilter * 0.26 + dropSignal
                    }

                    MeditationSoundStyle.BREATHING_WAVES -> {
                        // Pink noise base
                        val white = random.nextDouble() * 2.0 - 1.0
                        b0 = 0.99886 * b0 + white * 0.0555179
                        b1 = 0.99332 * b1 + white * 0.0750759
                        b2 = 0.96900 * b2 + white * 0.1538520
                        b3 = 0.86650 * b3 + white * 0.3104856
                        val pink = b0 + b1 + b2 + b3 + white * 0.5362

                        // 8-second wave: 4 seconds inhale (wave swells up), 4 seconds exhale (wave recedes)
                        // Phase 0..1 over 8s
                        val cycle = (t % 8.0) / 8.0
                        val breathEnvelope = 0.5 - 0.5 * kotlin.math.cos(2.0 * PI * cycle) // smooth swell 0..1

                        // Modulate filter cutoff with breath cycle (180Hz to 650Hz)
                        val filterCoeff = 0.02 + 0.09 * breathEnvelope
                        lowPassFilter += filterCoeff * (pink - lowPassFilter)

                        // Gentle ocean swell volume modulation
                        val waveVolume = 0.12 + 0.28 * breathEnvelope
                        lowPassFilter * waveVolume
                    }

                    MeditationSoundStyle.SILENT_HAPTIC -> 0.0
                }

                // Envelope calculation (Fade in / Preview fade out)
                val envelope: Double = if (currentSampleIndex < fadeInSamples) {
                    currentSampleIndex.toDouble() / fadeInSamples
                } else if (totalPreviewSamples != null && currentSampleIndex > totalPreviewSamples - fadeInSamples) {
                    ((totalPreviewSamples - currentSampleIndex).toDouble() / fadeInSamples).coerceAtLeast(0.0)
                } else {
                    1.0
                }

                val finalVal = (sample * envelope * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

                buffer[i] = finalVal.toShort()
                currentSampleIndex++
            }

            try {
                track.write(buffer, 0, buffer.size)
            } catch (e: Exception) {
                break
            }
        }

        try {
            track.stop()
            track.release()
        } catch (_: Exception) {}
        if (audioTrack === track) {
            audioTrack = null
        }
    }
}
