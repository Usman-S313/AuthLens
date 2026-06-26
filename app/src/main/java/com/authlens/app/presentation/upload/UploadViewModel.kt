package com.authlens.app.presentation.upload

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.authlens.app.core.Resource
import com.authlens.app.domain.model.DocumentInput
import com.authlens.app.domain.model.DocumentType
import com.authlens.app.domain.usecase.DetectFraudUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** UI state for the upload screen. */
data class UploadUiState(
    val selectedImageUri: Uri? = null,
    val documentType: DocumentType = DocumentType.GENERIC,
    val isAnalyzing: Boolean = false,
    val error: String? = null,
)

/**
 * Drives the upload screen: holds the chosen image + document type and runs the
 * [DetectFraudUseCase] on a background dispatcher.
 */
@HiltViewModel
class UploadViewModel @Inject constructor(
    private val detectFraud: DetectFraudUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    fun onImageSelected(uri: Uri?) {
        _uiState.update { it.copy(selectedImageUri = uri, error = null) }
    }

    fun onDocumentTypeSelected(type: DocumentType) {
        _uiState.update { it.copy(documentType = type) }
    }

    /**
     * Runs analysis. On success the [FraudResult] is pushed into [resultSink] and then
     * [onComplete] is invoked (on the main thread) so the screen can navigate.
     */
    fun analyze(
        resultSink: com.authlens.app.presentation.result.ResultViewModel?,
        onComplete: () -> Unit,
    ) {
        val uri = _uiState.value.selectedImageUri ?: run {
            _uiState.update { it.copy(error = "Please select a document image first.") }
            return
        }
        val input = DocumentInput(uri = uri, type = _uiState.value.documentType)

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, error = null) }
            val result = withContext(Dispatchers.Default) { detectFraud(input) }
            when (result.status) {
                Resource.Status.SUCCESS -> {
                    val data = result.data
                    if (data != null) {
                        resultSink?.setResult(data)
                        _uiState.update { it.copy(isAnalyzing = false, error = null) }
                        onComplete()
                    } else {
                        _uiState.update { it.copy(isAnalyzing = false, error = "Analysis succeeded but returned no data.") }
                    }
                }
                Resource.Status.ERROR -> {
                    _uiState.update {
                        it.copy(isAnalyzing = false, error = result.message ?: "Analysis failed.")
                    }
                }
                Resource.Status.LOADING -> Unit
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
