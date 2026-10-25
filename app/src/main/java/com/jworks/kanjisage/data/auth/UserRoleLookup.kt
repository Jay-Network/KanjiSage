package com.jworks.kanjisage.data.auth

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Optional UI privilege lookup: unavailable roles must never prevent ordinary app use. */
internal suspend fun hasPrivilegedRole(loadRoles: suspend () -> String): Boolean {
    return try {
        val rows = Json.parseToJsonElement(loadRoles()) as? JsonArray
        val role = rows?.firstOrNull()?.jsonObject?.get("role")?.jsonPrimitive?.contentOrNull
        role == "admin" || role == "developer"
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        false
    }
}
