package com.jworks.kanjisage.domain.usecases

import com.jworks.kanjisage.domain.models.FuriganaResult
import com.jworks.kanjisage.domain.repository.FuriganaRepository
import javax.inject.Inject

class GetFuriganaUseCase @Inject constructor(
    private val repository: FuriganaRepository
) {
    suspend fun execute(text: String): Result<FuriganaResult> {
        return repository.getFurigana(text)
    }

    suspend fun executeBatch(texts: List<String>): Result<Map<String, FuriganaResult>> {
        return repository.batchGetFurigana(texts)
    }
}
