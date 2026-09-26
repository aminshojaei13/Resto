package com.braveboy.calcuapp.data.repository

import com.braveboy.calcuapp.data.remote.CalcuappApiService
import com.braveboy.calcuapp.data.remote.NetworkModule

interface MessageRepository {
    suspend fun parseMessage(rawText: String, source: String = "manual_paste"): Map<String, Any>
    suspend fun getMessages(orgId: String): List<Map<String, Any>>
}

class MessageRepositoryImpl(
    private val apiService: CalcuappApiService = NetworkModule.apiService
) : MessageRepository {

    override suspend fun parseMessage(rawText: String, source: String): Map<String, Any> {
        val res = apiService.parseMessage(rawText, source)
        if (res.isSuccessful && res.body() != null) {
            return res.body() as Map<String, Any>
        }
        return emptyMap()
    }

    override suspend fun getMessages(orgId: String): List<Map<String, Any>> {
        val res = apiService.getImportedMessages(orgId)
        if (res.isSuccessful && res.body() != null) {
            return res.body() as List<Map<String, Any>>
        }
        return emptyList()
    }
}
