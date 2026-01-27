# Qamar KMP Libraries

Multiplatform Quran stack built from the legacy `QamarOld` assets. Modules:

- `quran-core`: SQLDelight schema + bundled Arabic text/page map (`quran.db`, `arabic.json`, `quran_paged.json`) and platform drivers.
- `quran-transliteration`: Loads transliteration JSON (KK/EN/RU) with `JsonTransliterationProvider`.
- `quran-translations`: Translation metadata + manager contracts (downloads TBD).
- `quran-api`: Public `QuranApi` facade on top of the core DB, transliteration, and translations.
- `quran-search`: Thin search helper delegating to `QuranApi`.

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
```

## Notes / TODO
- JS resource loading is stubbed; wire a bundler/loader for `arabic.json`, `quran_paged.json`, and transliteration JSON.
- Translation downloads/storage are contract-only; needs implementation backed by the metadata + downloaded DB files.
- iOS resource loading assumes the bundled files are retained in the app target.
