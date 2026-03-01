package com.jworks.kanjisage.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjisage.data.preferences.SecureKeyStore
import com.jworks.kanjisage.domain.models.AppSettings
import com.jworks.kanjisage.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val secureKeyStore: SecureKeyStore
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _geminiApiKey = MutableStateFlow(secureKeyStore.getGeminiApiKey())
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    fun setGeminiApiKey(key: String) {
        secureKeyStore.setGeminiApiKey(key)
        _geminiApiKey.value = key
    }

    fun updateStrokeWidth(value: Float) = updateSettings { it.copy(strokeWidth = value) }

    fun updateLabelFontSize(value: Float) = updateSettings { it.copy(labelFontSize = value) }

    fun updateFrameSkip(value: Int) = updateSettings { it.copy(frameSkip = value) }

    fun updateShowDebugHud(value: Boolean) = updateSettings { it.copy(showDebugHud = value) }

    fun updateShowBoxes(value: Boolean) = updateSettings { it.copy(showBoxes = value) }

    fun updateFuriganaIsBold(value: Boolean) = updateSettings { it.copy(furiganaIsBold = value) }

    fun updateFuriganaUseWhiteText(value: Boolean) = updateSettings { it.copy(furiganaUseWhiteText = value) }

    fun updateVerticalTextMode(value: Boolean) = updateSettings { it.copy(verticalTextMode = value) }

    fun updateFuriganaAdaptiveColor(value: Boolean) = updateSettings { it.copy(furiganaAdaptiveColor = value) }

    fun updateAiEnhanceEnabled(value: Boolean) = updateSettings { it.copy(aiEnhanceEnabled = value) }

    fun resetTokenUsage() {
        viewModelScope.launch { settingsRepository.resetTokenUsage() }
    }

    fun applyColorPreset(kanjiColor: Long, kanaColor: Long) = updateSettings {
        it.copy(kanjiColor = kanjiColor, kanaColor = kanaColor)
    }

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            try {
                val updated = transform(settings.value)
                settingsRepository.updateSettings(updated)
            } catch (e: Exception) {
                android.util.Log.w("SettingsVM", "Failed to update settings", e)
            }
        }
    }
}
