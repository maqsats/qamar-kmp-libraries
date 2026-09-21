package com.qamar.quran.translations

import com.qamar.quran.core.resource.ResourceReader
import com.qamar.quran.translations.model.TranslationInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Reads the catalog of downloadable translations.
 *
 * The same document exists in two places, and they are not interchangeable:
 *
 *  - the **bundled** copy, `translations.json` inside this library's artifact. It
 *    ships inside the APK, the .app and the desktop jar, so changing it needs a
 *    store release. It is the floor: always present, never absent, good enough to
 *    run on.
 *  - the **hosted** copy at [DefaultRemoteCatalogUrl], served by Firebase Hosting
 *    from the same file staged into `firebase/web`. Changing it is one
 *    `firebase deploy --only hosting` and it reaches every platform - phones
 *    included - with no app release at all.
 *
 * [DefaultTranslationManager] prefers the hosted copy and falls back to the
 * bundled one, so the two must be kept identical in the repo: the bundled copy is
 * the seed a fresh install runs on until its first successful fetch, and a user
 * whose app is older than the last deploy should see the same list either way.
 */
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

    /** The copy that shipped inside the app. Throws if it is missing. */
    fun loadBundled(): List<TranslationInfo> = parse(resourceReader.readText(CatalogFileName))

    /** Parses a catalog document. Throws if [raw] is not one. */
    fun parse(raw: String): List<TranslationInfo> {
        val wrapper = json.decodeFromString(Wrapper.serializer(), raw)
        return wrapper.data.ifEmpty { wrapper.result.orEmpty() }
    }

    companion object {
        const val CatalogFileName: String = "translations.json"

        /**
         * Where the runtime catalog is fetched from: the `qamar` Hosting site,
         * which serves this file with `no-cache, must-revalidate`, so a deploy is
         * visible on the next fetch instead of after a CDN TTL.
         *
         * It is the very same URL the web build already reads its catalog from,
         * which is what makes one deploy enough for all five targets.
         */
        const val DefaultRemoteCatalogUrl: String = "https://qamar.web.app/translations.json"

        /**
         * Whether a fetched catalog may replace the bundled one.
         *
         * A truncated upload or a half-written deploy must never be able to empty
         * the translation list or strand a user on a row with no download URL, so
         * a fetched document is accepted only when every entry is complete.
         * Anything less falls back to what already works rather than degrading it.
         *
         * Note this deliberately does NOT require the fetched catalog to be as
         * large as the bundled one: retiring an edition is a legitimate reason for
         * it to shrink (see RetiredQuranTranslations in the app).
         */
        fun isUsable(entries: List<TranslationInfo>): Boolean =
            entries.isNotEmpty() && entries.all { entry ->
                entry.fileName.endsWith(".db") &&
                    entry.fileUrl.isNotBlank() &&
                    entry.languageCode.isNotBlank() &&
                    entry.displayName.isNotBlank()
            }
    }
}
