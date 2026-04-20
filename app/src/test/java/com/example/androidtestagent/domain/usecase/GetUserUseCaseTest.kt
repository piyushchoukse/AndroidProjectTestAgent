package com.example.androidtestagent.domain.usecase

import com.example.androidtestagent.data.model.Result
import com.example.androidtestagent.data.repository.UserRepository
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
 * Unit tests for [GetUserUseCase].
 *
 * Verifies:
 * - id validation before any repository call
 * - cache-first strategy (getCachedUser → getUser fallback)
 * - repository error propagation
 */
@DisplayName("GetUserUseCase")
class GetUserUseCaseTest {

    private val repository: UserRepository = mockk()
    private lateinit var useCase: GetUserUseCase

    @BeforeEach
    fun setUp() {
        useCase = GetUserUseCase(repository)
    }

    @Nested
    @DisplayName("Given invalid id")
    inner class InvalidId {

        @Test
        @DisplayName("id = 0 returns Error without calling repository")
        fun `zero id returns error`() = runTest {
            val result = useCase(0L)

            assertTrue(result.isError)
            assertTrue(result.errorOrNull() is IllegalArgumentException)
            coVerify(exactly = 0) { repository.getCachedUser(any()) }
            coVerify(exactly = 0) { repository.getUser(any()) }
        }

        @Test
        @DisplayName("negative id returns Error without calling repository")
        fun `negative id returns error`() = runTest {
            val result = useCase(-1L)

            assertTrue(result.isError)
            coVerify(exactly = 0) { repository.getCachedUser(any()) }
        }
    }

    @Nested
    @DisplayName("Given valid id")
    inner class ValidId {

        @Test
        @DisplayName("returns cached user immediately without calling getUser")
        fun `cache hit`() = runTest {
            val user = TestFixtures.aUser(id = 5L)
            coEvery { repository.getCachedUser(5L) } returns user

            val result = useCase(5L)

            assertTrue(result.isSuccess)
            assertEquals(user, result.getOrNull())
            coVerify(exactly = 0) { repository.getUser(any()) }
        }

        @Test
        @DisplayName("falls back to getUser when cache is empty")
        fun `cache miss falls back to network`() = runTest {
            val user = TestFixtures.aUser(id = 5L)
            coEvery { repository.getCachedUser(5L) } returns null
            coEvery { repository.getUser(5L) } returns Result.Success(user)

            val result = useCase(5L)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.getUser(5L) }
        }

        @Test
        @DisplayName("propagates repository Error when getUser fails")
        fun `network error propagated`() = runTest {
            coEvery { repository.getCachedUser(any()) } returns null
            coEvery { repository.getUser(any()) } returns Result.Error(RuntimeException("timeout"))

            val result = useCase(5L)

            assertTrue(result.isError)
        }
    }
}
