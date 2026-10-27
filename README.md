# MediInsight — Edge AI Medicine Assistant

MediInsight is an Android application project exploring on-device AI to make medicine information easier to access. It combines a Jetpack Compose interface, local data persistence, medicine-related Android permissions, and the RunAnywhere SDK with a llama.cpp-backed language model for local text generation.

> **Medical disclaimer:** MediInsight provides informational assistance only. AI-generated responses may be incorrect or incomplete. Confirm medicine names, dosage instructions, interactions, and treatment decisions with a qualified healthcare professional or the medicine's official packaging.

## Project at a glance

- **Platform:** Android
- **Language:** Kotlin
- **UI:** Jetpack Compose and Material 3
- **App structure:** Activity, Navigation Compose, ViewModel, and StateFlow
- **On-device AI:** RunAnywhere Kotlin SDK with the LlamaCpp provider
- **Configured model:** Qwen 2.5 0.5B Instruct Q6_K
- **Local persistence:** Room database and repository classes
- **Camera/image/OCR dependencies:** CameraX, OpenCV, and Google ML Kit Text Recognition
- **Async operations:** Kotlin Coroutines and Flow
- **Minimum Android version:** Android 7.0 (API 24)

## Current codebase capabilities

- **Local LLM setup:** Initializes the RunAnywhere SDK, registers the LlamaCpp provider, registers the configured Qwen model, and scans for previously downloaded models.
- **Model management:** Supports listing, downloading, loading, and refreshing models, with status and download progress exposed to the UI.
- **AI chat interface:** A Compose chat screen observes ViewModel state for messages, loading status, the selected model, download progress, and SDK status.
- **Local persistence:** Room is used through database and repository classes for chat messages and user data. The ViewModel observes the logged-in user and loads recent chat history.
- **Navigation and theming:** The main activity hosts the app navigation graph and supports theme changes.
- **Medicine-related Android integrations:** The manifest declares camera, microphone, notification, and alarm permissions, and registers an alarm receiver for medicine reminders.

CameraX, OpenCV, and ML Kit text recognition are present as dependencies. Their presence alone does not prove that every medicine-scanning workflow is complete or production-ready; verify the corresponding implementation before relying on it.

## Architecture

```text
Android Application
└── MainActivity
    ├── Jetpack Compose UI / Material 3
    ├── AppNavGraph
    └── Chat UI
        └── ChatViewModel
            ├── UI state exposed through StateFlow
            ├── RunAnywhere SDK
            │   ├── SDK initialization
            │   ├── LlamaCpp service provider
            │   └── Model registration, download, and loading
            └── Repositories
                └── Room database
                    ├── Chat message data
                    └── User data

Android system integrations declared in the manifest:
Camera • Microphone • Notifications • Alarm receiver
```

### AI request flow

1. The user opens the AI chat and selects or loads an available model.
2. The ViewModel requests model operations through the RunAnywhere SDK.
3. The SDK's LlamaCpp provider performs supported text generation locally after the model is available on the device.
4. Compose observes StateFlow values and displays messages, loading indicators, model status, and download progress.
5. Chat history is accessed through repository classes backed by Room.

Model downloads require internet access. Once downloaded and loaded, inference can run locally; offline availability depends on the model being present and supported by the device. The app declares internet permission, so it should not be described as having no network activity in every workflow.

## Technology stack

| Area | Technologies |
|---|---|
| Android app | Kotlin, Android SDK |
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation Compose |
| State and lifecycle | ViewModel, Coroutines, StateFlow |
| On-device AI | RunAnywhere Kotlin SDK, LlamaCpp provider |
| Configured model | Qwen 2.5 0.5B Instruct Q6_K (GGUF) |
| Local database | Room |
| Camera/image processing dependencies | CameraX, OpenCV |
| OCR dependency | Google ML Kit Text Recognition |
| Networking dependencies | Ktor, OkHttp, Retrofit |
| Build system | Gradle Kotlin DSL, version catalog |

## Repository structure

Key source files verified in the current repository include:

```text
.
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/runanywhere/startup_hackathon20/
│           ├── MainActivity.kt
│           ├── ChatViewModel.kt
│           └── MyApplication.kt
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

The app also references navigation, database, and notification packages. This tree is an abbreviated overview rather than a complete listing of every source file.

## Getting started

### Requirements

- Android Studio compatible with the project's Android Gradle Plugin
- JDK 17
- Android SDK Platform 36
- Android device or emulator running Android 7.0 (API 24) or newer
- Internet connection for downloading the model and resolving dependencies
- Sufficient storage and memory for the selected model

### Build and run

1. Clone the repository:
   ```bash
   git clone https://github.com/hrx01-dev/MediInsight.git
   cd MediInsight
   ```
2. Open the project in Android Studio and allow Gradle sync to finish.
3. Connect an Android device or start an emulator.
4. Build and install the debug app:

   **macOS/Linux**
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

   **Windows**
   ```powershell
   .\gradlew.bat assembleDebug
   .\gradlew.bat installDebug
   ```

### Download and load a model

1. Launch the application.
2. Open the model selector in the AI chat screen.
3. Refresh the model list if necessary.
4. Download the configured Qwen model.
5. Load the model and wait for the ready status.
6. Send a test prompt and check the response.

The first model download requires internet access. Download time, generation speed, and memory use vary by device.

## Privacy and responsible use

- The configured LLM is intended for local inference after the model has been downloaded.
- Room provides local persistence for app data handled by the repository classes.
- The manifest includes internet and device-feature permissions; SDK dependencies may perform network operations for model discovery or downloads.
- This README does not claim that all data is encrypted, that every feature works offline, or that no telemetry/network traffic exists. Review SDK behavior, database handling, logs, backups, and network calls before using real health information.
- Do not use generated output as the sole basis for medication, dosage, interaction, or emergency decisions.

## Troubleshooting

**No models appear**
- Wait for SDK initialization to finish.
- Use the model selector's refresh action.
- Check Android Studio Logcat for initialization or model-registration errors.

**Model download fails**
- Check internet connectivity and available storage.
- Retry after app initialization completes.
- Check Logcat for download or SDK errors.

**Model loading or generation fails**
- Confirm the model download completed.
- Close memory-intensive applications and retry.
- Test on a supported physical device; emulator performance may differ.

**Camera, OCR, reminders, or voice features do not work**
- Check relevant runtime permissions and Android version.
- Declaring a permission or adding a dependency does not by itself confirm the complete feature flow is implemented.

## Development status

MediInsight is an evolving project. This README describes the architecture and dependencies verified in the repository; individual features may still need integration, testing, and validation. Before release, verify medicine scanning end to end, persistence behavior, reminder scheduling, permission handling, accessibility, offline behavior, and medical-safety messaging.

## License

No project-specific license was verified while preparing this README. Check the repository's license files and the licenses of bundled SDKs, model weights, and dependencies before distributing the application.
