package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppLanguage
import com.example.domain.model.CustomSound
import com.example.domain.model.LanguageOption
import com.example.domain.model.MoodTag
import com.example.domain.model.RitualLogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "padla_preferences")

class RitualPreferences(private val context: Context) {

    private val KEY_LOGS = stringPreferencesKey("ritual_logs_json")
    private val KEY_NOTIF_HOUR = intPreferencesKey("notification_hour")
    private val KEY_NOTIF_MINUTE = intPreferencesKey("notification_minute")
    private val KEY_NOTIF_ENABLED = booleanPreferencesKey("notification_enabled")
    private val KEY_TEST_MODE = booleanPreferencesKey("test_mode_override")
    private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_ambient_enabled")
    private val KEY_SOUND_STYLE = stringPreferencesKey("sound_ambient_style")
    private val KEY_CUSTOM_SOUNDS = stringPreferencesKey("custom_sounds_json")
    private val KEY_LANGUAGE = stringPreferencesKey("app_language")

    val notificationEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIF_ENABLED] ?: false
    }

    val languageOptionFlow: Flow<LanguageOption> = context.dataStore.data.map { preferences ->
        LanguageOption.fromCode(preferences[KEY_LANGUAGE])
    }

    val languageFlow: Flow<AppLanguage> = context.dataStore.data.map { preferences ->
        val option = LanguageOption.fromCode(preferences[KEY_LANGUAGE])
        option.resolveEffectiveLanguage()
    }

    val logsFlow: Flow<List<RitualLogEntry>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[KEY_LOGS] ?: "[]"
        deserializeLogs(jsonString)
    }

    val notificationHourFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIF_HOUR] ?: 17
    }

    val notificationMinuteFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIF_MINUTE] ?: 30
    }

    val testModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_TEST_MODE] ?: true // default to true so users can experience it immediately at any time
    }

    val soundEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SOUND_ENABLED] ?: true
    }

    /** Id of the selected sound: a built-in style id, or a [CustomSound] id. */
    val selectedSoundIdFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_SOUND_STYLE]
    }

    val customSoundsFlow: Flow<List<CustomSound>> = context.dataStore.data.map { preferences ->
        deserializeCustomSounds(preferences[KEY_CUSTOM_SOUNDS] ?: "[]")
    }

    suspend fun saveLogEntry(entry: RitualLogEntry) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[KEY_LOGS] ?: "[]"
            val currentList = deserializeLogs(currentJson).toMutableList()
            // If already logged for same date, replace with latest
            val existingIndex = currentList.indexOfFirst { it.date == entry.date }
            if (existingIndex >= 0) {
                currentList[existingIndex] = entry
            } else {
                currentList.add(0, entry)
            }
            preferences[KEY_LOGS] = serializeLogs(currentList)
        }
    }

    suspend fun setNotificationTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTIF_HOUR] = hour
            preferences[KEY_NOTIF_MINUTE] = minute
        }
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTIF_ENABLED] = enabled
        }
    }

    suspend fun setTestMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TEST_MODE] = enabled
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SOUND_ENABLED] = enabled
        }
    }

    suspend fun setSelectedSoundId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SOUND_STYLE] = id
        }
    }

    suspend fun addCustomSound(sound: CustomSound) {
        context.dataStore.edit { preferences ->
            val current = deserializeCustomSounds(preferences[KEY_CUSTOM_SOUNDS] ?: "[]").toMutableList()
            current.removeAll { it.id == sound.id }
            current.add(0, sound)
            preferences[KEY_CUSTOM_SOUNDS] = serializeCustomSounds(current)
        }
    }

    suspend fun removeCustomSound(id: String) {
        context.dataStore.edit { preferences ->
            val current = deserializeCustomSounds(preferences[KEY_CUSTOM_SOUNDS] ?: "[]")
            preferences[KEY_CUSTOM_SOUNDS] = serializeCustomSounds(current.filterNot { it.id == id })
        }
    }

    suspend fun setLanguageOption(option: LanguageOption) {
        context.dataStore.edit { preferences ->
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

    suspend fun clearLogs() {
        context.dataStore.edit { preferences ->
            preferences[KEY_LOGS] = "[]"
        }
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

    private fun serializeLogs(list: List<RitualLogEntry>): String {
        val array = JSONArray()
        for (entry in list) {
            val obj = JSONObject()
            obj.put("date", entry.date)
            obj.put("questionText", entry.questionText)
            obj.put("selectedAnswer", entry.selectedAnswer ?: "")
            obj.put("moodTag", entry.moodTag.id)
            obj.put("timestamp", entry.timestamp)
            array.put(obj)
        }
        return array.toString()
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
