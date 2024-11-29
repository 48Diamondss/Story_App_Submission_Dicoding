package com.dicoding.story_app.view.addStory

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.dicoding.story_app.R
import com.dicoding.story_app.data.Resource
import com.dicoding.story_app.databinding.ActivityAddStoryBinding
import com.dicoding.story_app.utils.reduceFileImage
import com.dicoding.story_app.view.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class AddStory : AppCompatActivity() {

    private lateinit var binding: ActivityAddStoryBinding
    private val viewModel: AddStoryViewModel by viewModels()

    private var currentImageUri: Uri? = null
    private val tag = "ScanActivity"

    // Launcher for Android 13 and above (gallery)
    private val launcherGallery =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            handleImageSelection(uri)
        }

    // Launcher for gallery (below Android 13)
    private val pickImageLauncherLegacy =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            handleImageSelection(uri)
        }

    // Launcher for taking a picture from the camera
    private val launcherCamera =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentImageUri?.let { uri ->

                    viewModel.imageUri.value?.let { oldUri -> deleteCacheFile(oldUri) }
                    handleCapturedImage(uri)
                }
            } else {
                currentImageUri?.let { deleteCacheFile(it) }
                currentImageUri = null
                showToast(getString(R.string.cancel))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up UI listeners
        setupUiListeners()

        // Observe changes in the image URI and update UI accordingly
        viewModel.imageUri.observe(this) { uri ->
            uri?.let { displayImage(it) }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the current image URI for state restoration
        viewModel.imageUri.value?.let {
            outState.putString(EXTRA_IMAGE_URI, it.toString())
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Restore image URI from saved instance state
        savedInstanceState.getString(EXTRA_IMAGE_URI)?.let { uriString ->
            Uri.parse(uriString).let { uri ->
                viewModel.setImageUri(uri)
            }
        }
    }

    // Set up UI listeners for back, capture again, and gallery pick actions
    private fun setupUiListeners() {

        binding.btnBack.setOnClickListener {
            viewModel.imageUri.value?.let { uri -> deleteCacheFile(uri) }
            finish()
        }

        binding.btnCamera.setOnClickListener {
            startCamera()
        }

        binding.btnGallery.setOnClickListener {
            // Launch appropriate gallery picker based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                pickImageLauncherLegacy.launch("image/*") // Launch legacy gallery picker
            }
        }

        binding.buttonAdd.setOnClickListener {
            val imageUri = viewModel.imageUri.value
            val description = binding.edAddDescription.text.toString()
            val lat = null
            val lon = null

            if (imageUri != null && description.isNotEmpty()) {
                uploadStory(imageUri, description, lat, lon)
            } else {
                showToast("Please select an image and provide a description.")
            }
        }

    }

    // Start the camera to take a new picture
    private fun startCamera() {
        currentImageUri = createCacheUri() // Create a new URI for the image
        currentImageUri?.let { launcherCamera.launch(it) } // Launch the camera with the URI
    }

    // Create a cache URI
    private fun createCacheUri(originalUri: Uri? = null): Uri? {
        return try {
            val cacheDir = cacheDir
            val file = File(cacheDir, "image_cache_${System.currentTimeMillis()}.jpg")

            // Copy originalUri contents to cache
            originalUri?.let {
                contentResolver.openInputStream(it)?.use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }

            FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        } catch (e: Exception) {
            Log.e("Cache", "Error creating cache URI: ${e.localizedMessage}")
            null
        }
    }

    // Display the selected image using Glide
    private fun displayImage(uri: Uri) {
        Glide.with(this)
            .clear(binding.ivPreview) // Clear previous image
        Glide.with(this)
            .load(uri)
            .placeholder(R.drawable.ic_place_holder)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(binding.ivPreview)
    }

    // handle image from gallery
    private fun handleImageSelection(uri: Uri?) {
        if (uri != null) {
            try {
                viewModel.imageUri.value?.let { oldUri ->
                    deleteCacheFile(oldUri)
                    Log.d(tag, "Old image file deleted: $oldUri")
                }

                val cacheUri = createCacheUri(uri) ?: uri
                val file = getFileFromUri(cacheUri)

                if (file != null) {
                    // Reduce the file image size
                    file.reduceFileImage()

                    // Update the URI after reduction
                    viewModel.setImageUri(Uri.fromFile(file))
                    displayImage(Uri.fromFile(file))

                    Log.d(tag, "Image selected, reduced, and cached: ${file.absolutePath}")
                } else {
                    showToast(getString(R.string.failed_to_process_selected_image))
                    Log.e(tag, getString(R.string.failed_to_process_selected_image))
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to handle selected image: ${e.localizedMessage}")
                showToast("Failed to process selected image")
            }
        } else {
            showToast(getString(R.string.no_image_selected))
            Log.w(tag, "No image selected")
        }
    }

    // handling capture image with reduce size
    private fun handleCapturedImage(uri: Uri) {
        try {
            val file = getFileFromUri(uri)

            if (file != null) {

                file.reduceFileImage()  // Mengurangi ukuran gambar

                // Update URI setelah reduksi
                viewModel.setImageUri(Uri.fromFile(file))
                displayImage(Uri.fromFile(file))  // Menampilkan gambar yang sudah diproses

                Log.d(tag, "Image captured, reduced, and cached: ${file.absolutePath}")
            } else {
                showToast(getString(R.string.failed_to_process_selected_image))
                Log.e(tag, getString(R.string.failed_to_process_selected_image))
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to handle captured image: ${e.localizedMessage}")
            showToast("Failed to process captured image")
        }
    }

    // Upload image with description
    private fun uploadStory(imageUri: Uri, description: String, lat: String?, lon: String?) {
        val userToken = intent.getStringExtra("USER_TOKEN")
        val token = "$userToken"

        Log.d("UploadStory", "Start uploading process for image: $imageUri with description: $description")

        val file = getFileFromUri(imageUri) ?: run {
            Log.e("UploadStory", "File conversion failed for Uri: $imageUri")
            showToast("Failed to prepare image for upload.")
            return
        }

        val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())

        val descriptionBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
        val latBody = lat?.toRequestBody("text/plain".toMediaTypeOrNull())
        val lonBody = lon?.toRequestBody("text/plain".toMediaTypeOrNull())

        val filePart = MultipartBody.Part.createFormData("photo", file.name, requestBody)

        Log.d("UploadStory", "Prepared file and description for upload: ${file.name}")

        // Call repository to upload the file
        viewModel.uploadImage(token, filePart, descriptionBody, latBody, lonBody)

        viewModel.uploadStatus.observe(this) { status ->
            when (status) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = android.view.View.VISIBLE
                    Log.d("UploadStory", "Uploading image...")
                    showToast("Uploading...")
                }
                is Resource.Success -> {
                    Log.d("UploadStory", "Upload successful. Proceeding to save to gallery.")
                    showToast("Upload successful!")

                    // Langkah 2: Simpan ke galeri
                    saveToGallery(imageUri) { gallerySuccess ->
                        if (gallerySuccess) {
                            Log.d("UploadStory", "Image successfully saved to gallery. Deleting cache file.")
                            // Langkah 3: Hapus cache
                            deleteCacheFile(imageUri)
                            Log.d("UploadStory", "Cache file deleted. Navigating to MainActivity.")
                            // Setelah semua selesai, arahkan ke MainActivity
                            binding.progressBar.visibility = android.view.View.GONE
                            navigateToMainActivity()
                        } else {
                            binding.progressBar.visibility = android.view.View.GONE
                            Log.e("UploadStory", "Failed to save image to gallery.")
                            showToast("Failed to save image to gallery.")
                        }
                    }
                }
                is Resource.Error -> {
                    Log.e("UploadStory", "Upload failed: ${status.message}")
                    showToast("Upload failed: ${status.message}")
                }
            }
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val cacheFile = File(cacheDir, "image_cache_${System.currentTimeMillis()}.jpg")
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(cacheFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            cacheFile
        } catch (e: Exception) {
            Log.e("FileFromUri", "Error: ${e.localizedMessage}")
            null
        }
    }

    private fun saveToGallery(uri: Uri, callback: (Boolean) -> Unit) {
        try {
            val resolver = contentResolver
            val cachedUri = createCacheUri(uri) ?: uri

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "image_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/StoryApp"
                )
            }

            val newImageUri =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (newImageUri != null) {
                resolver.openInputStream(cachedUri)?.use { inputStream ->
                    resolver.openOutputStream(newImageUri)?.use { outputStream ->
                        inputStream.copyTo(outputStream)
                        Log.d("Gallery", "Image successfully saved to gallery: $newImageUri")
                        callback(true) // Sukses
                    }
                }
            } else {
                Log.e("Gallery", "Failed to create new image entry in MediaStore")
                callback(false) // Gagal
            }
        } catch (e: Exception) {
            Log.e("Gallery", "Error saving image to gallery: ${e.localizedMessage}")
            callback(false) // Gagal
        }
    }

    private fun navigateToMainActivity() {
        Log.d("UploadStory", "Navigating to MainActivity.")
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun deleteCacheFile(uri: Uri? = null) {
        try {
            val cacheDir = cacheDir
            if (uri != null) {
                // Delete a specific file
                val file = File(cacheDir, uri.lastPathSegment ?: return)
                if (file.exists() && file.delete()) {
                    Log.i("Cache", "Deleted image file: ${file.absolutePath}")
                } else {
                    Log.w("Cache", "File not found or failed to delete: ${file.absolutePath}")
                }
            } else {
                // Delete all image cache files
                cacheDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("image_cache_") && file.name.endsWith(".jpg")) {
                        if (file.delete()) {
                            Log.i("Cache", "Deleted cache file: ${file.absolutePath}")
                        } else {
                            Log.w("Cache", "Failed to delete cache file: ${file.absolutePath}")
                        }
                    } else {
                        Log.i("Cache", "Skipped non-image cache file: ${file.absolutePath}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Cache", "Error deleting cache file: ${e.localizedMessage}")
        }
    }

    // Show a toast message
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_IMAGE_URI = "imageUri"
    }

}