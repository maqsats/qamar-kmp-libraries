import java.net.URI

configure<PublishingExtension> {
    val projectName = project.name
    val projectDescription = when (projectName) {
        "quran-core" -> "Core Quran database and models for Kotlin Multiplatform"
        "quran-api" -> "Public API facade for Quran data access"
        "quran-translations" -> "Translation management for Quran text"
        "quran-transliteration" -> "Transliteration support for Quran text"
        "quran-search" -> "Search functionality for Quran text"
        "quran-test" -> "Testing utilities for Quran libraries"
        else -> "Qamar KMP Libraries - $projectName"
    }

    publications {
        withType<MavenPublication> {
            pom {
                name.set(projectName)
                description.set(projectDescription)
                url.set("https://github.com/${project.findProperty("PUBLISHING_GITHUB_REPO") ?: "maqsats/qamar-kmp-libraries"}")
                
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                
                developers {
                    developer {
                        val developerId =
                            (project.findProperty("PUBLISHING_DEVELOPER_ID") as String?).takeIf { !it.isNullOrBlank() }
                        val developerName =
                            (project.findProperty("PUBLISHING_DEVELOPER_NAME") as String?).takeIf { !it.isNullOrBlank() }
                        val developerEmail =
                            (project.findProperty("PUBLISHING_DEVELOPER_EMAIL") as String?).takeIf { !it.isNullOrBlank() }

                        id.set(developerId ?: "maqsats")
                        name.set(developerName ?: "Maksat Inkar")
                        email.set(developerEmail ?: "")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/${project.findProperty("PUBLISHING_GITHUB_REPO") ?: "maqsats/qamar-kmp-libraries"}.git")
                    developerConnection.set("scm:git:ssh://github.com/${project.findProperty("PUBLISHING_GITHUB_REPO") ?: "maqsats/qamar-kmp-libraries"}.git")
                    url.set("https://github.com/${project.findProperty("PUBLISHING_GITHUB_REPO") ?: "maqsats/qamar-kmp-libraries"}")
                }
            }
        }
    }
    
    repositories {
        maven {
            val releasesRepoUrl = URI("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = URI("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (project.version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            
            credentials {
                username = project.findProperty("OSSRH_USERNAME") as String?
                password = project.findProperty("OSSRH_PASSWORD") as String?
            }
        }
    }
}

configure<SigningExtension> {
    val signingKeyId = project.findProperty("SIGNING_KEY_ID") as String?
    val signingPassword = project.findProperty("SIGNING_PASSWORD") as String?
    val rawKey = (project.findProperty("SIGNING_KEY") as String?).takeIf { !it.isNullOrBlank() }
        ?: System.getenv("SIGNING_KEY")?.takeIf { it.isNotBlank() }

    if (signingKeyId != null && signingPassword != null && rawKey != null) {
        // Gradle useInMemoryPgpKeys expects ASCII-armored key text.
        // If the key is base64-encoded (as in HOW-TO-PUBLISH / GitHub Secrets), decode it first.
        val signingKey = if (rawKey.contains("-----BEGIN PGP")) {
            rawKey
        } else {
            val normalized = rawKey.replace("\n", "").replace("\r", "").trim()
            String(java.util.Base64.getDecoder().decode(normalized), java.nio.charset.StandardCharsets.UTF_8)
        }
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        sign(extensions.getByType<PublishingExtension>().publications)
    }
}
