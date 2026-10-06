package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AiModelOption
import com.example.model.NovaLanguage
import com.example.model.ResponseDepth
import com.example.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.novaDataStore: DataStore<Preferences> by preferencesDataStore(name = "nova_settings_prefs")

data class NovaUserSettings(
    val language: NovaLanguage = NovaLanguage.HINGLISH,
    val responseDepth: ResponseDepth = ResponseDepth.CONCISE,
    val themePreference: ThemePreference = ThemePreference.DARK,
    val selectedModel: AiModelOption = AiModelOption.GEMINI_FLASH,
    val voiceInputEnabled: Boolean = true,
    val ttsAutoReadEnabled: Boolean = true,
    val speechRate: Float = 1.0f,
    val webSearchGroundingEnabled: Boolean = true,
    val strictPrivacyRedactionEnabled: Boolean = true,
    val showScreenReadingIndicator: Boolean = true,
    val floatingAssistantEnabled: Boolean = false
)

class NovaPreferencesRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language_code")
        val RESPONSE_DEPTH = stringPreferencesKey("response_depth")
        val THEME_PREF = stringPreferencesKey("theme_pref")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
        val VOICE_INPUT = booleanPreferencesKey("voice_input_enabled")
        val TTS_AUTO_READ = booleanPreferencesKey("tts_auto_read_enabled")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val WEB_SEARCH = booleanPreferencesKey("web_search_grounding")
        val STRICT_REDACTION = booleanPreferencesKey("strict_privacy_redaction")
        val SCREEN_INDICATOR = booleanPreferencesKey("show_screen_indicator")
        val FLOATING_ASSISTANT = booleanPreferencesKey("floating_assistant_enabled")
    }

    val settingsFlow: Flow<NovaUserSettings> = context.novaDataStore.data.map { prefs ->
        NovaUserSettings(
            language = NovaLanguage.fromCode(prefs[Keys.LANGUAGE] ?: NovaLanguage.HINGLISH.code),
            responseDepth = ResponseDepth.fromId(prefs[Keys.RESPONSE_DEPTH] ?: ResponseDepth.CONCISE.id),
            themePreference = ThemePreference.fromId(prefs[Keys.THEME_PREF] ?: ThemePreference.DARK.id),
            selectedModel = AiModelOption.fromModelId(prefs[Keys.SELECTED_MODEL] ?: AiModelOption.GEMINI_FLASH.modelId),
            voiceInputEnabled = prefs[Keys.VOICE_INPUT] ?: true,
            ttsAutoReadEnabled = prefs[Keys.TTS_AUTO_READ] ?: true,
            speechRate = prefs[Keys.SPEECH_RATE] ?: 1.0f,
            webSearchGroundingEnabled = prefs[Keys.WEB_SEARCH] ?: true,
            strictPrivacyRedactionEnabled = prefs[Keys.STRICT_REDACTION] ?: true,
            showScreenReadingIndicator = prefs[Keys.SCREEN_INDICATOR] ?: true,
            floatingAssistantEnabled = prefs[Keys.FLOATING_ASSISTANT] ?: false
        )
    }

    suspend fun setLanguage(language: NovaLanguage) {
        context.novaDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setResponseDepth(depth: ResponseDepth) {
        context.novaDataStore.edit { it[Keys.RESPONSE_DEPTH] = depth.id }
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        context.novaDataStore.edit { it[Keys.THEME_PREF] = theme.id }
    }

    suspend fun setSelectedModel(model: AiModelOption) {
        context.novaDataStore.edit { it[Keys.SELECTED_MODEL] = model.modelId }
    }

    suspend fun setVoiceInputEnabled(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.VOICE_INPUT] = enabled }
    }

    suspend fun setTtsAutoReadEnabled(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.TTS_AUTO_READ] = enabled }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.novaDataStore.edit { it[Keys.SPEECH_RATE] = rate.coerceIn(0.6f, 1.6f) }
    }

    suspend fun setWebSearchGroundingEnabled(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.WEB_SEARCH] = enabled }
    }

    suspend fun setStrictPrivacyRedactionEnabled(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.STRICT_REDACTION] = enabled }
    }

    suspend fun setShowScreenReadingIndicator(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.SCREEN_INDICATOR] = enabled }
    }

    suspend fun setFloatingAssistantEnabled(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.FLOATING_ASSISTANT] = enabled }
    }
}
