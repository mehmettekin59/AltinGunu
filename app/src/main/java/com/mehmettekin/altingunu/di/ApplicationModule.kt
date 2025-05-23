package com.mehmettekin.altingunu.di

import android.content.Context
import com.mehmettekin.altingunu.AltinGunuApplication
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
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope


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
            return ProcessLifecycleOwner.get().lifecycleScope
        }


        // AltinGunuApplication için provider ekleyin
        @Provides
        @Singleton
        fun provideApplication(@ApplicationContext context: Context): AltinGunuApplication {
            return context.applicationContext as AltinGunuApplication
        }

    }
}

