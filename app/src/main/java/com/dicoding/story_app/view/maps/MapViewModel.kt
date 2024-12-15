package com.dicoding.story_app.view.maps

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.story_app.data.Result
import com.dicoding.story_app.data.StoryRepository
import com.dicoding.story_app.data.response.ListStoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: StoryRepository
) : ViewModel() {

    private val _storiesWithLocation = MutableLiveData<Result<List<ListStoryItem>>>()
    val storiesWithLocation: LiveData<Result<List<ListStoryItem>>> = _storiesWithLocation

    fun fetchStoriesWithLocation(token: String) {
        viewModelScope.launch {
            _storiesWithLocation.value = Result.Loading
            try {
                val response = repository.getStoriesWithLocation(token)
                if (!response.error!!) {
                    _storiesWithLocation.value = Result.Success(response.listStory)
                } else {
                    _storiesWithLocation.value = Result.Error(response.message ?: "Unknown Error")
                }
            } catch (e: Exception) {
                _storiesWithLocation.value = Result.Error(e.message ?: "An error occurred")
            }
        }
    }
}