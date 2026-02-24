# Medicine OCR Scanner - Implementation Guide

## Overview

This document describes the OCR (Optical Character Recognition) implementation for scanning medicine labels using the device camera. The feature uses Google's ML Kit Text Recognition API with CameraX for on-device text extraction.

## Architecture

### Components

1. **TextRecognitionAnalyzer** (`ocr/TextRecognitionAnalyzer.kt`)
   - Analyzes camera frames in real-time
   - Uses ML Kit Text Recognition to extract text
   - Prevents processing overload with `isProcessing` flag

2. **ScannerViewModel** (`viewmodel/ScannerViewModel.kt`)
   - Manages OCR state (detected text, scanning status, errors)
   - Tracks scanning history
   - Provides functions to pause/resume scanning

3. **MedicineScannerScreen** (`ui_screens/MedicineScannerScreen.kt`)
   - Full-screen camera preview with CameraX
   - Real-time text detection overlay
   - Permission handling with Accompanist Permissions
   - Results display section

## Dependencies Added

```kotlin
// CameraX for camera functionality
implementation("androidx.camera:camera-core:1.3.1")
implementation("androidx.camera:camera-camera2:1.3.1")
implementation("androidx.camera:camera-lifecycle:1.3.1")
implementation("androidx.camera:camera-view:1.3.1")

// ML Kit Text Recognition (on-device OCR)
implementation("com.google.mlkit:text-recognition:16.0.0")

// Accompanist Permissions for runtime permission handling
implementation("com.google.accompanist:accompanist-permissions:0.34.0")
```

## Permissions

Added to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature
    android:name="android.hardware.camera"
    android:required="false" />
```

## Navigation

### Routes Added

In `routes.kt`:
```kotlin
const val MedicineScanner = "medicine_scanner"
```

### Navigation Integration

In `navgraph.kt`:
```kotlin
composable(route = Routes.MedicineScanner) {
    MedicineScannerScreen(
        onBack = { navController.popBackStack() },
        onUseCapturedText = { capturedText ->
            navController.navigate(Routes.AddMedicines)
        }
    )
}
```

In `homescreen.kt`:
- Added "Scan Label" category card with camera icon
- Routes to "scanner" which maps to `Routes.MedicineScanner`

## Features

### 1. Real-Time Text Detection
- Continuously scans camera feed for text
- Updates UI with detected text in real-time
- Maintains scanning history

### 2. Camera Controls
- **Pause/Resume**: Toggle scanning on/off
- **Clear**: Clear detected text and history
- **Use Text**: Navigate to Add Medicine screen with captured text

### 3. Visual Feedback
- Scanning frame overlay with corner indicators
- Color-coded status (green when scanning, gray when paused)
- Instructions overlay
- Status text at top

### 4. Permission Handling
- Initial permission request screen
- Permission rationale screen
- Graceful handling of denied permissions

### 5. Error Handling
- Displays errors in red banner
- Logs errors to console
- Continues operation after errors

## Usage Flow

1. **User taps "Scan Label"** on Home screen
2. **Permission check**: App requests camera permission if not granted
3. **Camera preview**: Live camera feed displays with scanning frame
4. **Text detection**: ML Kit automatically detects and extracts text
5. **Review text**: User reviews detected text in bottom panel
6. **Actions**:
   - Pause/resume scanning
   - Clear text
   - Use captured text for adding medicine

## Code Structure

### TextRecognitionAnalyzer.kt

```kotlin
class TextRecognitionAnalyzer(
    private val onTextDetected: (String) -> Unit,
    private val onError: (Exception) -> Unit
) : ImageAnalysis.Analyzer {
    // Processes camera frames
    // Extracts text using ML Kit
    // Calls callbacks with results
}
```

### ScannerViewModel.kt

```kotlin
class ScannerViewModel : ViewModel() {
    val detectedText: StateFlow<String>
    val isScanning: StateFlow<Boolean>
    val error: StateFlow<String?>
    val scannedHistory: StateFlow<List<String>>
    
    fun updateDetectedText(text: String)
    fun toggleScanning()
    fun clearHistory()
    // ... other methods
}
```

### MedicineScannerScreen.kt

Main composables:
- `MedicineScannerScreen` - Main screen with Scaffold
- `CameraPreview` - AndroidView with CameraX integration
- `ScanningOverlay` - Visual scanning frame and instructions
- `ResultsSection` - Bottom panel with controls and results
- `PermissionRequest` - Initial permission UI
- `PermissionRationale` - Rationale for denied permissions

## Testing

### Manual Testing Steps

1. **Build and install**:
   ```bash
   cd "C:\Users\palak goel\Downloads\MediInsight-main\MediInsight"
   .\gradlew :app:installDebug
   ```

2. **Test permission flow**:
   - Deny permission → Check rationale screen
   - Grant permission → Check camera preview

3. **Test text detection**:
   - Point camera at medicine label
   - Verify text appears in results section
   - Check text accuracy

4. **Test controls**:
   - Pause scanning → Green frame turns gray
   - Resume scanning → Frame turns green
   - Clear history → Text disappears

5. **Test navigation**:
   - Tap checkmark → Should navigate to Add Medicine
   - Tap back button → Should return to Home

### Common Issues

1. **Camera not starting**:
   - Check permission is granted
   - Check device has camera
   - Check logs for binding errors

2. **No text detected**:
   - Ensure good lighting
   - Hold steady for better focus
   - Check text is clear and readable

3. **Build errors**:
   - Sync Gradle dependencies
   - Clean and rebuild project
   - Check all imports are correct

## Performance Considerations

1. **Frame throttling**: Uses `STRATEGY_KEEP_ONLY_LATEST` to prevent frame buildup
2. **Processing flag**: Prevents overlapping ML Kit calls
3. **Single thread executor**: Dedicated thread for analysis
4. **Lifecycle binding**: Camera unbinds when screen closes

## Future Enhancements

1. **Auto-parse medicine info**: Extract name, dosage, frequency from text
2. **Flash control**: Toggle camera flash for low light
3. **Image capture**: Save scanned image with text
4. **Multi-language support**: Support non-English medicine labels
5. **Barcode scanning**: Add barcode/QR code support for medicine lookup
6. **Text-to-speech**: Read detected text aloud
7. **Smart field mapping**: Auto-fill Add Medicine form fields

## API Reference

### ML Kit Text Recognition
- **Package**: `com.google.mlkit.vision.text`
- **Recognition**: On-device (no internet required)
- **Languages**: Latin-based languages
- **Accuracy**: High for printed text, moderate for handwritten

### CameraX
- **Package**: `androidx.camera.*`
- **Min API**: 21 (Android 5.0)
- **Target API**: Uses latest camera features when available

## Troubleshooting

### Gradle sync fails
```bash
# Clean project
.\gradlew clean

# Rebuild
.\gradlew :app:assembleDebug
```

### Import errors
- Ensure all packages are imported
- Check `build.gradle.kts` has all dependencies
- Sync Gradle files

### Runtime crashes
- Check Logcat for stack traces
- Verify permissions in manifest
- Check camera availability on device

## Support

For issues specific to:
- **ML Kit**: https://developers.google.com/ml-kit/vision/text-recognition
- **CameraX**: https://developer.android.com/training/camerax
- **MediInsight**: See main project documentation

---

**Implementation Date**: February 2026  
**Version**: 1.0  
**Status**: Ready for testing
