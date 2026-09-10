package org.clockout

import org.clockout.domain.model.CustomSound
import org.clockout.domain.model.MeditationSoundStyle
import org.clockout.domain.model.SoundChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomSoundTest {

    private fun sound(id: String = CustomSound.ID_PREFIX + "abc") = CustomSound(
        id = id,
        displayName = "Ocean night",
        fileName = "abc.mp3",
        durationMs = 154_000,
        addedAt = 0L
    )

    @Test
    fun testFormattedDuration() {
        assertEquals("2:34", sound().formattedDuration)
        assertEquals("0:00", sound().copy(durationMs = 0L).formattedDuration)
        assertEquals("0:09", sound().copy(durationMs = 9_400).formattedDuration)
    }

    @Test
    fun testCustomIdDetection() {
        assertTrue(CustomSound.isCustomId(CustomSound.ID_PREFIX + "abc"))
        assertFalse(CustomSound.isCustomId(MeditationSoundStyle.TIBETAN_BOWL.id))
        assertFalse(CustomSound.isCustomId(null))
    }

    @Test
    fun testFromIdResolvesBuiltInStyles() {
        assertEquals(
            SoundChoice.BuiltIn(MeditationSoundStyle.RAIN_PINK_NOISE),
            SoundChoice.fromId(MeditationSoundStyle.RAIN_PINK_NOISE.id, emptyList())
        )
        // Unknown and missing ids fall back to the default style
        assertEquals(SoundChoice.Default, SoundChoice.fromId(null, emptyList()))
        assertEquals(SoundChoice.Default, SoundChoice.fromId("nonsense", emptyList()))
    }

    @Test
    fun testFromIdResolvesCustomSound() {
        val custom = sound()
        assertEquals(SoundChoice.Custom(custom), SoundChoice.fromId(custom.id, listOf(custom)))
    }

    @Test
    fun testDeletedCustomSoundFallsBackToDefault() {
        // Selected id survived in preferences but its metadata is gone
        assertEquals(SoundChoice.Default, SoundChoice.fromId(CustomSound.ID_PREFIX + "gone", listOf(sound())))
    }
}
