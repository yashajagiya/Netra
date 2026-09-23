package com.example.netra.viewmodel

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easyml.EasyML
import com.easyml.core.InferenceDevice
import com.easyml.core.LabelSource
import com.easyml.core.ModelSource
import com.easyml.detection.AutoDetectionDecoder
import com.easyml.detection.CoordinateFormat
import com.easyml.detection.Detection
import com.easyml.detection.InferenceMetrics
import com.easyml.detection.ObjectDetector
import com.easyml.detection.YoloV8Decoder
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class DetectionUiState(
    val isLoading: Boolean = true,
    val detector: ObjectDetector? = null,
    val detections: ImmutableList<Detection> = persistentListOf(),
    val metrics: InferenceMetrics? = null,
    val errorMessage: String? = null
)

@Stable
class DetectionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DetectionUiState())
    val uiState: StateFlow<DetectionUiState> = _uiState.asStateFlow()

    fun initializeDetector(context: Context) {
        if (_uiState.value.detector != null || !_uiState.value.isLoading) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Async loading off the UI thread via EasyML.loadDetectorAsync
                val detector = EasyML.loadDetectorAsync(context.applicationContext) {
                    model = ModelSource.Asset("yolos.tflite")
                    labels = LabelSource.Asset("labelss.txt")
                    confidenceThreshold = 0.20f
                    iouThreshold = 0.45f
                    maxResults = 20
                    classAgnosticNms = false
                    decoder = YoloV8Decoder(isTransposed = true, coordinateFormat = CoordinateFormat.NORMALIZED)
                    device = InferenceDevice.AUTO
                    useFp16 = true
                    numThreads = 4
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        detector = detector,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to load model"
                    )
                }
            }
        }
    }

    fun onResults(detections: List<Detection>) {
        _uiState.update {
            it.copy(detections = detections.toImmutableList())
        }
    }

    fun onInferenceMetrics(metrics: InferenceMetrics) {
        _uiState.update {
            it.copy(metrics = metrics)
        }
    }

    override fun onCleared() {
        // Release native TFLite memory resources when ViewModel is cleared
        _uiState.value.detector?.close()
    }
}
