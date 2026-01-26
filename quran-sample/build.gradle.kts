import com.android.build.api.dsl.androidLibrary

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "com.qamar.quran.sample"
        compileSdk = 36
        minSdk = 21
    }
    jvm("desktop") {
        withJava()
    }
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
            implementation(projects.quranApi)
            implementation(projects.quranTransliteration)
            implementation(projects.quranTranslations)
            implementation(projects.quranSearch)
            implementation(libs.coroutines.core)
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
    }
}
