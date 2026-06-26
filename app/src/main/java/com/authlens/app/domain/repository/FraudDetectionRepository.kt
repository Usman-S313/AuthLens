package com.authlens.app.domain.repository

import com.authlens.app.domain.model.DocumentInput
import com.authlens.app.domain.model.FraudResult

/**
 * Abstraction over the fraud-detection engine.
 *
 * Defined in the domain layer so use cases depend only on this contract, with the
 * concrete implementation (OpenCV-backed) living in the data layer.
 */
interface FraudDetectionRepository {

    /**
     * Runs the full detection pipeline on [input].
     *
     * @return the complete [FraudResult].
     * @throws Exception if the image cannot be read or OpenCV is unavailable.
     */
    suspend fun analyze(input: DocumentInput): FraudResult
}
