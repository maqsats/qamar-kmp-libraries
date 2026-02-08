plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
    id("signing")
}

group = project.findProperty("GROUP") as String? ?: "io.github.maqsats"
version = project.findProperty("VERSION_NAME") as String? ?: "1.0.0"

apply(from = rootProject.file("gradle/publishing.gradle.kts"))

kotlin {
    androidLibrary {
        namespace = "com.qamar.quran.core"
        compileSdk = 36
        minSdk = 21

        withHostTestBuilder {}
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
        val commonMain by getting
        commonMain.dependencies {
            implementation(libs.coroutines.core)
            implementation(libs.serialization.json)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.async.extensions)
        }

        val commonTest by getting
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        val androidMain by getting
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }

        val desktopMain by getting
        desktopMain.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }

        val iosMain by getting
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }

        val jsMain by getting
        jsMain.dependencies {
            implementation(libs.sqldelight.web.worker.driver)
            implementation(devNpm("copy-webpack-plugin", "9.1.0"))
            implementation(npm("@cashapp/sqldelight-sqljs-worker", "2.0.0"))
            implementation(npm("sql.js", "1.8.0"))
        }
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
