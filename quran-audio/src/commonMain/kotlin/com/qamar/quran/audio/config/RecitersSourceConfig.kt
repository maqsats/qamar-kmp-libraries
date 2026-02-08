package com.qamar.quran.audio.config

data class RecitersSourceConfig(
    val id: String,
    val url: String,
    val format: RecitersFormat,
    val headers: Map<String, String> = emptyMap(),
)

sealed class RecitersFormat {
    data class JsonMap(
        val listPath: String? = null,
        val namePath: String? = null,
        val meta: Map<String, String> = emptyMap(),
    ) : RecitersFormat()

    data class JsonArray(
        val listPath: String? = null,
        val idField: String = "id",
        val nameField: String = "name",
        val meta: Map<String, String> = emptyMap(),
    ) : RecitersFormat()

    data class TextLines(
        val separator: String = ":",
        val trim: Boolean = true,
    ) : RecitersFormat()
}
