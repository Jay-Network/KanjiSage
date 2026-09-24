package com.jworks.kanjisage.di

import android.content.Context
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import android.os.Build
import com.jworks.kanjisage.data.preferences.SettingsDataStore
import com.jworks.kanjisage.data.remote.KuroshiroApi
import com.jworks.kanjisage.data.repository.DictionaryRepositoryImpl
import com.jworks.kanjisage.data.repository.FuriganaRepositoryImpl
import com.jworks.kanjisage.data.repository.SettingsRepositoryImpl
import com.jworks.kanjisage.domain.repository.DictionaryRepository
import com.jworks.kanjisage.domain.repository.FuriganaRepository
import com.jworks.kanjisage.domain.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTextRecognizer(): TextRecognizer {
        return TextRecognition.getClient(
            JapaneseTextRecognizerOptions.Builder().build()
        )
    }

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        // No production Kuroshiro backend is deployed anywhere reachable by a real device.
        // FuriganaRepositoryImpl only calls this as a last-resort fallback after the bundled
        // 180k-entry JMDict database misses, and already catches failure gracefully — so this
        // client just needs to fail FAST rather than hang on Retrofit's 10s default connect
        // timeout on every real-user cache miss. The LAN IP below only ever resolves during
        // local dev (running backend/server.js on the same network as a physical test device).
        val baseUrl = if (Build.FINGERPRINT.contains("generic")) {
            "http://10.0.2.2:3000/"  // Emulator -> host loopback
        } else {
            "http://192.168.1.100:3000/"  // Dev-only LAN fallback; unreachable for real users by design
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideKuroshiroApi(retrofit: Retrofit): KuroshiroApi {
        return retrofit.create(KuroshiroApi::class.java)
    }

    @Provides
    @Singleton
    fun provideFuriganaRepository(impl: FuriganaRepositoryImpl): FuriganaRepository {
        return impl
    }

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository {
        return impl
    }

    @Provides
    @Singleton
    fun provideDictionaryRepository(impl: DictionaryRepositoryImpl): DictionaryRepository {
        return impl
    }
}
