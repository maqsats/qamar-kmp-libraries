package com.qamar.quran.audio

import com.qamar.quran.audio.config.AudioResponseMode
import com.qamar.quran.audio.config.AudioSourceConfig
import com.qamar.quran.audio.config.QuranAudioConfig
import com.qamar.quran.audio.config.RecitersFormat
import com.qamar.quran.audio.internal.asArrayOrNull
import com.qamar.quran.audio.internal.asObjectOrNull
import com.qamar.quran.audio.internal.asStringOrNull
import com.qamar.quran.audio.internal.atPath
import com.qamar.quran.audio.internal.lastPathSegment
import com.qamar.quran.audio.internal.resolveTemplate
import com.qamar.quran.audio.internal.sanitizeFileName
import com.qamar.quran.audio.model.AudioFetchResult
import com.qamar.quran.audio.model.AudioRequest
import com.qamar.quran.audio.model.AudioUrlCandidate
import com.qamar.quran.audio.model.AudioUrlResult
import com.qamar.quran.audio.model.CachePolicy
import com.qamar.quran.audio.model.Reciter
import com.qamar.quran.audio.platform.AudioFileStore
import com.qamar.quran.audio.platform.platformHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

class QuranAudioClient(
    private val config: QuranAudioConfig,
    httpClient: HttpClient? = null,
    fileStore: AudioFileStore? = null,
    platformContext: Any? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : QuranAudioApi {
    private val ownsClient = httpClient == null
    private val client: HttpClient = httpClient ?: platformHttpClient()
    private val store: AudioFileStore = fileStore ?: AudioFileStore(platformContext)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowTrailingComma = true
    }

    override suspend fun getReciters(): List<Reciter> = withContext(dispatcher) {
        for (source in config.reciters) {
            val body = fetchText(source.url, source.headers) ?: continue
            val parsed = parseReciters(body, source.format)
            if (parsed.isNotEmpty()) return@withContext parsed
        }
        emptyList()
    }

    override suspend fun getAudioUrl(
        request: AudioRequest,
        validateUrls: Boolean,
    ): AudioUrlResult = withContext(dispatcher) {
        val validate = validateUrls || config.network.validateUrls
        val candidates = resolveCandidates(request)
        if (candidates.isEmpty()) {
            throw IllegalStateException("No audio candidates found for ${request.kind}")
        }
        val selected = selectCandidate(candidates, validate)
        AudioUrlResult(
            url = selected.url,
            candidates = candidates,
            usedSourceId = selected.sourceId,
        )
    }

    override suspend fun fetchAudio(
        request: AudioRequest,
        cachePolicy: CachePolicy?,
    ): AudioFetchResult = withContext(dispatcher) {
        val requestedPolicy = cachePolicy ?: config.cache.defaultPolicy
        val policy = if (config.cache.enabled) requestedPolicy else CachePolicy.REMOTE_ONLY
        val candidates = resolveCandidates(request)
        if (candidates.isEmpty()) {
            throw IllegalStateException("No audio candidates found for ${request.kind}")
        }
        if (policy == CachePolicy.REMOTE_ONLY || !store.supportsCache) {
            if (policy == CachePolicy.CACHE_ONLY && !store.supportsCache) {
                throw IllegalStateException("Caching is not supported on this platform")
            }
            val selected = selectCandidate(candidates, config.network.validateUrls)
            return@withContext AudioFetchResult(
                url = selected.url,
                localPath = null,
                candidates = candidates,
                usedSourceId = selected.sourceId,
                fromCache = false,
            )
        }

        for (candidate in candidates) {
            val path = buildCachePath(request, candidate.url) ?: continue
            if (store.exists(path)) {
                return@withContext AudioFetchResult(
                    url = candidate.url,
                    localPath = path,
                    candidates = candidates,
                    usedSourceId = candidate.sourceId,
                    fromCache = true,
                )
            }
        }

        for (candidate in candidates) {
            val path = buildCachePath(request, candidate.url) ?: continue
            if (downloadToPath(candidate.url, path)) {
                return@withContext AudioFetchResult(
                    url = candidate.url,
                    localPath = path,
                    candidates = candidates,
                    usedSourceId = candidate.sourceId,
                    fromCache = false,
                )
            }
        }

        if (policy == CachePolicy.CACHE_ONLY) {
            throw IllegalStateException("Failed to download audio for cache-only policy")
        }
        val selected = selectCandidate(candidates, config.network.validateUrls)
        AudioFetchResult(
            url = selected.url,
            localPath = null,
            candidates = candidates,
            usedSourceId = selected.sourceId,
            fromCache = false,
        )
    }

    fun close() {
        if (ownsClient) {
            client.close()
        }
    }

    private suspend fun resolveCandidates(request: AudioRequest): List<AudioUrlCandidate> {
        val sources = when (request.kind) {
            com.qamar.quran.audio.model.AudioKind.AYAH -> config.audio.ayah
            com.qamar.quran.audio.model.AudioKind.SURA -> config.audio.sura
            com.qamar.quran.audio.model.AudioKind.PAGE -> config.audio.page
        }
        val results = ArrayList<AudioUrlCandidate>()
        for (source in sources) {
            results.addAll(resolveFromSource(source, request))
        }
        return results
    }

    private suspend fun resolveFromSource(
        source: AudioSourceConfig,
        request: AudioRequest,
    ): List<AudioUrlCandidate> {
        return when (source) {
            is AudioSourceConfig.Template -> {
                val url = resolveTemplate(source.template, request, request.reciter)
                if (url.isBlank() || !looksLikeUrl(url)) emptyList()
                else listOf(AudioUrlCandidate(url = url, sourceId = source.id))
            }

            is AudioSourceConfig.JsonEndpoint -> {
                val endpoint = resolveTemplate(source.urlTemplate, request, request.reciter)
                if (endpoint.isBlank() || !looksLikeUrl(endpoint)) return emptyList()
                val body = fetchText(endpoint, source.headers) ?: return emptyList()
                val jsonElement = runCatching { json.parseToJsonElement(body) }.getOrNull()
                    ?: return emptyList()
                parseAudioResponse(
                    sourceId = source.id,
                    format = source.format,
                    root = jsonElement,
                    reciterId = request.reciter.id,
                )
            }
        }
    }

    private fun parseAudioResponse(
        sourceId: String,
        format: com.qamar.quran.audio.config.AudioResponseFormat,
        root: JsonElement,
        reciterId: String,
    ): List<AudioUrlCandidate> {
        val container = root.atPath(format.listPath) ?: root
        return when (format.mode) {
            AudioResponseMode.OBJECT_MAP -> {
                val obj = container.asObjectOrNull() ?: return emptyList()
                val entry = obj[reciterId]
                entry?.let { extractUrlsFromEntry(sourceId, format.urlFields, it) }.orEmpty()
            }

            AudioResponseMode.ARRAY -> {
                val array = container.asArrayOrNull() ?: return emptyList()
                val match = array.firstOrNull { element ->
                    val obj = element.asObjectOrNull() ?: return@firstOrNull false
                    val idValue =
                        obj[format.reciterIdField]?.asStringOrNull() ?: return@firstOrNull false
                    idValue == reciterId
                } ?: return emptyList()
                extractUrlsFromEntry(sourceId, format.urlFields, match)
            }
        }
    }

    private fun extractUrlsFromEntry(
        sourceId: String,
        urlFields: List<String>,
        entry: JsonElement,
    ): List<AudioUrlCandidate> {
        val urls = ArrayList<AudioUrlCandidate>()
        if (entry is kotlinx.serialization.json.JsonPrimitive) {
            val url = entry.asStringOrNull()
            if (!url.isNullOrBlank()) {
                urls.add(AudioUrlCandidate(url = url, sourceId = sourceId))
            }
            return urls
        }
        for (field in urlFields) {
            val value = entry.atPath(field)?.asStringOrNull()
            if (!value.isNullOrBlank()) {
                urls.add(
                    AudioUrlCandidate(
                        url = value,
                        sourceId = sourceId,
                        isOriginal = field.contains("original", ignoreCase = true),
                    ),
                )
            }
        }
        return urls
    }

    private suspend fun parseReciters(body: String, format: RecitersFormat): List<Reciter> {
        return when (format) {
            is RecitersFormat.TextLines -> parseTextReciters(body, format)
            is RecitersFormat.JsonMap -> parseJsonMapReciters(body, format)
            is RecitersFormat.JsonArray -> parseJsonArrayReciters(body, format)
        }
    }

    private fun parseTextReciters(body: String, format: RecitersFormat.TextLines): List<Reciter> {
        val lines = body.split("\n")
        val result = ArrayList<Reciter>()
        for (line in lines) {
            val raw = if (format.trim) line.trim() else line
            if (raw.isBlank() || raw.startsWith("#") || raw.startsWith("//")) continue
            val parts = raw.split(format.separator, limit = 2)
            if (parts.size < 2) continue
            val id = parts[0].trim()
            val name = parts[1].trim()
            if (id.isBlank() || name.isBlank()) continue
            result.add(Reciter(id = id, name = name))
        }
        return result
    }

    private fun parseJsonMapReciters(body: String, format: RecitersFormat.JsonMap): List<Reciter> {
        val root = runCatching { json.parseToJsonElement(body) }.getOrNull() ?: return emptyList()
        val obj = (root.atPath(format.listPath) ?: root).asObjectOrNull() ?: return emptyList()
        return obj.entries.mapNotNull { (id, value) ->
            val name = when (value) {
                is kotlinx.serialization.json.JsonPrimitive -> value.asStringOrNull()
                else -> value.atPath(format.namePath ?: "name")?.asStringOrNull()
            }?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val meta = format.meta.mapNotNull { (key, path) ->
                val metaValue = value.atPath(path)?.asStringOrNull()
                if (metaValue.isNullOrBlank()) null else key to metaValue
            }.toMap()
            Reciter(id = id.trim(), name = name.trim(), meta = meta)
        }
    }

    private fun parseJsonArrayReciters(
        body: String,
        format: RecitersFormat.JsonArray
    ): List<Reciter> {
        val root = runCatching { json.parseToJsonElement(body) }.getOrNull() ?: return emptyList()
        val array = (root.atPath(format.listPath) ?: root).asArrayOrNull() ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = element.asObjectOrNull() ?: return@mapNotNull null
            val id = obj[format.idField]?.asStringOrNull()?.trim() ?: return@mapNotNull null
            val name = obj[format.nameField]?.asStringOrNull()?.trim() ?: return@mapNotNull null
            val meta = format.meta.mapNotNull { (key, path) ->
                val metaValue = element.atPath(path)?.asStringOrNull()
                if (metaValue.isNullOrBlank()) null else key to metaValue
            }.toMap()
            Reciter(id = id.trim(), name = name.trim(), meta = meta)
        }
    }

    private suspend fun selectCandidate(
        candidates: List<AudioUrlCandidate>,
        validate: Boolean,
    ): AudioUrlCandidate {
        if (!validate) return candidates.first()
        for (candidate in candidates) {
            if (isUrlAccessible(candidate.url)) return candidate
        }
        return candidates.first()
    }

    private suspend fun fetchText(url: String, headers: Map<String, String>): String? {
        return try {
            val response = client.get(url) {
                headers {
                    headers.forEach { (key, value) -> append(key, value) }
                }
            }
            if (response.status.value !in 200..299) return null
            response.bodyAsText()
        } catch (_: Throwable) {
            null
        }
    }

    private suspend fun isUrlAccessible(url: String): Boolean {
        return try {
            val response = client.head(url)
            response.status.value in 200..299
        } catch (_: Throwable) {
            try {
                val response = client.get(url) {
                    headers { append("Range", "bytes=0-1") }
                }
                response.status == HttpStatusCode.PartialContent || response.status.value in 200..299
            } catch (_: Throwable) {
                false
            }
        }
    }

    private suspend fun downloadToPath(url: String, path: String): Boolean {
        val parent = parentDir(path)
        if (parent != null) {
            store.createDirectories(parent)
        }
        return try {
            val response = client.get(url)
            if (response.status.value !in 200..299) return false
            val bytes: ByteArray = response.body()
            if (bytes.isEmpty()) return false
            store.writeBytes(path, bytes)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun buildCachePath(request: AudioRequest, candidateUrl: String): String? {
        val baseDir = store.cacheDir(config.cache.directoryName) ?: return null
        val reciterDir = if (config.cache.includeReciterSubdir) {
            joinPath(baseDir, sanitizeFileName(request.reciter.id))
        } else baseDir
        val template = config.cache.fileNameTemplateByKind[request.kind]
        val fileNameFromTemplate =
            template?.let { resolveTemplate(it, request, request.reciter, candidateUrl) }
                ?.takeIf { it.isNotBlank() }
        val fileName = fileNameFromTemplate
            ?: if (config.cache.fallbackToLastPathSegment) lastPathSegment(candidateUrl) else null
        val safeName =
            fileName?.let { sanitizeFileName(it) }?.takeIf { it.isNotBlank() } ?: return null
        return joinPath(reciterDir, safeName)
    }

    private fun looksLikeUrl(value: String): Boolean =
        value.startsWith("http://") || value.startsWith("https://")

    private fun joinPath(base: String, child: String): String =
        base.trimEnd('/') + "/" + child.trimStart('/')

    private fun parentDir(path: String): String? {
        val index = path.lastIndexOf('/')
        if (index <= 0) return null
        return path.substring(0, index)
    }
}
