package com.example.androidtestagent.domain.usecase

import com.example.androidtestagent.data.model.Credentials
import com.example.androidtestagent.data.model.Result
import com.example.androidtestagent.data.model.User
import com.example.androidtestagent.data.repository.UserRepository
import javax.inject.Inject

/**
 * Orchestrates the login flow with input validation before delegating to the repository.
 *
 * Responsibilities:
 * 1. Validate [email] format and [password] policy (min 8 chars, at least one digit).
 * 2. Delegate the authenticated network call to [UserRepository.login].
 * 3. Return a typed [Result] — never throw.
 */
class LoginUseCase @Inject constructor(
    private val repository: UserRepository
) {
    /**
     * Executes the login flow.
     *
     * @param email    Raw email string from the UI; leading/trailing spaces are trimmed.
     * @param password Raw password string from the UI.
     * @return [Result.Success] with the authenticated [User], or [Result.Error] describing
     *         why login failed (validation failure or network/server error).
     */
    suspend operator fun invoke(email: String, password: String): Result<User> {
        val trimmedEmail = email.trim()

        val validationError = validate(trimmedEmail, password)
        if (validationError != null) {
            return Result.Error(IllegalArgumentException(validationError))
        }

        return repository.login(Credentials(trimmedEmail, password))
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private fun validate(email: String, password: String): String? = when {
        email.isBlank()                      -> "Email must not be blank"
        !EMAIL_REGEX.matches(email)          -> "Email format is invalid"
        password.length < MIN_PASSWORD_LEN   -> "Password must be at least $MIN_PASSWORD_LEN characters"
        !password.any { it.isDigit() }       -> "Password must contain at least one digit"
        else                                 -> null
    }

    private companion object {
        val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        const val MIN_PASSWORD_LEN = 8
    }
}
