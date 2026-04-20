package com.example.androidtestagent.data.remote

import com.example.androidtestagent.data.model.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** Retrofit interface for the backend REST API. */
interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: Long): UserDto

    @GET("users")
    suspend fun getUsers(): List<UserDto>
}

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(val token: String, val userId: Long)

data class UserDto(
    val id: Long,
    val email: String,
    val name: String,
    val role: String
) {
    fun toDomain(token: String? = null) = User(
        id    = id,
        email = email,
        name  = name,
        role  = User.Role.valueOf(role.uppercase()),
        token = token
    )
}
