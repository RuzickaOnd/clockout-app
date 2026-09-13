package org.clockout.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.CustomSound
import org.clockout.domain.model.LanguageOption
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.RitualLogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "padla_preferences")

/**
 * Every Settings value, in DataStore.
 *
 * The store is a constructor parameter rather than a lookup on the context because
 * [preferencesDataStore] caches one instance for the whole process: tests that shared it would
 * also share the ritual counter and the review flag, and would then depend on each other's order.
 */
class RitualPreferences(private val dataStore: DataStore<Preferences>) : LegacyRitualLogSource {

    constructor(context: Context) : this(context.dataStore)

    // Legacy: ritual logs used to live here as a JSON blob. They now live in the Room
    // "ritual_logs" table; these keys only exist so RitualLogMigrator can import them once.
    private val KEY_LOGS = stringPreferencesKey("ritual_logs_json")
    private val KEY_LOGS_MIGRATED = booleanPreferencesKey("ritual_logs_migrated_to_room")
    private val KEY_NOTIF_HOUR = intPreferencesKey("notification_hour")
    private val KEY_NOTIF_MINUTE = intPreferencesKey("notification_minute")
    private val KEY_NOTIF_ENABLED = booleanPreferencesKey("notification_enabled")
    private val KEY_TEST_MODE = booleanPreferencesKey("test_mode_override")
    private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_ambient_enabled")
    private val KEY_SOUND_STYLE = stringPreferencesKey("sound_ambient_style")
    private val KEY_CUSTOM_SOUNDS = stringPreferencesKey("custom_sounds_json")
    private val KEY_LANGUAGE = stringPreferencesKey("app_language")
    private val KEY_INCLUDE_WEEKENDS = booleanPreferencesKey("include_weekends")
    private val KEY_COMPLETED_RITUALS = intPreferencesKey("ritual_completed_count")
    private val KEY_REVIEW_PROMPT_SHOWN = booleanPreferencesKey("review_prompt_shown")

    val notificationEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_NOTIF_ENABLED] ?: false
    }

    val languageOptionFlow: Flow<LanguageOption> = dataStore.data.map { preferences ->
        LanguageOption.fromCode(preferences[KEY_LANGUAGE])
    }

    val languageFlow: Flow<AppLanguage> = dataStore.data.map { preferences ->
        val option = LanguageOption.fromCode(preferences[KEY_LANGUAGE])
        option.resolveEffectiveLanguage()
    }

    private val legacyLogsFlow: Flow<List<RitualLogEntry>> = dataStore.data.map { preferences ->
        deserializeLogs(preferences[KEY_LOGS] ?: "[]")
    }

    override suspend fun readLegacyLogs(): List<RitualLogEntry> = legacyLogsFlow.first()

    override suspend fun isLogsMigratedToRoom(): Boolean =
        dataStore.data.map { it[KEY_LOGS_MIGRATED] ?: false }.first()

    override suspend fun markLogsMigratedToRoom() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_LOGS)
            preferences[KEY_LOGS_MIGRATED] = true
        }
    }

    val notificationHourFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_NOTIF_HOUR] ?: 17
    }

    val notificationMinuteFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_NOTIF_MINUTE] ?: 30
    }

    /** Whether Saturday and Sunday offer the ritual too; off means Monday to Friday only. */
    val includeWeekendsFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_INCLUDE_WEEKENDS] ?: false
    }

    val testModeFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_TEST_MODE] ?: true // default to true so users can experience it immediately at any time
    }

    val soundEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_SOUND_ENABLED] ?: true
    }

    /** Id of the selected sound: a built-in style id, or a [CustomSound] id. */
    val selectedSoundIdFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_SOUND_STYLE]
    }

    val customSoundsFlow: Flow<List<CustomSound>> = dataStore.data.map { preferences ->
        deserializeCustomSounds(preferences[KEY_CUSTOM_SOUNDS] ?: "[]")
    }

    /**
     * Counts every ritual the user has finished, for [org.clockout.domain.model.ReviewPromptPolicy].
     * It deliberately outlives "Clear data and history": that wipes the history, not the fact that
     * the app has already had its chance to ask for a review.
     */
    suspend fun incrementCompletedRitualCount(): Int {
        var updated = 0
        dataStore.edit { preferences ->
            updated = (preferences[KEY_COMPLETED_RITUALS] ?: 0) + 1
            preferences[KEY_COMPLETED_RITUALS] = updated
        }
        return updated
    }

    suspend fun getCompletedRitualCount(): Int =
        dataStore.data.map { it[KEY_COMPLETED_RITUALS] ?: 0 }.first()

    suspend fun isReviewPromptShown(): Boolean =
        dataStore.data.map { it[KEY_REVIEW_PROMPT_SHOWN] ?: false }.first()

    suspend fun markReviewPromptShown() {
        dataStore.edit { preferences ->
            preferences[KEY_REVIEW_PROMPT_SHOWN] = true
        }
    }

    suspend fun setNotificationTime(hour: Int, minute: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIF_HOUR] = hour
            preferences[KEY_NOTIF_MINUTE] = minute
        }
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIF_ENABLED] = enabled
        }
    }

    suspend fun setIncludeWeekends(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_INCLUDE_WEEKENDS] = enabled
        }
    }

    suspend fun setTestMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_TEST_MODE] = enabled
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SOUND_ENABLED] = enabled
        }
    }

    suspend fun setSelectedSoundId(id: String) {
        dataStore.edit { preferences ->
            preferences[KEY_SOUND_STYLE] = id
        }
    }

    suspend fun addCustomSound(sound: CustomSound) {
        dataStore.edit { preferences ->
            val current = deserializeCustomSounds(preferences[KEY_CUSTOM_SOUNDS] ?: "[]").toMutableList()
            current.removeAll { it.id == sound.id }
            current.add(0, sound)
            preferences[KEY_CUSTOM_SOUNDS] = serializeCustomSounds(current)
        }
    }

    suspend fun removeCustomSound(id: String) {
        dataStore.edit { preferences ->
            val current = deserializeCustomSounds(preferences[KEY_CUSTOM_SOUNDS] ?: "[]")
            preferences[KEY_CUSTOM_SOUNDS] = serializeCustomSounds(current.filterNot { it.id == id })
        }
    }

    suspend fun setLanguageOption(option: LanguageOption) {
        dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = option.code
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        setLanguageOption(
            when (language) {
                AppLanguage.ENGLISH -> LanguageOption.ENGLISH
                AppLanguage.CZECH -> LanguageOption.CZECH
            }
        )
    }

    private fun serializeCustomSounds(list: List<CustomSound>): String {
        val array = JSONArray()
        for (sound in list) {
            val obj = JSONObject()
            obj.put("id", sound.id)
            obj.put("displayName", sound.displayName)
            obj.put("fileName", sound.fileName)
            obj.put("durationMs", sound.durationMs)
            obj.put("addedAt", sound.addedAt)
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeCustomSounds(jsonString: String): List<CustomSound> {
        val list = mutableListOf<CustomSound>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id")
                val fileName = obj.optString("fileName")
                if (id.isEmpty() || fileName.isEmpty()) continue
                list.add(
                    CustomSound(
                        id = id,
                        displayName = obj.optString("displayName", fileName),
                        fileName = fileName,
                        durationMs = obj.optLong("durationMs", 0L),
                        addedAt = obj.optLong("addedAt", 0L)
                    )
                )
            }
        } catch (_: Exception) {
            // Return whatever parsed or empty
        }
        return list
    }

    private fun deserializeLogs(jsonString: String): List<RitualLogEntry> {
        val list = mutableListOf<RitualLogEntry>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val date = obj.getString("date")
                val questionText = obj.optString("questionText", "")
                val answer = obj.optString("selectedAnswer").takeIf { it.isNotEmpty() }
                val moodTagId = obj.optString("moodTag", MoodTag.CALM.id)
                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                list.add(
                    RitualLogEntry(
                        date = date,
                        questionText = questionText,
                        selectedAnswer = answer,
                        moodTag = MoodTag.fromId(moodTagId),
                        timestamp = timestamp
                    )
                )
            }
        } catch (_: Exception) {
            // Return whatever parsed or empty
        }
        return list
    }
}
