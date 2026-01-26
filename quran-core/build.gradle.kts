import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlin.serialization)
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
                implementation(libs.coroutines.core)
                implementation(libs.serialization.json)
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.async.extensions)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.sqldelight.android.driver)
            }
        }

        val androidUnitTest by getting

        val desktopMain by getting {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }

        val iosMain by getting {
            dependencies {
                implementation(libs.sqldelight.native.driver)
            }
        }

        val iosTest by getting

        val jsMain by getting {
            dependencies {
                implementation(libs.sqldelight.web.worker.driver)
                implementation(devNpm("copy-webpack-plugin", "9.1.0"))
                implementation(npm("@cashapp/sqldelight-sqljs-worker", "2.0.0"))
                implementation(npm("sql.js", "1.8.0"))
            }
        }
    }
}

android {
    namespace = "com.qamar.quran.core"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].assets.srcDir("src/commonMain/resources")
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

sqldelight {
    databases {
        create("QuranDatabase") {
            packageName.set("com.qamar.quran.core.database")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            generateAsync.set(true)
        }
    }
}
