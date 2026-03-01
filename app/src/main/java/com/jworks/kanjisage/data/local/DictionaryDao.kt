package com.jworks.kanjisage.data.local

import androidx.room.Dao
import androidx.room.Query
import com.jworks.kanjisage.data.local.entities.DictionaryEntry

@Dao
interface DictionaryDao {
    @Query("SELECT * FROM jmdict_meanings WHERE word = :word LIMIT 1")
    suspend fun getEntry(word: String): DictionaryEntry?

    @Query("SELECT * FROM jmdict_meanings WHERE word LIKE :prefix || '%' ORDER BY common DESC, LENGTH(word) ASC LIMIT 20")
    suspend fun search(prefix: String): List<DictionaryEntry>
}
