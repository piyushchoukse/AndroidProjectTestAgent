package com.example.androidtestagent.domain.usecase

import com.example.androidtestagent.data.model.Result
import com.example.androidtestagent.data.model.User
import com.example.androidtestagent.data.repository.UserRepository
import javax.inject.Inject

/**
 * Retrieves the pageable list of all users from the repository.
 *
 * Business rule: admins receive the full list; regular users receive only
 * their own record.  Role-based filtering happens here so that the
 * ViewModel stays presentation-only.
 */
class GetUsersUseCase @Inject constructor(
    private val repository: UserRepository
) {
    /**
     * @param requestingUser The currently authenticated user. If null, returns an error.
     */
    suspend operator fun invoke(requestingUser: User?): Result<List<User>> {
        if (requestingUser == null) {
            return Result.Error(IllegalStateException("No authenticated user"))
        }
        return when (val result = repository.getUsers()) {
            is Result.Error   -> result
            is Result.Success -> {
                val filtered = if (requestingUser.role == User.Role.ADMIN) {
                    result.data
                } else {
                    result.data.filter { it.id == requestingUser.id }
                }
                Result.Success(filtered)
            }
        }
    }
}
