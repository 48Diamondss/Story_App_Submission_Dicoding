package com.dicoding.story_app.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.liveData
import com.dicoding.story_app.data.pref.UserPreference
import com.dicoding.story_app.data.remote_mediator.StoryRemoteMediator
import com.dicoding.story_app.data.response.DetailResponse
import com.dicoding.story_app.data.response.FileUploadResponse
import com.dicoding.story_app.data.response.ListStoryItem
import com.dicoding.story_app.data.response.StoryResponse
import com.dicoding.story_app.data.room.StoryRoomDatabase
import com.dicoding.story_app.remote.ApiService
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

class StoryRepository @Inject constructor(
    private val apiService: ApiService,
    private val storyRoomDatabase: StoryRoomDatabase,
    private val userPreference: UserPreference
) {

    @OptIn(ExperimentalPagingApi::class)
    fun getAllStories(): LiveData<PagingData<ListStoryItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 5,
                enablePlaceholders = false
            ),
            remoteMediator = StoryRemoteMediator(storyRoomDatabase, apiService, userPreference),
            pagingSourceFactory = { storyRoomDatabase.storyDao().getAllStory() }
        ).liveData
    }

    suspend fun getDetail(storyId: String, token: String): DetailResponse {
        val response = apiService.getDetailStory("Bearer $token", storyId)
        Log.d("StoryRepository", "Response: $response")
        return response
    }

    suspend fun uploadImage(
        token: String,
        file: MultipartBody.Part,
        description: RequestBody,
        lat: RequestBody? = null,
        lon: RequestBody? = null
    ): Resource<FileUploadResponse> {
        return try {
            val response = apiService.uploadImage("Bearer $token", file, description, lat, lon)

            if (response.error == true) {
                Resource.Error(response.message ?: "Unknown error")
            } else {
                Resource.Success(response)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "An error occurred")
        }
    }

    suspend fun getStoriesWithLocation(token: String): StoryResponse {
        return apiService.getStoriesWithLocation("Bearer $token")
    }

}