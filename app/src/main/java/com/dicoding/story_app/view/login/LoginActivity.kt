package com.dicoding.story_app.view.login

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.dicoding.story_app.R
import com.dicoding.story_app.data.Result
import com.dicoding.story_app.databinding.ActivityLoginBinding
import com.dicoding.story_app.view.main.MainActivity
import com.dicoding.story_app.view.signup.SignupActivity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private val tag = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupView()
        setupAction()
        playAnimation()

        observeLoginResult()
        observeSessionSaved()
    }

    private fun setupView() {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        supportActionBar?.hide()
    }

    private fun setupAction() {
        Log.d(tag, "setupView: Setting up the view...")
        with(binding) {
            edLoginEmail.setParentLayout(emailEditTextLayout)
            edLoginPassword.setParentLayout(passwordEditTextLayout)

            loginButton.setOnClickListener {
                val email = edLoginEmail.text.toString()
                val password = edLoginPassword.text.toString()

                Log.d(tag, "loginButton clicked with email: $email and password: $password")

                viewModel.login(email, password)

            }
            btnRegister.setOnClickListener {
                Log.d(tag, "Register button clicked, navigating to signup...")
                moveToSignup()
            }

        }

    }

    private fun observeLoginResult() {
        Log.d(tag, "observeLoginResult: Observing login result...")
        viewModel.loginResult.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    Log.d(tag, "Login is in progress...")
                    showLoading(true)
                }

                is Result.Success -> {
                    Log.d(tag, "Login successful: ${result.data}")
                    showLoading(false)

                }

                is Result.Error -> {
                    Log.e(tag, "Login error: ${result.error}")
                    showLoading(false)
                    Toast.makeText(this, "Error: ${result.error}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeSessionSaved() {
        Log.d(tag, "observeSessionSaved: Checking if session is saved...")
        viewModel.sessionSaved.observe(this) { isSaved ->
            if (isSaved) {
                Log.d(tag, "Session saved successfully")
                showSnackbar(getString(R.string.login_succes))
                moveToMain()
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        with(binding) {
            if (isLoading) {
                loginButton.isEnabled = false
                progressBar.visibility = View.VISIBLE
            } else {
                loginButton.isEnabled = true
                progressBar.visibility = View.GONE
            }
        }

    }

    private fun playAnimation() {
        ObjectAnimator.ofFloat(binding.imageView, View.TRANSLATION_X, -30f, 30f).apply {
            duration = 6000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }.start()

        val title = ObjectAnimator.ofFloat(binding.titleTextView, View.ALPHA, 1f).setDuration(100)
        val message =
            ObjectAnimator.ofFloat(binding.messageTextView, View.ALPHA, 1f).setDuration(100)
        val emailTextView =
            ObjectAnimator.ofFloat(binding.emailTextView, View.ALPHA, 1f).setDuration(100)
        val emailEditTextLayout =
            ObjectAnimator.ofFloat(binding.emailEditTextLayout, View.ALPHA, 1f).setDuration(100)
        val passwordTextView =
            ObjectAnimator.ofFloat(binding.passwordTextView, View.ALPHA, 1f).setDuration(100)
        val passwordEditTextLayout =
            ObjectAnimator.ofFloat(binding.passwordEditTextLayout, View.ALPHA, 1f).setDuration(100)
        val login = ObjectAnimator.ofFloat(binding.loginButton, View.ALPHA, 1f).setDuration(100)

        AnimatorSet().apply {
            playSequentially(
                title,
                message,
                emailTextView,
                emailEditTextLayout,
                passwordTextView,
                passwordEditTextLayout,
                login
            )
            startDelay = 100
        }.start()
    }

    private fun moveToSignup() {
        val intent = Intent(this, SignupActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun moveToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

}