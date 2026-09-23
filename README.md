# Netra - Real-Time AI Object Detection App

**Netra** is a modern, ultra-fast, hardware-accelerated Android Object Detection app built with **Jetpack Compose**, **CameraX**, and the **[EasyML SDK](https://github.com/yashajagiya/EasyML)**. It leverages lightweight **YOLO (YOLOv8 / YOLO26)** TensorFlow Lite models to perform real-time multi-class object detection with bounding box overlays, confidence scores, and microsecond-level performance telemetry.

---

## ⚡ Features

- 🎯 **Real-time Object Detection**: Blazing-fast multi-object detection using YOLO models trained on 80 COCO dataset categories.
- ⚡ **3-Tier Hardware Acceleration**: Automatic delegate tiering leveraging **GPU FP16** shaders, **NNAPI** NPUs/DSPs, and **Multi-Core CPU XNNPACK** with ARM NEON SIMD.
- 🖼️ **CameraX Viewfinder**: Seamless live camera preview integrated directly into Jetpack Compose.
- 🎨 **Live Bounding Box Canvas**: Smooth, high-contrast bounding box overlays, class badges, and confidence percentages.
- 📈 **Microsecond Telemetry**: Real-time FPS badge along with overall frame latency (`ms`) and pure model inference time (`ms`).
- 🌊 **Temporal Box Smoothing**: Decoupled Exponential Moving Average (EMA) box smoothing to eliminate jitter between frames.
- 🏆 **Class-Aware NMS**: Preserves overlapping distinct objects without false positive suppression.
- 🛡️ **Clean MVVM Architecture**: Non-blocking asynchronous model loading (`EasyML.loadDetectorAsync`), StateFlow reactive state management, and `@Immutable` UI states.
- 🔒 **Runtime Permissions**: Intuitive camera permission flow powered by Accompanist Permissions.

---

## 📸 Screenshots

<p align="center">
  <img src="1.jpeg" width="300" alt="Camera Scanning" />
  <img src="2.jpeg" width="300" alt="Live Object Detection" />
</p>

---

## 🛠 Tech Stack

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3
- **ML SDK**: [EasyML SDK](https://github.com/yashajagiya/EasyML) (`v1.6.1`)
- **Models**: Ultralytics YOLO (`yolon.tflite` / `yolos.tflite`)
- **Camera Engine**: [CameraX](https://developer.android.com/training/camerax)
- **Architecture**: MVVM with Kotlin Coroutines & `StateFlow`
- **Language**: 100% Pure Kotlin
- **Permission Handling**: [Accompanist Permissions](https://google.github.io/accompanist/permissions/)

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Ladybug | 2024.2.1 or newer
- Android SDK 24 (Android 7.0) or higher
- A physical Android device with a camera for testing real-time ML inference

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yashajagiya/Netra.git
   ```
2. Open the project in Android Studio.
3. Sync Gradle dependencies.
4. Connect your physical Android device and run `:app`.

---

## 🏗 Project Structure

```text
Netra/
├── app/
│   ├── src/main/
│   │   ├── assets/                     # YOLO TFLite models (.tflite) and COCO labels (.txt)
│   │   │   ├── yolon.tflite            # YOLO Nano model (2.7 MB)
│   │   │   ├── yolos.tflite            # YOLO Small model (9.6 MB)
│   │   │   ├── labelsn.txt             # COCO 80 labels for Nano
│   │   │   └── labelss.txt             # COCO 80 labels for Small
│   │   ├── java/com/example/netra/
│   │   │   ├── core/                   # State data models
│   │   │   ├── ui/theme/               # Material 3 colors, typography & theme
│   │   │   ├── viewmodel/              # DetectionViewModel & DetectionUiState
│   │   │   └── MainActivity.kt         # Main UI, Camera View, and Custom Canvas Overlay
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml              # Version catalog
├── settings.gradle.kts
└── README.md
```

---

## 💡 How It Works

1. **Async Model Loading**: When camera permission is granted, [DetectionViewModel](file:///C:/Users/yash/AndroidStudioProjects/Netra/app/src/main/java/com/example/netra/viewmodel/DetectionViewModel.kt) calls `EasyML.loadDetectorAsync` on `Dispatchers.IO`. This compiles TFLite delegates without blocking the main UI thread.
2. **Camera Stream Analysis**: [EasyMLCameraView](class://com.easyml.camera.EasyMLCameraView) feeds camera preview frames into the `ObjectDetector`.
3. **YOLO Decoding**: `YoloV8Decoder` decodes candidate boxes, normalizes output coordinates `0..1`, and applies Class-Aware Non-Maximum Suppression (NMS).
4. **Overlay Rendering**: `DetectionOverlayCanvas` and `EasyMLCameraView` draw real-time rounded bounding box squares, translucent fills, and label badges over the detected objects.
5. **Telemetry Dashboard**: `DetectionSummaryCard` displays live object chips along with frame latency and pure model inference metrics.
---
Developed with ❤️ by [Yash Ajagiya](https://github.com/yashajagiya)
