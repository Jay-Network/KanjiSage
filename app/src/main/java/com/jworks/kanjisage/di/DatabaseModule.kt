package com.jworks.kanjisage.di

import android.content.Context
import androidx.room.Room
import com.jworks.kanjisage.data.bookmark.BookmarkRepositoryImpl
import com.jworks.kanjisage.data.local.BookmarkDao
import com.jworks.kanjisage.data.local.DictionaryDao
import com.jworks.kanjisage.data.local.JMDictDao
import com.jworks.kanjisage.data.local.JMDictDatabase
import com.jworks.kanjisage.data.local.KanjiInfoDao
import com.jworks.kanjisage.data.local.UserDataDatabase
import com.jworks.kanjisage.data.repository.KanjiInfoRepositoryImpl
import com.jworks.kanjisage.domain.repository.BookmarkRepository
import com.jworks.kanjisage.domain.repository.KanjiInfoRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): JMDictDatabase {
        return Room.databaseBuilder(
            context,
            JMDictDatabase::class.java,
            "jmdict.db"
        )
            .createFromAsset("jmdict.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideJMDictDao(database: JMDictDatabase): JMDictDao {
        return database.jmDictDao()
    }

    @Provides
    @Singleton
    fun provideDictionaryDao(database: JMDictDatabase): DictionaryDao {
        return database.dictionaryDao()
    }

    @Provides
    @Singleton
    fun provideKanjiInfoDao(database: JMDictDatabase): KanjiInfoDao {
        return database.kanjiInfoDao()
    }

    @Provides
    @Singleton
    fun provideUserDataDatabase(@ApplicationContext context: Context): UserDataDatabase {
        return Room.databaseBuilder(
            context,
            UserDataDatabase::class.java,
            "user_data.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideBookmarkDao(database: UserDataDatabase): BookmarkDao {
        return database.bookmarkDao()
    }

    @Provides
    @Singleton
    fun provideBookmarkRepository(bookmarkDao: BookmarkDao): BookmarkRepository {
        return BookmarkRepositoryImpl(bookmarkDao)
    }

    @Provides
    @Singleton
    fun provideKanjiInfoRepository(kanjiInfoDao: KanjiInfoDao): KanjiInfoRepository {
        return KanjiInfoRepositoryImpl(kanjiInfoDao)
    }
}
