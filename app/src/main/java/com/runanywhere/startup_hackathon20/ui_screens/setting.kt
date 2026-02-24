package com.runanywhere.startup_hackathon20.ui_screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runanywhere.startup_hackathon20.viewmodel.HomeViewModel
import com.runanywhere.startup_hackathon20.ui.theme.Startup_hackathon20Theme
import kotlinx.coroutines.flow.MutableStateFlow


@Composable
fun SettingsScreen(
    theme: String,
    onThemeChange: (String) -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit = {},
    viewModel: HomeViewModel? = viewModel()
) {
    val currentUser by (viewModel?.currentUser ?: MutableStateFlow(null)).collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showThemeSelector by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
    ) {

        // TOP BAR
        Row(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF34D399), Color(0xFF10B981))
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Settings", color = Color.White, style = MaterialTheme.typography.titleLarge)
        }

        Column(Modifier.padding(16.dp)) {

            // THEME SELECTOR CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(Modifier.fillMaxWidth()) {

                // Toggle Row
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { showThemeSelector = !showThemeSelector }
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // ICON BOX
                    Box(
                        Modifier
                            .size(45.dp)
                            .background(
                                when (theme) {
                                    "neon" -> Color(0xFFD946EF)
                                    "orange" -> Color(0xFFFF6B35)
                                    "teal" -> Color(0xFF0891B2)
                                    else -> Color(0xFF4CAF50)
                                },
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(Modifier.weight(1f)) {
                        Text("Theme", style = MaterialTheme.typography.titleMedium)
                        Text(
                            when (theme) {
                                "neon" -> "Vibrant Neon"
                                "orange" -> "Vibrant Orange"
                                "teal" -> "Vibrant Teal"
                                "green" -> "Light Green"
                                "dark" -> "Dark Mode"
                                else -> "Vibrant Neon"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.rotate(if (showThemeSelector) 90f else 0f)
                    )
                }

                // EXPANDED OPTIONS
                AnimatedVisibility(showThemeSelector) {

                    Column(Modifier.padding(16.dp)) {

                        // VIBRANT NEON THEME OPTION (DEFAULT)
                        ThemeOption(
                            selected = theme == "neon",
                            title = "Vibrant Neon",
                            subtitle = "Purple & Cyan - Default",
                            gradient = Brush.horizontalGradient(
                                listOf(Color(0xFFD946EF), Color(0xFF06B6D4))
                            ),
                            icon = Icons.Default.Palette
                        ) { onThemeChange("neon") }

                        Spacer(Modifier.height(14.dp))

                        // VIBRANT ORANGE THEME OPTION
                        ThemeOption(
                            selected = theme == "orange",
                            title = "Vibrant Orange",
                            subtitle = "Warm Sunset Colors",
                            gradient = Brush.horizontalGradient(
                                listOf(Color(0xFFFF6B35), Color(0xFFFFB703))
                            ),
                            icon = Icons.Default.WbSunny
                        ) { onThemeChange("orange") }

                        Spacer(Modifier.height(14.dp))

                        // VIBRANT TEAL THEME OPTION
                        ThemeOption(
                            selected = theme == "teal",
                            title = "Vibrant Teal",
                            subtitle = "Ocean-Inspired Blues",
                            gradient = Brush.horizontalGradient(
                                listOf(Color(0xFF0891B2), Color(0xFF14B8A6))
                            ),
                            icon = Icons.Default.Waves
                        ) { onThemeChange("teal") }

                        Spacer(Modifier.height(14.dp))

                        // LIGHT GREEN THEME OPTION
                        ThemeOption(
                            selected = theme == "green",
                            title = "Light Green",
                            subtitle = "Natural & Calming",
                            gradient = Brush.horizontalGradient(
                                listOf(Color(0xFFE8FDEB), Color(0xFFD1FADF))
                            ),
                            icon = Icons.Default.Eco
                        ) { onThemeChange("green") }

                        Spacer(Modifier.height(14.dp))

                        // DARK THEME OPTION
                        ThemeOption(
                            selected = theme == "dark",
                            title = "Dark Mode",
                            subtitle = "Slate & Indigo",
                            gradient = Brush.horizontalGradient(
                                listOf(Color(0xFF1E293B), Color(0xFF312E81))
                            ),
                            icon = Icons.Default.DarkMode
                        ) { onThemeChange("dark") }
                    }
                }
                }
            }

            Spacer(Modifier.height(22.dp))

            // ACCOUNT SECTION (only show if user is logged in)
            if (currentUser != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(45.dp)
                                .background(Color(0xFF10B981), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(Modifier.weight(1f)) {
                            Text("Account", style = MaterialTheme.typography.titleMedium)
                            Text(
                                currentUser?.username ?: "Not logged in",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }

                    Divider(Modifier.padding(horizontal = 20.dp))

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { showLogoutDialog = true }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(45.dp)
                                .background(Color(0xFFEF4444), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                "Logout",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFEF4444)
                            )
                            Text(
                                "Sign out of your account",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                    }
                }

                Spacer(Modifier.height(22.dp))
            }

            // ABOUT SECTION
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        Modifier
                            .size(45.dp)
                            .background(Color.Gray, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.White)
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(Modifier.weight(1f)) {
                        Text("About App", style = MaterialTheme.typography.titleMedium)
                        Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }

                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }

            Spacer(Modifier.height(32.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("MediInsight", color = Color.Gray)
                Text("All data stored securely on your device", color = Color(0xFF9CA3AF), fontSize = MaterialTheme.typography.labelSmall.fontSize)
            }
        }

        // Logout confirmation dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Logout") },
                text = { Text("Are you sure you want to logout? You'll need to login again to access your account.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel?.logout()
                            showLogoutDialog = false
                            onLogout()
                        }
                    ) {
                        Text("Logout", color = Color(0xFFEF4444))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ThemeOption(
    selected: Boolean,
    title: String,
    subtitle: String,
    gradient: Brush,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                2.dp,
                if (selected) Color(0xFF10B981) else Color(0xFFDDDDDD),
                RoundedCornerShape(16.dp)
            )
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            // GRADIENT ICON BOX
            Box(
                Modifier
                    .size(45.dp)
                    .background(gradient, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White)
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            if (selected) {
                Box(
                    Modifier
                        .size(24.dp)
                        .background(Color(0xFF10B981), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Settings Screen - Neon Theme (Default)")
@Composable
fun SettingsScreenNeonPreview() {
    Startup_hackathon20Theme(themeMode = "neon") {
        SettingsScreen(
            theme = "neon",
            onThemeChange = {},
            onBack = {},
            onLogout = {},
            viewModel = null
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Settings Screen - Orange Theme")
@Composable
fun SettingsScreenOrangePreview() {
    Startup_hackathon20Theme(themeMode = "orange") {
        SettingsScreen(
            theme = "orange",
            onThemeChange = {},
            onBack = {},
            onLogout = {},
            viewModel = null
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Settings Screen - Teal Theme")
@Composable
fun SettingsScreenTealPreview() {
    Startup_hackathon20Theme(themeMode = "teal") {
        SettingsScreen(
            theme = "teal",
            onThemeChange = {},
            onBack = {},
            onLogout = {},
            viewModel = null
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Settings Screen - Green Theme")
@Composable
fun SettingsScreenGreenPreview() {
    Startup_hackathon20Theme(themeMode = "green") {
        SettingsScreen(
            theme = "green",
            onThemeChange = {},
            onBack = {},
            onLogout = {},
            viewModel = null
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Settings Screen - Dark Theme")
@Composable
fun SettingsScreenDarkPreview() {
    Startup_hackathon20Theme(themeMode = "dark") {
        SettingsScreen(
            theme = "dark",
            onThemeChange = {},
            onBack = {},
            onLogout = {},
            viewModel = null
        )
    }
}

@Preview(showBackground = true, name = "Theme Option - Neon", widthDp = 350)
@Composable
fun ThemeOptionNeonPreview() {
    Startup_hackathon20Theme {
        Surface(modifier = Modifier.padding(16.dp)) {
            ThemeOption(
                selected = true,
                title = "Vibrant Neon",
                subtitle = "Purple & Cyan - Default",
                gradient = Brush.horizontalGradient(
                    listOf(Color(0xFFD946EF), Color(0xFF06B6D4))
                ),
                icon = Icons.Default.Palette,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Theme Option - Orange", widthDp = 350)
@Composable
fun ThemeOptionOrangePreview() {
    Startup_hackathon20Theme {
        Surface(modifier = Modifier.padding(16.dp)) {
            ThemeOption(
                selected = false,
                title = "Vibrant Orange",
                subtitle = "Warm Sunset Colors",
                gradient = Brush.horizontalGradient(
                    listOf(Color(0xFFFF6B35), Color(0xFFFFB703))
                ),
                icon = Icons.Default.WbSunny,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Theme Option - Teal", widthDp = 350)
@Composable
fun ThemeOptionTealPreview() {
    Startup_hackathon20Theme {
        Surface(modifier = Modifier.padding(16.dp)) {
            ThemeOption(
                selected = false,
                title = "Vibrant Teal",
                subtitle = "Ocean-Inspired Blues",
                gradient = Brush.horizontalGradient(
                    listOf(Color(0xFF0891B2), Color(0xFF14B8A6))
                ),
                icon = Icons.Default.Waves,
                onClick = {}
            )
        }
    }
}
