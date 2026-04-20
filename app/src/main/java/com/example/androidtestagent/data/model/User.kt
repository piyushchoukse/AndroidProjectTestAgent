package com.example.androidtestagent.data.model

/**
 * Domain/data model representing an authenticated user.
 *
 * @property id      Unique server-assigned identifier.
 * @property email   Canonical e-mail address (lower-cased at construction time).
 * @property name    Display name; trimmed at construction time.
 * @property role    Role assigned by the server (default: [Role.USER]).
 * @property token   Short-lived JWT; null before authentication succeeds.
 */
data class User(
    val id: Long,
    val email: String,
    val name: String,
    val role: Role = Role.USER,
    val token: String? = null
) {
    init {
        require(id > 0) { "User id must be positive, was $id" }
        require(email.contains('@')) { "Invalid email: $email" }
        require(name.isNotBlank()) { "Name must not be blank" }
    }

    /** Lower-cased canonical e-mail, suitable for comparisons and storage keys. */
    val canonicalEmail: String get() = email.trim().lowercase()

    /** True when a non-null, non-blank JWT token is held by this user object. */
    val isAuthenticated: Boolean get() = !token.isNullOrBlank()

    enum class Role { USER, ADMIN, MODERATOR }
}
