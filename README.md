# Netra — Real-Time AI Object Detection for Android

**Netra** is a real-time AI object detection application for Android, built with **Jetpack Compose, CameraX, and the EasyML SDK**. It uses lightweight **YOLO TensorFlow Lite models** to perform on-device multi-class object detection with live bounding boxes, confidence scores, and performance telemetry.

The application is designed around efficient on-device inference, hardware acceleration, asynchronous processing, and a responsive Compose-based UI.

## Features

* **Real-Time Object Detection**
  Detect multiple objects from the live camera feed using YOLO models trained on the 80-class COCO dataset.

* **Hardware-Accelerated Inference**
  Supports automatic delegate selection across:

  * GPU with FP16 acceleration
  * NNAPI for supported device accelerators
  * Multi-core CPU with XNNPACK and ARM NEON

* **CameraX Integration**
  Uses CameraX for live camera preview and continuous frame analysis.

* **Live Detection Overlay**
  Renders bounding boxes, class labels, confidence scores, and visual detection indicators directly over the camera feed.

* **Performance Telemetry**
  Displays real-time FPS, total frame latency, and model inference time to help evaluate on-device performance.

* **Temporal Box Smoothing**
  Applies Exponential Moving Average (EMA) smoothing to reduce bounding-box jitter between consecutive frames.

* **Class-Aware NMS**
  Applies class-aware Non-Maximum Suppression to prevent overlapping detections of the same class from producing redundant results while preserving different object classes.

* **Asynchronous Model Loading**
  Loads the detection model and initializes inference components asynchronously to avoid blocking the main UI thread.

* **MVVM Architecture**
  Uses ViewModel, Kotlin Coroutines, and StateFlow to maintain a reactive and maintainable application architecture.

* **Runtime Camera Permissions**
  Handles camera permission requests at runtime using Accompanist Permissions.

## Screenshots

<p align="center">
  <img src="app/src/main/assets/netra image.jpeg" width="300" alt="Netra real-time object detection" />
</p>

## Technology Stack

| Component        | Technology              |
| ---------------- | ----------------------- |
| Language         | Kotlin                  |
| UI               | Jetpack Compose         |
| Design System    | Material 3              |
| Camera           | CameraX                 |
| ML Runtime       | TensorFlow Lite         |
| ML SDK           | EasyML SDK v1.6.1       |
| Models           | Ultralytics YOLO        |
| Architecture     | MVVM                    |
| Async Processing | Kotlin Coroutines       |
| State Management | StateFlow               |
| Permissions      | Accompanist Permissions |
| Build System     | Gradle Version Catalog  |

## Models

Netra currently includes lightweight YOLO TensorFlow Lite models for on-device inference:

| Model      |    Size | Dataset         |
| ---------- | ------: | --------------- |
| YOLO Nano  | ~2.7 MB | COCO 80 classes |
| YOLO Small | ~9.6 MB | COCO 80 classes |

Model files and their corresponding COCO label files are stored in the application's `assets` directory.

## Requirements

* Android Studio Ladybug (2024.2.1) or newer
* Android SDK 24 (Android 7.0) or higher
* Physical Android device with a camera
* Device capable of running TensorFlow Lite inference

A physical device is recommended because real-time inference performance depends heavily on the device's CPU, GPU, and available hardware acceleration APIs.

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/yashajagiya/Netra.git
```

### 2. Open the Project

Open the cloned repository in Android Studio.

### 3. Sync Gradle

Allow Android Studio to download and synchronize the required Gradle dependencies.

### 4. Connect an Android Device

Connect a physical Android device with USB debugging enabled.

### 5. Run the Application

Run the `:app` configuration from Android Studio.

Grant camera permission when prompted to start real-time object detection.

## Project Structure

```text
Netra/
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   │   ├── yolon.tflite
│   │   │   ├── yolos.tflite
│   │   │   ├── labelsn.txt
│   │   │   └── labelss.txt
│   │   │
│   │   ├── java/com/example/netra/
│   │   │   ├── core/
│   │   │   │   └── State data models
│   │   │   │
│   │   │   ├── ui/
│   │   │   │   └── theme/
│   │   │   │       ├── Colors
│   │   │   │       ├── Theme
│   │   │   │       └── Typography
│   │   │   │
│   │   │   ├── viewmodel/
│   │   │   │   ├── DetectionViewModel
│   │   │   │   └── DetectionUiState
│   │   │   │
│   │   │   └── MainActivity.kt
│   │   │
│   │   └── AndroidManifest.xml
│   │
│   └── build.gradle.kts
│
├── gradle/
│   └── libs.versions.toml
│
├── settings.gradle.kts
└── README.md
```

## Architecture

Netra follows an MVVM-based architecture with reactive state management.

```text
CameraX
   │
   ▼
Camera Frame
   │
   ▼
EasyML Camera Pipeline
   │
   ▼
ObjectDetector
   │
   ▼
YOLO TFLite Model
   │
   ▼
YOLO Decoder
   │
   ▼
Class-Aware NMS
   │
   ▼
Detection Results
   │
   ├──► StateFlow
   │       │
   │       ▼
   │   Compose UI
   │
   └──► Detection Overlay
```

## Detection Pipeline

### 1. Model Initialization

After camera permission is granted, the `DetectionViewModel` initializes the detector asynchronously using `EasyML.loadDetectorAsync`.

Model initialization is performed away from the main thread to prevent UI blocking during TensorFlow Lite and delegate initialization.

### 2. Camera Frame Processing

CameraX provides the live camera stream to the EasyML camera pipeline.

The frames are processed continuously and passed to the configured `ObjectDetector`.

### 3. YOLO Output Decoding

The YOLO model produces raw TensorFlow Lite output tensors.

`YoloV8Decoder` processes these outputs and converts the model predictions into normalized detection coordinates.

### 4. Non-Maximum Suppression

Class-aware Non-Maximum Suppression is applied to remove redundant detections while allowing objects from different classes to overlap.

### 5. Temporal Smoothing

Detected bounding boxes are smoothed using an Exponential Moving Average (EMA) to reduce frame-to-frame positional jitter.

### 6. UI Rendering

Detection results are exposed through reactive state and rendered by Jetpack Compose.

The overlay displays:

* Bounding boxes
* Object class
* Confidence percentage
* Detection information
* Performance metrics

## Performance Monitoring

Netra exposes several runtime metrics to make on-device inference performance observable:

* **FPS** — processed frames per second
* **Frame Latency** — total processing time for a frame
* **Inference Time** — time spent running the ML model

These metrics can be used to evaluate the effect of different models and hardware acceleration strategies on real Android devices.

## EasyML Integration

Netra uses the **EasyML SDK** as its on-device ML inference layer.

EasyML provides the model loading, TensorFlow Lite inference, hardware acceleration, and detection pipeline used by the application.

Repository:

https://github.com/yashajagiya/EasyML

## Development

The project is written entirely in Kotlin and uses modern Android development components including:

* Jetpack Compose
* Material 3
* CameraX
* Kotlin Coroutines
* StateFlow
* TensorFlow Lite
* EasyML
* MVVM

The application is intended primarily for real-device testing because hardware acceleration and inference performance vary significantly between Android devices.

## License

Add the project's license information here.

---

## Author

**Yash Ajagiya**

Android Developer focused on Kotlin, Jetpack Compose, on-device machine learning, and modern Android application development.

GitHub:
https://github.com/yashajagiya
