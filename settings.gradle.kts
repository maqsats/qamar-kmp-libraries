enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "qamar-kmp-libraries"

include(
    ":quran-core",
    ":quran-api",
    ":quran-translations",
    ":quran-transliteration",
    ":quran-search",
)
