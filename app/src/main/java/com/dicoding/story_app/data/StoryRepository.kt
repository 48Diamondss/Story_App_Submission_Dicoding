package com.dicoding.story_app.data

import android.util.Log
import com.dicoding.story_app.data.response.DetailResponse
import com.dicoding.story_app.data.response.FileUploadResponse
import com.dicoding.story_app.data.response.StoryResponse
import com.dicoding.story_app.remote.ApiService
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

class StoryRepository @Inject constructor(private val apiService: ApiService) {

    suspend fun getStories(token: String): StoryResponse {
        return apiService.getStories(token, page = 1, size = 10, location = 0).body()!!
    }

    fun getStoriesPagingSource(token: String): StoryPagingSource {
        return StoryPagingSource(this, token)
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