package com.example.androidtestagent.data.repository

import com.example.androidtestagent.data.model.Credentials
import com.example.androidtestagent.data.model.Result
import com.example.androidtestagent.data.model.User
import com.example.androidtestagent.data.remote.ApiService
import com.example.androidtestagent.data.remote.LoginRequest
import com.example.androidtestagent.data.remote.LoginResponse
import com.example.androidtestagent.data.remote.UserDto
import com.example.androidtestagent.util.TestFixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [UserRepositoryImpl].
 *
 * Strategy:
 *  - [ApiService] is replaced by a MockK mock — no real network.
 *  - In-memory cache is exercised directly through the repository API.
 *  - Pattern: Given-When-Then with [runTest] for coroutine tests.
 */
@DisplayName("UserRepositoryImpl")
class UserRepositoryImplTest {

    private val api: ApiService = mockk()
    private lateinit var repository: UserRepositoryImpl

    @BeforeEach
    fun setUp() {
        repository = UserRepositoryImpl(api)
    }

    // ---------------------------------------------------------------
    // login()
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("login()")
    inner class Login {

        private val credentials = Credentials("alice@example.com", "Password1")
        private val loginResponse = LoginResponse(token = "jwt-tok", userId = 1L)
        private val userDto = UserDto(id = 1L, email = "alice@example.com", name = "Alice", role = "USER")

        @Test
        @DisplayName("returns Success with authenticated User on valid credentials")
        fun `login success`() = runTest {
            // Given
            coEvery { api.login(LoginRequest(credentials.email, credentials.password)) } returns loginResponse
            coEvery { api.getUser(1L) } returns userDto

            // When
            val result = repository.login(credentials)

            // Then
            assertTrue(result.isSuccess)
            val user = result.getOrNull()!!
            assertEquals(1L, user.id)
            assertEquals("jwt-tok", user.token)
            assertTrue(user.isAuthenticated)
        }

        @Test
        @DisplayName("caches the user after successful login")
        fun `login caches user`() = runTest {
            coEvery { api.login(any()) } returns loginResponse
            coEvery { api.getUser(any()) } returns userDto

            repository.login(credentials)

            // The user should now be in cache, no api.getUser call should happen
            val cached = repository.getCachedUser(1L)
            assertNotNull(cached)
            assertEquals(1L, cached!!.id)
        }

        @Test
        @DisplayName("returns Error when API throws an exception")
        fun `login network error returns Error`() = runTest {
            val networkError = RuntimeException("Network unavailable")
            coEvery { api.login(any()) } throws networkError

            val result = repository.login(credentials)

            assertTrue(result.isError)
            assertSame(networkError, result.errorOrNull())
        }

        @Test
        @DisplayName("returns Error when getUser call after login fails")
        fun `login fails when getUser throws`() = runTest {
            coEvery { api.login(any()) } returns loginResponse
            coEvery { api.getUser(any()) } throws RuntimeException("user not found")

            val result = repository.login(credentials)

            assertTrue(result.isError)
        }
    }

    // ---------------------------------------------------------------
    // getUser()
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("getUser()")
    inner class GetUser {

        private val userDto = UserDto(id = 42L, email = "bob@example.com", name = "Bob", role = "USER")

        @Test
        @DisplayName("returns cached user without calling API")
        fun `cache hit skips API`() = runTest {
            // Arrange: seed the cache
            val cachedUser = TestFixtures.aUser(id = 42L, email = "bob@example.com", name = "Bob")
            repository.cacheUser(cachedUser)

            // Act
            val result = repository.getUser(42L)

            // Assert: api.getUser should NOT be called
            assertTrue(result.isSuccess)
            coVerify(exactly = 0) { api.getUser(any()) }
        }

        @Test
        @DisplayName("fetches from API and caches on cache miss")
        fun `cache miss fetches from API`() = runTest {
            coEvery { api.getUser(42L) } returns userDto

            val result = repository.getUser(42L)

            assertTrue(result.isSuccess)
            assertEquals(42L, result.getOrNull()!!.id)
            coVerify(exactly = 1) { api.getUser(42L) }
            // should now be in cache
            assertNotNull(repository.getCachedUser(42L))
        }

        @Test
        @DisplayName("returns Error when API throws")
        fun `api error returned as Error`() = runTest {
            coEvery { api.getUser(any()) } throws RuntimeException("timeout")

            val result = repository.getUser(42L)

            assertTrue(result.isError)
        }
    }

    // ---------------------------------------------------------------
    // getUsers()
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("getUsers()")
    inner class GetUsers {

        @Test
        @DisplayName("returns list from API wrapped in Success")
        fun `getUsers success`() = runTest {
            val dtos = listOf(
                UserDto(1L, "a@x.com", "A", "USER"),
                UserDto(2L, "b@x.com", "B", "ADMIN")
            )
            coEvery { api.getUsers() } returns dtos

            val result = repository.getUsers()

            assertTrue(result.isSuccess)
            assertEquals(2, result.getOrNull()!!.size)
        }

        @Test
        @DisplayName("returns empty list when API returns no users")
        fun `empty list on no users`() = runTest {
            coEvery { api.getUsers() } returns emptyList()

            val result = repository.getUsers()

            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()!!.isEmpty())
        }

        @Test
        @DisplayName("returns Error on API failure")
        fun `api failure`() = runTest {
            coEvery { api.getUsers() } throws RuntimeException("server error")

            val result = repository.getUsers()

            assertTrue(result.isError)
        }
    }

    // ---------------------------------------------------------------
    // cache management
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("cache management")
    inner class CacheManagement {

        @Test
        @DisplayName("getCachedUser returns null before caching")
        fun `no cached user initially`() = runTest {
            assertNull(repository.getCachedUser(1L))
        }

        @Test
        @DisplayName("cacheUser then getCachedUser returns same user")
        fun `cache round-trip`() = runTest {
            val user = TestFixtures.aUser()
            repository.cacheUser(user)
            assertEquals(user, repository.getCachedUser(user.id))
        }

        @Test
        @DisplayName("clearCache removes all entries")
        fun `clearCache empties cache`() = runTest {
            repository.cacheUser(TestFixtures.aUser(id = 1L))
            repository.cacheUser(TestFixtures.aUser(id = 2L, email = "b@x.com", name = "B"))

            repository.clearCache()

            assertNull(repository.getCachedUser(1L))
            assertNull(repository.getCachedUser(2L))
        }

        @Test
        @DisplayName("caching same id overwrites previous entry")
        fun `caching overwrites`() = runTest {
            val original = TestFixtures.aUser(name = "Alice")
            val updated  = original.copy(name = "Alicia", token = "new-tok")

            repository.cacheUser(original)
            repository.cacheUser(updated)

            assertEquals("Alicia", repository.getCachedUser(1L)!!.name)
        }
    }
}
