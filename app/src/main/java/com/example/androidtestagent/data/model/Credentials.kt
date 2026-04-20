package com.example.androidtestagent.data.model

/**
 * Lightweight credentials value object.
 *
 * Validation is intentionally kept simple — richer validation
 * lives in [com.example.androidtestagent.domain.usecase.LoginUseCase].
 */
data class Credentials(val email: String, val password: String) {

    init {
        require(email.isNotBlank()) { "Email must not be blank" }
        require(password.isNotEmpty()) { "Password must not be empty" }
    }
}
