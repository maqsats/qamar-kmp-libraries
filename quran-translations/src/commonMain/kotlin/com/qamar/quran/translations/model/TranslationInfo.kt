package com.qamar.quran.translations.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TranslationInfo(
    val id: Int,
    val displayName: String,
    val translator: String? = null,
    val translatorForeign: String? = null,
    val languageCode: String,
    val fileUrl: String,
    val fileName: String,
    @SerialName("minimumVersion") val minimumVersion: Int = 1,
    @SerialName("currentVersion") val currentVersion: Int = 1,
    val saveTo: String? = null,
    val downloadType: String? = null,
) {
    val translationId: String get() = fileName.removeSuffix(".db")
}

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    EXTRACTING,
    COMPLETED,
    FAILED,
    CANCELLED,
}

data class DownloadProgress(
    val translationId: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val percentage: Float,
    val status: DownloadStatus,
)
