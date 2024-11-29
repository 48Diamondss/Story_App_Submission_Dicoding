package com.dicoding.story_app.view.detail

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.dicoding.story_app.data.Result
import com.dicoding.story_app.data.StoryRepository
import com.dicoding.story_app.data.response.Story
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class DetailViewModel @Inject constructor(
    private val storyRepository: StoryRepository
) : ViewModel() {

    fun getDetail(storyId: String, token: String): LiveData<Result<Story>> = liveData {
        emit(Result.Loading)
        try {
            val response = storyRepository.getDetail(storyId, token)
            Log.d("DetailViewModel", "API Response: $response")
            val story = response.story ?: Story(
                id = storyId,
                name = "Unknown",
                description = "No description available",
                photoUrl = "",
                createdAt = "",
                lat = null,
                lon = null
            )
            emit(Result.Success(story))
        } catch (e: Exception) {
            Log.e("DetailViewModel", "Error fetching detail: ${e.message}")
            emit(Result.Error(e.message ?: "An error occurred"))
        }
    }


}