package com.example.androidtestagent.domain.usecase

import com.example.androidtestagent.data.model.Result
import com.example.androidtestagent.data.model.User
import com.example.androidtestagent.data.repository.UserRepository
import com.example.androidtestagent.util.TestFixtures
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [GetUsersUseCase].
 *
 * Verifies:
 * - null requestingUser guard
 * - role-based filtering (USER sees only themselves; ADMIN sees all)
 * - repository error propagation
 */
@DisplayName("GetUsersUseCase")
class GetUsersUseCaseTest {

    private val repository: UserRepository = mockk()
    private lateinit var useCase: GetUsersUseCase

    @BeforeEach
    fun setUp() {
        useCase = GetUsersUseCase(repository)
    }

    @Test
    @DisplayName("returns Error when requestingUser is null")
    fun `null user returns error`() = runTest {
        val result = useCase(null)

        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is IllegalStateException)
    }

    @Nested
    @DisplayName("ADMIN user")
    inner class AdminUser {

        private val admin = TestFixtures.anAdminUser()
        private val allUsers = listOf(
            TestFixtures.aUser(id = 1L),
            TestFixtures.aUser(id = 2L, email = "bob@example.com", name = "Bob"),
            admin
        )

        @Test
        @DisplayName("receives the full user list")
        fun `admin sees all users`() = runTest {
            coEvery { repository.getUsers() } returns Result.Success(allUsers)

            val result = useCase(admin)

            assertTrue(result.isSuccess)
            assertEquals(3, result.getOrNull()!!.size)
        }
    }

    @Nested
    @DisplayName("Regular USER")
    inner class RegularUser {

        private val alice = TestFixtures.anAuthenticatedUser(id = 1L)
        private val allUsers = listOf(
            alice,
            TestFixtures.aUser(id = 2L, email = "bob@example.com", name = "Bob")
        )

        @Test
        @DisplayName("sees only their own record")
        fun `regular user filtered to self`() = runTest {
            coEvery { repository.getUsers() } returns Result.Success(allUsers)

            val result = useCase(alice)

            assertTrue(result.isSuccess)
            val list = result.getOrNull()!!
            assertEquals(1, list.size)
            assertEquals(alice.id, list.first().id)
        }

        @Test
        @DisplayName("receives empty list when they are not in the server response")
        fun `user not in list gets empty result`() = runTest {
            val otherUsers = listOf(
                TestFixtures.aUser(id = 2L, email = "bob@example.com", name = "Bob")
            )
            coEvery { repository.getUsers() } returns Result.Success(otherUsers)

            val result = useCase(alice)

            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()!!.isEmpty())
        }
    }

    @Test
    @DisplayName("propagates repository Error unchanged")
    fun `repository error propagated`() = runTest {
        val cause = RuntimeException("503")
        coEvery { repository.getUsers() } returns Result.Error(cause)

        val result = useCase(TestFixtures.anAuthenticatedUser())

        assertTrue(result.isError)
        assertSame(cause, result.errorOrNull())
    }
}
