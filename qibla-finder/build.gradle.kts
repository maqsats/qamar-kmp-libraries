plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.maven.publish)
}

kotlin {
    androidLibrary {
        namespace = "com.qamar.qibla.finder"
        compileSdk = 36
        minSdk = 21
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
        val commonTest by getting
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
