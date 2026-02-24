package com.runanywhere.startup_hackathon20.ui_screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runanywhere.startup_hackathon20.ui.theme.Startup_hackathon20Theme
import com.runanywhere.startup_hackathon20.viewmodel.MedicineViewModel
import com.runanywhere.startup_hackathon20.viewmodel.SharedMedicineViewModel
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun AddMedicineScreen(
    onBack: () -> Unit,
    onMedicineAdded: () -> Unit = {},
    viewModel: MedicineViewModel? = viewModel(),
    sharedViewModel: SharedMedicineViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }
    var showAIParsingDialog by remember { mutableStateOf(false) }

    // Create fallback flows once and reuse them
    val defaultLoadingFlow = remember { MutableStateFlow(false) }
    val defaultErrorFlow = remember { MutableStateFlow<String?>(null) }
    
    val isLoading by (viewModel?.isLoading ?: defaultLoadingFlow).collectAsState()
    val error by (viewModel?.error ?: defaultErrorFlow).collectAsState()
    
    // Shared ViewModel states for scanned text
    val parsedMedicine by sharedViewModel.parsedMedicine.collectAsState()
    val isParsing by sharedViewModel.isParsing.collectAsState()
    val parseError by sharedViewModel.parseError.collectAsState()
    
    // Auto-fill form when parsed medicine data is available
    LaunchedEffect(parsedMedicine) {
        parsedMedicine?.let { parsed ->
            name = parsed.name
            dosage = parsed.dosage
            frequency = parsed.frequency
            time = parsed.time
            duration = parsed.duration
            quantity = parsed.quantity
            instructions = parsed.instructions
            showAIParsingDialog = true
            // Clear after using
            // sharedViewModel.clearScannedData() // Don't clear immediately, let user see it
        }
    }

    // Success dialog
    if (showSuccess) {
        AlertDialog(
            onDismissRequest = {
                showSuccess = false
                onMedicineAdded()
                onBack()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(64.dp)
                )
            },
            title = {
                Text(
                    "Success!",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text("Medicine has been added successfully and stored securely offline.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccess = false
                        onMedicineAdded()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D399))
                ) {
                    Text("View Insights")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSuccess = false
                        // Clear form
                        name = ""
                        dosage = ""
                        frequency = ""
                        time = ""
                        duration = ""
                        quantity = ""
                        instructions = ""
                    }
                ) {
                    Text("Add Another")
                }
            }
        )
    }
    
    // AI Parsing Success Dialog
    if (showAIParsingDialog) {
        AlertDialog(
            onDismissRequest = {
                showAIParsingDialog = false
                sharedViewModel.clearScannedData()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(64.dp)
                )
            },
            title = {
                Text(
                    "AI Parsed Medicine Info",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text("The AI has extracted medicine information from the scanned text:")
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isParsing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Analyzing text with AI...")
                    } else {
                        Text("✓ Form fields have been auto-filled", fontWeight = FontWeight.Medium)
                        Text("Please review and edit if needed.", style = MaterialTheme.typography.bodySmall)
                    }
                    parseError?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Note: $err",
                            color = Color(0xFFEF4444),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAIParsingDialog = false
                        sharedViewModel.clearScannedData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("Got It!")
                }
            }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP BAR
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Text(
                "Add Medicine",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge
            )
        }

        // Error message
        error?.let { errorMessage ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = Color(0xFFDC2626)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFDC2626)
                    )
                }
            }
        }
        
        // AI Parsing Status Banner
        if (isParsing) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFDEEDFF))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF3B82F6)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "AI is analyzing scanned text...",
                        color = Color(0xFF3B82F6),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else if (parsedMedicine != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Auto-filled from Scan",
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Review and edit fields as needed",
                            color = Color(0xFF059669),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = { sharedViewModel.clearScannedData() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color(0xFF10B981)
                        )
                    }
                }
            }
        }

        Column(
            Modifier
                .weight(1f)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Medicine Details",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Fill in the information below",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Form fields
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Medicine Name *") },
                        leadingIcon = {
                            Icon(Icons.Default.MedicalServices, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF34D399),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = Color(0xFF34D399),
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedLeadingIconColor = Color(0xFF34D399),
                            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = dosage,
                        onValueChange = { dosage = it },
                        label = { Text("Dosage (e.g., 500mg) *") },
                        leadingIcon = {
                            Icon(Icons.Default.LocalPharmacy, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF34D399),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = Color(0xFF34D399),
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedLeadingIconColor = Color(0xFF34D399),
                            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = frequency,
                        onValueChange = { frequency = it },
                        label = { Text("Frequency (e.g., 3 times a day) *") },
                        leadingIcon = {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF34D399),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = Color(0xFF34D399),
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedLeadingIconColor = Color(0xFF34D399),
                            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Time (e.g., After meals) *") },
                        leadingIcon = {
                            Icon(Icons.Default.AccessTime, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF34D399),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = Color(0xFF34D399),
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedLeadingIconColor = Color(0xFF34D399),
                            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text("Duration (e.g., 7 days) *") },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF34D399),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = Color(0xFF34D399),
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedLeadingIconColor = Color(0xFF34D399),
                            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantity (e.g., 21 tablets) *") },
                        leadingIcon = {
                            Icon(Icons.Default.Tag, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF34D399),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = Color(0xFF34D399),
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedLeadingIconColor = Color(0xFF34D399),
                            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = { Text("Instructions *") },
                        leadingIcon = {
                            Icon(Icons.Default.Description, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF34D399),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = Color(0xFF34D399),
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedLeadingIconColor = Color(0xFF34D399),
                            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Add button
                    Button(
                        onClick = {
                            if (name.isNotBlank() && dosage.isNotBlank() &&
                                frequency.isNotBlank() && time.isNotBlank() &&
                                duration.isNotBlank() && quantity.isNotBlank() &&
                                instructions.isNotBlank()
                            ) {
                                viewModel?.insertMedicine(
                                    name = name.trim(),
                                    dosage = dosage.trim(),
                                    frequency = frequency.trim(),
                                    time = time.trim(),
                                    duration = duration.trim(),
                                    quantity = quantity.trim(),
                                    instructions = instructions.trim()
                                )
                                showSuccess = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isLoading && name.isNotBlank() && dosage.isNotBlank() &&
                                frequency.isNotBlank() && time.isNotBlank() &&
                                duration.isNotBlank() && quantity.isNotBlank() &&
                                instructions.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF34D399),
                            disabledContainerColor = Color(0xFFD1D5DB)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Add Medicine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Info text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Data stored securely on your device",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AddMedicineScreenPreview() {
    Startup_hackathon20Theme {
        AddMedicineScreen(
            onBack = {},
            onMedicineAdded = {},
            viewModel = null
        )
    }
}
