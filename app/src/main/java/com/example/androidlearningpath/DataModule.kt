package com.example.androidlearningpath

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideTareaDao(database: AppDatabase): TareaDao {
        return database.tareaDao()
    }

    // 🟢 AQUÍ ESTÁ EL CAMBIO: Hilt ahora fabrica el cliente directamente
    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    @Provides
    @Singleton
    fun provideTareaRepository(tareaDao: TareaDao, httpClient: HttpClient): TareaRepository {
        // 🟢 Ahora le pasamos ambas cosas al repositorio
        return TareaRepository(tareaDao, httpClient)
    }
}