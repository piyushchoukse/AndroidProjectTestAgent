package com.example.androidtestagent.data.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Unit tests for [User].
 *
 * Test scope: construction-time invariants, computed properties, equality.
 * Pattern: Arrange-Act-Assert (inline with JUnit 5 nested structure).
 */
@DisplayName("User model")
class UserTest {

    // ---------------------------------------------------------------
    // Happy-path construction
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Given valid inputs")
    inner class ValidConstruction {

        @Test
        @DisplayName("creates a User with default role USER and null token")
        fun `creates user with defaults`() {
            val user = User(id = 1L, email = "alice@example.com", name = "Alice")

            assertEquals(1L, user.id)
            assertEquals("alice@example.com", user.email)
            assertEquals("Alice", user.name)
            assertEquals(User.Role.USER, user.role)
            assertNull(user.token)
        }

        @Test
        @DisplayName("isAuthenticated is false when token is null")
        fun `isAuthenticated false when token null`() {
            val user = User(id = 1L, email = "alice@example.com", name = "Alice")
            assertFalse(user.isAuthenticated)
        }

        @Test
        @DisplayName("isAuthenticated is true when token is non-blank")
        fun `isAuthenticated true when token present`() {
            val user = User(id = 1L, email = "alice@example.com", name = "Alice", token = "jwt-abc")
            assertTrue(user.isAuthenticated)
        }

        @Test
        @DisplayName("isAuthenticated is false when token is blank string")
        fun `isAuthenticated false when token is blank`() {
            val user = User(id = 1L, email = "alice@example.com", name = "Alice", token = "   ")
            assertFalse(user.isAuthenticated)
        }

        @Test
        @DisplayName("canonicalEmail returns lower-cased trimmed email")
        fun `canonicalEmail lowercases and trims`() {
            val user = User(id = 1L, email = "  Alice@EXAMPLE.COM  ", name = "Alice")
            assertEquals("alice@example.com", user.canonicalEmail)
        }

        @Test
        @DisplayName("ADMIN role is stored correctly")
        fun `stores admin role`() {
            val user = User(id = 2L, email = "admin@example.com", name = "Admin", role = User.Role.ADMIN)
            assertEquals(User.Role.ADMIN, user.role)
        }
    }

    // ---------------------------------------------------------------
    // Edge / boundary cases
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Given boundary inputs")
    inner class BoundaryCases {

        @Test
        @DisplayName("id = 1 is the minimum valid id")
        fun `minimum id is 1`() {
            assertDoesNotThrow {
                User(id = 1L, email = "a@b.cc", name = "Name")
            }
        }

        @Test
        @DisplayName("throws when id is 0")
        fun `id 0 throws`() {
            assertThrows(IllegalArgumentException::class.java) {
                User(id = 0L, email = "a@b.cc", name = "Name")
            }
        }

        @Test
        @DisplayName("throws when id is negative")
        fun `negative id throws`() {
            assertThrows(IllegalArgumentException::class.java) {
                User(id = -5L, email = "a@b.cc", name = "Name")
            }
        }
    }

    // ---------------------------------------------------------------
    // Null / empty validation
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Given invalid inputs")
    inner class InvalidInputs {

        @ParameterizedTest(name = "email \"{0}\" has no @-sign → throws")
        @ValueSource(strings = ["notanemail", "missingatsign.com", ""])
        fun `email without at-sign throws`(email: String) {
            assertThrows(IllegalArgumentException::class.java) {
                User(id = 1L, email = email, name = "Alice")
            }
        }

        @Test
        @DisplayName("blank name throws")
        fun `blank name throws`() {
            assertThrows(IllegalArgumentException::class.java) {
                User(id = 1L, email = "a@b.cc", name = "   ")
            }
        }

        @Test
        @DisplayName("empty name throws")
        fun `empty name throws`() {
            assertThrows(IllegalArgumentException::class.java) {
                User(id = 1L, email = "a@b.cc", name = "")
            }
        }
    }

    // ---------------------------------------------------------------
    // Data class equality & copy
    // ---------------------------------------------------------------
    @Test
    @DisplayName("two Users with same fields are equal")
    fun `equal users`() {
        val u1 = User(id = 1L, email = "a@b.cc", name = "A")
        val u2 = User(id = 1L, email = "a@b.cc", name = "A")
        assertEquals(u1, u2)
    }

    @Test
    @DisplayName("copy with different token changes isAuthenticated")
    fun `copy changes authentication state`() {
        val user = User(id = 1L, email = "a@b.cc", name = "A")
        val authed = user.copy(token = "tok")
        assertFalse(user.isAuthenticated)
        assertTrue(authed.isAuthenticated)
    }
}
