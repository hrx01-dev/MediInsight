package com.runanywhere.startup_hackathon20.ui_screens

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.runanywhere.startup_hackathon20.ocr.TextRecognitionAnalyzer
import com.runanywhere.startup_hackathon20.viewmodel.ScannerViewModel
import com.runanywhere.startup_hackathon20.viewmodel.SharedMedicineViewModel
import java.util.concurrent.Executors

/**
 * Medicine Scanner Screen with OCR functionality
 * Uses device camera to scan and extract text from medicine labels/packages
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MedicineScannerScreen(
    onBack: () -> Unit,
    onUseCapturedText: (String) -> Unit = {},
    scannerViewModel: ScannerViewModel = viewModel(),
    sharedViewModel: SharedMedicineViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val detectedText by scannerViewModel.detectedText.collectAsState()
    val isScanning by scannerViewModel.isScanning.collectAsState()
    val error by scannerViewModel.error.collectAsState()

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var analyzer by remember { mutableStateOf<TextRecognitionAnalyzer?>(null) }

    // Initialize camera provider
    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProvider = cameraProviderFuture.get()
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            analyzer?.close()
            cameraProvider?.unbindAll()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Medicine Label") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                cameraPermissionState.status.isGranted -> {
                    // Camera preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        CameraPreview(
                            context = context,
                            lifecycleOwner = lifecycleOwner,
                            cameraProvider = cameraProvider,
                            isScanning = isScanning,
                            onTextDetected = { text ->
                                scannerViewModel.updateDetectedText(text)
                            },
                            onError = { exception ->
                                scannerViewModel.setError(exception.message ?: "Unknown error")
                            },
                            onAnalyzerCreated = { newAnalyzer ->
                                analyzer = newAnalyzer
                            }
                        )

                        // Scanning overlay
                        ScanningOverlay(
                            isScanning = isScanning,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Results section
                    ResultsSection(
                        detectedText = detectedText,
                        isScanning = isScanning,
                        error = error,
                        onToggleScanning = { scannerViewModel.toggleScanning() },
                        onClearHistory = { scannerViewModel.clearHistory() },
                        onUseCapturedText = {
                            // Set scanned text in shared ViewModel for AddMedicine screen
                            sharedViewModel.setScannedText(detectedText)
                            onUseCapturedText(detectedText)
                            onBack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.8f)
                    )
                }

                cameraPermissionState.status.shouldShowRationale -> {
                    // Permission rationale
                    PermissionRationale(
                        onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                    )
                }

                else -> {
                    // Initial permission request
                    PermissionRequest(
                        onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    cameraProvider: ProcessCameraProvider?,
    isScanning: Boolean,
    onTextDetected: (String) -> Unit,
    onError: (Exception) -> Unit,
    onAnalyzerCreated: (TextRecognitionAnalyzer) -> Unit
) {
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { previewView ->
            if (cameraProvider != null && isScanning) {
                try {
                    cameraProvider.unbindAll()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val analyzer = TextRecognitionAnalyzer(
                        onTextDetected = onTextDetected,
                        onError = onError
                    )
                    onAnalyzerCreated(analyzer)

                    imageAnalyzer.setAnalyzer(
                        Executors.newSingleThreadExecutor(),
                        analyzer
                    )

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Camera binding failed", e)
                    onError(e)
                }
            }
        }
    )
}

@Composable
private fun ScanningOverlay(
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Scanning frame
        Box(
            modifier = Modifier
                .size(280.dp)
                .background(Color.Transparent)
                .clip(RoundedCornerShape(16.dp))
        ) {
            // Corner indicators
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.TopStart)
                    .background(
                        if (isScanning) Color(0xFF34D399) else Color.Gray,
                        RoundedCornerShape(topStart = 16.dp)
                    )
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        if (isScanning) Color(0xFF34D399) else Color.Gray,
                        RoundedCornerShape(topEnd = 16.dp)
                    )
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.BottomStart)
                    .background(
                        if (isScanning) Color(0xFF34D399) else Color.Gray,
                        RoundedCornerShape(bottomStart = 16.dp)
                    )
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.BottomEnd)
                    .background(
                        if (isScanning) Color(0xFF34D399) else Color.Gray,
                        RoundedCornerShape(bottomEnd = 16.dp)
                    )
            )
        }

        // Status text
        Text(
            text = if (isScanning) "Scanning..." else "Paused",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
                .background(
                    Color.Black.copy(alpha = 0.6f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Instructions
        Text(
            text = "Position medicine label within the frame",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .background(
                    Color.Black.copy(alpha = 0.6f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ResultsSection(
    detectedText: String,
    isScanning: Boolean,
    error: String?,
    onToggleScanning: () -> Unit,
    onClearHistory: () -> Unit,
    onUseCapturedText: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilledIconButton(
                    onClick = onToggleScanning,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isScanning) Color(0xFFEF4444) else Color(0xFF34D399)
                    ),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isScanning) "Pause" else "Resume"
                    )
                }

                FilledIconButton(
                    onClick = onClearHistory,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }

                FilledIconButton(
                    onClick = onUseCapturedText,
                    enabled = detectedText.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFF3B82F6)
                    ),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Use Text")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error display
            if (error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFEE2E2)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = Color(0xFFDC2626),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Detected text section
            Text(
                text = "Detected Text:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                if (detectedText.isBlank()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Point camera at medicine label",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(
                            text = detectedText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Instructions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Tap ✓ to use this text for adding medicine",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PermissionRequest(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Camera Permission Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "We need camera access to scan medicine labels and extract text information.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Grant Camera Permission")
            }
        }
    }
}

@Composable
private fun PermissionRationale(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFFF59E0B)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Camera Access Needed",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Camera permission is essential for scanning medicine labels. Without it, you won't be able to use the OCR feature.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Try Again")
            }
        }
    }
}
