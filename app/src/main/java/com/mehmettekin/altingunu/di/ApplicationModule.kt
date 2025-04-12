package com.mehmettekin.altingunu.di

import com.mehmettekin.altingunu.data.remote.KapaliCarsiApi
import com.mehmettekin.altingunu.data.repository.DrawRepositoryImpl
import com.mehmettekin.altingunu.data.repository.KapaliCarsiRepositoryImpl
import com.mehmettekin.altingunu.domain.repository.DrawRepository
import com.mehmettekin.altingunu.domain.repository.KapaliCarsiRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {
    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideKapaliCarsiApi(moshi: Moshi): KapaliCarsiApi {
        return Retrofit.Builder()
            .baseUrl("https://kapalicarsi.apiluna.org")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(KapaliCarsiApi::class.java)
    }



    @Provides
    @Singleton
    fun provideKapaliCarsiApi(retrofit: Retrofit): KapaliCarsiApi {
        return retrofit.create(KapaliCarsiApi::class.java)
    }

    @Provides
    @Singleton
    fun provideKapaliCarsiRepository(api: KapaliCarsiApi): KapaliCarsiRepository {
        return KapaliCarsiRepositoryImpl(api)
    }

    @Provides
    @Singleton
    fun provideDrawRepository(impl: DrawRepositoryImpl): DrawRepository {
        return impl
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository {
        return impl
    }
}