package com.qamar.quran.audio.internal

import com.qamar.quran.audio.model.AudioRequest
import com.qamar.quran.audio.model.Reciter

private val PLACEHOLDER_REGEX = Regex("\\{([A-Za-z0-9_.-]+)(?::pad=?([0-9]+))?\\}")

internal fun resolveTemplate(
    template: String,
    request: AudioRequest,
    reciter: Reciter,
    url: String? = null,
): String {
    if (template.isBlank()) return ""
    val placeholders = buildPlaceholderMap(request, reciter, url)
    return PLACEHOLDER_REGEX.replace(template) { match ->
        val key = match.groupValues[1]
        val pad = match.groupValues[2].toIntOrNull()
        val raw = placeholders[key] ?: ""
        if (pad != null) raw.padStart(pad, '0') else raw
    }
}

internal fun buildPlaceholderMap(
    request: AudioRequest,
    reciter: Reciter,
    url: String? = null,
): Map<String, String> {
    val values = mutableMapOf<String, String>()
    values["reciterId"] = reciter.id
    values["reciterName"] = reciter.name
    values["kind"] = request.kind.name.lowercase()
    when (request) {
        is AudioRequest.Ayah -> {
            values["sura"] = request.sura.toString()
            values["ayah"] = request.ayah.toString()
        }
        is AudioRequest.Sura -> {
            values["sura"] = request.sura.toString()
        }
        is AudioRequest.Page -> {
            values["page"] = request.page.toString()
        }
    }
    reciter.meta.forEach { (key, value) ->
        if (key.isNotBlank() && key !in values) {
            values[key] = value
        }
    }
    values["ext"] = extensionFromUrl(url) ?: "mp3"
    return values
}

internal fun extensionFromUrl(url: String?): String? {
    val lastSegment = lastPathSegment(url) ?: return null
    val dot = lastSegment.lastIndexOf('.')
    if (dot <= 0 || dot == lastSegment.lastIndex) return null
    return lastSegment.substring(dot + 1)
}

internal fun lastPathSegment(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val cleaned = url.substringBefore('?').substringBefore('#')
    val index = cleaned.lastIndexOf('/')
    val segment = if (index >= 0) cleaned.substring(index + 1) else cleaned
    return segment.takeIf { it.isNotBlank() }
}

internal fun sanitizeFileName(name: String): String =
    name.replace("/", "_").replace("\\\\", "_").trim()
