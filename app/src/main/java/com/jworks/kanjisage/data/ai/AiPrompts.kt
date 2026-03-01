package com.jworks.kanjisage.data.ai

import com.jworks.kanjisage.domain.ai.AnalysisContext
import com.jworks.kanjisage.domain.ai.ScopeLevel

object AiPrompts {

    const val SYSTEM_PROMPT = "You are a Japanese language analysis assistant in KanjiSage, a camera-based kanji reading app. Your audience is Japanese learners at various levels. Be concise, educational, and practical. Format responses with markdown (bold, bullet points, headings)."

    fun buildPrompt(context: AnalysisContext): String = when (context.scopeLevel) {
        is ScopeLevel.Word -> wordPrompt(context)
        is ScopeLevel.Phrase -> phrasePrompt(context)
        is ScopeLevel.Sentence -> sentencePrompt(context)
        is ScopeLevel.FullSnapshot -> fullTextPrompt(context)
    }

    private fun wordPrompt(context: AnalysisContext): String = """
Explain this Japanese word in the context it appears.

Word: "${context.selectedText}"

Context: ${context.fullSnapshotText}

Provide:
1. **Reading** (hiragana) and meaning in this specific context
2. **Part of speech**
3. **Kanji breakdown** — explain each kanji component
4. **Example sentence** using this word
""".trimIndent()

    private fun phrasePrompt(context: AnalysisContext): String = """
Analyze this Japanese phrase selected from a larger text.

Selected text: "${context.selectedText}"

Full surrounding text:
${context.fullSnapshotText}

Provide a concise analysis:
1. **Meaning**: What does this phrase mean in context?
2. **Grammar**: Identify the grammatical patterns (e.g., て-form, conditional, passive).
3. **Key vocabulary**: List kanji words with readings and meanings.
4. **Formality level**: Is this casual, polite (です/ます), or formal/literary?
""".trimIndent()

    private fun sentencePrompt(context: AnalysisContext): String = """
Analyze this Japanese sentence from a larger text.

Sentence: "${context.selectedText}"

Full surrounding text:
${context.fullSnapshotText}

Provide a concise analysis:
1. **Translation**: Natural English translation.
2. **Grammar breakdown**: Subject, verb, particles, conjugation patterns.
3. **Key vocabulary**: Important kanji words with readings.
4. **Formality**: Casual, polite, honorific, or literary register.
""".trimIndent()

    private fun fullTextPrompt(context: AnalysisContext): String = """
Provide a comprehensive analysis of this Japanese text captured by camera.

Full text:
"${context.fullSnapshotText}"

Analyze the following:
## Summary
2-3 sentence overview and English translation of the main content.

## Key Vocabulary
List 5-10 notable kanji words/jukugo with readings (hiragana) and meanings. Format each as:
- **漢字** (かんじ) — meaning

## Grammar Patterns
Identify 2-4 notable grammar patterns used in the text.

## Context
What type of text is this (sign, menu, book, notice, etc.)? What is the formality level?
Note any cultural context that would help a learner understand.
""".trimIndent()
}
