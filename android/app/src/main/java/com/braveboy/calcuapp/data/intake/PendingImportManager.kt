package com.braveboy.calcuapp.data.intake

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PendingImport(
    val rawText: String,
    val source: String,
    val timestamp: Long = System.currentTimeMillis()
)

object PendingImportManager {

    private const val MAX_RAW_TEXT_LENGTH = 8000

    private val _pendingImport = MutableStateFlow<PendingImport?>(null)
    val pendingImportFlow: StateFlow<PendingImport?> = _pendingImport.asStateFlow()

    fun setPendingImport(rawText: String, source: String = "android_share") {
        val sanitized = sanitize(rawText)
        if (sanitized.isNotBlank()) {
            _pendingImport.value = PendingImport(
                rawText = sanitized,
                source = detectSource(sanitized, source)
            )
        }
    }

    fun consumePendingImport(): PendingImport? {
        val current = _pendingImport.value
        _pendingImport.value = null
        return current
    }

    fun peekPendingImport(): PendingImport? = _pendingImport.value

    fun clearPendingImport() {
        _pendingImport.value = null
    }

    fun sanitize(text: String): String {
        return text
            .take(MAX_RAW_TEXT_LENGTH)
            .replace("\u0000", "")
            .trim()
    }

    fun detectSource(text: String, fallbackSource: String = "manual_paste"): String {
        val lower = text.lowercase()
        return when {
            lower.contains("instagram.com") || lower.contains("اینستاگرام") || lower.contains("ig:") -> "INSTAGRAM"
            lower.contains("t.me/") || lower.contains("telegram") || lower.contains("تلگرام") -> "TELEGRAM"
            lower.contains("wa.me/") || lower.contains("whatsapp") || lower.contains("واتساپ") || lower.contains("واتس‌اپ") -> "WHATSAPP"
            fallbackSource.equals("android_share", ignoreCase = true) -> "ANDROID_SHARE"
            fallbackSource.equals("instagram", ignoreCase = true) -> "INSTAGRAM"
            fallbackSource.equals("telegram", ignoreCase = true) -> "TELEGRAM"
            fallbackSource.equals("whatsapp", ignoreCase = true) -> "WHATSAPP"
            else -> "MANUAL_PASTE"
        }
    }
}
