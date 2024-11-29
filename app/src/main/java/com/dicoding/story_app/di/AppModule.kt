package com.dicoding.story_app.di

import android.app.Application
import android.content.Context
import com.dicoding.story_app.data.pref.UserPreference
import com.dicoding.story_app.data.pref.dataStore
import com.dicoding.story_app.remote.ApiConfig
import com.dicoding.story_app.remote.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideApiService(): ApiService {
        return ApiConfig().getApiService()
    }

    @Singleton
    @Provides
    fun provideUserPreference(context: Context): UserPreference {
        return UserPreference.getInstance(context.dataStore)
    }


    @Singleton
    @Provides
    fun provideApplicationContext(application: Application): Context {
        return application.applicationContext
    }
}
