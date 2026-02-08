plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    id("maven-publish")
    id("signing")
}

group = project.findProperty("GROUP") as String? ?: "io.github.maqsats"
version = project.findProperty("VERSION_NAME") as String? ?: "1.0.0"

apply(from = rootProject.file("gradle/publishing.gradle.kts"))

// OpenJFX platform classifiers and module names (no magic strings in desktop source set).
private object JavafxPlatform {
    const val WINDOWS = "win"
    const val MAC = "mac"
    const val MAC_ARM64 = "mac-aarch64"
    const val LINUX = "linux"
    const val ARM64 = "aarch64"
    val MODULES = listOf("javafx-base", "javafx-media", "javafx-graphics")
}

private object OsProperty {
    const val NAME = "os.name"
    const val ARCH = "os.arch"
}

private fun javafxPlatformClassifier(): String {
    val osName = System.getProperty(OsProperty.NAME).lowercase()
    val osArch = System.getProperty(OsProperty.ARCH).lowercase()
    return when {
        osName.contains(JavafxPlatform.WINDOWS) -> JavafxPlatform.WINDOWS
        osName.contains(JavafxPlatform.MAC) -> if (osArch == JavafxPlatform.ARM64) JavafxPlatform.MAC_ARM64 else JavafxPlatform.MAC
        else -> JavafxPlatform.LINUX
    }
}

kotlin {
    androidLibrary {
        namespace = "com.qamar.quran.audio"
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
            implementation(libs.ktor.client.core)
        }
        val commonTest by getting
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        val androidMain by getting
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        val desktopMain by getting
        desktopMain.dependencies {
            implementation(libs.ktor.client.java)
            // OpenJFX publishes platform-specific JARs; main artifact has no classes.
            val javafxVersion = libs.versions.javafx.get()
            val platform = javafxPlatformClassifier()
            JavafxPlatform.MODULES.forEach { module ->
                implementation("org.openjfx:$module:$javafxVersion:$platform")
            }
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
