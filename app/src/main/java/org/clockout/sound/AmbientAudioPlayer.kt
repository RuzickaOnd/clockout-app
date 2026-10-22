package org.clockout.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import org.clockout.data.local.CustomSoundStorage
import org.clockout.domain.model.CustomSound
import org.clockout.domain.model.MeditationSoundStyle
import org.clockout.domain.model.SoundChoice
import org.clockout.domain.platform.SoundPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class AmbientAudioPlayer(context: Context) : SoundPlayer {

    private val appContext = context.applicationContext

    private var audioTrack: AudioTrack? = null
    private var mediaPlayer: MediaPlayer? = null
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

    override fun startAmbient(choice: SoundChoice) {
        when (choice) {
            is SoundChoice.Custom -> {
                stopAmbient()
                playbackJob = scope.launch {
                    playCustomSound(
                        context = appContext,
                        sound = choice.sound,
                        loop = true,
                        previewDurationSeconds = null,
                        onFinished = null
                    )
                }
            }

            is SoundChoice.BuiltIn -> {
                val style = choice.style
                if (style == MeditationSoundStyle.SILENT_HAPTIC) {
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
        }
    }

    override fun triggerBowlStrike(highPitch: Boolean) {
        synchronized(strikeLock) {
            strikeFundamental = if (highPitch) 350.0 else 280.0
            strikeGain = 0.65f
            lastStrikeSampleIndex = currentSampleIndex
        }
    }

    override fun playPreview(choice: SoundChoice, onFinished: () -> Unit) {
        stopAmbient()
        when (choice) {
            is SoundChoice.Custom -> {
                previewJob = scope.launch {
                    playCustomSound(
                        context = appContext,
                        sound = choice.sound,
                        loop = false,
                        previewDurationSeconds = CUSTOM_PREVIEW_SECONDS,
                        onFinished = onFinished
                    )
                }
            }

            is SoundChoice.BuiltIn -> {
                val style = choice.style
                if (style == MeditationSoundStyle.SILENT_HAPTIC) {
                    onFinished()
                    return
                }

                currentStyle = style
                isPlaying = true
                currentVolume = 0f

                previewJob = scope.launch {
                    runAudioLoop(style, previewDurationSeconds = 4)
                    onFinished()
                }
            }
        }
    }

    override fun stopAmbient() {
        isPlaying = false
        previewJob?.cancel()
        previewJob = null
        playbackJob?.cancel()
        playbackJob = null
        releaseMediaPlayer()

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

    /**
     * Plays a user imported file through [MediaPlayer]. Volume is faded in (and,
     * for previews, back out) so the file matches the calm feel of the synthesized styles.
     */
    private suspend fun playCustomSound(
        context: Context,
        sound: CustomSound,
        loop: Boolean,
        previewDurationSeconds: Int?,
        onFinished: (() -> Unit)?
    ) {
        val file = CustomSoundStorage.resolveFile(context, sound.fileName)
        if (!file.exists()) {
            onFinished?.invoke()
            return
        }

        val player = try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(file.absolutePath)
                isLooping = loop
                setVolume(0f, 0f)
                prepare()
            }
        } catch (_: Exception) {
            onFinished?.invoke()
            return
        }

        if (!coroutineContext.isActive) {
            try {
                player.release()
            } catch (_: Exception) {
            }
            return
        }

        mediaPlayer = player
        isPlaying = true

        try {
            player.start()
        } catch (_: Exception) {
            releaseMediaPlayer()
            onFinished?.invoke()
            return
        }

        try {
            fadeVolume(player, from = 0f, to = 1f, durationMs = FADE_MS)

            if (previewDurationSeconds == null && loop) {
                // Looping ambient playback: keep running until cancelled.
                while (coroutineContext.isActive) {
                    delay(500)
                }
            } else {
                val playbackMs = if (previewDurationSeconds != null) {
                    minOf(previewDurationSeconds * 1000L, sound.durationMs.coerceAtLeast(FADE_MS * 2))
                } else {
                    sound.durationMs
                }
                val beforeFadeOutMs = playbackMs - FADE_MS
                if (beforeFadeOutMs > 0) {
                    delay(beforeFadeOutMs)
                }
                fadeVolume(player, from = 1f, to = 0f, durationMs = FADE_MS)
            }
        } finally {
            if (mediaPlayer === player) {
                releaseMediaPlayer()
            } else {
                try {
                    player.release()
                } catch (_: Exception) {
                }
            }
        }
        onFinished?.invoke()
    }

    private suspend fun fadeVolume(player: MediaPlayer, from: Float, to: Float, durationMs: Long) {
        val steps = 24
        val stepDelay = durationMs / steps
        for (step in 1..steps) {
            if (!coroutineContext.isActive) return
            val volume = from + (to - from) * (step.toFloat() / steps)
            try {
                player.setVolume(volume, volume)
            } catch (_: Exception) {
                return
            }
            delay(stepDelay)
        }
    }

    private fun releaseMediaPlayer() {
        val player = mediaPlayer ?: return
        mediaPlayer = null
        try {
            if (player.isPlaying) {
                player.stop()
            }
        } catch (_: Exception) {
        }
        try {
            player.release()
        } catch (_: Exception) {
        }
    }

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

    companion object {
        private const val FADE_MS = 800L
        private const val CUSTOM_PREVIEW_SECONDS = 8
    }
}
