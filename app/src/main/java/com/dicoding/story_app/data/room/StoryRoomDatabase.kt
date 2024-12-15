package com.dicoding.story_app.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dicoding.story_app.data.remote_mediator.RemoteKeys
import com.dicoding.story_app.data.remote_mediator.RemoteKeysDao
import com.dicoding.story_app.data.response.ListStoryItem


@Database(entities = [ListStoryItem::class, RemoteKeys::class], version = 11, exportSchema = false)
abstract class StoryRoomDatabase : RoomDatabase() {

    abstract fun storyDao(): StoryDao
    abstract fun remoteKeysDao(): RemoteKeysDao
}