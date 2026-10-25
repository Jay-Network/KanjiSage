package com.jworks.kanjisage.data.feedback

import com.jworks.kanjisage.domain.models.Feedback
import com.jworks.kanjisage.domain.models.FeedbackCategory
import com.jworks.kanjisage.domain.models.FeedbackStatus
import com.jworks.kanjisage.domain.models.FeedbackStatusChange
import com.jworks.kanjisage.domain.models.FeedbackWithHistory
import com.jworks.kanjisage.domain.models.SubmitFeedbackResult
import com.jworks.kanjisage.domain.repository.FeedbackRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class FeedbackRepositoryImpl @Inject constructor(
    @Named("jcoin") private val supabaseClient: SupabaseClient
) : FeedbackRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun submitFeedback(
        accessToken: String,
        email: String,
        appId: String,
        category: FeedbackCategory,
        feedbackText: String,
        deviceInfo: Map<String, String>?
    ): SubmitFeedbackResult = withContext(Dispatchers.IO) {
        try {
            require(accessToken.isNotBlank()) { "Sign in required" }
            val response = supabaseClient.functions.invoke("feedback-submit") {
                headers[HttpHeaders.Authorization] = "Bearer $accessToken"
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("user_email", email)
                    put("app_id", appId)
                    put("category", category.value)
                    put("feedback_text", feedbackText)
                    if (deviceInfo != null) {
                        putJsonObject("device_info") {
                            deviceInfo.forEach { (key, value) ->
                                put(key, value)
                            }
                        }
                    }
                }.toString())
            }

            val responseBody = json.parseToJsonElement(response.body<String>()).jsonObject

            if (response.status.value == 429) {
                val errorMsg = responseBody["error"]?.jsonObject?.get("message")
                    ?.jsonPrimitive?.content ?: "Rate limit exceeded"
                return@withContext SubmitFeedbackResult.RateLimited(errorMsg)
            }

            if (response.status.value !in 200..299) {
                val errorMsg = responseBody["error"]?.jsonObject?.get("message")
                    ?.jsonPrimitive?.content ?: "Submit failed"
                return@withContext SubmitFeedbackResult.Error(errorMsg)
            }

            val data = responseBody["data"]?.jsonObject
                ?: return@withContext SubmitFeedbackResult.Error("Invalid response")

            SubmitFeedbackResult.Success(
                feedbackId = data["feedback_id"]?.jsonPrimitive?.long ?: 0,
                createdAt = data["created_at"]?.jsonPrimitive?.content ?: ""
            )
        } catch (e: Exception) {
            SubmitFeedbackResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun getFeedbackUpdates(
        accessToken: String,
        email: String,
        appId: String,
        sinceId: Long?
    ): List<FeedbackWithHistory> = withContext(Dispatchers.IO) {
        try {
            require(accessToken.isNotBlank()) { "Sign in required" }
            val response = supabaseClient.functions.invoke("feedback-get-updates") {
                headers[HttpHeaders.Authorization] = "Bearer $accessToken"
                // URLBuilder encodes each value, including '+' in mailbox aliases.
                url.parameters.append("user_email", email)
                url.parameters.append("app_id", appId)
                if (sinceId != null) url.parameters.append("since_id", sinceId.toString())
            }

            if (response.status.value !in 200..299) return@withContext emptyList()

            val body = json.parseToJsonElement(response.body<String>()).jsonObject
            val data = body["data"]?.jsonObject ?: return@withContext emptyList()
            val feedbackList = data["feedback"]?.jsonArray ?: return@withContext emptyList()

            feedbackList.map { element ->
                val item = element.jsonObject

                val feedback = Feedback(
                    id = item["id"]?.jsonPrimitive?.long ?: 0,
                    category = FeedbackCategory.fromString(
                        item["category"]?.jsonPrimitive?.content ?: "other"
                    ),
                    feedbackText = item["feedback_text"]?.jsonPrimitive?.content ?: "",
                    status = FeedbackStatus.fromString(
                        item["status"]?.jsonPrimitive?.content ?: "pending"
                    ),
                    createdAt = item["created_at"]?.jsonPrimitive?.content ?: "",
                    updatedAt = item["updated_at"]?.jsonPrimitive?.content ?: "",
                    completionNote = item["completion_note"]?.jsonPrimitive?.content
                )

                val history = item["status_history"]?.jsonArray?.map { historyElement ->
                    val h = historyElement.jsonObject
                    FeedbackStatusChange(
                        oldStatus = h["old_status"]?.jsonPrimitive?.content?.let {
                            FeedbackStatus.fromString(it)
                        },
                        newStatus = FeedbackStatus.fromString(
                            h["new_status"]?.jsonPrimitive?.content ?: "pending"
                        ),
                        changedBy = h["changed_by"]?.jsonPrimitive?.content ?: "",
                        changedAt = h["changed_at"]?.jsonPrimitive?.content ?: "",
                        note = h["note"]?.jsonPrimitive?.content
                    )
                } ?: emptyList()

                FeedbackWithHistory(
                    feedback = feedback,
                    statusHistory = history
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun registerFcmToken(
        accessToken: String,
        email: String,
        appId: String,
        fcmToken: String,
        deviceInfo: Map<String, String>?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            require(accessToken.isNotBlank()) { "Sign in required" }
            val response = supabaseClient.functions.invoke("fcm-register-token") {
                headers[HttpHeaders.Authorization] = "Bearer $accessToken"
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("user_email", email)
                    put("app_id", appId)
                    put("fcm_token", fcmToken)
                    if (deviceInfo != null) {
                        putJsonObject("device_info") {
                            deviceInfo.forEach { (key, value) ->
                                put(key, value)
                            }
                        }
                    }
                }.toString())
            }

            response.status.value in 200..299
        } catch (e: Exception) {
            false
        }
    }
}
