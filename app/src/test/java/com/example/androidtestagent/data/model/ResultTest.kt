package com.example.androidtestagent.data.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [Result].
 *
 * Focuses on the sealed-class contract, helper extensions, and the
 * [Result.runCatching] factory — NOT on the mechanics of data classes.
 */
@DisplayName("Result sealed class")
class ResultTest {

    // ---------------------------------------------------------------
    // Success branch
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Success")
    inner class SuccessBranch {

        @Test
        @DisplayName("isSuccess returns true")
        fun `isSuccess true`() {
            assertTrue(Result.Success("data").isSuccess)
        }

        @Test
        @DisplayName("isError returns false")
        fun `isError false`() {
            assertFalse(Result.Success("data").isError)
        }

        @Test
        @DisplayName("getOrNull returns wrapped data")
        fun `getOrNull returns data`() {
            assertEquals("hello", Result.Success("hello").getOrNull())
        }

        @Test
        @DisplayName("errorOrNull returns null")
        fun `errorOrNull is null`() {
            assertNull(Result.Success(42).errorOrNull())
        }
    }

    // ---------------------------------------------------------------
    // Error branch
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Error")
    inner class ErrorBranch {

        private val cause = RuntimeException("boom")
        private val error = Result.Error(cause)

        @Test
        @DisplayName("isSuccess returns false")
        fun `isSuccess false`() {
            assertFalse(error.isSuccess)
        }

        @Test
        @DisplayName("isError returns true")
        fun `isError true`() {
            assertTrue(error.isError)
        }

        @Test
        @DisplayName("getOrNull returns null")
        fun `getOrNull is null`() {
            assertNull(error.getOrNull())
        }

        @Test
        @DisplayName("errorOrNull returns the wrapped exception")
        fun `errorOrNull returns exception`() {
            assertSame(cause, error.errorOrNull())
        }

        @Test
        @DisplayName("message defaults to exception message")
        fun `message uses exception message`() {
            assertEquals("boom", error.message)
        }

        @Test
        @DisplayName("message uses custom string when provided")
        fun `custom message overrides`() {
            val custom = Result.Error(cause, "custom msg")
            assertEquals("custom msg", custom.message)
        }

        @Test
        @DisplayName("message is 'Unknown error' when exception has no message")
        fun `unknown error fallback`() {
            val noMessage = Result.Error(RuntimeException())
            assertEquals("Unknown error", noMessage.message)
        }
    }

    // ---------------------------------------------------------------
    // runCatching factory
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("runCatching factory")
    inner class RunCatchingFactory {

        @Test
        @DisplayName("wraps successful block in Success")
        fun `success block`() {
            val result = Result.runCatching { 42 }
            assertEquals(Result.Success(42), result)
        }

        @Test
        @DisplayName("wraps thrown Exception in Error")
        fun `exception block`() {
            val ex = IllegalStateException("bad state")
            val result = Result.runCatching { throw ex }
            assertTrue(result.isError)
            assertSame(ex, result.errorOrNull())
        }

        @Test
        @DisplayName("does NOT catch Errors (e.g. OutOfMemoryError)")
        fun `does not catch Error subclass`() {
            assertThrows(OutOfMemoryError::class.java) {
                Result.runCatching { throw OutOfMemoryError() }
            }
        }
    }
}
