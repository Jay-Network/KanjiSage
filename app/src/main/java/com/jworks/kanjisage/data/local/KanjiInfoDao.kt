package com.jworks.kanjisage.data.local

import androidx.room.Dao
import androidx.room.Query
import com.jworks.kanjisage.data.local.entities.KanjiInfoEntry

@Dao
interface KanjiInfoDao {
    @Query("SELECT * FROM kanji_info WHERE literal = :kanji LIMIT 1")
    suspend fun getKanji(kanji: String): KanjiInfoEntry?
}
