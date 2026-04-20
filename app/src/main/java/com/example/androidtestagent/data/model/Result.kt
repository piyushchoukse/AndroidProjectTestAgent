package com.example.androidtestagent.data.model

/**
 * Sealed result wrapper used across the data and domain layers.
 *
 * Prefer this over raw exceptions so that callers are forced by the
 * compiler to handle both success and failure branches.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String = exception.message ?: "Unknown error") : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    /** Returns [data] when [Success], or null otherwise. */
    fun getOrNull(): T? = (this as? Success)?.data

    /** Returns the wrapped [Throwable] when [Error], or null otherwise. */
    fun errorOrNull(): Throwable? = (this as? Error)?.exception

    companion object {
        /** Wraps [block] result, catching any [Exception] as an [Error]. */
        inline fun <T> runCatching(block: () -> T): Result<T> = try {
            Success(block())
        } catch (e: Exception) {
            Error(e)
        }
    }
}
