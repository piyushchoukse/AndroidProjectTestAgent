package com.example.androidtestagent.data.repository

import com.example.androidtestagent.data.model.Credentials
import com.example.androidtestagent.data.model.Result
import com.example.androidtestagent.data.model.User

/**
 * Contract for all user-related persistence and network operations.
 * Callers should depend on this interface, not the concrete implementation.
 */
interface UserRepository {

    /**
     * Authenticates with [credentials] and returns the resulting [User].
     * Returns [Result.Error] when credentials are wrong or the network is unavailable.
     */
    suspend fun login(credentials: Credentials): Result<User>

    /**
     * Fetches a single user by [id].
     * Returns [Result.Error] when the user is not found or the request fails.
     */
    suspend fun getUser(id: Long): Result<User>

    /**
     * Returns all users accessible to the current session.
     * Returns an empty list wrapped in [Result.Success] when no users exist.
     */
    suspend fun getUsers(): Result<List<User>>

    /**
     * Stores [user] in the local cache so that it is available offline.
     */
    suspend fun cacheUser(user: User)

    /**
     * Returns the locally cached user for [id], or null when not in cache.
     */
    suspend fun getCachedUser(id: Long): User?

    /** Removes all cached users (e.g., on logout). */
    suspend fun clearCache()
}
