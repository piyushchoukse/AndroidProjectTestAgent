package com.example.androidtestagent.presentation.viewmodel

import app.cash.turbine.test
import com.example.androidtestagent.data.model.Result
import com.example.androidtestagent.domain.usecase.GetUsersUseCase
import com.example.androidtestagent.util.MainDispatcherExtension
import com.example.androidtestagent.util.TestFixtures
import io.mockk.coEvery
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
 * Unit tests for [UserListViewModel].
 *
 * Pattern: Given-When-Then + Turbine flow testing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherExtension::class)
@DisplayName("UserListViewModel")
class UserListViewModelTest {

    private val getUsersUseCase: GetUsersUseCase = mockk()
    private lateinit var viewModel: UserListViewModel

    @BeforeEach
    fun setUp() {
        viewModel = UserListViewModel(getUsersUseCase)
    }

    @Test
    @DisplayName("initial state has empty users list and no loading/error")
    fun `initial state`() {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.users.isEmpty())
        assertNull(state.errorMessage)
    }

    @Nested
    @DisplayName("loadUsers()")
    inner class LoadUsers {

        @Test
        @DisplayName("happy path: loading → success with list")
        fun `success transitions`() = runTest {
            val users = listOf(TestFixtures.aUser(), TestFixtures.anAdminUser())
            val requestingAdmin = TestFixtures.anAdminUser()
            coEvery { getUsersUseCase(requestingAdmin) } returns Result.Success(users)

            viewModel.uiState.test {
                awaitItem() // initial

                viewModel.loadUsers(requestingAdmin)

                val loading = awaitItem()
                assertTrue(loading.isLoading)

                val success = awaitItem()
                assertFalse(success.isLoading)
                assertEquals(2, success.users.size)
                assertNull(success.errorMessage)

                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("error path: loading → error state with message")
        fun `error transitions`() = runTest {
            val error = Result.Error(RuntimeException("403"), "403")
            coEvery { getUsersUseCase(any()) } returns error

            viewModel.uiState.test {
                awaitItem() // initial

                viewModel.loadUsers(TestFixtures.anAuthenticatedUser())

                awaitItem() // loading
                val errorState = awaitItem()

                assertFalse(errorState.isLoading)
                assertTrue(errorState.users.isEmpty())
                assertEquals("403", errorState.errorMessage)

                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("empty list is allowed when use case returns no users")
        fun `empty user list`() = runTest {
            coEvery { getUsersUseCase(any()) } returns Result.Success(emptyList())

            viewModel.loadUsers(TestFixtures.anAuthenticatedUser())
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.users.isEmpty())
            assertNull(viewModel.uiState.value.errorMessage)
        }

        @Test
        @DisplayName("null requestingUser results in error state (use case rejects null)")
        fun `null user results in error`() = runTest {
            coEvery { getUsersUseCase(null) } returns
                Result.Error(IllegalStateException("No authenticated user"), "No authenticated user")

            viewModel.loadUsers(null)
            advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.errorMessage)
        }
    }

    @Test
    @DisplayName("clearError() sets errorMessage to null")
    fun `clearError works`() = runTest {
        coEvery { getUsersUseCase(any()) } returns Result.Error(RuntimeException("err"), "err")

        viewModel.loadUsers(TestFixtures.anAuthenticatedUser())
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()

        assertNull(viewModel.uiState.value.errorMessage)
    }
}
