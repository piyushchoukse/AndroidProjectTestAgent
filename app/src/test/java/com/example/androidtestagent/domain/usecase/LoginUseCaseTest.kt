package com.example.androidtestagent.domain.usecase

import com.example.androidtestagent.data.model.Credentials
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

/**
 * Unit tests for [LoginUseCase].
 *
 * Key concerns:
 *  1. Input validation — email format and password policy are enforced BEFORE
 *     hitting the repository.
 *  2. Happy path — valid credentials are delegated to [UserRepository.login].
 *  3. Repository errors are propagated unchanged.
 *  4. Double-invocation isolation — each call is independent.
 *
 * The [UserRepository] is replaced with a MockK fake so that no network
 * or database is exercised.
 */
@DisplayName("LoginUseCase")
class LoginUseCaseTest {

    private val repository: UserRepository = mockk()
    private lateinit var useCase: LoginUseCase

    @BeforeEach
    fun setUp() {
        useCase = LoginUseCase(repository)
    }

    // ---------------------------------------------------------------
    // Happy path
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Given valid credentials")
    inner class ValidCredentials {

        private val authedUser = TestFixtures.anAuthenticatedUser()

        @Test
        @DisplayName("returns Success with the authenticated User")
        fun `login succeeds`() = runTest {
            // Arrange
            coEvery { repository.login(any()) } returns Result.Success(authedUser)

            // Act
            val result = useCase("alice@example.com", "Password1")

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(authedUser, result.getOrNull())
        }

        @Test
        @DisplayName("delegates to repository with trimmed email")
        fun `trims email before delegation`() = runTest {
            coEvery { repository.login(any()) } returns Result.Success(authedUser)

            useCase("  alice@example.com  ", "Password1")

            coVerify {
                repository.login(
                    withArg { credentials ->
                        assertEquals("alice@example.com", credentials.email)
                    }
                )
            }
        }
    }

    // ---------------------------------------------------------------
    // Email validation
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Given invalid email")
    inner class InvalidEmail {

        @Test
        @DisplayName("blank email returns Error without calling repository")
        fun `blank email`() = runTest {
            val result = useCase("   ", "Password1")

            assertTrue(result.isError)
            assertTrue(result.errorOrNull() is IllegalArgumentException)
            coVerify(exactly = 0) { repository.login(any()) }
        }

        @ParameterizedTest(name = "email \"{0}\" should fail format check")
        @ValueSource(strings = [
            "notanemail",
            "missing@tld",
            "@nodomain.com",
            "spaces in@email.com",
            "double@@at.com"
        ])
        fun `invalid email format returns Error`(email: String) = runTest {
            val result = useCase(email, "Password1")

            assertTrue(result.isError)
            coVerify(exactly = 0) { repository.login(any()) }
        }

        @ParameterizedTest(name = "email \"{0}\" is valid")
        @ValueSource(strings = [
            "user@example.com",
            "user+tag@example.co.uk",
            "user.name@sub.domain.org"
        ])
        fun `valid email formats proceed to repository`(email: String) = runTest {
            coEvery { repository.login(any()) } returns Result.Success(TestFixtures.anAuthenticatedUser())

            val result = useCase(email, "Password1")

            assertTrue(result.isSuccess)
        }
    }

    // ---------------------------------------------------------------
    // Password validation
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Given invalid password")
    inner class InvalidPassword {

        @Test
        @DisplayName("password shorter than 8 chars returns Error")
        fun `short password`() = runTest {
            val result = useCase("alice@example.com", "Pass1")

            assertTrue(result.isError)
            coVerify(exactly = 0) { repository.login(any()) }
        }

        @Test
        @DisplayName("password with exactly 8 chars but no digit returns Error")
        fun `no digit in password`() = runTest {
            val result = useCase("alice@example.com", "Password")

            assertTrue(result.isError)
            coVerify(exactly = 0) { repository.login(any()) }
        }

        @Test
        @DisplayName("empty password returns Error")
        fun `empty password`() = runTest {
            // Credentials itself throws for empty password
            val result = useCase("alice@example.com", "")

            assertTrue(result.isError)
        }

        @ParameterizedTest(name = "\"{0}\" / \"{1}\" → should pass validation")
        @CsvSource(
            "alice@example.com, Password1",
            "alice@example.com, 12345678",
            "alice@example.com, abcdefg9"
        )
        fun `valid passwords proceed to repository`(email: String, password: String) = runTest {
            coEvery { repository.login(any()) } returns Result.Success(TestFixtures.anAuthenticatedUser())

            val result = useCase(email, password)

            // even if repository returns success we mainly care no validation error occurred
            coVerify(exactly = 1) { repository.login(any()) }
        }
    }

    // ---------------------------------------------------------------
    // Repository error propagation
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Given repository failure")
    inner class RepositoryFailure {

        @Test
        @DisplayName("propagates repository Error unchanged")
        fun `repository error propagated`() = runTest {
            val cause = RuntimeException("Auth server down")
            coEvery { repository.login(any()) } returns Result.Error(cause)

            val result = useCase("alice@example.com", "Password1")

            assertTrue(result.isError)
            assertSame(cause, result.errorOrNull())
        }
    }
}
