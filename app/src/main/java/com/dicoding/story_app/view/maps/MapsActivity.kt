package com.dicoding.story_app.view.maps

import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dicoding.story_app.R
import com.dicoding.story_app.data.Result
import com.dicoding.story_app.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val mapViewModel: MapViewModel by viewModels()
    private lateinit var binding: ActivityMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mendapatkan ID dan token dari intent
        val userToken = intent.getStringExtra("USER_TOKEN")

        Log.d("MapsActivity", "Received token: $userToken")

        setupToolbar()

        // Initialize the map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Observe data
        observeStories(userToken)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.map)
            setHomeAsUpIndicator(R.drawable.ic_arrow_back)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun observeStories(userToken: String?) {

        if (userToken != null) {
            mapViewModel.fetchStoriesWithLocation(userToken)
        }
        mapViewModel.storiesWithLocation.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.visibility = android.view.View.VISIBLE
                }

                is Result.Success -> {
                    binding.progressBar.visibility = android.view.View.GONE

                    if (result.data.isNotEmpty()) {
                        val boundsBuilder = LatLngBounds.Builder()

                        result.data.forEach { story ->
                            val lat = story.lat
                            val lon = story.lon
                            if (lat != null && lon != null) {
                                val position = LatLng(lat, lon)
                                mMap.addMarker(
                                    MarkerOptions()
                                        .position(position)
                                        .title(story.name)
                                        .snippet(story.description)
                                )
                                boundsBuilder.include(position)
                            }
                        }
                        // Zoom peta ke lokasi marker
                        val bounds = boundsBuilder.build()
                        val padding = 100
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                    } else {
                        Toast.makeText(this, "No markers to display", Toast.LENGTH_SHORT).show()
                    }
                }

                is Result.Error -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, result.error, Toast.LENGTH_SHORT).show()
                }
            }
            Log.d("MapsActivity", "Data Maps story: $result")
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setupMapSettings()
        getMyLocation()
        setMapStyle()
    }

    private fun setupMapSettings() {
        mMap.apply {
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isIndoorLevelPickerEnabled = true
            uiSettings.isCompassEnabled = true
            uiSettings.isMapToolbarEnabled = true

        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                getMyLocation()
            }
        }

    private fun getMyLocation() {
        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun setMapStyle() {
        try {
            val success =
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))
            if (!success) {
                Toast.makeText(this, "Style parsing failed.", Toast.LENGTH_SHORT).show()
            }
        } catch (exception: Resources.NotFoundException) {
            Toast.makeText(this, "Can't find style. Error: $exception", Toast.LENGTH_SHORT).show()
        }
    }
}
