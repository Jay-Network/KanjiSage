package com.jworks.kanjisage.domain.repository

import com.jworks.kanjisage.domain.models.KanjiInfo

interface KanjiInfoRepository {
    suspend fun getKanji(literal: String): KanjiInfo?
}
