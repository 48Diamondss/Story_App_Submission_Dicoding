package com.dicoding.story_app.view.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.story_app.data.Result
import com.dicoding.story_app.data.UserRepository
import com.dicoding.story_app.data.pref.UserModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _loginResult = MutableLiveData<Result<UserModel>>()
    val loginResult: LiveData<Result<UserModel>> get() = _loginResult

    private val _sessionSaved = MutableLiveData<Boolean>()
    val sessionSaved: LiveData<Boolean> get() = _sessionSaved

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                _loginResult.value = Result.Loading
                // Login request response
                val response = repository.login(email, password)
                // Check for valid response and map it to UserModel
                val user = response.loginResult?.let {
                    UserModel(email = email, token = it.token ?: "", isLogin = true)
                }
                if (user != null) {
                    _loginResult.value = Result.Success(user)
                    saveSession(user)
                    Log.d("LoginViewModel", "User session: ${user.isLogin}")
                } else {
                    _loginResult.value = Result.Error("Invalid credentials")
                }
            } catch (e: Exception) {
                _loginResult.value = Result.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    private fun saveSession(user: UserModel) {
        viewModelScope.launch {
            repository.saveSession(user)
            _sessionSaved.postValue(true)
        }
    }

}
