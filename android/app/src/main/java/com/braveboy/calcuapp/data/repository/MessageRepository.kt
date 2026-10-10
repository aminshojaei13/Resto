package com.braveboy.calcuapp.data.repository

import com.braveboy.calcuapp.data.intake.DeterministicMessageParser
import com.braveboy.calcuapp.data.intake.ParsedOrderDraft
import com.braveboy.calcuapp.data.model.Product
import com.braveboy.calcuapp.data.remote.CalcuappApiService
import com.braveboy.calcuapp.data.remote.NetworkModule
import com.braveboy.calcuapp.data.remote.dto.ParseMessageRequestDto
import com.braveboy.calcuapp.data.remote.dto.ParseMessageResponseDto

interface MessageRepository {
    suspend fun parseMessage(rawText: String, source: String = "manual_paste", locale: String = "fa"): ParseMessageResponseDto?
    fun parseMessageLocally(rawText: String, source: String = "manual_paste", catalog: List<Product>, taxRate: Double = 0.0, warehouseId: String = ""): ParsedOrderDraft
    suspend fun getMessages(orgId: String): List<Map<String, Any>>
}

class MessageRepositoryImpl(
    private val apiService: CalcuappApiService = NetworkModule.apiService
) : MessageRepository {

    override suspend fun parseMessage(rawText: String, source: String, locale: String): ParseMessageResponseDto? {
        return try {
            val res = apiService.parseMessage(
                ParseMessageRequestDto(rawText = rawText, source = source, locale = locale)
            )
            if (res.isSuccessful && res.body() != null) {
                res.body()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun parseMessageLocally(
        rawText: String,
        source: String,
        catalog: List<Product>,
        taxRate: Double,
        warehouseId: String
    ): ParsedOrderDraft {
        return DeterministicMessageParser.parse(
            rawText = rawText,
            source = source,
            catalog = catalog,
            taxRate = taxRate,
            warehouseId = warehouseId
        )
    }

    override suspend fun getMessages(orgId: String): List<Map<String, Any>> {
        return try {
            val res = apiService.getImportedMessages(orgId)
            if (res.isSuccessful && res.body() != null) {
                res.body() as List<Map<String, Any>>
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
