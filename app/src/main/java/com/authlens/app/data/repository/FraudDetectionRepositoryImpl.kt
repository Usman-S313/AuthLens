package com.authlens.app.data.repository

import android.content.Context
import com.authlens.app.detection.pipeline.FraudDetectionPipeline
import com.authlens.app.domain.model.DocumentInput
import com.authlens.app.domain.model.FraudResult
import com.authlens.app.domain.repository.FraudDetectionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [FraudDetectionRepository].
 *
 * Delegates to [FraudDetectionPipeline] for the actual analysis. Runs on a background
 * dispatcher (the calling use case/ViewModel is responsible for switching off the main
 * thread — see [com.authlens.app.domain.usecase.DetectFraudUseCase]).
 */
@Singleton
class FraudDetectionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pipeline: FraudDetectionPipeline,
) : FraudDetectionRepository {

    override suspend fun analyze(input: DocumentInput): FraudResult {
        // The pipeline does blocking bitmap/OpenCV work — keep it off the calling thread.
        // We intentionally run on the caller's dispatcher; the use case wraps us in
        // Dispatchers.Default so image decoding + native ops never hit the main thread.
        return pipeline.run(context, input)
    }
}
