package com.qamar.quran.audio.internal

import com.qamar.quran.audio.config.AudioResponseFormat
import com.qamar.quran.audio.config.AudioResponseMode
import com.qamar.quran.audio.config.AudioSourceConfig
import com.qamar.quran.audio.config.AudioSourceSet
import com.qamar.quran.audio.config.CacheConfig
import com.qamar.quran.audio.config.NetworkConfig
import com.qamar.quran.audio.config.QuranAudioConfig
import com.qamar.quran.audio.config.RecitersFormat
import com.qamar.quran.audio.config.RecitersSourceConfig
import com.qamar.quran.audio.model.AudioKind
import com.qamar.quran.audio.model.CachePolicy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal object QuranAudioConfigParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowTrailingComma = true
    }

    fun fromJson(text: String): QuranAudioConfig {
        val root = json.parseToJsonElement(text)
        val obj = root as? JsonObject ?: return QuranAudioConfig(emptyList(), AudioSourceSet())
        val reciters = (obj["reciters"] as? JsonArray)?.mapNotNull { parseReciterSource(it) }
            ?: emptyList()
        val audioObj = obj["audio"] as? JsonObject
        val ayahSources = parseAudioSources(audioObj?.get("ayah"), AudioKind.AYAH)
        val suraSources = parseAudioSources(audioObj?.get("sura"), AudioKind.SURA)
        val pageSources = parseAudioSources(audioObj?.get("page"), AudioKind.PAGE)
        val cacheConfig = parseCacheConfig(obj["cache"])
        val networkConfig = parseNetworkConfig(obj["network"])
        return QuranAudioConfig(
            reciters = reciters,
            audio = AudioSourceSet(
                ayah = ayahSources,
                sura = suraSources,
                page = pageSources,
            ),
            cache = cacheConfig,
            network = networkConfig,
        )
    }

    private fun parseReciterSource(element: JsonElement): RecitersSourceConfig? {
        val obj = element as? JsonObject ?: return null
        val id = obj.string("id") ?: return null
        val url = obj.string("url") ?: return null
        val formatObj = obj["format"] as? JsonObject ?: return null
        val formatType = formatObj.string("type")?.lowercase()
        val format: RecitersFormat = when (formatType) {
            "json_map", "jsonmap", "map" -> RecitersFormat.JsonMap(
                listPath = formatObj.string("listPath"),
                namePath = formatObj.string("namePath"),
                meta = formatObj.objectMap("meta"),
            )
            "json_array", "jsonarray", "array" -> RecitersFormat.JsonArray(
                listPath = formatObj.string("listPath"),
                idField = formatObj.string("idField") ?: "id",
                nameField = formatObj.string("nameField") ?: "name",
                meta = formatObj.objectMap("meta"),
            )
            "text", "text_lines", "textlines" -> RecitersFormat.TextLines(
                separator = formatObj.string("separator") ?: ":",
                trim = formatObj.boolean("trim") ?: true,
            )
            else -> return null
        }
        val headers = obj.objectMap("headers")
        return RecitersSourceConfig(
            id = id,
            url = url,
            format = format,
            headers = headers,
        )
    }

    private fun parseAudioSources(element: JsonElement?, kind: AudioKind): List<AudioSourceConfig> {
        val array = element as? JsonArray ?: return emptyList()
        return array.mapNotNullIndexed { index, item ->
            val obj = item as? JsonObject ?: return@mapNotNullIndexed null
            val type = obj.string("type")?.lowercase() ?: return@mapNotNullIndexed null
            val id = obj.string("id") ?: "$type-$index"
            when (type) {
                "template" -> {
                    val template = obj.string("template") ?: return@mapNotNullIndexed null
                    AudioSourceConfig.Template(
                        id = id,
                        kind = kind,
                        template = template,
                    )
                }
                "json_endpoint", "json" -> {
                    val urlTemplate = obj.string("urlTemplate") ?: return@mapNotNullIndexed null
                    val format = parseAudioResponseFormat(obj["format"]) ?: AudioResponseFormat()
                    val headers = obj.objectMap("headers")
                    AudioSourceConfig.JsonEndpoint(
                        id = id,
                        kind = kind,
                        urlTemplate = urlTemplate,
                        format = format,
                        headers = headers,
                    )
                }
                else -> null
            }
        }
    }

    private fun parseAudioResponseFormat(element: JsonElement?): AudioResponseFormat? {
        val obj = element as? JsonObject ?: return null
        val mode = when (obj.string("mode")?.lowercase()) {
            "array" -> AudioResponseMode.ARRAY
            else -> AudioResponseMode.OBJECT_MAP
        }
        val listPath = obj.string("listPath")
        val reciterIdField = obj.string("reciterIdField") ?: "id"
        val urlFields = obj.stringList("urlFields") ?: listOf("originalUrl", "url")
        return AudioResponseFormat(
            mode = mode,
            listPath = listPath,
            reciterIdField = reciterIdField,
            urlFields = urlFields,
        )
    }

    private fun parseCacheConfig(element: JsonElement?): CacheConfig {
        val obj = element as? JsonObject ?: return CacheConfig()
        val templates = obj["fileNameTemplateByKind"] as? JsonObject
        val parsedTemplates = templates?.entries?.mapNotNull { (key, value) ->
            val kind = runCatching { AudioKind.valueOf(key.uppercase()) }.getOrNull() ?: return@mapNotNull null
            val template = (value as? JsonPrimitive)?.content ?: return@mapNotNull null
            kind to template
        }?.toMap()
        val policy = when (obj.string("defaultPolicy")?.uppercase()) {
            "REMOTE_ONLY" -> CachePolicy.REMOTE_ONLY
            "CACHE_ONLY" -> CachePolicy.CACHE_ONLY
            "CACHE_IF_POSSIBLE" -> CachePolicy.CACHE_IF_POSSIBLE
            else -> CachePolicy.CACHE_IF_POSSIBLE
        }
        return CacheConfig(
            enabled = obj.boolean("enabled") ?: true,
            directoryName = obj.string("directoryName") ?: "quran_audio",
            includeReciterSubdir = obj.boolean("includeReciterSubdir") ?: true,
            fileNameTemplateByKind = parsedTemplates ?: CacheConfig.DEFAULT_TEMPLATES,
            fallbackToLastPathSegment = obj.boolean("fallbackToLastPathSegment") ?: true,
            defaultPolicy = policy,
        )
    }

    private fun parseNetworkConfig(element: JsonElement?): NetworkConfig {
        val obj = element as? JsonObject ?: return NetworkConfig()
        return NetworkConfig(
            validateUrls = obj.boolean("validateUrls") ?: false,
            connectTimeoutMs = obj.long("connectTimeoutMs") ?: 15_000,
            requestTimeoutMs = obj.long("requestTimeoutMs") ?: 30_000,
        )
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.content

    private fun JsonObject.boolean(key: String): Boolean? = (this[key] as? JsonPrimitive)?.content?.toBooleanStrictOrNull()

    private fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.content?.toLongOrNull()

    private fun JsonObject.stringList(key: String): List<String>? {
        val array = this[key] as? JsonArray ?: return null
        return array.mapNotNull { (it as? JsonPrimitive)?.content }
    }

    private fun JsonObject.objectMap(key: String): Map<String, String> {
        val obj = this[key] as? JsonObject ?: return emptyMap()
        return obj.entries.mapNotNull { (field, value) ->
            val text = (value as? JsonPrimitive)?.content ?: return@mapNotNull null
            field to text
        }.toMap()
    }

    private inline fun <T> JsonArray.mapNotNullIndexed(transform: (index: Int, element: JsonElement) -> T?): List<T> {
        val result = ArrayList<T>(size)
        for (i in indices) {
            val value = transform(i, this[i])
            if (value != null) result.add(value)
        }
        return result
    }
}
