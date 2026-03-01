package com.jworks.kanjisage.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jworks.kanjisage.domain.models.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kanjisage_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val KANJI_COLOR = longPreferencesKey("kanji_color")
        val KANA_COLOR = longPreferencesKey("kana_color")
        val STROKE_WIDTH = floatPreferencesKey("stroke_width")
        val LABEL_FONT_SIZE = floatPreferencesKey("label_font_size")
        val FRAME_SKIP = intPreferencesKey("frame_skip")
        val SHOW_DEBUG_HUD = booleanPreferencesKey("show_debug_hud")
        val SHOW_BOXES = booleanPreferencesKey("show_boxes")
        val FURIGANA_IS_BOLD = booleanPreferencesKey("furigana_is_bold")
        val FURIGANA_USE_WHITE_TEXT = booleanPreferencesKey("furigana_use_white_text")
        val PARTIAL_MODE_BOUNDARY_RATIO = floatPreferencesKey("partial_mode_boundary_ratio")
        val VERTICAL_TEXT_MODE = booleanPreferencesKey("vertical_text_mode")
        val FURIGANA_ADAPTIVE_COLOR = booleanPreferencesKey("furigana_adaptive_color")
        val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
        val GEMINI_INPUT_TOKENS = longPreferencesKey("gemini_input_tokens")
        val GEMINI_OUTPUT_TOKENS = longPreferencesKey("gemini_output_tokens")
        val CLAUDE_INPUT_TOKENS = longPreferencesKey("claude_input_tokens")
        val CLAUDE_OUTPUT_TOKENS = longPreferencesKey("claude_output_tokens")
    }

    private val defaults = AppSettings()

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            kanjiColor = prefs[Keys.KANJI_COLOR] ?: defaults.kanjiColor,
            kanaColor = prefs[Keys.KANA_COLOR] ?: defaults.kanaColor,
            strokeWidth = prefs[Keys.STROKE_WIDTH] ?: defaults.strokeWidth,
            labelFontSize = prefs[Keys.LABEL_FONT_SIZE] ?: defaults.labelFontSize,
            frameSkip = prefs[Keys.FRAME_SKIP] ?: defaults.frameSkip,
            showDebugHud = prefs[Keys.SHOW_DEBUG_HUD] ?: defaults.showDebugHud,
            showBoxes = prefs[Keys.SHOW_BOXES] ?: defaults.showBoxes,
            furiganaIsBold = prefs[Keys.FURIGANA_IS_BOLD] ?: defaults.furiganaIsBold,
            furiganaUseWhiteText = prefs[Keys.FURIGANA_USE_WHITE_TEXT] ?: defaults.furiganaUseWhiteText,
            partialModeBoundaryRatio = prefs[Keys.PARTIAL_MODE_BOUNDARY_RATIO] ?: defaults.partialModeBoundaryRatio,
            verticalTextMode = prefs[Keys.VERTICAL_TEXT_MODE] ?: defaults.verticalTextMode,
            furiganaAdaptiveColor = prefs[Keys.FURIGANA_ADAPTIVE_COLOR] ?: defaults.furiganaAdaptiveColor,
            geminiInputTokens = prefs[Keys.GEMINI_INPUT_TOKENS] ?: 0L,
            geminiOutputTokens = prefs[Keys.GEMINI_OUTPUT_TOKENS] ?: 0L,
            claudeInputTokens = prefs[Keys.CLAUDE_INPUT_TOKENS] ?: 0L,
            claudeOutputTokens = prefs[Keys.CLAUDE_OUTPUT_TOKENS] ?: 0L
        )
    }

    val hasSeenOnboardingFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HAS_SEEN_ONBOARDING] ?: false
    }

    suspend fun setOnboardingSeen() {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAS_SEEN_ONBOARDING] = true
        }
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.KANJI_COLOR] = settings.kanjiColor
            prefs[Keys.KANA_COLOR] = settings.kanaColor
            prefs[Keys.STROKE_WIDTH] = settings.strokeWidth
            prefs[Keys.LABEL_FONT_SIZE] = settings.labelFontSize
            prefs[Keys.FRAME_SKIP] = settings.frameSkip
            prefs[Keys.SHOW_DEBUG_HUD] = settings.showDebugHud
            prefs[Keys.SHOW_BOXES] = settings.showBoxes
            prefs[Keys.FURIGANA_IS_BOLD] = settings.furiganaIsBold
            prefs[Keys.FURIGANA_USE_WHITE_TEXT] = settings.furiganaUseWhiteText
            prefs[Keys.PARTIAL_MODE_BOUNDARY_RATIO] = settings.partialModeBoundaryRatio
            prefs[Keys.VERTICAL_TEXT_MODE] = settings.verticalTextMode
            prefs[Keys.FURIGANA_ADAPTIVE_COLOR] = settings.furiganaAdaptiveColor
        }
    }

    suspend fun addTokenUsage(provider: String, inputTokens: Int, outputTokens: Int) {
        context.dataStore.edit { prefs ->
            when (provider) {
                "Claude" -> {
                    prefs[Keys.CLAUDE_INPUT_TOKENS] = (prefs[Keys.CLAUDE_INPUT_TOKENS] ?: 0L) + inputTokens
                    prefs[Keys.CLAUDE_OUTPUT_TOKENS] = (prefs[Keys.CLAUDE_OUTPUT_TOKENS] ?: 0L) + outputTokens
                }
                "Gemini" -> {
                    prefs[Keys.GEMINI_INPUT_TOKENS] = (prefs[Keys.GEMINI_INPUT_TOKENS] ?: 0L) + inputTokens
                    prefs[Keys.GEMINI_OUTPUT_TOKENS] = (prefs[Keys.GEMINI_OUTPUT_TOKENS] ?: 0L) + outputTokens
                }
            }
        }
    }

    suspend fun resetTokenUsage() {
        context.dataStore.edit { prefs ->
            prefs[Keys.CLAUDE_INPUT_TOKENS] = 0L
            prefs[Keys.CLAUDE_OUTPUT_TOKENS] = 0L
            prefs[Keys.GEMINI_INPUT_TOKENS] = 0L
            prefs[Keys.GEMINI_OUTPUT_TOKENS] = 0L
        }
    }
}
