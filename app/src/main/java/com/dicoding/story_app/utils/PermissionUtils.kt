package com.dicoding.story_app.utils

import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import javax.inject.Inject

class PermissionUtils @Inject constructor(private val app: Application) {

    // Check Permission
    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(app, permission) == PackageManager.PERMISSION_GRANTED
    }

}


