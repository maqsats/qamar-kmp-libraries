plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish)
}

kotlin {
    androidLibrary {
        namespace = "com.qamar.quran.translations"
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
            implementation(libs.serialization.json)
            implementation(libs.coroutines.core)
            implementation(libs.ktor.client.core)
        }
        val commonTest by getting
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.coroutines.core)
            implementation(projects.quranTest)
        }
        val androidMain by getting
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        val desktopMain by getting
        desktopMain.dependencies {
            implementation(libs.ktor.client.java)
        }
        val iosMain by getting
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        val jsMain by getting
        jsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}
