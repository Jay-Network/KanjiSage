package com.jworks.kanjisage.domain.repository

import com.jworks.kanjisage.domain.models.FuriganaResult

interface FuriganaRepository {
    suspend fun getFurigana(text: String): Result<FuriganaResult>
    suspend fun batchGetFurigana(texts: List<String>): Result<Map<String, FuriganaResult>>
    fun clearCache()
}
