package com.runanywhere.startup_hackathon20.ui_screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runanywhere.startup_hackathon20.R
import com.runanywhere.startup_hackathon20.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.MutableStateFlow

data class Category(
    val icon: ImageVector,
    val title: String,
    val gradient: List<Color>
)

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    viewModel: HomeViewModel? = viewModel()
) {
    val userName by (viewModel?.userName ?: MutableStateFlow("User")).collectAsState()
    val recentInsights by (viewModel?.recentInsights ?: MutableStateFlow(emptyList())).collectAsState()

    val categories = listOf(
        Category(Icons.Default.Add, "Add Medicine", listOf(Color(0xFF4CAF50), Color(0xFF2ECC71))),
        Category(Icons.Default.Insights, "Insights", listOf(Color(0xFF2ECC71), Color(0xFF4CAF50))),
        Category(Icons.Default.CameraAlt, "Scan", listOf(Color(0xFF0891B2), Color(0xFF06B6D4))),
        Category(Icons.Default.Analytics, "AI Analysis", listOf(Color(0xFFFF6B6B), Color(0xFFEE5A6F)))
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        item {
            // 🌈 HEADER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .padding(top = 60.dp, bottom = 30.dp, start = 24.dp, end = 24.dp)
            ) {

                Column {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column {
                            Text(
                                "Hello, $userName",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = MaterialTheme.typography.headlineSmall.fontSize
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WifiOff,
                                    contentDescription = "",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Offline Mode Active",
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize
                                )
                            }
                        }

                        // Top buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            CircleIconButton(
                                icon = Icons.Default.Notifications
                            ) { onNavigate("notifications") }

                            CircleIconButton(
                                icon = Icons.Default.Settings
                            ) { onNavigate("settings") }
                        }
                    }

                    Spacer(Modifier.height(25.dp))

                    // 💬 Ask Expert Button
                    Button(
                        onClick = { onNavigate("chat") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Ask Expert", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // 🟢 Categories Grid
        item {
            Text(
                "Categories",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                categories.forEach { category ->
                    CategoryCard(
                        icon = category.icon,
                        label = category.title,
                        gradient = category.gradient
                    ) {
                        onNavigate(
                            when (category.title) {
                                "Add Medicine" -> "addMedicine"
                                "Insights" -> "insights"
                                "Scan" -> "scanner"
                                "AI Analysis" -> "analysis"
                                else -> ""
                            }
                        )
                    }
                }
            }
        }

        // 📝 Recent Insights
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent Insights",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (recentInsights.isNotEmpty()) {
                    Text(
                        "Based on your queries",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (recentInsights.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No insights yet",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Start asking questions in the chat to see personalized health insights here",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        items(recentInsights) { insight ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
                    .clickable { onNavigate("chat") },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            insight.title,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        // Category badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = getCategoryColor(insight.category)
                        ) {
                            Text(
                                insight.category,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        insight.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            insight.time,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "View details",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun getCategoryColor(category: String): Color {
    return when (category) {
        "Medication" -> Color(0xFF3B82F6)
        "Chronic Care" -> Color(0xFFEC4899)
        "Allergies" -> Color(0xFFF59E0B)
        "Common Illness" -> Color(0xFF10B981)
        "Nutrition" -> Color(0xFF8B5CF6)
        "Wellness" -> Color(0xFF06B6D4)
        "Pain Management" -> Color(0xFFEF4444)
        "Digestive" -> Color(0xFF14B8A6)
        "General Health" -> Color(0xFF6366F1)
        "Best Practice" -> Color(0xFF10B981)
        "Safety" -> Color(0xFFEF4444)
        else -> Color(0xFF6B7280)
    }
}

@Composable
fun CircleIconButton(icon: ImageVector, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "",
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun RowScope.CategoryCard(
    icon: ImageVector,
    label: String,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(label, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
@Preview(showBackground = true, showSystemUi = true, name = "Home Screen - Full")
@Composable
fun HomeScreenPreview() {
    com.runanywhere.startup_hackathon20.ui.theme.Startup_hackathon20Theme {
        HomeScreen(onNavigate = {}, viewModel = null)
    }
}

@Preview(showBackground = true, name = "Category Card", widthDp = 200, heightDp = 180)
@Composable
fun CategoryCardPreview() {
    com.runanywhere.startup_hackathon20.ui.theme.Startup_hackathon20Theme {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CategoryCard(
                icon = Icons.Default.Add,
                label = "Add Medicine",
                gradient = listOf(Color(0xFF4CAF50), Color(0xFF2ECC71))
            ) {}
        }
    }
}