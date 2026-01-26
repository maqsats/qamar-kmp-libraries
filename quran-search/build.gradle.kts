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
                implementation(projects.quranApi)
                implementation(projects.quranCore)
                implementation(projects.quranTransliteration)
                implementation(libs.coroutines.core)
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
    namespace = "com.qamar.quran.search"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
}
