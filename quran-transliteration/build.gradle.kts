plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    jvm("desktop")
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    js(IR) {
        browser()
        nodejs()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.quranCore)
                implementation(libs.serialization.json)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.coroutines.core)
            }
        }
        val androidMain by getting
        val androidUnitTest by getting
        val desktopMain by getting
        val iosMain by getting
        val iosTest by getting
        val jsMain by getting
    }
}

android {
    namespace = "com.qamar.quran.transliteration"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
    sourceSets["main"].assets.srcDir("src/commonMain/resources")
}
