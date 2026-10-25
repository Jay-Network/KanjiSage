package com.jworks.kanjisage.data.feedback

import com.jworks.kanjisage.domain.models.FeedbackCategory
import com.jworks.kanjisage.domain.models.SubmitFeedbackResult
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackRepositoryImplTest {
    private val email = "fixture+one@example.test"

    @Test
    fun submissionSendsExactlyOneUserBearerAndPreservesBody() = runBlocking {
        withRepository { repository, requests ->
            val result = repository.submitFeedback(
                "tok", email, "test-app", FeedbackCategory.BUG, "Offline test feedback"
            )
            assertTrue(result is SubmitFeedbackResult.Success)
            assertEquals(7L, (result as SubmitFeedbackResult.Success).feedbackId)
            val request = assertUserBearer(requests, "feedback-submit")
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals(email, body.getValue("user_email").jsonPrimitive.content)
            assertEquals("bug", body.getValue("category").jsonPrimitive.content)
            assertEquals("Offline test feedback", body.getValue("feedback_text").jsonPrimitive.content)
        }
    }

    @Test
    fun updatesSendUserBearerAndEncodeEachQueryValue() = runBlocking {
        withRepository { repository, requests ->
            val result = repository.getFeedbackUpdates("tok", email, "test app&one", 6L)
            assertEquals(7L, result.single().feedback.id)
            val request = assertUserBearer(requests, "feedback-get-updates")
            assertEquals(email, request.url.parameters["user_email"])
            assertEquals("test app&one", request.url.parameters["app_id"])
            assertEquals("6", request.url.parameters["since_id"])
            assertEquals(setOf("user_email", "app_id", "since_id"), request.url.parameters.names())
            assertTrue(request.url.encodedQuery.contains("%2B", ignoreCase = true))
        }
    }

    @Test
    fun fcmRegistrationSendsUserBearerAndPreservesDeviceToken() = runBlocking {
        withRepository { repository, requests ->
            assertTrue(repository.registerFcmToken("tok", email, "test-app", "fake-fcm-token"))
            val request = assertUserBearer(requests, "fcm-register-token")
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals(email, body.getValue("user_email").jsonPrimitive.content)
            assertEquals("fake-fcm-token", body.getValue("fcm_token").jsonPrimitive.content)
        }
    }

    @Test
    fun blankUserTokensNeverSendAnonymousIdentityRequests() = runBlocking {
        withRepository { repository, requests ->
            assertTrue(repository.submitFeedback(
                " ", email, "test-app", FeedbackCategory.BUG, "Offline test feedback"
            ) is SubmitFeedbackResult.Error)
            assertTrue(repository.getFeedbackUpdates("", email, "test-app").isEmpty())
            assertFalse(repository.registerFcmToken("", email, "test-app", "fake-fcm-token"))
            assertTrue(requests.isEmpty())
        }
    }

    private fun assertUserBearer(requests: List<HttpRequestData>, function: String): HttpRequestData {
        val request = requests.single()
        assertEquals("/functions/v1/$function", request.url.encodedPath)
        assertEquals(HttpMethod.Post, request.method)
        assertEquals(listOf("Bearer tok"), request.headers.getAll(HttpHeaders.Authorization))
        return request
    }

    private suspend fun withRepository(
        check: suspend (FeedbackRepositoryImpl, List<HttpRequestData>) -> Unit
    ) {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            respond(
                """{"success":true,"data":{"feedback_id":7,"created_at":"test-time","feedback":[{"id":7,"feedback_text":"test"}]}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val supabase = createSupabaseClient("https://example.test", "test-anon-key") {
            httpEngine = engine
            install(Functions)
        }
        try {
            check(FeedbackRepositoryImpl(supabase), requests)
        } finally {
            supabase.close()
        }
    }
}
