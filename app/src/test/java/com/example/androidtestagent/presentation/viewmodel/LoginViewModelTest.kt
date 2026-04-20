package com.example.androidtestagent.presentation.viewmodel

import app.cash.turbine.test
import com.example.androidtestagent.data.model.Result
import com.example.androidtestagent.domain.usecase.LoginUseCase
import com.example.androidtestagent.util.MainDispatcherExtension
import com.example.androidtestagent.util.TestFixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Unit tests for [LoginViewModel].
 *
 * Uses:
 *  - [MainDispatcherExtension] to control coroutine scheduling without Robolectric.
 *  - Turbine for [StateFlow] assertion.
 *  - MockK for [LoginUseCase] isolation.
 *
 * Pattern: Given-When-Then with explicit state assertions via Turbine.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherExtension::class)
@DisplayName("LoginViewModel")
class LoginViewModelTest {

    private val loginUseCase: LoginUseCase = mockk()
    private lateinit var viewModel: LoginViewModel

    @BeforeEach
    fun setUp() {
        viewModel = LoginViewModel(loginUseCase)
    }

    // ---------------------------------------------------------------
    // Initial state
    // ---------------------------------------------------------------
    @Test
    @DisplayName("initial state is idle — not loading, no user, no error")
    fun `initial state is idle`() = runTest {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.user)
        assertNull(state.errorMessage)
        assertFalse(state.isSuccess)
    }

    // ---------------------------------------------------------------
    // Happy path
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Successful login")
    inner class SuccessfulLogin {

        @Test
        @DisplayName("transitions idle → loading → success with correct user")
        fun `state transitions on success`() = runTest {
            val authedUser = TestFixtures.anAuthenticatedUser()
            coEvery { loginUseCase(any(), any()) } returns Result.Success(authedUser)

            viewModel.uiState.test {
                // initial
                val idle = awaitItem()
                assertFalse(idle.isLoading)

                viewModel.login("alice@example.com", "Password1")

                // loading
                val loading = awaitItem()
                assertTrue(loading.isLoading)
                assertNull(loading.errorMessage)

                // success
                val success = awaitItem()
                assertFalse(success.isLoading)
                assertEquals(authedUser, success.user)
                assertTrue(success.isSuccess)
                assertNull(success.errorMessage)

                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("delegates exact email and password to use case")
        fun `delegates correct args`() = runTest {
            coEvery { loginUseCase(any(), any()) } returns Result.Success(TestFixtures.anAuthenticatedUser())

            viewModel.login("alice@example.com", "Password1")
            advanceUntilIdle()

            coVerify { loginUseCase("alice@example.com", "Password1") }
        }
    }

    // ---------------------------------------------------------------
    // Error path
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Failed login")
    inner class FailedLogin {

        @Test
        @DisplayName("transitions idle → loading → error with message")
        fun `state transitions on error`() = runTest {
            val error = Result.Error(RuntimeException("Invalid credentials"), "Invalid credentials")
            coEvery { loginUseCase(any(), any()) } returns error

            viewModel.uiState.test {
                awaitItem() // idle

                viewModel.login("alice@example.com", "WrongPass1")

                val loading = awaitItem()
                assertTrue(loading.isLoading)

                val errorState = awaitItem()
                assertFalse(errorState.isLoading)
                assertNull(errorState.user)
                assertEquals("Invalid credentials", errorState.errorMessage)
                assertFalse(errorState.isSuccess)

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // ---------------------------------------------------------------
    // Double-submit guard
    // ---------------------------------------------------------------
    @Test
    @DisplayName("second login() call while loading is ignored")
    fun `double submit is ignored`() = runTest {
        coEvery { loginUseCase(any(), any()) } returns Result.Success(TestFixtures.anAuthenticatedUser())

        viewModel.login("alice@example.com", "Password1")
        viewModel.login("alice@example.com", "Password1")   // should be no-op

        advanceUntilIdle()

        // use case should only have been invoked once
        coVerify(exactly = 1) { loginUseCase(any(), any()) }
    }

    // ---------------------------------------------------------------
    // clearError()
    // ---------------------------------------------------------------
    @Test
    @DisplayName("clearError() removes errorMessage from state")
    fun `clearError clears message`() = runTest {
        val error = Result.Error(RuntimeException("msg"), "msg")
        coEvery { loginUseCase(any(), any()) } returns error

        viewModel.login("alice@example.com", "Password1")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()

        assertNull(viewModel.uiState.value.errorMessage)
    }
}
