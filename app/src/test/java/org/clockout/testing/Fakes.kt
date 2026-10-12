package org.clockout.testing

import org.clockout.domain.model.CustomSound
import org.clockout.domain.model.SoundChoice
import org.clockout.domain.model.SoundImportResult
import org.clockout.domain.platform.ReminderScheduler
import org.clockout.domain.platform.SoundFileStore
import org.clockout.domain.platform.SoundPlayer

class FakeReminderScheduler : ReminderScheduler {
    val scheduled = mutableListOf<Pair<Int, Int>>()
    var cancelCount = 0
    override fun schedule(hour: Int, minute: Int) { scheduled += hour to minute }
    override fun cancel() { cancelCount++ }
}

class FakeSoundPlayer : SoundPlayer {
    var ambient: SoundChoice? = null
    var stopCount = 0
    override fun startAmbient(choice: SoundChoice) { ambient = choice }
    override fun triggerBowlStrike(highPitch: Boolean) = Unit
    override fun playPreview(choice: SoundChoice, onFinished: () -> Unit) { ambient = choice }
    override fun stopAmbient() { ambient = null; stopCount++ }
}

class FakeSoundFileStore(
    var onImport: (existingCount: Int) -> SoundImportResult
) : SoundFileStore {
    val deleted = mutableListOf<CustomSound>()
    override suspend fun import(source: String, existingCount: Int) = onImport(existingCount)
    override suspend fun delete(sound: CustomSound) { deleted += sound }
}
