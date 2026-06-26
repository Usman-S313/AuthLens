package com.authlens.app.di

import com.authlens.app.data.repository.FraudDetectionRepositoryImpl
import com.authlens.app.domain.repository.FraudDetectionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds the repository interface to its implementation.
 *
 * Analyzers and the pipeline are annotated with [@Singleton][javax.inject.Singleton]
 * directly, so they're picked up automatically — this module only needs to cover the
 * interface ↔ impl binding.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindFraudDetectionRepository(
        impl: FraudDetectionRepositoryImpl,
    ): FraudDetectionRepository
}
