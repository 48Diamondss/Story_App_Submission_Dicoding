package com.dicoding.story_app.di

import android.content.Context
import androidx.room.Room
import com.dicoding.story_app.data.pref.UserPreference
import com.dicoding.story_app.data.remote_mediator.RemoteKeysDao
import com.dicoding.story_app.data.remote_mediator.StoryRemoteMediator
import com.dicoding.story_app.data.room.StoryDao
import com.dicoding.story_app.data.room.StoryRoomDatabase
import com.dicoding.story_app.remote.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideStoryRoomDatabase(context: Context): StoryRoomDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            StoryRoomDatabase::class.java,
            "story_db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideStoryDao(database: StoryRoomDatabase): StoryDao {
        return database.storyDao()
    }

    @Provides
    @Singleton
    fun provideRemoteKeysDao(database: StoryRoomDatabase): RemoteKeysDao {
        return database.remoteKeysDao()
    }

    @Provides
    @Singleton
    fun provideStoryRemoteMediator(
        database: StoryRoomDatabase,
        apiService: ApiService,
        userPreference: UserPreference
    ): StoryRemoteMediator {
        return StoryRemoteMediator(database, apiService, userPreference)
    }
}
