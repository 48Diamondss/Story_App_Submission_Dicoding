package com.dicoding.story_app.view.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.dicoding.story_app.data.StoryRepository
import com.dicoding.story_app.data.UserRepository
import com.dicoding.story_app.data.pref.UserModel
import com.dicoding.story_app.data.response.ListStoryItem
import com.dicoding.story_app.utils.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: UserRepository,
    storyRepository: StoryRepository,
    private val permissionUtils: PermissionUtils
) : ViewModel() {

    private val _stories = MutableLiveData<PagingData<ListStoryItem>>()
    val stories: LiveData<PagingData<ListStoryItem>> =
        storyRepository.getAllStories().cachedIn(viewModelScope)

    val session: Flow<UserModel> = repository.getSession()

    fun fetchStoriesWithSession() {
        viewModelScope.launch {
            session.collect { user ->
                if (user.isLogin) {
                   Log.d("MainViewModel", "Fetching stories with token: ${user.token}")
                } else {
                    _stories.value = PagingData.empty()
                }
            }
        }
    }

    // Method to refresh stories
    fun refreshStories() {
        fetchStoriesWithSession()
    }


    // Trigger refresh stories
    fun triggerRefreshStories() {
        _stories.value = PagingData.empty() // Reset the stories to show loading state
        fetchStoriesWithSession() // Re-fetch the stories after upload
    }

    fun logout(){
        viewModelScope.launch {
            repository.logout()
        }
    }

    // Method to check if the required permissions for scanning are granted
    fun checkPermissionForScan(
        requiredMediaPermission: String, // Media permission (e.g., storage or images)
        requiredCameraPermission: String, // Camera permission
        onPermissionGranted: () -> Unit, // Action to take if permissions are granted
        onPermissionDenied: () -> Unit // Action to take if permissions are denied
    ) {
        val cameraPermissionGranted = permissionUtils.hasPermission(requiredCameraPermission)
        val mediaPermissionGranted = permissionUtils.hasPermission(requiredMediaPermission)

        // Check different permission scenarios and take appropriate actions
        when {
            cameraPermissionGranted && mediaPermissionGranted -> {
                onPermissionGranted() // Both permissions granted, proceed with showing scan bottom sheet
            }

            cameraPermissionGranted -> {
                // Camera permission granted, but media permission not granted, request media permission
                onPermissionDenied()
            }

            mediaPermissionGranted -> {
                // Media permission granted, but camera permission not granted, request camera permission
                onPermissionDenied()
            }

            else -> {
                // Neither permission granted, request both permissions
                onPermissionDenied()
            }
        }
    }

}