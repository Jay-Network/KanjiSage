package com.jworks.kanjisage.data.jcoin

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import io.ktor.http.content.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JCoinClientTest {
    @Test
    fun blankUserTokensNeverSendARequest() = runBlocking {
        var requestCount = 0
        val engine = MockEngine {
            requestCount++
            respond("{}", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val supabase = createSupabaseClient("https://example.test", "test-anon-key") {
            httpEngine = engine
            install(Functions)
        }
        try {
            val client = JCoinClient(supabase)
            assertTrue(client.getBalance(" ").isFailure)
            assertTrue(client.earn("", "test-source", 1).isFailure)
            assertTrue(client.spend("", "test-source", 1, "offline test").isFailure)
            assertEquals(0, requestCount)
        } finally {
            supabase.close()
        }
    }

    @Test
    fun balanceSendsExactlyOneUserBearer() = runBlocking {
        assertUserBearer("jcoin-unified-balance") { it.getBalance("tok").getOrThrow() }
    }

    @Test
    fun earnSendsExactlyOneUserBearer() = runBlocking {
        assertUserBearer("jcoin-unified-earn") {
            it.earn("tok", "test-source", 1).getOrThrow()
        }
    }

    @Test
    fun spendSendsExactlyOneUserBearer() = runBlocking {
        assertUserBearer("jcoin-unified-spend") {
            it.spend("tok", "test-source", 1, "offline test").getOrThrow()
        }
    }

    private suspend fun assertUserBearer(
        function: String,
        call: suspend (JCoinClient) -> Unit
    ) {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            respond(
                """{"success":true,"data":{"customer_id":"00000000-0000-0000-0000-000000000001"}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        // Match AuthModule's J Coin client: Functions only, no Auth session.
        val supabase = createSupabaseClient("https://example.test", "test-anon-key") {
            httpEngine = engine
            install(Functions)
        }
        try {
            call(JCoinClient(supabase))
            val request = requests.single()
            assertEquals("/functions/v1/$function", request.url.encodedPath)
            assertEquals(HttpMethod.Post, request.method)
            assertEquals(listOf("Bearer tok"), request.headers.getAll(HttpHeaders.Authorization))
            val payload = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals("kanjisage", payload.getValue("source_business").jsonPrimitive.content)
            when (function) {
                "jcoin-unified-earn" -> {
                    assertEquals("test-source", payload.getValue("source_type").jsonPrimitive.content)
                    assertEquals("1", payload.getValue("base_amount").jsonPrimitive.content)
                }
                "jcoin-unified-spend" -> {
                    assertEquals("direct", payload.getValue("mode").jsonPrimitive.content)
                    assertEquals("1", payload.getValue("amount").jsonPrimitive.content)
                    assertEquals("offline test", payload.getValue("description").jsonPrimitive.content)
                }
            }
        } finally {
            supabase.close()
        }
    }
}
