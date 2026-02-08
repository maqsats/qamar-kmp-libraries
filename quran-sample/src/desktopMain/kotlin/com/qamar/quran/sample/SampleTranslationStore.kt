package com.qamar.quran.sample

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.java.Java
import java.io.File
import java.sql.DriverManager

actual class SampleTranslationStore {
    private val client = HttpClient(Java)
    private val baseDir = File(System.getProperty("user.home"), ".qamar/translations").apply { mkdirs() }

    actual fun isSupported(): Boolean = true

    actual suspend fun downloadTranslation(url: String, fileName: String): String {
        val target = File(baseDir, fileName)
        val bytes: ByteArray = client.get(url).body()
        target.writeBytes(bytes)
        return target.absolutePath
    }

    actual suspend fun readVerse(path: String, sura: Int, ayah: Int): String? {
        DriverManager.getConnection("jdbc:sqlite:$path").use { connection ->
            connection.prepareStatement("SELECT text FROM verses WHERE sura = ? AND ayah = ? LIMIT 1").use { stmt ->
                stmt.setInt(1, sura)
                stmt.setInt(2, ayah)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.getString(1) else null
                }
            }
        }
    }
}
