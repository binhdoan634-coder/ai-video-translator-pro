package com.avtpro.translator.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.avtpro.translator.data.api.RetrofitClient
import com.avtpro.translator.data.model.ProcessingMode
import com.avtpro.translator.data.model.VoiceGender
import com.avtpro.translator.data.model.VoiceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "avtpro_settings")

data class AppSettings(
    val appLanguage: String = "vi",
    val defaultTargetLanguage: String = "vi",
    val voiceGender: VoiceGender = VoiceGender.FEMALE,
    val defaultVoiceMode: VoiceMode = VoiceMode.NATURAL,
    val processingMode: ProcessingMode = ProcessingMode.AUTO,
    val exportQuality: String = "Giữ nguyên",
    val audioQuality: String = "Cao",
    val defaultSubtitle: Boolean = true,
    val autoSaveVideo: Boolean = true,
    val autoDeleteTemp: Boolean = true,
    val backendUrl: String = RetrofitClient.DEFAULT_BASE_URL
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val DEFAULT_TARGET_LANGUAGE = stringPreferencesKey("default_target_language")
        val VOICE_GENDER = stringPreferencesKey("voice_gender")
        val DEFAULT_VOICE_MODE = stringPreferencesKey("default_voice_mode")
        val PROCESSING_MODE = stringPreferencesKey("processing_mode")
        val EXPORT_QUALITY = stringPreferencesKey("export_quality")
        val AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        val DEFAULT_SUBTITLE = booleanPreferencesKey("default_subtitle")
        val AUTO_SAVE_VIDEO = booleanPreferencesKey("auto_save_video")
        val AUTO_DELETE_TEMP = booleanPreferencesKey("auto_delete_temp")
        val BACKEND_URL = stringPreferencesKey("backend_url")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            appLanguage = prefs[Keys.APP_LANGUAGE] ?: "vi",
            defaultTargetLanguage = prefs[Keys.DEFAULT_TARGET_LANGUAGE] ?: "vi",
            voiceGender = runCatching {
                VoiceGender.valueOf(prefs[Keys.VOICE_GENDER] ?: "FEMALE")
            }.getOrDefault(VoiceGender.FEMALE),
            defaultVoiceMode = runCatching {
                VoiceMode.valueOf(prefs[Keys.DEFAULT_VOICE_MODE] ?: "NATURAL")
            }.getOrDefault(VoiceMode.NATURAL),
            processingMode = runCatching {
                ProcessingMode.valueOf(prefs[Keys.PROCESSING_MODE] ?: "AUTO")
            }.getOrDefault(ProcessingMode.AUTO),
            exportQuality = prefs[Keys.EXPORT_QUALITY] ?: "Giữ nguyên",
            audioQuality = prefs[Keys.AUDIO_QUALITY] ?: "Cao",
            defaultSubtitle = prefs[Keys.DEFAULT_SUBTITLE] ?: true,
            autoSaveVideo = prefs[Keys.AUTO_SAVE_VIDEO] ?: true,
            autoDeleteTemp = prefs[Keys.AUTO_DELETE_TEMP] ?: true,
            backendUrl = prefs[Keys.BACKEND_URL] ?: RetrofitClient.DEFAULT_BASE_URL
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings, current: AppSettings) {
        val next = transform(current)
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.APP_LANGUAGE] = next.appLanguage
            prefs[Keys.DEFAULT_TARGET_LANGUAGE] = next.defaultTargetLanguage
            prefs[Keys.VOICE_GENDER] = next.voiceGender.name
            prefs[Keys.DEFAULT_VOICE_MODE] = next.defaultVoiceMode.name
            prefs[Keys.PROCESSING_MODE] = next.processingMode.name
            prefs[Keys.EXPORT_QUALITY] = next.exportQuality
            prefs[Keys.AUDIO_QUALITY] = next.audioQuality
            prefs[Keys.DEFAULT_SUBTITLE] = next.defaultSubtitle
            prefs[Keys.AUTO_SAVE_VIDEO] = next.autoSaveVideo
            prefs[Keys.AUTO_DELETE_TEMP] = next.autoDeleteTemp
            prefs[Keys.BACKEND_URL] = next.backendUrl.trim()
        }
    }
}
