package com.dicoding.story_app.view.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.story_app.R
import com.dicoding.story_app.data.Result
import com.dicoding.story_app.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class SignupViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _signupResult = MutableLiveData<Result<Int>>()
    val signupResult: LiveData<Result<Int>> get() = _signupResult


    fun signup(name: String, email: String, password: String) {
        viewModelScope.launch {
            try {
                _signupResult.value = Result.Loading
                val response = repository.signup(name, email, password)
                if (response.error == false) {
                    // Berhasil mendaftar
                    _signupResult.value = Result.Success(R.string.signup_success)
                } else {
                    // Gagal mendaftar, tampilkan pesan error dari server
                    _signupResult.value = Result.Error(response.message ?: "Unknown error")
                }

            } catch (e: Exception) {
                _signupResult.value = Result.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }
}