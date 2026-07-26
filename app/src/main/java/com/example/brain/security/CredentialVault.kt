package com.example.brain.security

interface CredentialVault {
    suspend fun storeApiKey(provider: String, apiKey: String)
    suspend fun getApiKey(provider: String): String?
    suspend fun deleteApiKey(provider: String)
}
