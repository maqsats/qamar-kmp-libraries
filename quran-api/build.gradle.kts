import com.android.build.api.dsl.androidLibrary

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    id("maven-publish")
    id("signing")
}

group = project.findProperty("GROUP") as String? ?: "io.github.maqsats"
version = project.findProperty("VERSION_NAME") as String? ?: "1.0.0"

apply(from = rootProject.file("gradle/publishing.gradle.kts"))

kotlin {
    androidLibrary {
        namespace = "com.qamar.quran.api"
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
            implementation(projects.quranCore)
            implementation(projects.quranTransliteration)
            implementation(projects.quranTranslations)
            implementation(libs.coroutines.core)
            implementation(libs.sqldelight.async.extensions)
        }
        val commonTest by getting
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.coroutines.core)
            implementation(projects.quranTest)
        }
        val desktopTest by getting
        desktopTest.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }
        val androidHostTest by getting
        androidHostTest.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }
        val iosTest by getting
        iosTest.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
        val jsTest by getting
        jsTest.dependencies {
            implementation(libs.sqldelight.web.worker.driver)
        }
    }
}
