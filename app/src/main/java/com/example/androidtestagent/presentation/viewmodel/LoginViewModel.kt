package com.example.androidtestagent.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidtestagent.data.model.Result
import com.example.androidtestagent.data.model.User
import com.example.androidtestagent.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Represents the full state of the Login screen. */
data class LoginUiState(
    val isLoading: Boolean  = false,
    val user: User?         = null,
    val errorMessage: String? = null
) {
    val isSuccess: Boolean get() = user != null
}

/**
 * ViewModel for the Login screen.
 *
 * Emits a [LoginUiState] via [uiState]; UI layers collect this flow and
 * render accordingly.  No Android framework types leak through the public API,
 * making this trivially unit-testable without Robolectric.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Triggers login.  The state machine transitions:
     *   idle → loading → success | error
     */
    fun login(email: String, password: String) {
        if (_uiState.value.isLoading) return   // guard: no double-submit

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = loginUseCase(email, password)) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, user = result.data)
                }
                is Result.Error   -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    /** Clears any transient error so the UI can reset its error indicator. */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
