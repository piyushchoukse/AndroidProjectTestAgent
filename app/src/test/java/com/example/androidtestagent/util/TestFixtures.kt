package com.example.androidtestagent.util

import com.example.androidtestagent.data.model.User

/**
 * Test Data Builder helpers — single place to construct valid test fixtures.
 *
 * Usage:
 * ```
 * val user = aUser()
 * val admin = aUser(role = User.Role.ADMIN, token = "tok123")
 * ```
 */
object TestFixtures {

    fun aUser(
        id: Long       = 1L,
        email: String  = "alice@example.com",
        name: String   = "Alice",
        role: User.Role = User.Role.USER,
        token: String? = null
    ) = User(id = id, email = email, name = name, role = role, token = token)

    fun anAuthenticatedUser(
        id: Long       = 1L,
        email: String  = "alice@example.com",
        name: String   = "Alice",
        role: User.Role = User.Role.USER
    ) = aUser(id = id, email = email, name = name, role = role, token = "jwt-token-123")

    fun anAdminUser(
        id: Long       = 99L,
        email: String  = "admin@example.com",
        name: String   = "Admin"
    ) = aUser(id = id, email = email, name = name, role = User.Role.ADMIN, token = "admin-token")
}
