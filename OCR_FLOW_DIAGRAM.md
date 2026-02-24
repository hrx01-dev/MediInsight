# OCR Scanner - Visual Flow Diagram

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          MediInsight App                                │
│                                                                         │
│  ┌───────────────────────────────────────────────────────────────────┐ │
│  │                         Home Screen                               │ │
│  │                                                                   │ │
│  │  ┌─────────────┐  ┌──────────────┐  ┌─────────────┐            │ │
│  │  │ Add Medicine│  │  Scan Label  │  │  Insights   │            │ │
│  │  │   (Green)   │  │    (Blue)    │  │   (Green)   │            │ │
│  │  └──────┬──────┘  └──────┬───────┘  └─────────────┘            │ │
│  │         │                │                                        │ │
│  │         │                └─────────┐                             │ │
│  └─────────┼──────────────────────────┼─────────────────────────────┘ │
│            │                           │                               │
│            │                           ↓                               │
│            │         ┌─────────────────────────────────────┐          │
│            │         │    Medicine Scanner Screen          │          │
│            │         │                                     │          │
│            │         │  ┌───────────────────────────────┐ │          │
│            │         │  │     Camera Permission?        │ │          │
│            │         │  │  ┌──────────┐  ┌───────────┐ │ │          │
│            │         │  │  │  Denied  │  │  Granted  │ │ │          │
│            │         │  │  └─────┬────┘  └─────┬─────┘ │ │          │
│            │         │  └────────┼─────────────┼───────┘ │          │
│            │         │           │             │         │          │
│            │         │  ┌────────▼─────┐  ┌───▼──────────────────┐  │
│            │         │  │  Permission  │  │   Camera Preview     │  │
│            │         │  │   Rationale  │  │                      │  │
│            │         │  └──────────────┘  │  ┌───────────────┐  │  │
│            │         │                    │  │ Scanning Frame│  │  │
│            │         │                    │  │   (Green)     │  │  │
│            │         │                    │  └───────┬───────┘  │  │
│            │         │                    │          │          │  │
│            │         │                    │          ↓          │  │
│            │         │                    │  ┌───────────────┐  │  │
│            │         │                    │  │   ML Kit OCR  │  │  │
│            │         │                    │  │   Processing  │  │  │
│            │         │                    │  └───────┬───────┘  │  │
│            │         │                    │          │          │  │
│            │         │                    │          ↓          │  │
│            │         │                    │  ┌───────────────┐  │  │
│            │         │                    │  │ Detected Text │  │  │
│            │         │                    │  └───────────────┘  │  │
│            │         │                    └──────────────────────┘  │
│            │         │                                              │
│            │         │  ┌────────────────────────────────────────┐ │
│            │         │  │        Results Panel                   │ │
│            │         │  │  ┌──────┐  ┌───────┐  ┌───────────┐  │ │
│            │         │  │  │Pause │  │ Clear │  │ Use Text ✓│  │ │
│            │         │  │  └──────┘  └───────┘  └─────┬─────┘  │ │
│            │         │  │                              │        │ │
│            │         │  │  ┌───────────────────────┐  │        │ │
│            │         │  │  │   Detected Text       │  │        │ │
│            │         │  │  │   (Scrollable)        │  │        │ │
│            │         │  │  └───────────────────────┘  │        │ │
│            │         │  └──────────────────────────────┼────────┘ │
│            │         └─────────────────────────────────┼──────────┘
│            │                                           │            │
│            └───────────────────────────────────────────┘            │
│                                                                     │
│            ┌─────────────────────────────────────────┐             │
│            │      Add Medicine Screen                │             │
│            │  (Pre-filled with scanned text)         │             │
│            └─────────────────────────────────────────┘             │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Component Flow

```
┌─────────────────────┐
│   User Action       │
│  "Tap Scan Label"   │
└──────────┬──────────┘
           │
           ↓
┌─────────────────────────────────────┐
│      Navigation System              │
│  Routes.MedicineScanner             │
└──────────┬──────────────────────────┘
           │
           ↓
┌─────────────────────────────────────┐
│   MedicineScannerScreen             │
│   - Composable UI                   │
│   - Permission Check                │
│   - Camera Setup                    │
└──────────┬──────────────────────────┘
           │
           ↓
┌─────────────────────────────────────┐
│   CameraProvider                    │
│   - Initialize CameraX              │
│   - Bind lifecycle                  │
└──────────┬──────────────────────────┘
           │
           ↓
┌─────────────────────────────────────┐
│   CameraPreview (AndroidView)       │
│   - PreviewView                     │
│   - Surface Provider                │
└──────────┬──────────────────────────┘
           │
           ↓
┌─────────────────────────────────────┐
│   ImageAnalysis                     │
│   - Frame capture                   │
│   - Backpressure strategy           │
└──────────┬──────────────────────────┘
           │
           ↓
┌─────────────────────────────────────┐
│   TextRecognitionAnalyzer           │
│   - Implements ImageAnalysis        │
│   - Process frames                  │
└──────────┬──────────────────────────┘
           │
           ↓
┌─────────────────────────────────────┐
│   ML Kit Text Recognition           │
│   - InputImage creation             │
│   - Text detection                  │
│   - On-device processing            │
└──────────┬──────────────────────────┘
           │
           ↓
┌─────────────────────────────────────┐
│   Callback: onTextDetected          │
│   - Extracted text string           │
└──────────┬──────────────────────────┘
           │
           ↓
┌─────────────────────────────────────┐
│   ScannerViewModel                  │
│   - updateDetectedText()            │
│   - Update StateFlow                │
└──────────┬──────────────────────────┘
           │
           ↓
┌─────────────────────────────────────┐
│   UI Recomposition                  │
│   - Observe StateFlow               │
│   - Display text                    │
└──────────┬──────────────────────────┘
           │
           ↓
┌─────────────────────────────────────┐
│   User Reviews Text                 │
│   - Pause/Resume options            │
│   - Clear history option            │
│   - Use text (✓) option             │
└──────────┬──────────────────────────┘
           │
           ↓
┌─────────────────────────────────────┐
│   Navigation Action                 │
│   onUseCapturedText(text)           │
│   → Navigate to AddMedicines        │
└─────────────────────────────────────┘
```

---

## State Management Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    ScannerViewModel                         │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │         Private Mutable State                       │   │
│  │                                                     │   │
│  │  _detectedText: MutableStateFlow<String>           │   │
│  │  _isScanning: MutableStateFlow<Boolean>            │   │
│  │  _error: MutableStateFlow<String?>                 │   │
│  │  _scannedHistory: MutableStateFlow<List<String>>   │   │
│  │                                                     │   │
│  └──────────────────────┬──────────────────────────────┘   │
│                         │                                   │
│                         │ .asStateFlow()                    │
│                         ↓                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │         Public Immutable State                      │   │
│  │                                                     │   │
│  │  detectedText: StateFlow<String>                   │   │
│  │  isScanning: StateFlow<Boolean>                    │   │
│  │  error: StateFlow<String?>                         │   │
│  │  scannedHistory: StateFlow<List<String>>           │   │
│  │                                                     │   │
│  └──────────────────────┬──────────────────────────────┘   │
│                         │                                   │
│                         │ collectAsState()                  │
│                         ↓                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              UI Composables                         │   │
│  │                                                     │   │
│  │  val detectedText by viewModel.detectedText        │   │
│  │      .collectAsState()                             │   │
│  │                                                     │   │
│  │  val isScanning by viewModel.isScanning            │   │
│  │      .collectAsState()                             │   │
│  │                                                     │   │
│  └──────────────────────┬──────────────────────────────┘   │
│                         │                                   │
│                         ↓                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           Automatic Recomposition                   │   │
│  │          (Jetpack Compose Magic!)                   │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Camera Lifecycle

```
┌─────────────────────────────────────┐
│     Screen Composition              │
└──────────┬──────────────────────────┘
           │
           ↓
┌─────────────────────────────────────┐
│   LaunchedEffect(Unit)              │
│   - Get CameraProvider              │
│   - Store in State                  │
└──────────┬──────────────────────────┘
           │
           ↓
┌─────────────────────────────────────┐
│   AndroidView Update                │
│   - Check cameraProvider != null    │
│   - Check isScanning == true        │
└──────────┬──────────────────────────┘
           │
           ↓
┌─────────────────────────────────────┐
│   Unbind All Use Cases              │
│   cameraProvider.unbindAll()        │
└──────────┬──────────────────────────┘
           │
           ↓
┌─────────────────────────────────────┐
│   Build Use Cases                   │
│   - Preview                         │
│   - ImageAnalysis                   │
└──────────┬──────────────────────────┘
           │
           ↓
┌─────────────────────────────────────┐
│   Set Analyzer                      │
│   - Create TextRecognitionAnalyzer  │
│   - Set executor (SingleThread)     │
└──────────┬──────────────────────────┘
           │
           ↓
┌─────────────────────────────────────┐
│   Bind to Lifecycle                 │
│   cameraProvider.bindToLifecycle(   │
│     lifecycleOwner,                 │
│     cameraSelector,                 │
│     preview,                        │
│     imageAnalyzer                   │
│   )                                 │
└──────────┬──────────────────────────┘
           │
           ↓
┌─────────────────────────────────────┐
│   Camera Running                    │
│   - Frames → Analyzer               │
│   - Text Detection Active           │
└──────────┬──────────────────────────┘
           │
           ↓
┌─────────────────────────────────────┐
│   Screen Disposed                   │
│   DisposableEffect onDispose        │
└──────────┬──────────────────────────┘
           │
           ↓
┌─────────────────────────────────────┐
│   Cleanup                           │
│   - analyzer.close()                │
│   - cameraProvider.unbindAll()      │
└─────────────────────────────────────┘
```

---

## Permission Flow

```
                    ┌──────────────┐
                    │  App Launch  │
                    └──────┬───────┘
                           │
                           ↓
                 ┌─────────────────────┐
                 │ Tap "Scan Label"    │
                 └─────────┬───────────┘
                           │
                           ↓
              ┌────────────────────────────┐
              │ Check Permission Status    │
              └────────┬───────────────────┘
                       │
        ┌──────────────┼──────────────┐
        │              │              │
        ↓              ↓              ↓
┌──────────────┐  ┌──────────┐  ┌─────────────┐
│   Granted    │  │ Rationale│  │   Denied    │
└──────┬───────┘  └─────┬────┘  └──────┬──────┘
       │                │               │
       │                │               │
       ↓                ↓               ↓
┌─────────────┐  ┌─────────────┐  ┌──────────────┐
│Show Camera  │  │Show Rationale│ │Request Again │
│   Preview   │  │   Screen    │  │   Screen    │
└──────┬──────┘  └──────┬──────┘  └──────┬───────┘
       │                │                 │
       │                ↓                 │
       │         ┌──────────────┐         │
       │         │Request Again │         │
       │         └──────┬───────┘         │
       │                │                 │
       │         ┌──────┴──────┐          │
       │         │             │          │
       │         ↓             ↓          │
       │    ┌────────┐    ┌────────┐     │
       │    │Granted │    │Denied  │     │
       │    └───┬────┘    └───┬────┘     │
       │        │             │          │
       └────────┼─────────────┼──────────┘
                │             │
                ↓             ↓
         ┌─────────────┐  ┌──────────────┐
         │Start Camera │  │Show Settings │
         │   Preview   │  │   Hint      │
         └─────────────┘  └──────────────┘
```

---

## Data Structures

### ScannerViewModel State

```kotlin
data class ScannerState(
    val detectedText: String = "",           // Current detected text
    val isScanning: Boolean = true,          // Is actively scanning?
    val error: String? = null,               // Error message if any
    val scannedHistory: List<String> = []    // History of all scans
)
```

### Text Recognition Result

```kotlin
// ML Kit returns VisionText
VisionText {
    text: String                    // Full extracted text
    textBlocks: List<TextBlock>     // Blocks of text
    
    TextBlock {
        text: String                // Block text
        lines: List<Line>           // Lines in block
        
        Line {
            text: String            // Line text
            elements: List<Element> // Words in line
            
            Element {
                text: String        // Word text
                confidence: Float   // Accuracy (0.0-1.0)
                boundingBox: Rect   // Position
            }
        }
    }
}
```

---

## Thread Model

```
┌───────────────────────────────────────────────────────────┐
│                     Main Thread (UI)                      │
│  - Jetpack Compose rendering                              │
│  - User interactions                                      │
│  - StateFlow collection                                   │
└─────────────────┬─────────────────────────────────────────┘
                  │
                  │ viewModelScope.launch
                  ↓
┌───────────────────────────────────────────────────────────┐
│                   Coroutine Context                        │
│  - ViewModel operations                                    │
│  - StateFlow updates                                       │
└─────────────────┬─────────────────────────────────────────┘
                  │
                  │ Single Thread Executor
                  ↓
┌───────────────────────────────────────────────────────────┐
│                  Analysis Thread                           │
│  - Camera frame analysis                                   │
│  - TextRecognitionAnalyzer                                 │
│  - ML Kit processing (calls native code)                   │
└─────────────────┬─────────────────────────────────────────┘
                  │
                  │ Callback
                  ↓
┌───────────────────────────────────────────────────────────┐
│               Callback Thread (Main)                       │
│  - onTextDetected() / onError()                            │
│  - Update ViewModel                                        │
└───────────────────────────────────────────────────────────┘
```

---

## Error Handling

```
┌─────────────────────────────┐
│   Potential Error Points    │
└──────────┬──────────────────┘
           │
    ┌──────┼──────────┬──────────────┬────────────┐
    │      │          │              │            │
    ↓      ↓          ↓              ↓            ↓
┌────────┐ ┌──────┐ ┌──────┐ ┌────────────┐ ┌──────────┐
│Camera  │ │Frame │ │ML Kit│ │Permission  │ │ Memory │
│Binding │ │Proc. │ │Error │ │  Denied    │ │  Error │
└───┬────┘ └───┬──┘ └───┬──┘ └─────┬──────┘ └────┬─────┘
    │          │        │          │             │
    └──────────┼────────┼──────────┼─────────────┘
               │        │          │
               ↓        ↓          ↓
         ┌────────────────────────────┐
         │  Try-Catch in Analyzer     │
         │  - Log error               │
         │  - Call onError()          │
         └────────┬───────────────────┘
                  │
                  ↓
         ┌────────────────────────────┐
         │  ViewModel.setError()      │
         │  - Update error StateFlow  │
         └────────┬───────────────────┘
                  │
                  ↓
         ┌────────────────────────────┐
         │  UI Shows Error Banner     │
         │  - Red background          │
         │  - Error icon              │
         │  - Error message           │
         └────────────────────────────┘
```

---

## Performance Optimization

```
Camera Frame Rate: 30 FPS
         │
         ↓
┌──────────────────────────┐
│ Backpressure Strategy    │
│ KEEP_ONLY_LATEST         │
│ (Drop frames if busy)    │
└────────┬─────────────────┘
         │
         ↓
┌──────────────────────────┐
│  isProcessing Flag       │
│  (Prevent overlapping)   │
└────────┬─────────────────┘
         │
         ↓
┌──────────────────────────┐
│  Process 1 frame/sec     │
│  (Effective rate)        │
└────────┬─────────────────┘
         │
         ↓
┌──────────────────────────┐
│  ML Kit Processing       │
│  ~100-500ms per frame    │
└────────┬─────────────────┘
         │
         ↓
┌──────────────────────────┐
│  Result to UI            │
│  (Real-time feel)        │
└──────────────────────────┘
```

---

**Visual diagrams complete! Use these for understanding the OCR scanner architecture.**
