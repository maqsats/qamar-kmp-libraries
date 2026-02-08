# Qamar KMP Libraries

Multiplatform Quran stack built from the legacy `QamarOld` assets. Modules:

- `quran-core`: SQLDelight schema + bundled Arabic text/page map (`quran.db`, `arabic.json`, `quran_paged.json`) and platform drivers.
- `quran-transliteration`: Loads transliteration JSON (KK/EN/RU) with `JsonTransliterationProvider`.
- `quran-translations`: Translation metadata + manager contracts (downloads TBD).
- `quran-api`: Public `QuranApi` facade on top of the core DB, transliteration, and translations.
- `quran-search`: Thin search helper delegating to `QuranApi`.
- `quran-tajweed`: Tajweed (recitation rules) detection: input Arabic verse text, output `List<TajweedSpan>` (start, end, rule). Platform-agnostic; use with verse text from `QuranApi` or any source. Rules: MAD, GHUNNA, QALQALA, IQLAB, IDGHAM, IKHFA, HAMZAT_WASL, MAD_HEY, ONE_MAD, MADDAH.
- `prayer-core`: Multiplatform prayer time calculation engine (ported from QamarOld). Works on **Android, iOS, JS, Desktop**. Supports multiple calculation methods (MWL, ISNA, Egypt, Makkah, etc.), Asr (Shafii/Hanafi), high-latitude adjustments, and timezone resolution (device default or Android CSV-based).

## Installation

Add the dependencies to your `build.gradle.kts`:

```kotlin
dependencies {
    // Core database and models
    implementation("io.github.maqsats:quran-core:1.0.0")
    
    // Public API
    implementation("io.github.maqsats:quran-api:1.0.0")
    
    // Translations
    implementation("io.github.maqsats:quran-translations:1.0.0")
    
    // Transliteration
    implementation("io.github.maqsats:quran-transliteration:1.0.0")
    
    // Search
    implementation("io.github.maqsats:quran-search:1.0.0")
    
    // Tajweed (recitation rules: spans for coloring)
    implementation("io.github.maqsats:quran-tajweed:1.0.0")
    
    // Prayer times (KMP: Android, iOS, JS, Desktop)
    implementation("io.github.maqsats:prayer-core:1.0.0")
}
```

For Kotlin Multiplatform projects, add the appropriate platform-specific dependencies as needed.

## Data generation (already run)
Source data comes from `/Users/maqsat/StudioProjects/QamarOld/app/src/main`:

- Arabic & transliterations: `res/values/arabic.xml`, `translit*.xml`
- Page map: `assets/databases/qurankz.db`
- Translation metadata: `assets/translations.json`

Regenerate artifacts if the source changes:

```bash
cd /Users/maqsat/StudioProjects/qamar-kmp-libraries
python3 tools/generate_quran_db.py
# Copies are expected at:
# quran-core/src/commonMain/resources/{databases/quran.db,arabic.json,quran_paged.json}
# quran-transliteration/src/commonMain/resources/translit_{en,ru,kk}.json
# quran-translations/src/commonMain/resources/translations.json
```

## Quick usage
```kotlin
// Platform-side: create driver (Android requires Context)
val driver = DatabaseFactory(context).createDriver()
val db = QuranDatabase(driver)

val api = DefaultQuranApi(
    database = db,
    translationManager = null, // wire in later
    transliterationProvider = JsonTransliterationProvider(context)
)

val verse1 = api.getVerse(1, 1)
val page1 = api.getPage(1)
val search = api.searchArabic("بسم")

// Tajweed: get spans for coloring verse text (use verse.arabicText from API)
verse1?.let { v -> TajweedApi.getTajweedSpans(v.arabicText) }
// Platform applies color per span.rule (MAD, GHUNNA, QALQALA, IQLAB, IDGHAM, IKHFA, etc.)
```

## Tajweed (quran-tajweed)

Input: Arabic verse string (e.g. from `QuranApi.getVerse(1, 1)?.arabicText`). Output: `List<TajweedSpan>(start, end, rule)`. Apply your own colors/spans per `TajweedRule` on Android (`ForegroundColorSpan`), Compose (`SpanStyle`), or iOS (`NSAttributedString`).

```kotlin
val verse = api.getVerse(1, 1)?.arabicText ?: ""
val spans = TajweedApi.getTajweedSpans(verse)
spans.forEach { (start, end, rule) -> /* apply color for rule */ }
```

## Prayer times (prayer-core)

Use a platform-specific `TimeZoneProvider` (all use the device's default time zone):

```kotlin
// Android / iOS / JS / Desktop: device timezone
val tzProvider = DefaultTimeZoneProvider() // or AndroidTimeZoneProvider() / IosTimeZoneProvider() / JsTimeZoneProvider() / JvmTimeZoneProvider()

val api = PrayerTimeApi(timeZoneProvider = tzProvider)
api.withCalculator { calculationMethod = CalculationMethod.MWL; timeFormat = TimeFormat.TIME_24 }

val date = DateComponents(2025, 6, 15)
val times = api.getPrayerTimes(date, latitude = 51.5074, longitude = -0.1278)
// times.fajr, times.sunrise, times.dhuhr, times.asr, times.sunset, times.maghrib, times.isha

val monthTimes = api.getPrayerTimesForMonth(2025, 6, 51.5074, -0.1278)
```

On Android/iOS/JS/Desktop, use `DateComponents(year, month, day)` (1-based month). On JVM/Android you can use `calendar.toDateComponents()` from the platform extension.

## Notes / TODO
- JS resource loading is stubbed; wire a bundler/loader for `arabic.json`, `quran_paged.json`, and transliteration JSON.
- Translation downloads/storage are contract-only; needs implementation backed by the metadata + downloaded DB files.
- iOS resource loading assumes the bundled files are retained in the app target.
