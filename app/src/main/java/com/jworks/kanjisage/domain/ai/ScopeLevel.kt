package com.jworks.kanjisage.domain.ai

sealed class ScopeLevel {
    data class Word(val word: String) : ScopeLevel()
    data class Phrase(val text: String) : ScopeLevel()
    data class Sentence(val text: String) : ScopeLevel()
    data object FullSnapshot : ScopeLevel()
}
