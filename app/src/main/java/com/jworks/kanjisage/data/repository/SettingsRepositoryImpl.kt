package com.jworks.kanjisage.data.repository

import com.jworks.kanjisage.data.preferences.SettingsDataStore
import com.jworks.kanjisage.domain.models.AppSettings
import com.jworks.kanjisage.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.settingsFlow

    override suspend fun updateSettings(settings: AppSettings) {
        dataStore.updateSettings(settings)
    }

    override suspend fun addTokenUsage(provider: String, inputTokens: Int, outputTokens: Int) {
        dataStore.addTokenUsage(provider, inputTokens, outputTokens)
    }

    override suspend fun resetTokenUsage() {
        dataStore.resetTokenUsage()
    }
}
