package com.example.netra

import android.Manifest
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.easyml.camera.EasyMLCameraView
import com.easyml.detection.Detection
import com.easyml.detection.InferenceMetrics
import com.example.netra.ui.theme.NetraTheme
import com.example.netra.viewmodel.DetectionViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.collections.immutable.ImmutableList

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
fun CameraPer(
    modifier: Modifier = Modifier,
    viewModel: DetectionViewModel = viewModel()
) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted) {
            viewModel.initializeDetector(context)
        }
    }

    if (cameraPermissionState.status.isGranted) {
        Box(modifier = modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF00E676))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Initializing EasyML Hardware Acceleration...",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error loading model:\n${uiState.errorMessage}",
                            color = Color.Red,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                uiState.detector != null -> {
                    // Camera Preview
                    EasyMLCameraView(
                        detector = uiState.detector!!,
                        modifier = Modifier.fillMaxSize(),
                        showOverlay = true,                               // Draw bounding boxes on screen
                        showFps = true,                                   // Show FPS badge in top-left
                        showInferenceTime = true,                         // Show latency in ms in the badge
                        showLabels = true,
                        showConfidence = true,
                        enableSmoothing = true,                           // Decoupled temporal box smoothing
                        smoothingFactor = 0.35f,
                        cornerRadius = 8f,
                        strokeWidth = 4f,
                        overlayColor = Color(0xFF00E676),                 // Bounding box stroke color
                        onResults = { detections ->
                            viewModel.onResults(detections)
                        },
                        onInferenceMetrics = { metrics ->
                            viewModel.onInferenceMetrics(metrics)
                        }
                    )
                    // Custom Overlay Canvas guarantees bounding box squares are always drawn over live stream
                    DetectionOverlayCanvas(
                        detections = uiState.detections,
                        modifier = Modifier.fillMaxSize()
                    )
                    DetectionSummaryCard(
                        detections = uiState.detections,
                        metrics = uiState.metrics,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
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
    metrics: InferenceMetrics?,
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
                if (metrics != null && metrics.totalMs > 0) {
                    Text(
                        text = "⚡ %.1fms (inf: %.1fms)".format(metrics.totalMs, metrics.inferenceMs),
                        color = Color(0xFF00E676),
                        fontSize = 12.sp,
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

@Composable
fun DetectionOverlayCanvas(
    detections: ImmutableList<Detection>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        for (detection in detections) {
            val rect = detection.boundingBox

            // Map coordinates cleanly for normalized (0..1) or pixel space (0..640)
            val left = if (rect.left in 0f..1f && rect.right in 0f..1f) {
                rect.left * canvasWidth
            } else {
                (rect.left / 640f) * canvasWidth
            }
            val top = if (rect.top in 0f..1f && rect.bottom in 0f..1f) {
                rect.top * canvasHeight
            } else {
                (rect.top / 640f) * canvasHeight
            }
            val right = if (rect.left in 0f..1f && rect.right in 0f..1f) {
                rect.right * canvasWidth
            } else {
                (rect.right / 640f) * canvasWidth
            }
            val bottom = if (rect.top in 0f..1f && rect.bottom in 0f..1f) {
                rect.bottom * canvasHeight
            } else {
                (rect.bottom / 640f) * canvasHeight
            }

            val boxWidth = (right - left).coerceAtLeast(0f)
            val boxHeight = (bottom - top).coerceAtLeast(0f)

            if (boxWidth > 0f && boxHeight > 0f) {
                // Translucent fill box
                drawRect(
                    color = Color(0x2200E676),
                    topLeft = Offset(left, top),
                    size = Size(boxWidth, boxHeight)
                )

                // High-contrast stroke box
                drawRoundRect(
                    color = Color(0xFF00E676),
                    topLeft = Offset(left, top),
                    size = Size(boxWidth, boxHeight),
                    cornerRadius = CornerRadius(12f, 12f),
                    style = Stroke(width = 6f)
                )

                // Label Badge
                val labelText = "${detection.label} ${(detection.confidence * 100).toInt()}%"
                drawContext.canvas.nativeCanvas.apply {
                    val textPaint = Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 36f
                        isAntiAlias = true
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    val bgPaint = Paint().apply {
                        color = android.graphics.Color.argb(255, 30, 136, 229)
                        style = Paint.Style.FILL
                    }
                    val textWidth = textPaint.measureText(labelText)
                    val textHeight = 40f

                    val badgeTop = (top - textHeight - 12f).coerceAtLeast(0f)
                    val badgeBottom = badgeTop + textHeight + 12f

                    drawRect(
                        left,
                        badgeTop,
                        left + textWidth + 24f,
                        badgeBottom,
                        bgPaint
                    )
                    drawText(
                        labelText,
                        left + 12f,
                        badgeBottom - 10f,
                        textPaint
                    )
                }
            }
        }
    }
}