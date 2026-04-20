package com.example.androidtestagent.presentation

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.androidtestagent.data.model.User
import com.example.androidtestagent.databinding.ActivityUserListBinding
import com.example.androidtestagent.presentation.viewmodel.UserListViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserListBinding
    private val viewModel: UserListViewModel by viewModels()
    private val adapter = UserAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        observeUiState()

        // Reconstruct the logged-in user from extras and trigger data load
        val currentUser = buildUserFromExtras()
        viewModel.loadUsers(currentUser)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE

                    if (!state.isLoading) {
                        if (state.users.isEmpty() && state.errorMessage == null) {
                            binding.tvEmpty.visibility = View.VISIBLE
                        } else {
                            binding.tvEmpty.visibility = View.GONE
                        }
                    }

                    adapter.submitList(state.users)

                    state.errorMessage?.let { msg ->
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG)
                            .setAction("Dismiss") { viewModel.clearError() }
                            .show()
                    }
                }
            }
        }
    }

    private fun buildUserFromExtras(): User? {
        val id    = intent.getLongExtra(EXTRA_USER_ID, -1L)
        val name  = intent.getStringExtra(EXTRA_USER_NAME)
        val email = intent.getStringExtra(EXTRA_USER_EMAIL)
        val role  = intent.getStringExtra(EXTRA_USER_ROLE)
        val token = intent.getStringExtra(EXTRA_USER_TOKEN)

        if (id <= 0 || name == null || email == null || role == null) return null

        return try {
            User(
                id    = id,
                email = email,
                name  = name,
                role  = User.Role.valueOf(role),
                token = token
            )
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    companion object {
        const val EXTRA_USER_ID    = "extra_user_id"
        const val EXTRA_USER_NAME  = "extra_user_name"
        const val EXTRA_USER_EMAIL = "extra_user_email"
        const val EXTRA_USER_ROLE  = "extra_user_role"
        const val EXTRA_USER_TOKEN = "extra_user_token"
    }
}
