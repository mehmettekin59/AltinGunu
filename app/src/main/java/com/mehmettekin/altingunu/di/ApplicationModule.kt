package com.mehmettekin.altingunu.di

import com.mehmettekin.altingunu.data.remote.KapaliCarsiApi
import com.mehmettekin.altingunu.data.repository.DrawRepositoryImpl
import com.mehmettekin.altingunu.data.repository.KapaliCarsiRepositoryImpl
import com.mehmettekin.altingunu.data.repository.UserPreferencesRepositoryImpl
import com.mehmettekin.altingunu.domain.repository.DrawRepository
import com.mehmettekin.altingunu.domain.repository.KapaliCarsiRepository
import com.mehmettekin.altingunu.domain.repository.UserPreferencesRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ApplicationModule {

    @Binds
    abstract fun bindKapaliCarsiRepository(impl: KapaliCarsiRepositoryImpl): KapaliCarsiRepository

    @Binds
    abstract fun bindDrawRepository(impl: DrawRepositoryImpl): DrawRepository

    @Binds
    abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository

    companion object {
        @Provides
        @Singleton
        fun provideMoshi(): Moshi {
            return Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        }

        /*
        @Provides

        @Singleton
        fun provideKapaliCarsiApi(moshi: Moshi): KapaliCarsiApi {
            return Retrofit.Builder()
                .baseUrl("https://kapalicarsi.apiluna.org")
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(KapaliCarsiApi::class.java)
        }


         */
        @Provides
        @Singleton
        fun provideKapaliCarsiApi(moshi: Moshi): KapaliCarsiApi {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val dns = Dns.SYSTEM
            val client = OkHttpClient.Builder()
                .dns(dns)
                .addInterceptor(loggingInterceptor)
                .build()

            return Retrofit.Builder()
                .baseUrl("https://kapalicarsi.apiluna.org")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(KapaliCarsiApi::class.java)
        }



        @Provides
        @Singleton
        fun provideApplicationCoroutineScope(): CoroutineScope {
            return CoroutineScope(SupervisorJob() + Dispatchers.IO)
        }
    }
}

