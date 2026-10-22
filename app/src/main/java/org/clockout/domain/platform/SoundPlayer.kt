package org.clockout.domain.platform

import org.clockout.domain.model.SoundChoice

interface SoundPlayer {
    fun startAmbient(choice: SoundChoice)
    fun triggerBowlStrike(highPitch: Boolean)
    fun playPreview(choice: SoundChoice, onFinished: () -> Unit)
    fun stopAmbient()
}
