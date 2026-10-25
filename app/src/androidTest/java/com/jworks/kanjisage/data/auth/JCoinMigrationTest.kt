package com.jworks.kanjisage.data.auth

import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.platform.app.InstrumentationRegistry
import com.jworks.kanjisage.data.jcoin.JCoinClient
import com.jworks.kanjisage.di.AuthModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID

/** Explicitly opted-in emulator check, not part of routine connected tests.
 * Requires owner-confirmed fleet mailbox aliases and a temporary membership value.
 * Stages: preflight (local state + read-only health) -> signupA -> mailbox/link confirmation -> confirmA -> signupB ->
 * second mailbox/link confirmation -> probeB. Never repeat either signup stage.
 * Approval: jayhub:0 dlg-254d005632a54fb5aa188587, relayed by jayhub:110.
 * No service-role credentials, earn/spend calls, feedback submissions or deletions.
 */
class JCoinMigrationTest {
    private val http = HttpClient(Android) { expectSuccess = false }
    private val args = InstrumentationRegistry.getArguments()
    private val state by lazy {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(context, "jcoin_migration_20261025", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    }

    private class Fixture(
        val userId: String,
        val token: String,
        val customerId: String,
        val membership: String,
        val accountIds: List<String>
    )

    private fun record(probe: String, vararg fields: Pair<String, Any>) {
        val event = JSONObject().put("probe", probe)
        fields.forEach { (name, value) -> event.put(name, value) }
        Log.i("KSJCoinTest", event.toString())
    }

    private suspend fun request(
        label: String,
        method: HttpMethod,
        path: String,
        token: String = SupabaseConfig.SUPABASE_ANON_KEY,
        body: JSONObject? = null
    ): Pair<Int, String> {
        require(path.startsWith("/"))
        val response = http.request(SupabaseConfig.SUPABASE_URL + path) {
            this.method = method
            header("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
            header("Authorization", "Bearer $token")
            if (body != null) {
                header("Content-Type", "application/json")
                header("Prefer", "return=representation")
                setBody(body.toString())
            }
        }
        record(label, "http" to response.status.value)
        return response.status.value to response.bodyAsText()
    }

    private suspend fun rows(label: String, token: String, path: String): JSONArray {
        val (status, body) = request(label, HttpMethod.Get, path, token)
        assertEquals("$label HTTP", 200, status)
        val rows = JSONArray(body)
        record(label, "rows" to rows.length())
        return rows
    }

    private suspend fun signUp(label: String, email: String) {
        check(!state.getBoolean("attempted_$label", false)) {
            "$label signup already attempted; obtain owner direction before any rerun"
        }
        val password = UUID.randomUUID().toString() + UUID.randomUUID().toString()
        // Preserve the one approved signup if confirmation or a later check stops this run.
        // These values stay encrypted in the isolated emulator; never log or export them.
        check(state.edit().putBoolean("attempted_$label", true)
            .putString("signup_email_$label", email)
            .putString("signup_password_$label", password).commit())
        val (status, body) = request("signup_$label", HttpMethod.Post, "/auth/v1/signup",
            body = JSONObject().put("email", email)
                .put("password", password))
        assertTrue("signup_$label HTTP=$status", status == 200 || status == 201)
        val signup = JSONObject(body)
        val userId = signup.optJSONObject("user")?.optString("id") ?: signup.optString("id")
        UUID.fromString(userId)
        record("fixture_$label", "auth_user_id" to userId)
        val token = signup.optString("access_token")
        check(state.edit().putString("signup_user_id_$label", userId)
            .putString("signup_token_$label", token).commit())
        record("await_mailbox_confirmation", "fixture" to label,
            "session_returned" to token.isNotBlank())
    }

    private suspend fun confirmFixture(label: String, email: String): Fixture {
        check(state.getString("signup_email_$label", null) == email) { "Fixture identity mismatch" }
        val userId = checkNotNull(state.getString("signup_user_id_$label", null)) {
            "Missing prior signup; do not create another user"
        }
        val password = checkNotNull(state.getString("signup_password_$label", null))
        // W_Gmail/browser owners confirm the delivered link before this stage.
        // A fresh password session also avoids reusing an expired token between stages.
        val (status, body) = request("sign_in_$label", HttpMethod.Post,
            "/auth/v1/token?grant_type=password",
            body = JSONObject().put("email", email).put("password", password))
        assertEquals("Confirmed signup sign-in HTTP", 200, status)
        val session = JSONObject(body)
        assertEquals("Confirmed fixture user", userId, session.getJSONObject("user").getString("id"))
        val token = session.getString("access_token")
        check(token.isNotBlank())
        check(state.edit().putString("signup_token_$label", token).commit())

        val functionsClient = AuthModule.provideJCoinSupabaseClient()
        val balance = try {
            JCoinClient(functionsClient).getBalance(token).getOrThrow()
        } finally {
            functionsClient.close()
        }
        UUID.fromString(balance.customerId)
        val customer = rows("customer_$label", token,
            "/rest/v1/customers?select=id,auth_user_id,membership_type&auth_user_id=eq.$userId")
        assertEquals("Exactly one own customer", 1, customer.length())
        val own = customer.getJSONObject(0)
        assertEquals(userId, own.getString("auth_user_id"))
        assertEquals(balance.customerId, own.getString("id"))
        record("fixture_$label", "auth_user_id" to userId, "customer_id" to balance.customerId)
        val accounts = rows("own_accounts_$label", token,
            "/rest/v1/coin_accounts?select=id&customer_id=eq.${balance.customerId}")
        check(accounts.length() > 0) { "$label has no account positive control; stop" }
        val ids = (0 until accounts.length()).map { accounts.getJSONObject(it).getString("id") }
        ids.forEach { UUID.fromString(it) }
        val fixture = Fixture(userId, token, balance.customerId, own.getString("membership_type"), ids)
        // Tokens remain inside the emulator's encrypted application sandbox between stages.
        val saved = JSONObject().put("user_id", userId).put("token", token)
            .put("customer_id", fixture.customerId).put("membership", fixture.membership)
            .put("accounts", JSONArray(ids)).put("email", email)
        check(state.edit().putString("fixture_$label", saved.toString()).commit())
        return fixture
    }

    private fun loadFixture(label: String, email: String): Fixture {
        val saved = JSONObject(checkNotNull(state.getString("fixture_$label", null)) {
            "Missing prior fixture; do not repeat signup"
        })
        check(saved.getString("email") == email) { "Fixture identity mismatch" }
        val ids = saved.getJSONArray("accounts")
        return Fixture(saved.getString("user_id"), saved.getString("token"),
            saved.getString("customer_id"), saved.getString("membership"),
            (0 until ids.length()).map { ids.getString(it) })
    }

    private fun transactionPath(fixture: Fixture) =
        "/rest/v1/coin_transactions?select=id&account_id=in.(${fixture.accountIds.joinToString(",")})"

    private suspend fun transactions(label: String, fixture: Fixture): Set<String> {
        val result = rows(label, fixture.token, transactionPath(fixture))
        return (0 until result.length()).map { result.getJSONObject(it).getString("id") }.toSet()
    }

    private suspend fun membership(label: String, fixture: Fixture): String =
        rows(label, fixture.token,
            "/rest/v1/customers?select=membership_type&id=eq.${fixture.customerId}")
            .getJSONObject(0).getString("membership_type")

    @Test
    fun approvedPublicSignupAndIsolationCheck() = runBlocking {
        assumeTrue(args.getString("allowLiveJCoinWrites") == "dlg-254d005632a54fb5aa188587")
        check(android.os.Build.FINGERPRINT.contains("generic") ||
            android.os.Build.MODEL.contains("Emulator") || android.os.Build.MODEL.contains("sdk_gphone")) {
            "Emulator only"
        }
        val mailboxDomain = requireNotNull(args.getString("approvedMailboxDomain"))
        val emailA = requireNotNull(args.getString("testEmailA"))
        val emailB = requireNotNull(args.getString("testEmailB"))
        check(emailA != emailB && listOf(emailA, emailB).all { it.substringAfter('@') == mailboxDomain })
        val probeMembership = requireNotNull(args.getString("probeMembership"))
        val stage = requireNotNull(args.getString("stage"))
        check(stage in setOf("preflight", "signupA", "confirmA", "signupB", "probeB"))
        try {
            if (stage == "preflight") {
                val attemptedA = state.getBoolean("attempted_A", false)
                record("preflight_state", "ready" to true, "signup_A_attempted" to attemptedA)
                val (status, _) = request("preflight_health", HttpMethod.Get, "/auth/v1/health")
                assertEquals("Auth health HTTP", 200, status)
                record("preflight_complete", "remote_writes" to false)
                return@runBlocking
            }
            if (stage == "signupA") {
                signUp("A", emailA)
                record("await_mailbox_receipt", "next_stage" to "confirmA")
                return@runBlocking
            }
            check(!args.getString("mailboxReceipt").isNullOrBlank()) {
                "W_Gmail must confirm first-signup delivery before continuing"
            }
            if (stage == "confirmA") {
                confirmFixture("A", emailA)
                record("fixture_A_ready", "next_stage" to "signupB")
                return@runBlocking
            }
            if (stage == "signupB") {
                loadFixture("A", emailA)
                signUp("B", emailB)
                record("await_mailbox_receipt_B", "next_stage" to "probeB")
                return@runBlocking
            }
            check(!args.getString("mailboxReceiptB").isNullOrBlank()) {
                "W_Gmail must confirm second-signup delivery before the isolation probes"
            }
            val a = confirmFixture("A", emailA)
            val b = confirmFixture("B", emailB)
            val beforeA = transactions("transactions_A_before", a)
            val beforeB = transactions("transactions_B_before", b)
            val roles = rows("user_roles_A", a.token, "/rest/v1/user_roles?select=role")
            assertEquals("New test user must have no privileged role", 0, roles.length())
            val crossAccounts = rows("A_reads_B_accounts", a.token,
                "/rest/v1/coin_accounts?select=id&customer_id=eq.${b.customerId}")
            val crossTransactions = rows("A_reads_B_transactions", a.token, transactionPath(b))
            record("transaction_positive_control", "B_rows" to beforeB.size,
                "coverage" to if (beforeB.isEmpty()) "EMPTY_FIXTURE_NOT_ISOLATION_PROOF" else "PRESENT")
            check(probeMembership != a.membership) { "Probe value must differ from the original" }

            var crossPatchRows = -1
            var ownPatchRows = -1
            var after = a.membership
            var ownPatchAttempted = false
            try {
                val cross = request("A_patches_B", HttpMethod.Patch,
                    "/rest/v1/customers?id=eq.${b.customerId}&select=id", a.token,
                    JSONObject().put("membership_type", b.membership))
                assertEquals("Cross PATCH HTTP", 200, cross.first)
                crossPatchRows = JSONArray(cross.second).length()
                record("A_patches_B", "expected_rows" to 0, "actual_rows" to crossPatchRows)
                assertEquals("B membership unchanged", b.membership, membership("B_after_cross_patch", b))
                ownPatchAttempted = true
                val own = request("A_patches_own_membership", HttpMethod.Patch,
                    "/rest/v1/customers?id=eq.${a.customerId}&select=id", a.token,
                    JSONObject().put("membership_type", probeMembership))
                assertEquals("Own PATCH HTTP", 200, own.first)
                ownPatchRows = JSONArray(own.second).length()
                after = membership("A_after_own_patch", a)
                record("A_patches_own_membership", "expected_rows" to 1, "actual_rows" to ownPatchRows,
                    "before" to a.membership, "after" to after)
            } finally {
                val restore = if (ownPatchAttempted) request("A_restore_membership", HttpMethod.Patch,
                    "/rest/v1/customers?id=eq.${a.customerId}&select=id", a.token,
                    JSONObject().put("membership_type", a.membership)) else 200 to "[]"
                val restored = membership("A_restored_readback", a)
                val afterA = transactions("transactions_A_after", a)
                val afterB = transactions("transactions_B_after", b)
                record("restore_and_side_effects", "restore_http" to restore.first,
                    "before" to a.membership, "after" to after, "reverted" to restored,
                    "A_new_transaction_rows" to (afterA - beforeA).size,
                    "B_new_transaction_rows" to (afterB - beforeB).size)
                assertEquals("Restore HTTP", 200, restore.first)
                assertEquals("Membership restored", a.membership, restored)
                assertEquals("A ledger unchanged", beforeA, afterA)
                assertEquals("B ledger unchanged", beforeB, afterB)
            }
            assertEquals("Cross-account read", 0, crossAccounts.length())
            assertEquals("Cross-transaction read", 0, crossTransactions.length())
            assertEquals("Cross-customer PATCH", 0, crossPatchRows)
            assertEquals("Known PL-17 self-write reproduced", 1, ownPatchRows)
            assertEquals(probeMembership, after)
            record("complete", "known_self_write_reproduced" to true)
        } catch (failure: Exception) {
            val frames = failure.stackTrace.take(3).map {
                "${it.className}.${it.methodName}:${it.lineNumber}"
            }
            record("stopped", "error_class" to failure.javaClass.simpleName,
                "frames" to JSONArray(frames))
            throw AssertionError("Live check stopped; consult redacted KSJCoinTest events, not response bodies")
        } finally {
            http.close()
        }
    }
}
