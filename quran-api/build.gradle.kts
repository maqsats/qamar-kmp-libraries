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
                implementation(projects.quranTransliteration)
                implementation(projects.quranTranslations)
                implementation(libs.coroutines.core)
                implementation(libs.sqldelight.async.extensions)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.coroutines.core)
                implementation(projects.quranTest)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
        val iosTest by getting {
            dependencies {
                implementation(libs.sqldelight.native.driver)
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(libs.sqldelight.web.worker.driver)
            }
        }
        val androidMain by getting
        val desktopMain by getting
        val iosMain by getting
        val jsMain by getting
    }
}

android {
    namespace = "com.qamar.quran.api"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
}
