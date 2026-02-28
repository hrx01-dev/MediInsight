package com.runanywhere.startup_hackathon20.ui_screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runanywhere.startup_hackathon20.R
import com.runanywhere.startup_hackathon20.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun AuthScreen(
    onComplete: () -> Unit,
    viewModel: AuthViewModel? = viewModel()
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("login") }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var justSignedUp by remember { mutableStateOf(false) }
    var signupUserName by remember { mutableStateOf("") }

    val isLoading by (viewModel?.isLoading ?: MutableStateFlow(false)).collectAsState()
    val errorMessage by (viewModel?.errorMessage ?: MutableStateFlow<String?>(null)).collectAsState()
    val authSuccess by (viewModel?.authSuccess ?: MutableStateFlow(false)).collectAsState()

    // Notification permission launcher (for Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d("AuthScreen", "Notification permission granted: $isGranted")
        if (isGranted && justSignedUp && signupUserName.isNotEmpty()) {
            // Send welcome notification now that permission is granted
            viewModel?.sendWelcomeNotificationNow(signupUserName)
        }
    }

    // Navigate when auth is successful
    LaunchedEffect(authSuccess) {
        if (authSuccess) {
            // Request notification permission on Android 13+ after signup
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && justSignedUp) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            onComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFE8FBEA), Color(0xFFD2F6E4))
                )
            )
    ) {

        FloatingBubbles()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                "Welcome Back",
                fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Sign in to continue",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Card(
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    // Toggle Buttons Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(20.dp)
                            )
                            .padding(4.dp)
                    ) {
                        TabButton("login", activeTab) { activeTab = "login" }
                        Spacer(Modifier.width(8.dp))
                        TabButton("signup", activeTab) { activeTab = "signup" }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (activeTab == "signup") {
                        InputField(
                            label = "Name",
                            value = name,
                            onValueChange = { name = it },
                            icon = R.drawable.ic_user
                        )
                    }

                    InputField(
                        label = "Username",
                        value = username,
                        onValueChange = { username = it },
                        icon = R.drawable.ic_mail
                    )

                    InputField(
                        label = "Password",
                        value = password,
                        onValueChange = { password = it },
                        icon = R.drawable.ic_lock,
                        isPassword = true
                    )

                    if (activeTab == "signup") {
                        InputField(
                            label = "Confirm Password",
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            icon = R.drawable.ic_lock,
                            isPassword = true
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    // Error message display
                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (activeTab == "login") {
                                justSignedUp = false
                                viewModel?.login(username, password)
                            } else {
                                justSignedUp = true
                                signupUserName = name
                                viewModel?.register(name, username, password, confirmPassword)
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF4CAF50),
                                            Color(0xFF2EAD73)
                                        )
                                    ),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(14.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (activeTab == "login") "Login" else "Create Account",
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Text(
                        "🔒 Data stored securely offline",
                        color = Color.Gray,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingBubbles() {
    val infinite = rememberInfiniteTransition()
    val anim1 by infinite.animateFloat(
        initialValue = 0f, targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(Color(0x8032CD32), radius = 100f, center = Offset(200f, 200f + anim1))
        drawCircle(Color(0x4028A745), radius = 150f, center = Offset(size.width - 150f, 300f - anim1))
        drawCircle(Color(0x6020B2AA), radius = 120f, center = Offset(100f, size.height - 300f + anim1))
    }
}

@Composable
fun RowScope.TabButton(text: String, active: String, onClick: () -> Unit) {
    val isActive = text == active
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isActive) MaterialTheme.colorScheme.surface else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text.replaceFirstChar { it.uppercase() },
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: Int,
    isPassword: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(Modifier.padding(bottom = 16.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    label,
                    color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            placeholder = {
                Text(
                    "Enter $label",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isFocused)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = label,
                        tint = if (isFocused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            singleLine = true
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AuthScreenPreview() {
    AuthScreen(onComplete = {}, viewModel = null)
}