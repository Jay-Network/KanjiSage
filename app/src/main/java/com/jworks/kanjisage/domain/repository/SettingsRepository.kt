package com.jworks.kanjisage.domain.repository

import com.jworks.kanjisage.domain.models.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
    suspend fun addTokenUsage(provider: String, inputTokens: Int, outputTokens: Int)
    suspend fun resetTokenUsage()
}
