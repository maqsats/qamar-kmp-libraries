# Qamar KMP Libraries API Reference (Maven + Full API)

This document is a detailed API guide for integrating these libraries into another application, with a focus on Maven usage and all available public methods.

Artifacts are published under:

- Group: `io.github.maqsats`
- Version: `1.0.0`

---

## 1) Maven Integration

### 1.1 Add Maven Central repository

```xml
<repositories>
  <repository>
    <id>central</id>
    <url>https://repo1.maven.org/maven2</url>
  </repository>
</repositories>
```

### 1.2 Artifact naming for Maven consumers

Because this is a Kotlin Multiplatform project, Maven consumers typically use target-specific artifacts:

- JVM/Desktop apps: `*-jvm`
- Android target: `*-android`

Example for JVM:

```xml
<dependency>
  <groupId>io.github.maqsats</groupId>
  <artifactId>quran-api-jvm</artifactId>
  <version>1.0.0</version>
</dependency>
```

If your resolver/tooling supports variant-aware metadata, the base artifact ids may also work (for example `quran-api`). For plain Maven JVM projects, prefer `-jvm`.

### 1.3 Available modules

- `quran-core`
- `quran-api`
- `quran-audio`
- `quran-translations`
- `quran-transliteration`
- `quran-search`
- `quran-tajweed`
- `prayer-core`
- `qibla-finder`

Example dependencies for a JVM app:

```xml
<dependencies>
  <dependency>
    <groupId>io.github.maqsats</groupId>
    <artifactId>quran-core-jvm</artifactId>
    <version>1.0.0</version>
  </dependency>
  <dependency>
    <groupId>io.github.maqsats</groupId>
    <artifactId>quran-api-jvm</artifactId>
    <version>1.0.0</version>
  </dependency>
  <dependency>
    <groupId>io.github.maqsats</groupId>
    <artifactId>quran-transliteration-jvm</artifactId>
    <version>1.0.0</version>
  </dependency>
  <dependency>
    <groupId>io.github.maqsats</groupId>
    <artifactId>quran-translations-jvm</artifactId>
    <version>1.0.0</version>
  </dependency>
  <dependency>
    <groupId>io.github.maqsats</groupId>
    <artifactId>quran-search-jvm</artifactId>
    <version>1.0.0</version>
  </dependency>
  <dependency>
    <groupId>io.github.maqsats</groupId>
    <artifactId>quran-audio-jvm</artifactId>
    <version>1.0.0</version>
  </dependency>
  <dependency>
    <groupId>io.github.maqsats</groupId>
    <artifactId>quran-tajweed-jvm</artifactId>
    <version>1.0.0</version>
  </dependency>
  <dependency>
    <groupId>io.github.maqsats</groupId>
    <artifactId>prayer-core-jvm</artifactId>
    <version>1.0.0</version>
  </dependency>
  <dependency>
    <groupId>io.github.maqsats</groupId>
    <artifactId>qibla-finder-jvm</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```

---

## 2) `quran-core` API

Package roots:

- `com.qamar.quran.core.database`
- `com.qamar.quran.core.model`
- `com.qamar.quran.core.resource`

### 2.1 Models

- `data class Verse`
  - `sura: Int`
  - `ayah: Int`
  - `arabicText: String`
  - `translation: String? = null`
  - `transliteration: String? = null`
  - `page: Int? = null`

### 2.2 Database creation

- `data class DatabaseConfig`
  - `name: String = "quran.db"`
  - `copyBundledIfMissing: Boolean = true`

- `expect class DatabaseFactory(platformContext: Any?)`
  - `val platformContext: Any?`
  - `suspend fun createDriver(config: DatabaseConfig = DatabaseConfig()): SqlDriver`

Behavior notes:

- Android requires `platformContext` to be an Android `Context`, otherwise throws `IllegalArgumentException`.
- iOS/Desktop attempt to copy bundled `databases/quran.db` first (when enabled), then seed from JSON if needed.
- JS uses SQLDelight WebWorker driver and seeds from JSON resources.

### 2.3 Resource loading

- `expect class ResourceReader(platformContext: Any?)`
  - `val platformContext: Any?`
  - `fun readText(path: String): String`
  - `fun readBytes(path: String): ByteArray`

Behavior notes:

- Throws if resource cannot be found.
- Android checks classpath and `Context.assets`.
- iOS uses `NSBundle.mainBundle`.
- Desktop uses classpath.
- JS expects resource loading via `require(...)`.

### 2.4 Optional seeding helper

- `class DatabaseSeeder`
  - `suspend fun seedIfEmpty()`

Seeds from:

- `arabic.json`
- `quran_paged.json`

### 2.5 SQLDelight queries in `QuranDatabase.sq`

Generated query names available from `quranDatabaseQueries`:

- `getSura`
- `getSuraWithPage`
- `getVerse`
- `getVerseWithPage`
- `getPage`
- `getAllVerses`
- `countVerses`
- `getAyahPage`
- `getPageStart`
- `searchArabic`
- `insertVerse`
- `insertPage`

---

## 3) `quran-api` API

Package root: `com.qamar.quran.api`

### 3.1 `QuranApi` (main facade)

Methods:

- `suspend fun getVerse(sura: Int, ayah: Int): Verse?`
- `suspend fun getSura(sura: Int): List<Verse>`
- `suspend fun getPage(page: Int): List<Verse>`
- `suspend fun getAllVerses(): List<Verse>`
- `suspend fun getAyahPage(sura: Int, ayah: Int): Int`
- `suspend fun getPageStart(page: Int): Pair<Int, Int>?`
- `suspend fun getTranslation(sura: Int, ayah: Int, translationId: String? = null): String?`
- `suspend fun getSuraTranslation(sura: Int, translationId: String? = null): List<String>`
- `suspend fun getAvailableTranslations(): List<TranslationInfo>`
- `suspend fun isTranslationDownloaded(translationId: String): Boolean`
- `suspend fun getTransliteration(sura: Int, ayah: Int, language: TransliterationLanguage): String?`
- `suspend fun getSuraTransliteration(sura: Int, language: TransliterationLanguage): List<String>`
- `suspend fun searchArabic(query: String): List<Verse>`
- `suspend fun searchTranslation(query: String, translationId: String? = null): List<Verse>`
- `suspend fun searchTransliteration(query: String, language: TransliterationLanguage): List<Verse>`
- `suspend fun getVerses(requests: List<VerseRequest>): List<Verse>`
- `suspend fun getVerseRange(sura: Int, startAyah: Int, endAyah: Int): List<Verse>`
- `suspend fun clearCache()`
- `suspend fun preloadSura(sura: Int)`
- `suspend fun preloadPage(page: Int)`

Supporting model:

- `data class VerseRequest(val sura: Int, val ayah: Int)`

### 3.2 `DefaultQuranApi`

Constructor:

- `DefaultQuranApi(database, translationManager, transliterationProvider, cacheSize, dispatcher)`

Important implementation details:

- Uses an in-memory LRU-like verse cache.
- `searchArabic` uses SQL `LIKE`.
- `searchTransliteration` delegates to `TransliterationProvider.search(...)` then resolves verses.
- Current translation methods are placeholders:
  - `getTranslation(...)` currently returns `null`.
  - `getSuraTranslation(...)` currently returns `emptyList()`.
  - `searchTranslation(...)` currently returns `emptyList()`.

---

## 4) `quran-transliteration` API

Package root: `com.qamar.quran.transliteration`

### 4.1 Types

- `enum class TransliterationLanguage`
  - `ENGLISH`
  - `RUSSIAN`
  - `KAZAKH`

### 4.2 Provider contract

- `interface TransliterationProvider`
  - `suspend fun getTransliteration(sura: Int, ayah: Int, language: TransliterationLanguage): String?`
  - `suspend fun getSuraTransliteration(sura: Int, language: TransliterationLanguage): List<String>`
  - `suspend fun search(query: String, language: TransliterationLanguage): List<Pair<Int, Int>>`

### 4.3 `JsonTransliterationProvider`

Constructor:

- `JsonTransliterationProvider(platformContext: Any? = null, resourceReader: ResourceReader = ResourceReader(platformContext))`

Reads bundled files:

- `translit_en.json`
- `translit_ru.json`
- `translit_kk.json`

---

## 5) `quran-translations` API

Package root: `com.qamar.quran.translations`

### 5.1 Models

- `data class TranslationInfo(...)`
  - Includes metadata fields like `displayName`, `languageCode`, `fileUrl`, `fileName`, versions.
  - Computed property: `translationId: String` (from `fileName` without `.db`).

- `enum class DownloadStatus`
  - `PENDING`, `DOWNLOADING`, `EXTRACTING`, `COMPLETED`, `FAILED`, `CANCELLED`

- `data class DownloadProgress(...)`
  - `translationId`, `bytesDownloaded`, `totalBytes`, `percentage`, `status`

### 5.2 `TranslationManager`

Methods:

- `suspend fun downloadTranslation(translationId: String): Flow<DownloadProgress>`
- `suspend fun autoDownloadTranslation(languageCode: String): Result<TranslationInfo>`
- `suspend fun deleteTranslation(translationId: String): Result<Unit>`
- `suspend fun checkForUpdates(): List<TranslationInfo>`
- `suspend fun updateTranslation(translationId: String): Flow<DownloadProgress>`
- `suspend fun getDownloadStatus(translationId: String): DownloadStatus`
- `suspend fun cancelDownload(translationId: String): Boolean`
- `suspend fun getAvailableTranslations(): List<TranslationInfo>`
- `suspend fun isTranslationDownloaded(translationId: String): Boolean`

### 5.3 `DefaultTranslationManager`

Constructor:

- `DefaultTranslationManager(platformContext: Any? = null, metadataSource: TranslationMetadataSource = TranslationMetadataSource(platformContext))`

Current behavior:

- Stub/in-memory implementation.
- Emits pending and completed states without real file download/extraction.
- Statuses are tracked in memory only.

### 5.4 `TranslationMetadataSource`

- `fun loadBundled(): List<TranslationInfo>`

Reads bundled `translations.json`.

---

## 6) `quran-search` API

Package root: `com.qamar.quran.search`

- `class QuranSearchEngine(private val api: QuranApi)`
  - `suspend fun searchArabic(query: String): List<Verse>`
  - `suspend fun searchTranslation(query: String, translationId: String? = null): List<Verse>`
  - `suspend fun searchTransliteration(query: String, language: TransliterationLanguage): List<Verse>`

This module is a thin wrapper around `QuranApi`.

---

## 7) `quran-audio` API

Package root: `com.qamar.quran.audio`

### 7.1 Core API contract

- `interface QuranAudioApi`
  - `suspend fun getReciters(): List<Reciter>`
  - `suspend fun getAudioUrl(request: AudioRequest, validateUrls: Boolean = false): AudioUrlResult`
  - `suspend fun fetchAudio(request: AudioRequest, cachePolicy: CachePolicy? = null): AudioFetchResult`

Convenience overloads also exist for ayah/sura/page in the same interface:

- `getAudioUrl(sura, ayah, reciter, validateUrls)`
- `getAudioUrlForSura(sura, reciter, validateUrls)`
- `getAudioUrlForPage(page, reciter, validateUrls)`
- `fetchAudio(sura, ayah, reciter, cachePolicy)`
- `fetchAudioForSura(sura, reciter, cachePolicy)`
- `fetchAudioForPage(page, reciter, cachePolicy)`

### 7.2 Primary implementation: `QuranAudioClient`

Constructor:

- `QuranAudioClient(config, httpClient, fileStore, platformContext, dispatcher)`

Public methods:

- Implements all `QuranAudioApi` methods.
- `fun close()` to close internally-owned HTTP client.

### 7.3 Audio request/result models

- `enum class AudioKind`: `AYAH`, `SURA`, `PAGE`

- `sealed class AudioRequest`
  - `AudioRequest.Ayah(sura, ayah, reciter)`
  - `AudioRequest.Sura(sura, reciter)`
  - `AudioRequest.Page(page, reciter)`
  - `val kind: AudioKind`

- `data class Reciter(id, name, meta = emptyMap())`

- `data class AudioUrlCandidate(url, sourceId, isOriginal = false)`
- `data class AudioUrlResult(url, candidates, usedSourceId)`
- `data class AudioFetchResult(url, localPath, candidates, usedSourceId, fromCache)`
  - `fun effectivePlaybackPath(): String`

- `enum class CachePolicy`
  - `REMOTE_ONLY`
  - `CACHE_IF_POSSIBLE`
  - `CACHE_ONLY`

### 7.4 Config API

- `data class QuranAudioConfig(reciters, audio, cache, network)`
  - `companion object { fun fromJson(text: String): QuranAudioConfig }`

- `data class RecitersSourceConfig(id, url, format, headers = emptyMap())`

- `sealed class RecitersFormat`
  - `JsonMap(listPath, namePath, meta)`
  - `JsonArray(listPath, idField, nameField, meta)`
  - `TextLines(separator = ":", trim = true)`

- `data class AudioSourceSet(ayah, sura, page)`

- `sealed class AudioSourceConfig`
  - `Template(id, kind, template)`
  - `JsonEndpoint(id, kind, urlTemplate, format, headers)`

- `enum class AudioResponseMode`
  - `OBJECT_MAP`
  - `ARRAY`

- `data class AudioResponseFormat(mode, listPath, reciterIdField, urlFields)`

- `data class CacheConfig(...)`
  - `companion object { val DEFAULT_TEMPLATES }`

- `data class NetworkConfig(validateUrls, connectTimeoutMs, requestTimeoutMs)`

- `object QuranAudioDefaults`
  - `fun quranApi(): QuranAudioConfig`

### 7.5 Audio playback abstraction

- `expect class QuranAudioPlayer(platformContext: Any?)`
  - `val platformContext: Any?`
  - `val isSupported: Boolean`
  - `val state: AudioPlaybackState`
  - `val isPlaying: Boolean`
  - `val durationMs: Long?`
  - `val positionMs: Long`
  - `val playbackRate: Float`
  - `fun load(source: String, autoPlay: Boolean = false)`
  - `fun play()`
  - `fun pause()`
  - `fun stop()`
  - `fun seekTo(positionMs: Long)`
  - `fun setVolume(volume: Float)`
  - `fun setPlaybackRate(rate: Float)`
  - `fun release()`

- `enum class AudioPlaybackState`
  - `IDLE`, `LOADING`, `READY`, `PLAYING`, `PAUSED`, `STOPPED`, `COMPLETED`, `ERROR`

- Extension:
  - `fun QuranAudioPlayer.load(result: AudioFetchResult, autoPlay: Boolean = true)`

Platform support notes:

- Android: supported (`MediaPlayer`).
- iOS: supported (`AVPlayer`).
- JS: supported (`HTMLAudioElement`).
- Desktop/JVM: supported (JavaFX `MediaPlayer`, JavaFX runtime required).

### 7.6 Low-level storage/client helpers

- `expect class AudioFileStore(platformContext: Any?)`
  - `val supportsCache: Boolean`
  - `fun cacheDir(subdirectory: String): String?`
  - `fun exists(path: String): Boolean`
  - `fun createDirectories(path: String)`
  - `suspend fun writeBytes(path: String, bytes: ByteArray)`

- `fun platformHttpClient(): HttpClient`

JS cache note:

- `AudioFileStore` on JS reports `supportsCache = false` and `writeBytes` is unsupported.

---

## 8) `quran-tajweed` API

Package root: `com.qamar.quran.tajweed`

- `object TajweedApi`
  - `fun getTajweedSpans(verse: String): List<TajweedSpan>`

- `data class TajweedSpan(start: Int, end: Int, rule: TajweedRule)`
  - Range is `[start, end)` (end-exclusive).

- `enum class TajweedRule`
  - `MAD`
  - `GHUNNA`
  - `QALQALA`
  - `IQLAB`
  - `IDGHAM`
  - `IDGHAM_WITHOUT_GHUNNA`
  - `IKHFA`
  - `HAMZAT_WASL`
  - `MAD_HEY`
  - `ONE_MAD`
  - `MADDAH`

---

## 9) `prayer-core` API

Package root: `com.qamar.prayer.core`

### 9.1 Models and enums

- `data class Coordinates(latitude, longitude)`
  - Validates latitude `[-90..90]`, longitude `[-180..180]`.

- `data class DateComponents(year, month, day)`
  - 1-based month.
  - Validates year/day ranges and month/day validity.

- `data class PrayerOffsets(fajr, sunrise, dhuhr, asr, sunset, maghrib, isha)`
- `data class PrayerTimesRaw(fajr, sunrise, dhuhr, asr, sunset, maghrib, isha)`
- `data class PrayerTimes(fajr, sunrise, dhuhr, asr, sunset, maghrib, isha, day, month, year)`

- `enum class ParameterType`
  - `ANGLE`, `MINUTES`

- `data class MethodParams(fajrAngle, maghribType, maghribParameter, ishaType, ishaParameter)`

- `enum class CalculationMethod`
  - `QMDB`, `KARACHI`, `ISNA`, `MWL`, `MAKKAH`, `EGYPT`, `TEHRAN`, `JAFARI`, `DIYANET`, `CUSTOM`

- `enum class JuristicMethod`
  - `SHAFII`, `HANAFI`

- `enum class HighLatitudeRule`
  - `NONE`, `MID_NIGHT`, `ONE_SEVENTH`, `ANGLE_BASED`

- `enum class TimeFormat`
  - `TIME_24`, `TIME_12`, `TIME_12_NO_SUFFIX`, `FLOATING`

### 9.2 Time zone abstraction

- `fun interface TimeZoneProvider`
  - `fun timeZoneOffsetHours(lat: Double, lon: Double): Double`

Implementations:

- `DefaultTimeZoneProvider`
- `AndroidTimeZoneProvider`
- `IosTimeZoneProvider`
- `JsTimeZoneProvider`
- `JvmTimeZoneProvider(instantMillis: Long = System.currentTimeMillis())`

### 9.3 Calculation engine

- `class PrayerTimesCalculator(...)`
  - Mutable settings fields:
    - `calculationMethod`
    - `asrJuristic`
    - `adjustHighLats`
    - `timeFormat`
    - `dhuhrMinutes`
    - `offsets`
    - `numIterations`
    - `invalidTimeText`
  - Methods:
    - `fun setCustomMethod(params: MethodParams)`
    - `fun prayerTimesRaw(date, coordinates, timeZone): PrayerTimesRaw`
    - `fun prayerTimes(date, coordinates, timeZone, format = timeFormat): PrayerTimes`
    - `fun prayerTimesForMonth(year, month, coordinates, timeZone, format = timeFormat): List<PrayerTimes>`

### 9.4 High-level API

- `class PrayerTimeApi(calculator: PrayerTimesCalculator = PrayerTimesCalculator(), timeZoneProvider: TimeZoneProvider)`
  - `fun getPrayerTimes(date, latitude, longitude): PrayerTimes`
  - `fun getPrayerTimesForMonth(year, month, latitude, longitude): List<PrayerTimes>`
  - `fun getPrayerTimesRaw(date, latitude, longitude): PrayerTimesRaw`
  - `fun withCalculator(block: PrayerTimesCalculator.() -> Unit): PrayerTimeApi`

---

## 10) `qibla-finder` API

Package root: `com.qamar.qibla.finder`

### 10.1 Constants and pure functions

- `const val KAABA_LATITUDE`
- `const val KAABA_LONGITUDE`
- `fun qiblaDirectionFrom(longitudeDegrees: Double, latitudeDegrees: Double): Double`
- `fun cot(angleRadians: Double): Double`
- `fun isQiblaAligned(qiblaDirectionDegrees: Double, deviceAzimuthDegrees: Double, toleranceDegrees: Double = 14.0): Boolean`

### 10.2 Models

- `data class QiblaInfo(longitude, latitude, directionDegrees)`
- `data class QiblaLocation(latitude, longitude)`
- `data class QiblaAlignmentState(directionDegrees, currentAzimuth, isAligned)`

### 10.3 Location provider

- `expect class QiblaLocationProvider(platformContext: Any?)`
  - `val platformContext: Any?`
  - `val isSupported: Boolean`
  - `fun getCurrentLocation(callback: (QiblaLocation?) -> Unit)`

Platform behavior:

- Android: supported (`LocationManager`, permission required by host app).
- iOS: supported (`CLLocationManager`, authorization flow handled by host app permissions).
- Desktop: not supported (`isSupported = false`).
- JS: not supported (`isSupported = false`).

### 10.4 Compass provider

- `typealias QiblaAzimuthListener = (Float) -> Unit`

- `expect class QiblaCompass(platformContext: Any?)`
  - `val platformContext: Any?`
  - `val isSupported: Boolean`
  - `fun setListener(listener: QiblaAzimuthListener?)`
  - `fun setAzimuthFix(fix: Float)`
  - `fun start()`
  - `fun stop()`

Platform behavior:

- Android: supported (`SensorManager` accelerometer + magnetic field).
- iOS: supported (`CLLocationManager` heading updates).
- Desktop: not supported (`isSupported = false`).
- JS: not supported (`isSupported = false`).

---

## 11) End-to-End Initialization Example (KMP/Android-first)

```kotlin
import com.qamar.quran.api.DefaultQuranApi
import com.qamar.quran.api.QuranApi
import com.qamar.quran.core.database.DatabaseFactory
import com.qamar.quran.core.database.QuranDatabase
import com.qamar.quran.transliteration.JsonTransliterationProvider
import com.qamar.quran.translations.DefaultTranslationManager

suspend fun createQuranApi(platformContext: Any?): QuranApi {
    val driver = DatabaseFactory(platformContext).createDriver()
    val db = QuranDatabase(driver)
    val transliteration = JsonTransliterationProvider(platformContext)
    val translationManager = DefaultTranslationManager(platformContext)

    return DefaultQuranApi(
        database = db,
        translationManager = translationManager,
        transliterationProvider = transliteration,
    )
}
```

Android call:

```kotlin
val api = createQuranApi(context.applicationContext)
```

iOS/Desktop/JS call:

```kotlin
val api = createQuranApi(null)
```

---

## 12) Current Limitations (Important)

- Translation storage/download logic is currently stubbed in `DefaultTranslationManager`.
- `DefaultQuranApi` translation read/search methods currently return `null`/`emptyList`.
- JS `AudioFileStore` does not support caching.
- `qibla-finder` location/compass are unsupported on JS/Desktop in current implementation.
- JS resource loading in `quran-core`/transliteration depends on runtime/bundler `require` behavior.

