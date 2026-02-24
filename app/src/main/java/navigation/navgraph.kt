package com.runanywhere.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.runanywhere.startup_hackathon20.ui_screens.AddMedicineScreen
import com.runanywhere.startup_hackathon20.ui_screens.AuthScreen
import com.runanywhere.startup_hackathon20.ui_screens.ChatScreen
import com.runanywhere.startup_hackathon20.ui_screens.HomeScreen
import com.runanywhere.startup_hackathon20.ui_screens.InsightsScreen
import com.runanywhere.startup_hackathon20.ui_screens.MedicineScannerScreen
import com.runanywhere.startup_hackathon20.ui_screens.NotificationsScreen
import com.runanywhere.startup_hackathon20.ui_screens.OnboardingScreens
import com.runanywhere.startup_hackathon20.ui_screens.SettingsScreen
import com.runanywhere.startup_hackathon20.ui_screens.SplashScreen

/**
 * Main navigation graph for the MediInsight application.
 * Handles navigation between all screens in the app.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    onThemeChange: (String) -> Unit = {}
) {
    // State for theme in settings screen
    val theme: MutableState<String> = remember { mutableStateOf("light") }

    NavHost(
        navController = navController,
        startDestination = Routes.Splash
    ) {
        // Splash Screen
        composable(route = Routes.Splash) {
            SplashScreen(
                onComplete = {
                    navController.navigate(Routes.Onboard)
                }
            )
        }

        // Onboarding Screen
        composable(route = Routes.Onboard) {
            OnboardingScreens(
                onComplete = {
                    navController.navigate(Routes.Auth)
                }
            )
        }

        // Authentication Screen
        composable(route = Routes.Auth) {
            AuthScreen(
                onComplete = {
                    navController.navigate(Routes.Home)
                }
            )
        }

        // Home Screen
        composable(route = Routes.Home) {
            HomeScreen(
                onNavigate = { route ->
                    when (route) {
                        "insights" -> navController.navigate(Routes.MedicalInsights)
                        "addMedicine" -> navController.navigate(Routes.AddMedicines)
                        "scanner" -> navController.navigate(Routes.MedicineScanner)
                        "settings" -> navController.navigate(Routes.Settings)
                        "notifications" -> navController.navigate(Routes.Notification)
                        "chat" -> navController.navigate(Routes.Chat)
                    }
                }
            )
        }

        // Medical Insights Screen
        composable(route = Routes.MedicalInsights) {
            InsightsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Add Medicines Screen
        composable(route = Routes.AddMedicines) {
            AddMedicineScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Settings Screen
        composable(route = Routes.Settings) {
            SettingsScreen(
                theme = theme.value,
                onThemeChange = { newTheme ->
                    theme.value = newTheme
                    onThemeChange(newTheme) // Propagate theme change to MainActivity
                },
                onBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    // Navigate to auth screen and clear back stack
                    navController.navigate(Routes.Auth) {
                        popUpTo(Routes.Home) { inclusive = true }
                    }
                }
            )
        }

        // Notifications Screen
        composable(route = Routes.Notification) {
            NotificationsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Chat Screen
        composable(route = Routes.Chat) {
            ChatScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Medicine Scanner Screen (OCR)
        composable(route = Routes.MedicineScanner) {
            MedicineScannerScreen(
                onBack = {
                    navController.popBackStack()
                },
                onUseCapturedText = { capturedText ->
                    // Navigate to Add Medicine screen with captured text
                    // You can pass the text as a navigation argument if needed
                    navController.navigate(Routes.AddMedicines)
                }
            )
        }
    }
}
