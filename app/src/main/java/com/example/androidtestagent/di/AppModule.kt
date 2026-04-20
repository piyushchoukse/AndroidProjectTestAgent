package com.example.androidtestagent.di

import com.example.androidtestagent.data.repository.FakeUserRepository
import com.example.androidtestagent.data.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that wires up the application's dependencies.
 *
 * Currently binds [FakeUserRepository] so the app runs without a real server.
 * To switch to a real backend, replace the binding with [UserRepositoryImpl]
 * and provide a [NetworkModule] that supplies a configured [ApiService].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: FakeUserRepository): UserRepository
}
