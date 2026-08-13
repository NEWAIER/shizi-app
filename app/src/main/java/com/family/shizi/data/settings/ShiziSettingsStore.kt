package com.family.shizi.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.shiziSettingsDataStore by preferencesDataStore(name = ShiziSettingsStore.FILE_NAME)

class ShiziSettingsStore(private val context: Context) {
    val settings: Flow<ShiziSettings> = context.shiziSettingsDataStore.data.map { prefs ->
        ShiziSettings(
            schemaVersion = 2,
            onboardingCompleted = prefs[Keys.onboardingCompleted] ?: true,
            nickname = prefs[Keys.nickname] ?: "",
            // Upgrade old 1-character defaults to the child-friendly 3-character course.
            dailyNewCharacterCount = if ((prefs[Keys.schemaVersion] ?: 1) < 2) 3 else (prefs[Keys.dailyNewCharacterCount] ?: 3),
            sessionLimitMinutes = prefs[Keys.sessionLimitMinutes] ?: 10,
            volumePercent = prefs[Keys.volumePercent] ?: 80,
            isMuted = prefs[Keys.isMuted] ?: false,
            lastKnownLocalDate = prefs[Keys.lastKnownLocalDate],
            lastSuccessfulSaveAt = prefs[Keys.lastSuccessfulSaveAt],
            contentVersion = prefs[Keys.contentVersion] ?: "1.0.0",
        )
    }

    suspend fun updateSettings(transform: (ShiziSettings) -> ShiziSettings): ShiziSettings {
        var updated = ShiziSettings()
        context.shiziSettingsDataStore.edit { prefs ->
            val current = ShiziSettings(
                schemaVersion = 2,
                onboardingCompleted = prefs[Keys.onboardingCompleted] ?: true,
                nickname = prefs[Keys.nickname] ?: "",
                dailyNewCharacterCount = if ((prefs[Keys.schemaVersion] ?: 1) < 2) 3 else (prefs[Keys.dailyNewCharacterCount] ?: 3),
                sessionLimitMinutes = prefs[Keys.sessionLimitMinutes] ?: 10,
                volumePercent = prefs[Keys.volumePercent] ?: 80,
                isMuted = prefs[Keys.isMuted] ?: false,
                lastKnownLocalDate = prefs[Keys.lastKnownLocalDate],
                lastSuccessfulSaveAt = prefs[Keys.lastSuccessfulSaveAt],
                contentVersion = prefs[Keys.contentVersion] ?: "1.0.0",
            )
            updated = transform(current).validated()
            prefs[Keys.schemaVersion] = updated.schemaVersion
            prefs[Keys.onboardingCompleted] = updated.onboardingCompleted
            prefs[Keys.nickname] = updated.nickname
            prefs[Keys.dailyNewCharacterCount] = updated.dailyNewCharacterCount
            prefs[Keys.sessionLimitMinutes] = updated.sessionLimitMinutes
            prefs[Keys.volumePercent] = updated.volumePercent
            prefs[Keys.isMuted] = updated.isMuted
            updated.lastKnownLocalDate?.let { prefs[Keys.lastKnownLocalDate] = it } ?: prefs.remove(Keys.lastKnownLocalDate)
            updated.lastSuccessfulSaveAt?.let { prefs[Keys.lastSuccessfulSaveAt] = it } ?: prefs.remove(Keys.lastSuccessfulSaveAt)
            prefs[Keys.contentVersion] = updated.contentVersion
        }
        return updated
    }

    private fun ShiziSettings.validated(): ShiziSettings =
        copy(
            schemaVersion = 2,
            nickname = nickname.take(8),
            // This prototype has five characters. Parents can choose a manageable daily batch.
            dailyNewCharacterCount = dailyNewCharacterCount.coerceIn(1, 5),
            sessionLimitMinutes = when (sessionLimitMinutes) {
                8, 10, 12 -> sessionLimitMinutes
                else -> 10
            },
            volumePercent = volumePercent.coerceIn(0, 100),
            contentVersion = contentVersion.ifBlank { "1.0.0" },
        )

    private object Keys {
        val schemaVersion = intPreferencesKey("schemaVersion")
        val onboardingCompleted = booleanPreferencesKey("onboardingCompleted")
        val nickname = stringPreferencesKey("nickname")
        val dailyNewCharacterCount = intPreferencesKey("dailyNewCharacterCount")
        val sessionLimitMinutes = intPreferencesKey("sessionLimitMinutes")
        val volumePercent = intPreferencesKey("volumePercent")
        val isMuted = booleanPreferencesKey("isMuted")
        val lastKnownLocalDate = stringPreferencesKey("lastKnownLocalDate")
        val lastSuccessfulSaveAt = longPreferencesKey("lastSuccessfulSaveAt")
        val contentVersion = stringPreferencesKey("contentVersion")
    }

    companion object {
        const val FILE_NAME = "settings.preferences_pb"
    }
}
