package com.dicoding.story_app.view.detail

import android.os.Bundle
import android.util.Log
import android.view.View

import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

import com.bumptech.glide.Glide
import com.dicoding.story_app.data.Result
import com.dicoding.story_app.databinding.ActivityDetailBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private val viewModel: DetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Detail"

        // Mendapatkan ID dan token dari intent
        val storyId = intent.getStringExtra("STORY_ID")
        val userToken = intent.getStringExtra("USER_TOKEN")

        Log.d("DetailActivity", "Received storyId: $storyId and token: $userToken")


        storyId?.let { userToken?.let { it1 -> observeDetail(it, it1) } }
    }

    private fun observeDetail(storyId: String, token: String) {

        viewModel.getDetail(storyId, token).observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }

                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val story = result.data
                    binding.apply {
                        tvDetailName.text = story.name
                        tvDetailDescription.text = story.description
                        dateTextView.text = story.createdAt
                        Log.d(
                            "DetailActivity",
                            "Story name: ${story.name}, description: ${story.description}"
                        )
                        Glide.with(this@DetailActivity)
                            .load(story.photoUrl)
                            .centerCrop()
                            .into(binding.ivDetailPhoto)
                    }
                }

                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Log.e("DetailActivity", "Error: ${result.error}")
                }
            }
        }
    }

}
