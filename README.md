<div align="center">

  <img src="https://github.com/user-attachments/assets/emoticon-eye" width="80" height="80" alt="AuthLens Icon">

  # 🔍 AuthLens

  **On-Device Document Fraud Detection for Android**

  [![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
  [![Android](https://img.shields.io/badge/Android-minSDK%2024-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
  [![OpenCV](https://img.shields.io/badge/OpenCV-4.10.0-5C3EE8?logo=opencv&logoColor=white)](https://opencv.org)
  [![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
  [![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

  <br />
  <sub>Upload any document image → get a 0–100 fraud score. All analysis runs locally, 100% offline.</sub>
</div>

---

## 📖 Overview

**AuthLens** is an Android application that performs forensic image analysis to detect document fraud and tampering. It analyzes uploaded document images (IDs, passports, financial documents, or generic documents) through a multi-stage detection pipeline — checking metadata for editing software traces, verifying layout consistency against reference templates, and performing pixel-level integrity analysis using **Error Level Analysis (ELA)** for JPEG or **Noise Consistency Analysis** for PNG.

### Key Highlights

| Feature | Details |
|---------|---------|
| 🔒 **100% Offline** | All image processing runs on-device. No images are uploaded to any server. |
| 🧠 **Multi-Stage Pipeline** | 4 forensic checks chained together: metadata → template → ELA/noise → scoring |
| 📊 **Quantified Risk** | Produces a 0–100 fraud score with risk buckets: Clean / Suspicious / Likely Fraud / High Risk |
| 🗺️ **Anomaly Heatmap** | Visual heatmap showing exactly which regions of the image are suspicious |
| 📷 **Camera + Gallery** | Capture directly via CameraX or pick from the device gallery |
| 📑 **Multiple Doc Types** | National ID, Passport, Driver's License, Bank Statement, Check, Invoice, Generic |
| ⚡ **OpenCV Native** | Uses OpenCV's C++ backend for high-performance ORB, homography, and image processing |
| 🎨 **Modern UI** | Built with Jetpack Compose + Material 3, edge-to-edge, dark mode support |

---

## 🏗️ Detection Pipeline

AuthLens follows a sequential forensic analysis pipeline. Each stage feeds its result to the next, and certain findings can short-circuit the pipeline for immediate rejection.

```
┌──────────────────────────────────────────────────────┐
│              [Uploaded Document Image]                │
└──────────────────────┬───────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────┐
│  STAGE 1: Metadata Check                            │
│  Reads EXIF/XMP tags. Flags known editors:          │
│  Photoshop, GIMP, Lightroom, Snapseed, PicsArt…    │
│                                                      │
│  ⚠ Editing software detected? → TERMINAL: HIGH RISK  │
└──────────────────────┬───────────────────────────────┘
                       │ (clean)
                       ▼
┌──────────────────────────────────────────────────────┐
│  STAGE 2: Alignment & Template Matching               │
│  ORB feature detection + Hamming distance matching   │
│  + RANSAC homography for geometric verification.     │
│  Compares against bundled reference templates.       │
│                                                      │
│  ⚠ Layout doesn't match? → Reject signal            │
└──────────────────────┬───────────────────────────────┘
                       │ (clean)
                       ▼
┌──────────────────────────────────────────────────────┐
│  STAGE 3: Format Detection                           │
│  Sniffs file header bytes to detect JPEG vs PNG.     │
│                                                      │
│  ├──► JPEG: Error Level Analysis (ELA)               │
│  │     Re-encode at quality 95, compute per-pixel   │
│  │     error map. Tampered regions have distinct     │
│  │     error levels → anomaly heatmap + score.      │
│  │                                                   │
│  └──► PNG:  Noise Consistency Analysis              │
│        Extract noise residual via Gaussian filter.   │
│        Partition into grid; measure grain-energy      │
│        uniformity. Splices show inconsistent noise.   │
└──────────────────────┬───────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────┐
│  STAGE 4: Fraud Score Calculation                    │
│  Weighted aggregation:                               │
│    • Metadata:    35% weight                         │
│    • Template:    30% weight                         │
│    • Integrity:   35% weight                         │
│                                                      │
│  → Final Score: 0–100                               │
│  → Risk Level:  CLEAN | SUSPICIOUS |                │
│                 LIKELY FRAUD | HIGH RISK FRAUD       │
└──────────────────────────────────────────────────────┘
```

### Stage Deep-Dive

| # | Stage | Algorithm | Input | Output |
|---|-------|-----------|-------|--------|
| 1 | **Metadata** | EXIF/XMP tag scanning | `ExifInterface` | List of flagged editor signatures (severity 0–100) |
| 2 | **Template Matching** | ORB + BF-Hamming + RANSAC homography | Document bitmap + reference templates | Inlier count → layout fidelity score (0–100) |
| 3a | **ELA** (JPEG) | JPEG re-encode diff at quality 95 | BGR Mat → grayscale diff → heatmap | Anomaly heatmap PNG + score |
| 3b | **Noise** (PNG) | Gaussian residual → grid variance (CoV) | BGR Mat → noise residual → CoV | Anomaly heatmap PNG + score |
| 4 | **Scoring** | Weighted sum + terminal override | Per-stage scores | Final fraud score + risk bucket |

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Kotlin 2.0.20 |
| **UI** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM + Clean Architecture (3-layer: Presentation → Domain → Data) |
| **DI** | Hilt (Dagger) |
| **Image Processing** | OpenCV Android SDK 4.10.0 (native C++ backend) |
| **Camera** | CameraX |
| **Image Loading** | Coil |
| **Metadata** | AndroidX ExifInterface |
| **Async** | Kotlin Coroutines + StateFlow |
| **Navigation** | Jetpack Navigation Compose |
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 34 (Android 14) |
| **Backend** | None — fully offline |

---

## 📁 Project Structure

```
AuthLens/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   └── templates/          # Reference document templates
│       │       ├── national_id/
│       │       ├── passport/
│       │       └── generic/
│       ├── java/com/authlens/app/
│       │   ├── AuthLensApplication.kt      # Application + OpenCV init
│       │   ├── MainActivity.kt             # Single-activity host
│       │   │
│       │   ├── core/                      # Shared utilities
│       │   │   ├── Constants.kt           # Tunable thresholds & weights
│       │   │   ├── Resource.kt            # Generic async state wrapper
│       │   │   ├── theme/                 # Compose Material 3 theme
│       │   │   │   ├── Color.kt
│       │   │   │   ├── Theme.kt
│       │   │   │   └── Type.kt
│       │   │   └── utils/
│       │   │       └── ImageFileUtil.kt   # Cache URI helpers
│       │   │
│       │   ├── domain/                    # Business logic layer (pure Kotlin)
│       │   │   ├── model/                 # Data classes & enums
│       │   │   │   ├── CheckResult.kt
│       │   │   │   ├── DocumentInput.kt
│       │   │   │   ├── DocumentType.kt
│       │   │   │   ├── Enums.kt
│       │   │   │   ├── FraudResult.kt
│       │   │   │   ├── FraudScore.kt
│       │   │   │   ├── MetadataFinding.kt
│       │   │   │   └── RiskLevel.kt
│       │   │   ├── repository/
│       │   │   │   └── FraudDetectionRepository.kt   # Interface
│       │   │   └── usecase/
│       │   │       └── DetectFraudUseCase.kt         # Single entry point
│       │   │
│       │   ├── data/                     # Data layer (implementation)
│       │   │   └── repository/
│       │   │       └── FraudDetectionRepositoryImpl.kt
│       │   │
│       │   ├── detection/                 # 🔬 Core analysis engine
│       │   │   ├── ImageUtils.kt         # Bitmap↔Mat, format detection
│       │   │   ├── pipeline/
│       │   │   │   └── FraudDetectionPipeline.kt   # Orchestrator
│       │   │   ├── metadata/
│       │   │   │   └── MetadataAnalyzer.kt         # Stage 1: EXIF scan
│       │   │   ├── alignment/
│       │   │   │   └── TemplateMatcher.kt          # Stage 2: ORB + RANSAC
│       │   │   ├── ela/
│       │   │   │   └── ElaAnalyzer.kt              # Stage 3a: JPEG ELA
│       │   │   ├── noise/
│       │   │   │   └── NoiseAnalyzer.kt             # Stage 3b: PNG noise
│       │   │   ├── scoring/
│       │   │   │   └── FraudScoreCalculator.kt     # Stage 4: Aggregation
│       │   │   └── template/
│       │   │       └── DocumentTemplateStore.kt    # Loads asset templates
│       │   │
│       │   ├── di/
│       │   │   └── AppModule.kt                    # Hilt bindings
│       │   │
│       │   └── presentation/              # UI layer
│       │       ├── navigation/
│       │       │   └── AppNavigation.kt           # NavHost (Upload ↔ Result)
│       │       ├── upload/
│       │       │   ├── UploadScreen.kt            # Gallery/Camera + doc type
│       │       │   └── UploadViewModel.kt
│       │       ├── result/
│       │       │   ├── ResultScreen.kt            # Score gauge + findings
│       │       │   └── ResultViewModel.kt
│       │       └── components/
│       │           ├── ScoreGauge.kt               # Animated circular gauge
│       │           ├── AnomalyHeatmap.kt           # Heatmap viewer
│       │           ├── FindingCard.kt              # Per-stage result card
│       │           └── SimpleButton.kt             # Secondary action button
│       │
│       └── res/
│           ├── drawable/ic_launcher_foreground.xml
│           ├── mipmap-anydpi-v26/
│           ├── values/
│           │   ├── colors.xml
│           │   ├── strings.xml
│           │   └── themes.xml
│           └── xml/file_paths.xml
│
├── gradle/
│   ├── libs.versions.toml          # Version catalog
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle.kts                 # Root build file
├── settings.gradle.kts              # Project settings
├── gradle.properties                # Gradle config
├── gradlew / gradlew.bat           # Wrapper scripts
├── .gitignore
├── LICENSE
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

| Requirement | Version |
|------------|---------|
| Android Studio | Hedgehog (2023.1.1) or newer |
| JDK | 17 |
| Android SDK | API 34 (compile), API 24 (min) |
| Gradle | 8.9 (bundled via wrapper) |

### Clone & Build

```bash
# Clone the repository
git clone https://github.com/<your-username>/AuthLens.git
cd AuthLens

# Build the debug APK
./gradlew assembleDebug

# Install on a connected device / emulator
./gradlew installDebug
```

Or simply open the project in Android Studio and hit **Run** ▶️.

### APK Location

After building, the APK is at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📸 How It Works (User Flow)

```
┌─────────────┐     ┌──────────────┐     ┌─────────────────┐
│  1. UPLOAD   │────►│  2. ANALYZE  │────►│  3. VIEW RESULT  │
│             │     │              │     │                 │
│ • Pick from │     │ • Metadata   │     │ • Fraud Score   │
│   Gallery   │     │ • Template   │     │   Gauge (0-100)│
│ • Capture   │     │ • ELA/Noise  │     │ • Risk Level    │
│   via Camera│     │ • Scoring    │     │ • Stage Details │
│ • Select    │     │              │     │ • Heatmap       │
│   Doc Type  │     │              │     │ • New Scan      │
└─────────────┘     └──────────────┘     └─────────────────┘
```

1. **Upload** — Choose an image from the gallery or capture one with the camera. Select the document type (National ID, Passport, etc.).
2. **Analyze** — The pipeline runs all 4 detection stages automatically. A spinner shows while processing.
3. **View Result** — An animated score gauge shows the fraud score. Below it, each stage's findings are displayed as cards with score bars. An anomaly heatmap highlights suspicious regions visually.

---

## 🔧 Configuration & Tuning

### Adjusting Detection Sensitivity

All tunable parameters are centralized in `core/Constants.kt`:

```kotlin
object Constants {
    // Image processing
    const val ELA_JPEG_QUALITY = 95          // Re-encode quality for ELA
    const val MAX_IMAGE_DIMENSION = 1600     // Max px before downsampling
    const val NOISE_GRID_SIZE = 8            // NxN grid for noise analysis

    // Template matching
    const val TEMPLATE_MAX_FEATURES = 1000   // ORB feature cap
    const val TEMPLATE_MIN_GOOD_MATCHES = 25 // Min inliers for "authentic"

    // Scoring weights (must sum to 1.0)
    object Scoring {
        const val WEIGHT_METADATA = 0.35
        const val WEIGHT_TEMPLATE = 0.30
        const val WEIGHT_FORMAT_ANALYSIS = 0.35

        // Risk bucket thresholds
        const val SUSPICIOUS_THRESHOLD = 30
        const val LIKELY_FRAUD_THRESHOLD = 55
        const val HIGH_RISK_THRESHOLD = 80
    }
}
```

### Adding Reference Templates

Drop genuine document images into the assets directory:

```
app/src/main/assets/templates/
├── national_id/     ← National ID card references
├── passport/        ← Passport photo page references
└── generic/         ← Fallback generic document references
```

- Accepted formats: `.jpg`, `.png`
- Use straight, well-lit captures of genuine documents
- One image per issuer/country variant is recommended
- Without templates, the matcher returns a neutral score (pipeline still runs)

### Adding Editor Signatures

Edit `detection/metadata/MetadataAnalyzer.kt` → `editorSignatures` list to add or remove editing software names to detect:

```kotlin
private val editorSignatures = listOf(
    "photoshop", "gimp", "lightroom", "snapseed", "picsart", ...
)
```

---

## 📋 Risk Levels

| Score Range | Risk Level | Color | Description |
|------------|-----------|-------|-------------|
| 0–29 | ✅ **Clean** | 🟢 Green | No signs of tampering detected |
| 30–54 | ⚠️ **Suspicious** | 🟡 Yellow | Minor anomalies found. Recommend manual review |
| 55–79 | 🔶 **Likely Fraud** | 🟠 Orange | Strong indicators of tampering detected |
| 80–100 | 🚨 **High Risk Fraud** | 🔴 Red | Severe tampering or editing software detected. Reject |

---

## 🏗️ Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                        │
│  ┌────────────┐  ┌──────────────┐  ┌───────────────────┐   │
│  │UploadScreen│  │ ResultScreen │  │ Components        │   │
│  │  ViewModel │  │  ViewModel   │  │ (Gauge, Heatmap,  │   │
│  └─────┬──────┘  └──────┬───────┘  │  FindingCard...)  │   │
│        │                │          └───────────────────┘   │
└────────┼────────────────┼──────────────────────────────────┘
         │                │
         ▼                ▼
┌─────────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER                             │
│  ┌──────────────────┐  ┌────────────────────────┐          │
│  │DetectFraudUseCase│  │ Models (FraudResult,    │          │
│  └────────┬─────────┘  │  FraudScore, CheckResult,│          │
│           │            │  RiskLevel, DocumentType)│          │
│           ▼            └────────────────────────┘          │
│  ┌──────────────────────────────────────┐                   │
│  │ FraudDetectionRepository (interface) │                   │
│  └──────────────────┬───────────────────┘                   │
└─────────────────────┼───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                       DATA LAYER                             │
│  ┌─────────────────────────────────┐                         │
│  │ FraudDetectionRepositoryImpl    │                         │
│  └──────────────┬──────────────────┘                         │
└─────────────────┼───────────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────────┐
│                  DETECTION ENGINE                             │
│  ┌──────────────┐ ┌────────────┐ ┌────────────┐            │
│  │   Metadata   │ │  Template   │ │   ELA /    │            │
│  │  Analyzer    │ │  Matcher    │ │  Noise     │            │
│  └──────┬───────┘ └─────┬──────┘ └─────┬──────┘            │
│         └───────────────┼──────────────┘                    │
│                         ▼                                   │
│              ┌────────────────────┐                          │
│              │ FraudDetection     │                          │
│              │ Pipeline           │                          │
│              └────────┬───────────┘                          │
│                       ▼                                      │
│              ┌────────────────────┐                          │
│              │ FraudScore         │                          │
│              │ Calculator         │                          │
│              └────────────────────┘                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. **Fork** the repository
2. **Create a feature branch**: `git checkout -b feature/your-feature-name`
3. **Make changes** and ensure they compile: `./gradlew assembleDebug`
4. **Commit** with descriptive messages
5. **Push** to your fork: `git push origin feature/your-feature-name`
6. Open a **Pull Request**

### Areas where contributions are especially welcome

- 📸 **Reference templates** — real document template images for various countries
- 🧪 **Unit tests** — per-analyzer tests in `app/src/test/`
- 🌍 **Localization** — translations for non-English users
- 🎨 **UI polish** — animations, accessibility, tablet layout
- 📊 **New analyzers** — font analysis, MRZ/OCR verification, color profile checks

---

## ⚠️ Disclaimer

> **AuthLens is a heuristic forensic tool designed to assist human reviewers — it is NOT a definitive legal proof of fraud.** Detection scores are based on statistical analysis of image properties and may produce both false positives and false negatives. Always combine automated analysis with manual expert review for critical decisions.

---

## 📝 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

<div align="center">

**Built with ❤️ using Kotlin, Jetpack Compose, and OpenCV**

</div>
