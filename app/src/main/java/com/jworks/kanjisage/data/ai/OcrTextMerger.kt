package com.jworks.kanjisage.data.ai

import android.util.Log
import com.jworks.kanjisage.domain.models.DetectedText
import com.jworks.kanjisage.domain.models.KanjiSegment
import com.jworks.kanjisage.domain.models.TextElement

/**
 * Merges Gemini Vision's corrected text + readings with ML Kit's bounding boxes.
 *
 * Strategy: align lines by index, then for each ML Kit TextElement find overlapping
 * GeminiWords by character position in the line. Build KanjiSegments directly from
 * Gemini readings (bypassing Kuromoji for enhanced lines).
 */
object OcrTextMerger {

    private const val TAG = "OcrMerger"

    fun merge(
        mlKitTexts: List<DetectedText>,
        geminiLines: List<GeminiLine>
    ): List<DetectedText> {
        if (mlKitTexts.isEmpty() || geminiLines.isEmpty()) return mlKitTexts

        // Only attempt merge when line counts match to avoid cascading misalignment
        if (mlKitTexts.size != geminiLines.size) {
            Log.d(TAG, "Line count mismatch (ML Kit: ${mlKitTexts.size}, Gemini: ${geminiLines.size}), keeping ML Kit result")
            return mlKitTexts
        }

        val corrected = mutableListOf<DetectedText>()

        for (i in mlKitTexts.indices) {
            val mlLine = mlKitTexts[i]
            val geminiLine = geminiLines[i]

            val mergedElements = mergeElements(mlLine.elements, mlLine.text, geminiLine)

            corrected.add(
                mlLine.copy(
                    text = geminiLine.text,
                    elements = mergedElements
                )
            )
        }

        val correctedSegments = corrected.sumOf { it.elements.sumOf { e -> e.kanjiSegments.size } }
        Log.d(TAG, "Merged: ${mlKitTexts.size} lines, $correctedSegments AI reading segments")

        return corrected
    }

    /**
     * For each ML Kit element, find its span in the line text, then find overlapping
     * Gemini words to build KanjiSegments with AI-corrected readings.
     */
    private fun mergeElements(
        mlKitElements: List<TextElement>,
        mlLineText: String,
        geminiLine: GeminiLine
    ): List<TextElement> {
        if (mlKitElements.isEmpty()) return mlKitElements
        if (geminiLine.words.isEmpty()) return mlKitElements

        // Pre-compute word positions in the Gemini line text
        val wordPositions = computeWordPositions(geminiLine.text, geminiLine.words)

        // Build element spans in ML Kit line text (left-to-right cursor)
        val elementSpans = buildElementSpans(mlLineText, mlKitElements)

        // Map ML Kit element spans to Gemini line text spans (approximate: same ratio)
        return mlKitElements.mapIndexed { index, element ->
            if (!element.containsKanji) return@mapIndexed element

            val (elemStart, elemEnd) = elementSpans.getOrNull(index)
                ?: return@mapIndexed element

            // Find Gemini words that overlap with this element's character range
            val segments = findOverlappingSegments(
                element.text, elemStart, elemEnd, wordPositions
            )

            if (segments.isNotEmpty()) {
                val reading = buildPositionalReading(element.text, segments)
                element.copy(reading = reading, kanjiSegments = segments)
            } else {
                element
            }
        }
    }

    /**
     * Compute character positions of each GeminiWord in the line text.
     */
    private fun computeWordPositions(
        lineText: String,
        words: List<GeminiWord>
    ): List<Triple<GeminiWord, Int, Int>> {
        val positions = mutableListOf<Triple<GeminiWord, Int, Int>>()
        var cursor = 0

        for (word in words) {
            val idx = lineText.indexOf(word.w, cursor)
            if (idx >= 0) {
                positions.add(Triple(word, idx, idx + word.w.length))
                cursor = idx + word.w.length
            } else {
                // Fallback: global search
                val globalIdx = lineText.indexOf(word.w)
                if (globalIdx >= 0) {
                    positions.add(Triple(word, globalIdx, globalIdx + word.w.length))
                }
            }
        }

        return positions
    }

    /**
     * Build element spans (start, end) in line text, preserving left-to-right order.
     */
    private fun buildElementSpans(
        lineText: String,
        elements: List<TextElement>
    ): List<Pair<Int, Int>> {
        val spans = mutableListOf<Pair<Int, Int>>()
        var cursor = 0

        for (element in elements) {
            val text = element.text
            if (text.isEmpty()) {
                spans.add(cursor to cursor)
                continue
            }

            val idxFromCursor = lineText.indexOf(text, cursor)
            val idx = if (idxFromCursor >= 0) {
                idxFromCursor
            } else {
                lineText.indexOf(text).takeIf { it >= 0 } ?: cursor
            }
            val end = (idx + text.length).coerceAtMost(lineText.length)
            spans.add(idx to end)
            cursor = end
        }

        return spans
    }

    /**
     * Find GeminiWords that overlap with an element's character range and build KanjiSegments.
     * Uses character-position matching (no word boundaries in Japanese).
     */
    private fun findOverlappingSegments(
        elementText: String,
        elemStart: Int,
        elemEnd: Int,
        wordPositions: List<Triple<GeminiWord, Int, Int>>
    ): List<KanjiSegment> {
        val segments = mutableListOf<KanjiSegment>()

        for ((word, wordStart, wordEnd) in wordPositions) {
            // Check if this word overlaps with the element span
            if (wordStart >= elemEnd || wordEnd <= elemStart) continue

            // Calculate local position within the element text
            val localStart = (wordStart - elemStart).coerceAtLeast(0)
            val localEnd = (wordEnd - elemStart).coerceAtMost(elementText.length)

            if (localStart >= localEnd) continue

            // Verify the text actually matches at this position
            val localText = elementText.substring(localStart, localEnd)
            if (localText != word.w) {
                // Text mismatch — skip this word (ML Kit and Gemini disagree on this segment)
                Log.d(TAG, "Text mismatch at [$localStart,$localEnd]: element='$localText' vs gemini='${word.w}'")
                continue
            }

            if (word.r.isNotBlank()) {
                segments.add(
                    KanjiSegment(
                        text = word.w,
                        reading = word.r,
                        startIndex = localStart,
                        endIndex = localEnd
                    )
                )
            }
        }

        return segments
    }

    /**
     * Build positional reading string with full-width spaces for non-kanji positions.
     */
    private fun buildPositionalReading(text: String, segments: List<KanjiSegment>): String? {
        if (segments.isEmpty()) return null
        val result = StringBuilder()
        var i = 0
        while (i < text.length) {
            val segment = segments.find { it.startIndex == i }
            if (segment != null) {
                result.append(segment.reading)
                i = segment.endIndex
            } else {
                result.append('\u3000')
                i++
            }
        }
        return result.toString().trimEnd('\u3000').ifEmpty { null }
    }
}
