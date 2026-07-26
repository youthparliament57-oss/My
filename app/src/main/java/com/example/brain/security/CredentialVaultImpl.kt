package com.example.brain.security

import com.example.brain.memory.MemoryDao
import com.example.brain.memory.MemoryEncryption
import com.example.brain.memory.UserPreferenceEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialVaultImpl @Inject constructor(
    private val memoryDao: MemoryDao
) : CredentialVault {

    override suspend fun storeApiKey(provider: String, apiKey: String) {
        val storageKey = getStorageKeyForProvider(provider)
        val encryptedValue = MemoryEncryption.encrypt(apiKey)
        val entity = UserPreferenceEntity(
            key = storageKey,
            encryptedValue = encryptedValue,
            timestamp = System.currentTimeMillis()
        )
        memoryDao.insertUserPreference(entity)
    }

    override suspend fun getApiKey(provider: String): String? {
        val storageKey = getStorageKeyForProvider(provider)
        val entity = memoryDao.getUserPreference(storageKey) ?: return null
        val decrypted = MemoryEncryption.decrypt(entity.encryptedValue)
        return decrypted.ifEmpty { null }
    }

    override suspend fun deleteApiKey(provider: String) {
        storeApiKey(provider, "")
    }

    private fun getStorageKeyForProvider(provider: String): String {
        return "api_key_${provider.lowercase().trim()}"
    }
}
