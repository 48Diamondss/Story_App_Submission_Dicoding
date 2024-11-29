package com.dicoding.story_app.data

import android.util.Log
import com.dicoding.story_app.data.pref.UserModel
import com.dicoding.story_app.data.pref.UserPreference
import com.dicoding.story_app.data.response.LoginResponse
import com.dicoding.story_app.data.response.RegisterResponse
import com.dicoding.story_app.remote.ApiService
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val apiService: ApiService,
    private val userPreference: UserPreference
) {
    suspend fun login(email: String, password: String): LoginResponse {
        val response = apiService.login(email, password)

        if (response.isSuccessful) {
            return response.body() ?: throw Exception("Unknown error")
        } else {
            // Jika respons gagal (400 Bad Request, dll), ambil pesan error dari body respons
            val errorBody = response.errorBody()?.string()
            val errorMessage = parseErrorMessage(errorBody)
            throw Exception(errorMessage)
        }
    }

    suspend fun signup(name: String, email: String, password: String): RegisterResponse {
        val response = apiService.register(name, email, password)

        if (response.isSuccessful) {
            return response.body() ?: throw Exception("Unknown error")
        } else {
            // Jika respons gagal (400 Bad Request, dll), ambil pesan error dari body respons
            val errorBody = response.errorBody()?.string()
            val errorMessage = parseErrorMessage(errorBody)
            throw Exception(errorMessage)
        }
    }

    private fun parseErrorMessage(errorBody: String?): String {
        return try {
            val gson = Gson()
            val errorResponse = gson.fromJson(errorBody, RegisterResponse::class.java)
            errorResponse.message ?: "Unknown error"
        } catch (e: Exception) {
            "An error occurred"
        }
    }


    suspend fun saveSession(user: UserModel) {
        Log.d("UserRepository", "Saving session for user: ${user.isLogin}")
        userPreference.saveSession(user)
    }

    // Method to get session from UserPreference
    fun getSession(): Flow<UserModel> {
        return userPreference.getSession()
    }

    suspend fun logout() {
        userPreference.logout()
    }


}
