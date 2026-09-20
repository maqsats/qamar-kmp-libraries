plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.maven.publish) apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

// Pin the bytecode level for every published module.
//
// Without this, `jvm("desktop")` compiles against whatever JDK happens to be on
// the publisher's machine, so the class-file version of a release is an accident
// of the build host. That is how 1.0.0-1.0.20 shipped as class-file 66 (Java 22):
// they were published from a JDK 22 box, which silently forces every consumer
// onto JDK 22+ — a non-LTS release that is already end-of-life. Consumers on
// LTS 21 fail at class load with UnsupportedClassVersionError, not at compile.
//
// 17 is the floor AGP requires and matches the consuming app's sourceCompatibility.
subprojects {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
            jvmToolchain(17)
        }
    }
}
