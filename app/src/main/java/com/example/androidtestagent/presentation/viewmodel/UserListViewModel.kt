package com.example.androidtestagent.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidtestagent.data.model.Result
import com.example.androidtestagent.data.model.User
import com.example.androidtestagent.domain.usecase.GetUsersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Full state of the User List screen. */
data class UserListUiState(
    val isLoading: Boolean    = false,
    val users: List<User>     = emptyList(),
    val errorMessage: String? = null
)

/**
 * ViewModel for the User List screen.
 *
 * Role-based filtering is delegated to [GetUsersUseCase]; this ViewModel
 * is responsible only for state management and lifecycle awareness.
 */
@HiltViewModel
class UserListViewModel @Inject constructor(
    private val getUsersUseCase: GetUsersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserListUiState())
    val uiState: StateFlow<UserListUiState> = _uiState.asStateFlow()

    fun loadUsers(requestingUser: User?) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = getUsersUseCase(requestingUser)) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, users = result.data)
                }
                is Result.Error   -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
