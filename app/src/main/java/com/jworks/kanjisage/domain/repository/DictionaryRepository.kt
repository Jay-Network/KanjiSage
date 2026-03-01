package com.jworks.kanjisage.domain.repository

import com.jworks.kanjisage.domain.models.DictionaryResult

interface DictionaryRepository {
    suspend fun lookup(word: String): DictionaryResult?
    suspend fun search(prefix: String): List<DictionaryResult>
}
