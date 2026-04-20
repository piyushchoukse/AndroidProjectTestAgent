package com.example.androidtestagent.data.repository

import com.example.androidtestagent.data.model.Credentials
import com.example.androidtestagent.data.model.Result
import com.example.androidtestagent.data.model.User
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-process fake repository used when no real backend is available.
 *
 * Demo credentials: test@example.com / Password1
 *
 * Replace this with [UserRepositoryImpl] (bound to a real API) when a backend is running.
 */
@Singleton
class FakeUserRepository @Inject constructor() : UserRepository {

    private val cache = mutableMapOf<Long, User>()

    private val fakeUsers = listOf(
        User(id = 1L, email = "test@example.com",  name = "Alice Demo",  role = User.Role.USER,  token = null),
        User(id = 2L, email = "bob@example.com",   name = "Bob Demo",    role = User.Role.USER,  token = null),
        User(id = 99L, email = "admin@example.com", name = "Admin Demo", role = User.Role.ADMIN, token = null)
    )

    override suspend fun login(credentials: Credentials): Result<User> {
        delay(800) // simulate network latency
        val match = fakeUsers.firstOrNull {
            it.email.equals(credentials.email.trim(), ignoreCase = true)
        }
        return if (match != null && credentials.password == "Password1") {
            val authed = match.copy(token = "fake-jwt-${match.id}")
            cacheUser(authed)
            Result.Success(authed)
        } else {
            Result.Error(
                RuntimeException("Invalid credentials"),
                "Invalid email or password. Try test@example.com / Password1"
            )
        }
    }

    override suspend fun getUser(id: Long): Result<User> {
        delay(300)
        val user = fakeUsers.firstOrNull { it.id == id }
        return if (user != null) Result.Success(user)
        else Result.Error(RuntimeException("User $id not found"), "User not found")
    }

    override suspend fun getUsers(): Result<List<User>> {
        delay(500)
        return Result.Success(fakeUsers)
    }

    override suspend fun cacheUser(user: User) {
        cache[user.id] = user
    }

    override suspend fun getCachedUser(id: Long): User? = cache[id]

    override suspend fun clearCache() {
        cache.clear()
    }
}
