package com.dicoding.story_app.view.addStory

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.story_app.data.Resource
import com.dicoding.story_app.data.StoryRepository
import com.dicoding.story_app.data.response.FileUploadResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

@HiltViewModel
class AddStoryViewModel @Inject constructor(
    private val repository: StoryRepository
) :
    ViewModel() {

    private val _imageUri = MutableLiveData<Uri?>()
    val imageUri: LiveData<Uri?> get() = _imageUri

    private val _uploadStatus = MutableLiveData<Resource<FileUploadResponse>>()
    val uploadStatus: LiveData<Resource<FileUploadResponse>> get() = _uploadStatus

    // Method to update the image URI
    fun setImageUri(uri: Uri) {
        _imageUri.value = uri
    }

    // Function to upload the story image with description
    fun uploadImage(token: String,
                    file: MultipartBody.Part,
                    description: RequestBody,
                    lat: RequestBody? = null,
                    lon: RequestBody? = null) {
        viewModelScope.launch {
            _uploadStatus.value = Resource.Loading()
            val response = repository.uploadImage(token, file, description,  lat, lon)

            if (response is Resource.Success) {
                // Handle success
                _uploadStatus.value = response // Set the success response to LiveData
            } else {
                // Handle error
                _uploadStatus.value = response // Set error state to LiveData
            }
        }
    }

}