package com.dicoding.story_app.view.main

import android.Manifest
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.story_app.R
import com.dicoding.story_app.adapter.StoryAdapter
import com.dicoding.story_app.databinding.ActivityMainBinding
import com.dicoding.story_app.view.addStory.AddStory
import com.dicoding.story_app.view.detail.DetailActivity
import com.dicoding.story_app.view.login.LoginActivity
import com.dicoding.story_app.view.maps.MapsActivity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: StoryAdapter
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    private val viewModel: MainViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()

    private var userToken: String? = null
    private var isDialogShown = false

    // Variabel untuk menyimpan posisi scroll terakhir
    private var lastScrollPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Story App"

        // Check if the permission dialog was shown previously (handle screen rotation)
        if (savedInstanceState != null) {
            isDialogShown = savedInstanceState.getBoolean("isDialogShown", false)
            if (isDialogShown) {
                showPermissionDialog()
            }

            // Ambil posisi scroll yang tersimpan
            lastScrollPosition = savedInstanceState.getInt("lastScrollPosition", 0)
        }

        initConnectivityManager()
        setupObservers()
        setupRecyclerView()

        addStory()
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    // Initialization
    private fun initConnectivityManager() {
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        observeNetwork()
        checkInitialConnection()
    }

    private fun setupObservers() {
        observeConnection()
        observeSession()
        observeStories()
    }

    private fun setupRecyclerView() {
        adapter = StoryAdapter { story ->
            moveToDetail(story.id)
        }
        binding.rvStory.adapter = adapter
    }


    // Network Handling
    private fun observeNetwork() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("NetworkStatus", "Network available")
                // Post the value on the main thread
                runOnUiThread {
                    networkViewModel.setConnectionStatus(true)
                }
            }

            override fun onLost(network: Network) {
                Log.d("NetworkStatus", "Network lost")
                // Post the value on the main thread
                runOnUiThread {
                    networkViewModel.setConnectionStatus(false)
                }
            }
        }

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun checkInitialConnection() {
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        val isConnected =
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        networkViewModel.setConnectionStatus(isConnected)
    }

    private fun observeConnection() {
        networkViewModel.isConnected.observe(this) { isConnected ->
            with(binding) {
                if (isConnected) {
                    tvNoData.visibility = View.GONE
                    rvStory.visibility = View.VISIBLE
                    fab.visibility = View.VISIBLE
                    showConnectionSnackbar(isConnected)
                    viewModel.fetchStoriesWithSession()
                } else {
                    showConnectionSnackbar(isConnected)
                    fab.visibility = View.GONE
                    tvNoData.visibility = View.VISIBLE
                    rvStory.visibility = View.GONE
                }
            }
        }
    }

    private fun showConnectionSnackbar(isConnected: Boolean) {
        val message = if (isConnected) "Internet connected" else "No internet connection"
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    // Session Handling
    private fun observeSession() {
        lifecycleScope.launch {
            viewModel.session
                .distinctUntilChanged()
                .collect { user ->
                    if (user.isLogin) {
                        Log.d("MainActivity", "Session valid, user is logged in. ${user.token}")
                        userToken = user.token
                        viewModel.fetchStoriesWithSession()
                    } else {
                        Log.d("MainActivity", "No session, redirecting to LoginActivity.")
                        moveToLogin()
                    }
                }
        }
    }

    private fun moveToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Stories Handling
    private fun observeStories() {
        viewModel.stories.observe(this) { pagingData ->
            lifecycleScope.launch {
                adapter.submitData(pagingData)

                // Observe the loading state and toggle the progress bar visibility
                adapter.loadStateFlow.collectLatest { loadState ->
                    when (loadState.refresh) {
                        is LoadState.Loading -> {
                            // Show the progress bar when data is loading
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is LoadState.NotLoading -> {
                            // Hide the progress bar when data is loaded
                            binding.progressBar.visibility = View.GONE
                        }
                        is LoadState.Error -> {
                            // Handle error state and hide the progress bar
                            binding.progressBar.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }


    override fun onPause() {
        super.onPause()

        val layoutManager = binding.rvStory.layoutManager as LinearLayoutManager
        lastScrollPosition = layoutManager.findFirstVisibleItemPosition()
    }

    override fun onResume() {
        super.onResume()


        val layoutManager = binding.rvStory.layoutManager as LinearLayoutManager
        layoutManager.scrollToPosition(lastScrollPosition)

        Log.d("MainActivity", "onResume called")
        viewModel.refreshStories()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isDialogShown", isDialogShown)
        outState.putInt("lastScrollPosition", lastScrollPosition) // Simpan posisi scroll
    }


    private fun moveToDetail(storyId: String) {
        Log.d("MainActivity", "Navigating to detail with storyId: $storyId and token: $userToken")
        val intent = Intent(this, DetailActivity::class.java)
        intent.putExtra("STORY_ID", storyId)
        intent.putExtra("USER_TOKEN", userToken)
        startActivity(intent)
    }

    private fun addStory() {
        binding.fab.setOnClickListener {
            checkPermissionForScan()
        }


    }
    // Handle story upload success and trigger refresh
    private fun handleStoryUploadSuccess() {
        viewModel.triggerRefreshStories()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.story_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.logout -> {
                showLogoutConfirmationDialog()
                true
            }

            R.id.maps -> {
                Log.d("MainActivity", "Navigating to MapsActivity with token: $userToken")
                val intent = Intent(this, MapsActivity::class.java)
                intent.putExtra("USER_TOKEN", userToken)
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this).apply {
            setTitle(getString(R.string.konfirmasi_logout))
            setMessage(getString(R.string.apakah_kamu_yakin_ingin_keluar))
            setPositiveButton("Ya") { _, _ ->
                viewModel.logout()
            }
            setNegativeButton("Batal") { dialog, _ ->
                // Tutup dialog
                dialog.dismiss()
            }
            setCancelable(true)
        }.create().show()
    }

    // Define the required camera permission for all Android versions
    private val requiredCameraPermission = Manifest.permission.CAMERA


    // Define the required media permission based on the Android version (Tiramisu or older)
    private val requiredMediaPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES // For Android Tiramisu (API 33) and above
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE // For older versions
        }

    // Register the permission request result callback
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraPermissionGranted = permissions[Manifest.permission.CAMERA] == true
            val mediaPermissionGranted = permissions[requiredMediaPermission] == true

            if (cameraPermissionGranted && mediaPermissionGranted) {
                showAddStoryActivity()
            } else {
                showPermissionDialog() // Show permission dialog if permissions are denied
            }
        }

    private fun showAddStoryActivity() {
        Log.d("MainActivity", "Navigating to AddStoryActivity with token: $userToken")
        val intent = Intent(this, AddStory::class.java)
        intent.putExtra("USER_TOKEN", userToken)
        startActivity(intent)
        handleStoryUploadSuccess()
        Log.d("MainActivity", "All permissions granted")
    }


    // Check if the required permissions are granted, if not, request them
    private fun checkPermissionForScan() {
        viewModel.checkPermissionForScan(
            requiredCameraPermission = requiredCameraPermission,
            requiredMediaPermission = requiredMediaPermission,
            onPermissionGranted = { showAddStoryActivity() },
            onPermissionDenied = {
                requestPermissionLauncher.launch(
                    arrayOf(requiredCameraPermission, requiredMediaPermission)
                )
            }
        )
    }

    // Show a dialog if the permission is denied, asking the user to enable permissions manually
    private fun showPermissionDialog() {
        isDialogShown = true
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.permission_rationale)) // Permission rationale message
            .setPositiveButton(getString(R.string.grant)) { _, _ ->
                // Navigate to app settings to enable permissions
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton(getString(R.string.cancel), null) // Cancel button to dismiss dialog
            .setOnDismissListener {
                isDialogShown = false // Reset dialog status when dismissed
            }
            .create()
            .show()
    }

}