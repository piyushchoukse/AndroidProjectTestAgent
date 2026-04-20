package com.example.androidtestagent.presentation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.androidtestagent.data.model.User
import com.example.androidtestagent.databinding.ActivityMainBinding
import com.example.androidtestagent.presentation.viewmodel.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInputActions()
        observeUiState()
    }

    private fun setupInputActions() {
        // Trigger login when the Done IME action is fired from the password field
        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin()
                true
            } else false
        }

        binding.btnLogin.setOnClickListener { attemptLogin() }
    }

    private fun attemptLogin() {
        val email    = binding.etEmail.text?.toString().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        viewModel.login(email, password)
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Loading indicator
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.btnLogin.isEnabled     = !state.isLoading

                    // Error
                    if (state.errorMessage != null) {
                        binding.tvError.text       = state.errorMessage
                        binding.tvError.visibility = View.VISIBLE
                    } else {
                        binding.tvError.visibility = View.GONE
                    }

                    // Navigate to user list on success
                    if (state.isSuccess && state.user != null) {
                        navigateToUserList(state.user)
                    }
                }
            }
        }
    }

    private fun navigateToUserList(user: User) {
        val intent = Intent(this, UserListActivity::class.java).apply {
            putExtra(UserListActivity.EXTRA_USER_ID,    user.id)
            putExtra(UserListActivity.EXTRA_USER_NAME,  user.name)
            putExtra(UserListActivity.EXTRA_USER_EMAIL, user.email)
            putExtra(UserListActivity.EXTRA_USER_ROLE,  user.role.name)
            putExtra(UserListActivity.EXTRA_USER_TOKEN, user.token)
        }
        startActivity(intent)
    }
}

