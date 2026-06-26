package com.authlens.app.domain.usecase

import com.authlens.app.core.Resource
import com.authlens.app.domain.model.DocumentInput
import com.authlens.app.domain.model.FraudResult
import com.authlens.app.domain.repository.FraudDetectionRepository
import javax.inject.Inject

/**
 * Single entry point the presentation layer uses to run fraud detection.
 *
 * Wraps the repository call in a [Resource] so the ViewModel gets loading/error/success
 * in one shape. Thrown exceptions are converted to [Resource.error].
 */
class DetectFraudUseCase @Inject constructor(
    private val repository: FraudDetectionRepository,
) {

    suspend operator fun invoke(input: DocumentInput): Resource<FraudResult> = try {
        Resource.success(repository.analyze(input))
    } catch (t: Throwable) {
        Resource.error(t.message ?: "Analysis failed", null)
    }
}
