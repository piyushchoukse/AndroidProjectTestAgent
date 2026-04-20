package com.example.androidtestagent.data.repository

import com.example.androidtestagent.data.model.Credentials
import com.example.androidtestagent.data.model.Result
import com.example.androidtestagent.data.model.User
import com.example.androidtestagent.data.remote.ApiService
import com.example.androidtestagent.data.remote.LoginRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [UserRepository].
 *
 * Strategy:
 *  - network-first for mutating operations (login)
 *  - cache-first for reads, falling back to network
 *  - all network calls are wrapped in [Result.runCatching] so that
 *    no raw exception escapes to the domain / presentation layers.
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val api: ApiService
) : UserRepository {

    // In-memory cache (replace with Room in a real project).
    private val cache = mutableMapOf<Long, User>()

    override suspend fun login(credentials: Credentials): Result<User> =
        Result.runCatching {
            val response = api.login(LoginRequest(credentials.email, credentials.password))
            val userDto  = api.getUser(response.userId)
            val user     = userDto.toDomain(token = response.token)
            cacheUser(user)
            user
        }

    override suspend fun getUser(id: Long): Result<User> {
        val cached = cache[id]
        if (cached != null) return Result.Success(cached)

        return Result.runCatching {
            val user = api.getUser(id).toDomain()
            cacheUser(user)
            user
        }
    }

    override suspend fun getUsers(): Result<List<User>> =
        Result.runCatching {
            api.getUsers().map { it.toDomain() }
        }

    override suspend fun cacheUser(user: User) {
        cache[user.id] = user
    }

    override suspend fun getCachedUser(id: Long): User? = cache[id]

    override suspend fun clearCache() {
        cache.clear()
    }
}
