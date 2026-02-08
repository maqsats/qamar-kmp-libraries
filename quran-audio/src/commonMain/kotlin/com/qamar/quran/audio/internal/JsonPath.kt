package com.qamar.quran.audio.internal

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal fun JsonElement.atPath(path: String?): JsonElement? {
    if (path.isNullOrBlank()) return this
    var current: JsonElement? = this
    val segments = path.split('.').filter { it.isNotBlank() }
    for (segment in segments) {
        current = current?.navigate(segment) ?: return null
    }
    return current
}

private fun JsonElement.navigate(segment: String): JsonElement? {
    if (segment.isBlank()) return this
    val matcher = Regex("([^\\[]+)?(?:\\[(\\d+)\\])?").matchEntire(segment)
    if (matcher == null) return null
    val name = matcher.groupValues[1]
    val indexText = matcher.groupValues[2]
    var current: JsonElement = this
    if (name.isNotBlank()) {
        val obj = current as? JsonObject ?: return null
        current = obj[name] ?: return null
    }
    if (indexText.isNotBlank()) {
        val index = indexText.toIntOrNull() ?: return null
        val array = current as? JsonArray ?: return null
        current = array.getOrNull(index) ?: return null
    }
    return current
}

internal fun JsonElement.asStringOrNull(): String? =
    (this as? JsonPrimitive)?.content

internal fun JsonElement.asObjectOrNull(): JsonObject? = this as? JsonObject

internal fun JsonElement.asArrayOrNull(): JsonArray? = this as? JsonArray
