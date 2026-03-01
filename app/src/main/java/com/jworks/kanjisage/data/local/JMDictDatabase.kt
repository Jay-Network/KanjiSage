package com.jworks.kanjisage.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jworks.kanjisage.data.local.entities.DictionaryEntry
import com.jworks.kanjisage.data.local.entities.FuriganaEntry
import com.jworks.kanjisage.data.local.entities.KanjiInfoEntry

@Database(
    entities = [FuriganaEntry::class, DictionaryEntry::class, KanjiInfoEntry::class],
    version = 3,
    exportSchema = false
)
abstract class JMDictDatabase : RoomDatabase() {
    abstract fun jmDictDao(): JMDictDao
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun kanjiInfoDao(): KanjiInfoDao
}
