plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.maven.publish)
}

kotlin {
    androidLibrary {
        namespace = "com.qamar.quran.search"
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
            implementation(projects.quranApi)
            implementation(projects.quranCore)
            implementation(projects.quranTransliteration)
            implementation(libs.coroutines.core)
        }
        val commonTest by getting
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.coroutines.core)
            implementation(projects.quranTranslations)
            implementation(projects.quranTest)
        }
    }
}
