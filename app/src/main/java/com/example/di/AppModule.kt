package com.example.di

import com.example.di.DatabaseKeyManager
import android.content.Context
import androidx.room.Room
import net.sqlcipher.database.SupportFactory
import com.example.data.local.NousDao
import com.example.data.local.NousDatabase
import com.example.data.remote.GeminiApiService
import com.example.data.remote.GeminiClient
import com.example.data.repository.NousRepositoryImpl
import com.example.domain.repository.NousRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NousDatabase {
        val passphrase = DatabaseKeyManager.getDatabasePassphrase()
        val factory = SupportFactory(passphrase)
        
        return Room.databaseBuilder(
            context,
            NousDatabase::class.java,
            "nous_db"
        )
        .openHelperFactory(factory)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideNousDao(database: NousDatabase): NousDao {
        return database.nousDao()
    }

    @Provides
    @Singleton
    fun provideMemoryDao(database: NousDatabase): com.example.brain.memory.MemoryDao {
        return database.memoryDao()
    }


    @Provides
    @Singleton
    fun provideMemoryInterface(
        memoryDao: com.example.brain.memory.MemoryDao,
        omniSlm: com.example.vision.OmniSlmRuntime
    ): com.example.brain.memory.MemoryInterface {
        return com.example.brain.memory.MemoryInterfaceImpl(memoryDao, omniSlm)
    }

    @Provides
    @Singleton
    fun provideCredentialVault(
        memoryDao: com.example.brain.memory.MemoryDao
    ): com.example.brain.security.CredentialVault {
        return com.example.brain.security.CredentialVaultImpl(memoryDao)
    }

    @Provides
    @Singleton
    fun provideGeminiApiService(): GeminiApiService {
        return GeminiClient.service
    }

    @Provides
    @Singleton
    fun provideNousRepository(
        nousDao: NousDao,
        geminiApiService: GeminiApiService
    ): NousRepository {
        return NousRepositoryImpl(nousDao, geminiApiService)
    }

    @Provides
    @Singleton
    fun provideBrainInterface(brainFacade: com.example.brain.BrainFacade): com.example.brain.BrainInterface {
        return brainFacade
    }
}
