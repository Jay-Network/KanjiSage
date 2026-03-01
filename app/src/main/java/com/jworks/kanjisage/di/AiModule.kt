package com.jworks.kanjisage.di

import android.content.Context
import com.jworks.kanjisage.data.ai.AiProviderManager
import com.jworks.kanjisage.data.ai.ClaudeProvider
import com.jworks.kanjisage.data.ai.GeminiOcrCorrector
import com.jworks.kanjisage.data.ai.GeminiProvider
import com.jworks.kanjisage.data.preferences.SecureKeyStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    @Named("ai")
    fun provideAiHttpClient(): HttpClient {
        return HttpClient(Android) {
            engine {
                connectTimeout = 15_000
                socketTimeout = 30_000
            }
        }
    }

    @Provides
    @Singleton
    fun provideSecureKeyStore(
        @ApplicationContext context: Context
    ): SecureKeyStore {
        return SecureKeyStore(context)
    }

    @Provides
    @Singleton
    fun provideGeminiOcrCorrector(
        @Named("ai") httpClient: HttpClient,
        secureKeyStore: SecureKeyStore
    ): GeminiOcrCorrector {
        return GeminiOcrCorrector(httpClient, secureKeyStore)
    }

    @Provides
    @Singleton
    fun provideClaudeProvider(
        @Named("ai") httpClient: HttpClient,
        secureKeyStore: SecureKeyStore
    ): ClaudeProvider {
        return ClaudeProvider(httpClient, secureKeyStore)
    }

    @Provides
    @Singleton
    fun provideGeminiProvider(
        @Named("ai") httpClient: HttpClient,
        secureKeyStore: SecureKeyStore
    ): GeminiProvider {
        return GeminiProvider(httpClient, secureKeyStore)
    }

    @Provides
    @Singleton
    fun provideAiProviderManager(
        claudeProvider: ClaudeProvider,
        geminiProvider: GeminiProvider
    ): AiProviderManager {
        val manager = AiProviderManager()
        manager.registerProvider(geminiProvider)  // Gemini first (cheaper)
        manager.registerProvider(claudeProvider)
        manager.setActiveProvider("Gemini")
        return manager
    }
}
