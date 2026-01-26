# Gradle & Module Management (Qamar KMP Libraries)

This repo mirrors the modular/layout ideas from the reference structure, adapted to the Quran KMP libraries.

## Table of Contents
1. Project Structure Overview
2. Root Configuration
3. Module Organization
4. Convention Plugins (future-ready)
5. Version Catalog
6. Module Inclusion Strategy
7. Dependency Management
8. Best Practices

---

## 1) Project Structure Overview

```
qamar-kmp-libraries/
├── build.gradle.kts                # Root build (plugins via aliases once catalog added)
├── settings.gradle.kts             # Plugin mgmt, repos, module includes
├── gradle/
│   └── libs.versions.toml          # Version catalog (centralized deps/plugins) [planned]
├── plugins/                        # Custom convention plugins (future) [planned]
├── quran-core/                     # Core DB, drivers, resources
├── quran-api/                      # Public API facade
├── quran-translations/             # Translation contracts/metadata
├── quran-transliteration/          # Transliteration provider + JSON assets
└── quran-search/                   # Search helper on top of QuranApi
```

## 2) Root Configuration

### settings.gradle.kts
- Declares repositories once (google/mavenCentral).
- Includes all modules directly (no delegated includes yet).
- Ready to enable type-safe project accessors or includeBuild("plugins") when convention plugins are added.

### build.gradle.kts (root)
- Applies plugin versions via `apply false`.
- Registers `clean`.
- Will use version-catalog aliases once `gradle/libs.versions.toml` is added.

## 3) Module Organization

- `quran-core`: SQLDelight schema + drivers + resource seeding.
- `quran-api`: `QuranApi` interface + default implementation; depends on core/translations/transliteration.
- `quran-translations`: Metadata loader + manager contract.
- `quran-transliteration`: JSON-backed transliteration provider.
- `quran-search`: Thin search engine delegating to `QuranApi`.

Each module uses KMP targets: Android, iOS (x64/arm64/sim), JS (IR), JVM desktop.

## 4) Convention Plugins (future-ready)

- Place custom plugins under `plugins/convention/*`.
- Example plugin IDs (to be created): `com.qamar.convention.mpp`, `com.qamar.convention.compose`.
- Include via `includeBuild("plugins")` in `settings.gradle.kts`, then apply by alias in modules to reduce boilerplate.

## 5) Version Catalog

- Location: `gradle/libs.versions.toml` (to add).
- Centralize versions:
  - kotlin = "1.9.22"
  - agp = "8.1.2"
  - coroutines = "1.7.3"
  - serialization = "1.6.2"
  - sqldelight = "2.0.0"
  - ktor = "2.3.6"
- Use plugin aliases (kmp, android library) and library aliases in all modules once catalog is present.

## 6) Module Inclusion Strategy

- Direct includes in `settings.gradle.kts`:
  - `include(":quran-core", ":quran-api", ":quran-translations", ":quran-transliteration", ":quran-search")`
- If feature groups grow, add delegated include files (e.g., `features/include.gradle.kts`) and apply them from settings.

## 7) Dependency Management

- Internal deps: reference via `project(":module")` (later switch to type-safe `projects.*` after enabling accessors).
- External deps: use version catalog aliases (planned) instead of hardcoded versions.
- Use `implementation` by default; `api` only when transitive exposure is required.
- Platform-specific deps go into the matching source set blocks (androidMain, iosMain, jsMain, desktopMain).

## 8) Best Practices

- Keep modules single-purpose; add new modules rather than crowding existing ones.
- Prefer convention plugins to remove repetition across modules.
- Keep all versions in the catalog; update in one place.
- Enable Gradle configuration cache and type-safe project accessors when stable for the toolchain.
- For new features: create `include.gradle.kts` per feature group and aggregate them for clarity.

---

### Minimal “next steps” to fully align
1) Add `gradle/libs.versions.toml` with shared versions and plugin aliases.
2) Switch module build files to use `alias(libs.plugins...)` and `implementation(libs...)`.
3) (Optional) Add `plugins/convention` modules and include them via composite build, then apply convention plugins in modules.
