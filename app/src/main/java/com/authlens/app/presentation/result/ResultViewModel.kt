package com.authlens.app.presentation.result

import androidx.lifecycle.ViewModel
import com.authlens.app.domain.model.FraudResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Holds the most recent [FraudResult] produced by analysis so the result screen can
 * render it. Kept as a separate activity-scoped VM so it survives the navigation
 * transition from the upload screen.
 */
@HiltViewModel
class ResultViewModel @Inject constructor() : ViewModel() {

    private val _result = MutableStateFlow<FraudResult?>(null)
    val result: StateFlow<FraudResult?> = _result.asStateFlow()

    /** Stored by the upload screen immediately after a successful analysis. */
    fun setResult(result: FraudResult) {
        _result.value = result
    }

    fun clear() {
        _result.value = null
    }
}
