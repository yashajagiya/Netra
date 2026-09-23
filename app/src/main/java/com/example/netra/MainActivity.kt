package com.example.netra

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easyml.EasyML
import com.easyml.camera.EasyMLCameraView
import com.easyml.core.InferenceDevice
import com.easyml.core.LabelSource
import com.easyml.core.ModelSource
import com.easyml.detection.Detection
import com.example.netra.ui.theme.NetraTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NetraTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CameraPer(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPer(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var currentDetections by remember { mutableStateOf<ImmutableList<Detection>>(persistentListOf()) }
    var lastInferenceMs by remember { mutableLongStateOf(0L) }

    if (cameraPermissionState.status.isGranted) {
        val detector = remember {
            EasyML.objectDetector(context) {
                model = ModelSource.Asset("yolon.tflite")
                labels = LabelSource.Asset("labels.txt")
                confidenceThreshold = 0.35f
                iouThreshold = 0.45f
                maxResults = 20
                device = InferenceDevice.AUTO
                useFp16 = true
                enableSmoothing = true
                smoothingFactor = 0.3f
                numThreads = 4
            }
        }

        DisposableEffect(detector) {
            onDispose {
                detector.close()
            }
        }

        Box(modifier = modifier.fillMaxSize()) {
            // Camera Preview + Real-time Bounding Box Canvas + FPS & Latency Counter
            EasyMLCameraView(
                detector = detector,
                modifier = Modifier.fillMaxSize(),
                showOverlay = true,                               // Draw bounding boxes on screen
                showFps = true,                                   // Show FPS badge in top-left
                showInferenceTime = true,                         // Show latency in ms in the badge
                showLabels = true,
                showConfidence = true,
                cornerRadius = 8f,
                overlayColor = Color(0xFF00E676),                 // Bounding box stroke color
                onResults = { detections ->
                    currentDetections = detections.toImmutableList() // Update state with live results
                },
                onInferenceTime = { ms ->
                    lastInferenceMs = ms
                }
            )
            DetectionSummaryCard(
                detections = currentDetections,
                inferenceMs = lastInferenceMs,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    } else {
        Column(
            modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val textShow = if (cameraPermissionState.status.shouldShowRationale) {
                "We need camera permission to show the camera preview"
            } else {
                "Permission Required"
            }
            Text(text = textShow, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text(text = "Permission Request")
            }
        }
    }
}

@Composable
fun DetectionSummaryCard(
    detections: ImmutableList<Detection>,
    inferenceMs: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xDD121212)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Objects (${detections.size})",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (inferenceMs > 0) {
                    Text(
                        text = "⚡ ${inferenceMs}ms",
                        color = Color(0xFF00E676),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (detections.isEmpty()) {
                Text(
                    text = "Point camera at objects...",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            } else {
                LazyRow {
                    items(
                        items = detections,
                        key = { "${it.labelIndex}_${it.boundingBox.left}_${it.confidence}" }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E88E5),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = it.label,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${(it.confidence * 100).toInt()}%",
                                    color = Color(0xFFFFD54F),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}