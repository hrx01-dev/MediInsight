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
import com.runanywhere.startup_hackathon20.viewmodel.MedicineViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import java.util.concurrent.Executors

/**
 * Medicine Scanner Screen with OCR functionality
 * Uses device camera to scan and extract text from medicine labels/packages
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MedicineScannerScreen(
    onBack: () -> Unit,
    onNavigateToChat: (String) -> Unit = {},
    onNavigateToInsights: (String) -> Unit = {},
    scannerViewModel: ScannerViewModel = viewModel(),
    sharedViewModel: SharedMedicineViewModel = viewModel(),
    medicineViewModel: MedicineViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val detectedText by scannerViewModel.detectedText.collectAsState()
    val isScanning by scannerViewModel.isScanning.collectAsState()
    val error by scannerViewModel.error.collectAsState()

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var analyzer by remember { mutableStateOf<TextRecognitionAnalyzer?>(null) }
    var prescriptionMode by remember { mutableStateOf(false) }
    var prescriptionSaved by remember { mutableStateOf(false) }
    val prescriptionMedicines = remember(detectedText, prescriptionMode) {
        if (prescriptionMode) parsePrescriptionMedicines(detectedText) else emptyList()
    }

    // Initialize camera provider
    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()
            },
            ContextCompat.getMainExecutor(context)
        )
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
                title = { Text(if (prescriptionMode) "Scan Prescription" else "Scan Medicine Label") },
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
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !prescriptionMode, onClick = { prescriptionMode = false; prescriptionSaved = false }, label = { Text("Medicine") }, leadingIcon = { Icon(Icons.Default.Medication, null) })
                FilterChip(selected = prescriptionMode, onClick = { prescriptionMode = true; prescriptionSaved = false }, label = { Text("Prescription") }, leadingIcon = { Icon(Icons.Default.Description, null) })
            }
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
                    if (prescriptionMode) {
                        PrescriptionResultsSection(
                            detectedText = detectedText,
                            medicines = prescriptionMedicines,
                            isScanning = isScanning,
                            error = error,
                            saved = prescriptionSaved,
                            onToggleScanning = { scannerViewModel.toggleScanning() },
                            onSave = {
                                prescriptionMedicines.forEach { medicine ->
                                    medicineViewModel.insertMedicine(medicine.name, medicine.dosage, medicine.frequency, "", medicine.duration, medicine.quantity, medicine.instructions)
                                }
                                prescriptionSaved = prescriptionMedicines.isNotEmpty()
                            },
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        )
                    } else ResultsSection(
                        detectedText = detectedText,
                        isScanning = isScanning,
                        error = error,
                        onToggleScanning = { scannerViewModel.toggleScanning() },
                        onClearHistory = { scannerViewModel.clearHistory() },
                        onAskAI = {
                            // Send extracted text to chat with ultra-concise preset message
                            // Optimized for small model token limits - single focused question
                            val presetMessage = """Medicine: $detectedText

What is this medicine used for? (Brief answer)"""
                            onNavigateToChat(presetMessage)
                        },
                        onAddToInsights = {
                            // Navigate to insights with scanned text
                            onNavigateToInsights(detectedText)
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

private data class PrescriptionMedicine(
    val name: String,
    val dosage: String = "",
    val frequency: String = "",
    val duration: String = "",
    val quantity: String = "",
    val instructions: String = ""
)

/** Split OCR text into medicine-like rows and attach nearby prescription instructions. */
private fun parsePrescriptionMedicines(text: String): List<PrescriptionMedicine> {
    val lines = text.lines().map { it.replace(Regex("\\s+"), " ").trim() }.filter { it.isNotBlank() }
    val dosageRegex = Regex("(?i)\\b\\d+(?:\\.\\d+)?\\s*(?:mg|mcg|µg|g|ml|mL|IU|units?)\\b")
    val frequencyRegex = Regex("(?i)\\b(?:once|twice|thrice|\\d+\\s*(?:times|x)\\s*(?:a|per)\\s*day|every\\s*\\d+\\s*hours?|OD|BD|BID|TID|QID|HS|SOS)\\b")
    val durationRegex = Regex("(?i)\\b\\d+\\s*(?:days?|weeks?|months?)\\b")
    val dosageFormRegex = Regex("(?i)\\b(?:tab(?:let)?s?|cap(?:sule)?s?|syrup|suspension|injection|inj|cream|ointment|drops?)\\b")
    val stopWords = Regex("(?i)\\b(?:patient|doctor|dr|date|age|address|phone|hospital|clinic|diagnosis|prescription|rx|signature|registration|reg|follow.?up|allergy|complaint|history|mrn|name)\\b")
    val result = mutableListOf<PrescriptionMedicine>()
    lines.forEach { line ->
        val doseMatch = dosageRegex.find(line)
        val formMatch = dosageFormRegex.find(line)
        val hasMedicineEvidence = doseMatch != null || formMatch != null
        if (!hasMedicineEvidence || stopWords.containsMatchIn(line)) return@forEach

        val boundary = listOfNotNull(doseMatch?.range?.first, formMatch?.range?.first).minOrNull() ?: return@forEach
        val prefix = line.substring(0, boundary).trim().trimEnd(':', '-', '.', ',', ';', ')')
        val name = prefix.replace(Regex("(?i)^(?:\\d+[.)]?\\s*|(?:tab|tablet|cap|capsule)\\s+)"), "").trim()
        if (name.length < 2 || name.split(Regex("\\s+")).size > 5 || stopWords.containsMatchIn(name)) return@forEach

        val dosage = listOfNotNull(doseMatch?.value, formMatch?.value).joinToString(" ").trim()
        val frequency = frequencyRegex.find(line)?.value.orEmpty()
        val duration = durationRegex.find(line)?.value.orEmpty()
        val quantity = Regex("(?i)\\b(?:qty|quantity)\\s*[:.-]?\\s*(\\d+)\\b").find(line)?.groupValues?.get(1)?.let { "$it units" }.orEmpty()
        val instructionBits = buildList {
            Regex("(?i)\\b(?:after|before|with)\\s+(?:food|meals?|breakfast|lunch|dinner)\\b|\\bon empty stomach\\b|\\b(?:morning|afternoon|evening|night|bedtime)\\b")
                .findAll(line).forEach { add(it.value) }
        }.distinct()
        result.add(PrescriptionMedicine(name, dosage, frequency, "", duration, quantity, instructionBits.joinToString("; ")))
    }
    return result.distinctBy { it.name.lowercase() }
}

@Composable
private fun PrescriptionResultsSection(
    detectedText: String,
    medicines: List<PrescriptionMedicine>,
    isScanning: Boolean,
    error: String?,
    saved: Boolean,
    onToggleScanning: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Prescription medicines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Keep the full prescription in view. Review the recognized entries before saving.", style = MaterialTheme.typography.bodySmall)
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
        if (medicines.isEmpty()) {
            Text(if (detectedText.isBlank()) "Point the camera at a prescription." else "No medicine rows recognized yet. Try adjusting the camera or lighting.", modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("${medicines.size} medicine${if (medicines.size == 1) "" else "s"} found", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelLarge)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(medicines) { medicine ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(medicine.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(listOf(medicine.dosage, medicine.frequency, medicine.duration).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Details need review" }, style = MaterialTheme.typography.bodyMedium)
                            if (medicine.quantity.isNotBlank()) Text("Quantity: ${medicine.quantity}", style = MaterialTheme.typography.bodySmall)
                            if (medicine.instructions.isNotBlank()) Text(medicine.instructions, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Button(onClick = onToggleScanning, modifier = Modifier.fillMaxWidth()) {
            Icon(if (isScanning) Icons.Default.Pause else Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(8.dp)); Text(if (isScanning) "Pause scanning" else "Resume scanning")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onSave, enabled = medicines.isNotEmpty() && !saved, modifier = Modifier.fillMaxWidth()) {
            Icon(if (saved) Icons.Default.Check else Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp)); Text(if (saved) "Added to Insights" else "Add all to Insights")
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
    onAskAI: () -> Unit,
    onAddToInsights: () -> Unit,
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

            if (detectedText.isBlank()) {
                // Empty state
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

                // Medicine Details Section
                Text(
                    text = "Medicine Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ✅ Action Buttons (MOVED ABOVE)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Ask AI Button
                    Button(
                        onClick = onAskAI,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8B5CF6)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Ask AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }

                    // Add to Insights Button
                    Button(
                        onClick = onAddToInsights,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF06B6D4)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Add to Insights",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ✅ Pause Button (ALSO MOVED UP)
                Button(
                    onClick = onToggleScanning,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScanning) Color(0xFFEF4444) else Color(0xFF34D399)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isScanning) "Pause Scanning" else "Resume Scanning")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ✅ Detected Text Card (NOW BELOW BUTTONS)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(
                            text = detectedText,
                            style = MaterialTheme.typography.bodyMedium
                        )
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
                        text = "Choose an action or continue scanning",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }    // Medicine Details Section
    }
}
@Composable

fun PermissionRequest(
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
fun PermissionRationale(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Camera permission is required to scan medicine labels.",
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}



