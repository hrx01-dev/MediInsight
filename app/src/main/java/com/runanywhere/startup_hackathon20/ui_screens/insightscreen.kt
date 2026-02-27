package com.runanywhere.startup_hackathon20.ui_screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runanywhere.startup_hackathon20.database.MedicineEntity
import com.runanywhere.startup_hackathon20.ui.theme.Startup_hackathon20Theme
import com.runanywhere.startup_hackathon20.viewmodel.MedicineViewModel
import java.text.SimpleDateFormat
import java.util.*

data class Medicine(
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val frequency: String,
    val time: String,
    val duration: String,
    val quantity: String,
    val instructions: String,
    val createdAt: Long = System.currentTimeMillis()
)

fun MedicineEntity.toMedicine() = Medicine(
    id = id,
    name = name,
    dosage = dosage,
    frequency = frequency,
    time = time,
    duration = duration,
    quantity = quantity,
    instructions = instructions,
    createdAt = createdAt
)

@Composable
fun InsightsScreen(
    onBack: () -> Unit,
    scannedText: String? = null,
    viewModel: MedicineViewModel = viewModel()
) {
    val medicines by viewModel.allMedicines.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // If we have scanned text, show a dialog to add it to insights
    var showAddDialog by remember { mutableStateOf(!scannedText.isNullOrBlank()) }
    var medicineName by remember { mutableStateOf(scannedText ?: "") }
    var dosage by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("") }
    
    if (showAddDialog && !scannedText.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Scanned Medicine to Insights") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    TextField(
                        value = medicineName,
                        onValueChange = { medicineName = it },
                        label = { Text("Medicine Name/Details") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                    TextField(
                        value = dosage,
                        onValueChange = { dosage = it },
                        label = { Text("Dosage") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                    TextField(
                        value = frequency,
                        onValueChange = { frequency = it },
                        label = { Text("Frequency") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.insertMedicine(
                            name = medicineName.ifBlank { "Scanned Medicine" },
                            dosage = dosage.ifBlank { "N/A" },
                            frequency = frequency.ifBlank { "N/A" },
                            time = "",
                            duration = "",
                            quantity = "",
                            instructions = scannedText
                        )
                        showAddDialog = false
                    }
                ) {
                    Text("Save to Insights")
                }
            },
            dismissButton = {
                Button(onClick = { showAddDialog = false }) {
                    Text("Cancel")
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
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
            }
            Text(
                "Medical Insights",
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
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFFDC2626)
                )
            }
        }

        Column(
            Modifier
                .weight(1f)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (medicines.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Medicines Added Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add medicines from the home screen to see them here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                medicines.forEach { medicineEntity ->
                    MedicineCard(
                        medicine = medicineEntity.toMedicine(),
                        onDelete = { viewModel.deleteMedicine(medicineEntity) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "🔒 All medicines stored securely offline",
                Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}



@Composable
fun InfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        Modifier
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = MaterialTheme.typography.labelSmall.fontSize
            )
            Text(value, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun MedicineCard(medicine: Medicine, onDelete: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(medicine.createdAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(20.dp)) {
        // Header with medicine name and delete button
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    medicine.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Added: $formattedDate",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete medicine",
                    tint = Color(0xFFEF4444)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Medicine icon with dosage
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(60.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MedicalServices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    "Dosage",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    medicine.dosage,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Medicine details
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    RoundedCornerShape(18.dp)
                )
                .border(1.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            InfoGrid("Frequency", medicine.frequency)
            InfoGrid("Time", medicine.time)
            InfoGrid("Duration", medicine.duration)
            InfoGrid("Quantity", medicine.quantity)
        }

        Spacer(Modifier.height(12.dp))

        // Instructions section
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Instructions",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                medicine.instructions,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(12.dp))

        Divider()

        Text(
            "🔒 Stored securely on your device",
            Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )
        }
    }
}

@Composable
fun InfoGrid(label: String, value: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = MaterialTheme.typography.labelSmall.fontSize
        )
        Spacer(Modifier.height(4.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Insights Screen - Empty")
@Composable
fun InsightsScreenEmptyPreview() {
    Startup_hackathon20Theme {
        InsightsScreen(onBack = {})
    }
}

@Preview(showBackground = true, name = "Medicine Card", widthDp = 400)
@Composable
fun MedicineCardPreview() {
    Startup_hackathon20Theme {
        Column(modifier = Modifier.padding(16.dp)) {
            MedicineCard(
                medicine = Medicine(
                    id = 1,
                    name = "Aspirin",
                    dosage = "500mg",
                    frequency = "Twice daily",
                    time = "After meals",
                    duration = "7 days",
                    quantity = "14 tablets",
                    instructions = "Take with plenty of water. Do not exceed recommended dosage.",
                    createdAt = System.currentTimeMillis()
                ),
                onDelete = {}
            )
        }
    }
}
