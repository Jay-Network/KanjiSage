package com.jworks.kanjisage.data.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import com.jworks.kanjisage.data.preferences.SecureKeyStore
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class GeminiWord(
    val w: String,
    val r: String
)

@Serializable
data class GeminiLine(
    val text: String,
    val words: List<GeminiWord>
)

@Singleton
class GeminiOcrCorrector @Inject constructor(
    private val httpClient: HttpClient,
    private val secureKeyStore: SecureKeyStore
) {
    companion object {
        private const val TAG = "GeminiOCR"
        private const val MODEL = "gemini-2.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private val json = Json { ignoreUnknownKeys = true }
    }

    private val apiKey: String get() = secureKeyStore.getGeminiApiKey()

    val isAvailable: Boolean get() = apiKey.isNotBlank()

    suspend fun extractTextWithReadings(bitmap: Bitmap): Result<List<GeminiLine>> {
        if (!isAvailable) return Result.failure(IllegalStateException("Gemini API key not set"))

        val startTime = System.currentTimeMillis()
        val base64Image = bitmapToBase64(bitmap)

        val requestBody = buildJsonObject {
            putJsonArray("contents") {
                add(buildJsonObject {
                    putJsonArray("parts") {
                        add(buildJsonObject {
                            put("text", EXTRACT_WITH_READINGS_PROMPT)
                        })
                        add(buildJsonObject {
                            putJsonObject("inline_data") {
                                put("mime_type", "image/jpeg")
                                put("data", base64Image)
                            }
                        })
                    }
                })
            }
            putJsonObject("generationConfig") {
                put("maxOutputTokens", 4096)
                put("temperature", 0.1)
            }
        }.toString()

        val url = "$BASE_URL/$MODEL:generateContent?key=$apiKey"

        return try {
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val responseText = response.bodyAsText()
            val elapsed = System.currentTimeMillis() - startTime

            if (response.status.value != 200) {
                Log.e(TAG, "API error ${response.status.value}: $responseText")
                return Result.failure(RuntimeException("Gemini Vision error: ${response.status.value}"))
            }

            val jsonResponse = Json.parseToJsonElement(responseText).jsonObject
            val content = jsonResponse["candidates"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("text")?.jsonPrimitive?.content
                ?: return Result.failure(RuntimeException("Empty response from Gemini Vision"))

            val lines = parseGeminiLines(content)
            Log.d(TAG, "Extracted ${lines.size} lines with readings in ${elapsed}ms")

            Result.success(lines)
        } catch (e: Exception) {
            Log.e(TAG, "Vision OCR with readings failed", e)
            Result.failure(e)
        }
    }

    suspend fun getContextualReading(kanji: String, surroundingText: String): Result<String> {
        if (!isAvailable) return Result.failure(IllegalStateException("Gemini API key not set"))

        val startTime = System.currentTimeMillis()
        val prompt = """漢字: "$kanji"
文脈: "$surroundingText"

上の文脈での「$kanji」の読みをひらがなで1つだけ答えてください。読みのみを出力し、他には何も書かないでください。"""

        val requestBody = buildJsonObject {
            putJsonArray("contents") {
                add(buildJsonObject {
                    putJsonArray("parts") {
                        add(buildJsonObject {
                            put("text", prompt)
                        })
                    }
                })
            }
            putJsonObject("generationConfig") {
                put("maxOutputTokens", 64)
                put("temperature", 0.1)
            }
        }.toString()

        val url = "$BASE_URL/$MODEL:generateContent?key=$apiKey"

        return try {
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val responseText = response.bodyAsText()
            val elapsed = System.currentTimeMillis() - startTime

            if (response.status.value != 200) {
                Log.e(TAG, "Reading API error ${response.status.value}: $responseText")
                return Result.failure(RuntimeException("Gemini error: ${response.status.value}"))
            }

            val jsonResponse = Json.parseToJsonElement(responseText).jsonObject
            val content = jsonResponse["candidates"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("text")?.jsonPrimitive?.content?.trim()
                ?: return Result.failure(RuntimeException("Empty reading response"))

            Log.d(TAG, "Contextual reading for '$kanji': '$content' in ${elapsed}ms")
            Result.success(content)
        } catch (e: Exception) {
            Log.e(TAG, "Contextual reading failed", e)
            Result.failure(e)
        }
    }

    private fun parseGeminiLines(content: String): List<GeminiLine> {
        // Extract JSON array from response (may have markdown code fences)
        val jsonStr = content
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .trim()

        return try {
            val jsonArray = Json.parseToJsonElement(jsonStr).jsonArray
            jsonArray.map { element ->
                val obj = element.jsonObject
                val text = obj["text"]?.jsonPrimitive?.content ?: ""
                val words = obj["words"]?.jsonArray?.map { wordEl ->
                    val wordObj = wordEl.jsonObject
                    GeminiWord(
                        w = wordObj["w"]?.jsonPrimitive?.content ?: "",
                        r = wordObj["r"]?.jsonPrimitive?.content ?: ""
                    )
                } ?: emptyList()
                GeminiLine(text = text, words = words)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse structured response, falling back to line-only", e)
            // Fallback: treat each line as plain text with no word-level readings
            content.lines().filter { it.isNotBlank() }.map { line ->
                GeminiLine(text = line.trim(), words = emptyList())
            }
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val bytes = stream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}

private const val EXTRACT_WITH_READINGS_PROMPT = """Extract ALL Japanese text from this image with perfect accuracy and provide readings for kanji words.

Rules:
- One line of text per output entry
- Preserve reading order (top to bottom for vertical text, left to right for horizontal)
- For each line, list kanji-containing words with their correct contextual reading in hiragana
- Use surrounding context to determine correct readings (e.g. 生ビール→なま, 生活→せい, 行く→い, 行動→こう)
- Resolve ambiguous characters (カ vs 力, ロ vs 口, ニ vs 二)
- If text appears handwritten or stylized, use context to determine correct characters
- Support both horizontal (横書き) and vertical (縦書き) text layouts
- Do NOT skip any text, even small or partial words

Output ONLY a JSON array in this exact format (no commentary, no markdown):
[{"text":"東京駅に行く","words":[{"w":"東京駅","r":"とうきょうえき"},{"w":"行","r":"い"}]},{"text":"生ビール","words":[{"w":"生","r":"なま"}]}]

Each entry: "text" = full line text, "words" = array of kanji words with "w" (word) and "r" (reading in hiragana).
Only include words that contain kanji in the "words" array. Kana-only words should be omitted from "words"."""
