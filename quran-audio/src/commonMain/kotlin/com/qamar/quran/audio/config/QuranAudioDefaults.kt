package com.qamar.quran.audio.config

import com.qamar.quran.audio.model.AudioKind

/**
 * Default configs for known Quran audio providers. The application can replace
 * these with its own config (e.g. from remote config) so reciter list and audio
 * URLs keep working when providers or formats change.
 */
object QuranAudioDefaults {

    /**
     * Config for quranapi.pages.dev (reciters + verse/chapter JSON) and fallbacks.
     * Reciters: GET reciters.json → `{"1": "Name", "2": "Name", ...}`.
     * Verse audio: GET audio/{sura}/{ayah}.json → reciter id → `{ "url", "originalUrl" }`.
     * Chapter audio: GET audio/{sura}.json → same shape. Audio files may be served
     * from the-quran-project.github.io or original URLs; config does not hardcode a single host.
     */
    fun quranApi(): QuranAudioConfig {
        val reciters = listOf(
            RecitersSourceConfig(
                id = "quranapi",
                url = "https://quranapi.pages.dev/api/reciters.json",
                format = RecitersFormat.JsonMap(),
            ),
            RecitersSourceConfig(
                id = "mp3quran",
                url = "https://www.mp3quran.net/api/v3/reciters?language=eng",
                format = RecitersFormat.JsonArray(
                    listPath = "reciters",
                    idField = "id",
                    nameField = "name",
                    meta = mapOf(
                        "server" to "moshaf[0].server",
                        "surah_list" to "moshaf[0].surah_list",
                    ),
                ),
            ),
        )

        val ayahSources = listOf(
            AudioSourceConfig.JsonEndpoint(
                id = "quranapi-ayah",
                kind = AudioKind.AYAH,
                urlTemplate = "https://quranapi.pages.dev/api/audio/{sura}/{ayah}.json",
                format = AudioResponseFormat(
                    mode = AudioResponseMode.OBJECT_MAP,
                    urlFields = listOf("originalUrl", "url"),
                ),
            ),
            AudioSourceConfig.Template(
                id = "quranapi-direct",
                kind = AudioKind.AYAH,
                template = "https://the-quran-project.github.io/Quran-Audio/Data/{reciterId}/{sura}_{ayah}.mp3",
            ),
            AudioSourceConfig.Template(
                id = "legacy-pages",
                kind = AudioKind.AYAH,
                template = "https://quranaudio.pages.dev/{reciterId}/{sura}_{ayah}.mp3",
            ),
        )

        val suraSources = listOf(
            AudioSourceConfig.JsonEndpoint(
                id = "quranapi-sura",
                kind = AudioKind.SURA,
                urlTemplate = "https://quranapi.pages.dev/api/audio/{sura}.json",
                format = AudioResponseFormat(
                    mode = AudioResponseMode.OBJECT_MAP,
                    urlFields = listOf("originalUrl", "url"),
                ),
            ),
            AudioSourceConfig.Template(
                id = "mp3quran-server",
                kind = AudioKind.SURA,
                template = "{server}{sura:pad3}.mp3",
            ),
        )

        return QuranAudioConfig(
            reciters = reciters,
            audio = AudioSourceSet(
                ayah = ayahSources,
                sura = suraSources,
                page = emptyList(),
            ),
        )
    }
}
