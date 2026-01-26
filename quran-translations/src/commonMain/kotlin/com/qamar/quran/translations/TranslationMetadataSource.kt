package com.qamar.quran.translations

import com.qamar.quran.core.resource.ResourceReader
import com.qamar.quran.translations.model.TranslationInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class TranslationMetadataSource(
    platformContext: Any? = null,
    private val resourceReader: ResourceReader = ResourceReader(platformContext),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    @Serializable
    private data class Wrapper(
        val data: List<TranslationInfo> = emptyList(),
        @SerialName("result") val result: List<TranslationInfo>? = null,
    )

    fun loadBundled(): List<TranslationInfo> {
        val raw = resourceReader.readText("translations.json")
        val wrapper = json.decodeFromString(Wrapper.serializer(), raw)
        return wrapper.data.ifEmpty { wrapper.result.orEmpty() }
    }
}
