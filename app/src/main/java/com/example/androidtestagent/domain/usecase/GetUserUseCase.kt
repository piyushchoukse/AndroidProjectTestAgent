package com.example.androidtestagent.domain.usecase

import com.example.androidtestagent.data.model.Result
import com.example.androidtestagent.data.model.User
import com.example.androidtestagent.data.repository.UserRepository
import javax.inject.Inject

/**
 * Retrieves a single user, preferring the local cache over the network.
 *
 * Responsibilities:
 * 1. Validate that [id] is a positive long.
 * 2. Return cached data immediately when available.
 * 3. Fall back to [UserRepository.getUser] (which updates the cache).
 */
class GetUserUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(id: Long): Result<User> {
        if (id <= 0) {
            return Result.Error(IllegalArgumentException("User id must be positive, was $id"))
        }

        // Check local cache first to avoid unnecessary network I/O.
        val cached = repository.getCachedUser(id)
        if (cached != null) return Result.Success(cached)

        return repository.getUser(id)
    }
}
