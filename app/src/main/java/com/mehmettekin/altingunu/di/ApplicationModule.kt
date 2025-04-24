package com.mehmettekin.altingunu.di

import com.mehmettekin.altingunu.data.remote.KapaliCarsiApi
import com.mehmettekin.altingunu.data.repository.DrawRepositoryImpl
import com.mehmettekin.altingunu.data.repository.KapaliCarsiRepositoryImpl
import com.mehmettekin.altingunu.data.repository.UserPreferencesRepositoryImpl
import com.mehmettekin.altingunu.domain.repository.DrawRepository
import com.mehmettekin.altingunu.domain.repository.KapaliCarsiRepository
import com.mehmettekin.altingunu.domain.repository.UserPreferencesRepository
import com.mehmettekin.altingunu.domain.usecase.GetCurrentRateUseCase
import com.mehmettekin.altingunu.domain.usecase.ValidateDrawSettingsUseCase
import com.mehmettekin.altingunu.domain.usecase.ValidateParticipantsUseCase
import com.mehmettekin.altingunu.presentation.screens.enter.KapaliCarsiViewModel
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
        fun provideApplicationCoroutineScope(): CoroutineScope {
            return CoroutineScope(SupervisorJob() + Dispatchers.IO)
        }

        @Provides
        @Singleton
        fun provideValidateDrawSettingsUseCase(): ValidateDrawSettingsUseCase {
            return ValidateDrawSettingsUseCase()
        }

        @Provides
        @Singleton
        fun provideValidateParticipantsUseCase(): ValidateParticipantsUseCase {
            return ValidateParticipantsUseCase()
        }

        @Provides
        @Singleton
        fun provideGetCurrentRateUseCase(kapaliCarsiViewModel: KapaliCarsiViewModel): GetCurrentRateUseCase {
            return GetCurrentRateUseCase(kapaliCarsiViewModel)
        }

    }
}

/*
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

 */