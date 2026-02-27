package com.runanywhere.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import android.net.Uri
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
import com.runanywhere.startup_hackathon20.ui_screens.VoiceAssistantScreen

/**
 * Main navigation graph for the MediInsight application.
 * Handles navigation between all screens in the app with smooth animated transitions.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    onThemeChange: (String) -> Unit = {}
) {
    // State for theme in settings screen
    val theme: MutableState<String> = remember { mutableStateOf("neon") }

    NavHost(
        navController = navController,
        startDestination = Routes.Splash
    ) {
        // Splash Screen
        composable(
            route = Routes.Splash,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            SplashScreen(
                onComplete = {
                    navController.navigate(Routes.Onboard)
                }
            )
        }

        // Onboarding Screen - Slide in from right
        composable(
            route = Routes.Onboard,
            enterTransition = { ScreenTransitions.slideInFromRightTransition().targetContentEnter },
            exitTransition = { ScreenTransitions.slideInFromRightTransition().initialContentExit }
        ) {
            OnboardingScreens(
                onComplete = {
                    navController.navigate(Routes.Auth) {
                        popUpTo(Routes.Onboard) { inclusive = true }
                    }
                }
            )
        }

        // Authentication Screen - Slide in from right
        composable(
            route = Routes.Auth,
            enterTransition = { ScreenTransitions.slideInFromRightTransition().targetContentEnter },
            exitTransition = { ScreenTransitions.slideInFromRightTransition().initialContentExit }
        ) {
            AuthScreen(
                onComplete = {
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Auth) { inclusive = true }
                    }
                }
            )
        }

        // Home Screen - Main hub (zoom in from previous screens)
        composable(
            route = Routes.Home,
            enterTransition = { ScreenTransitions.zoomInTransition().targetContentEnter },
            exitTransition = { ScreenTransitions.zoomInTransition().initialContentExit }
        ) {
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

        // Medical Insights Screen - Zoom in from home
        composable(
            route = Routes.MedicalInsights,
            enterTransition = { ScreenTransitions.zoomInTransition(duration = 500).targetContentEnter },
            exitTransition = { ScreenTransitions.zoomOutTransition(duration = 400).initialContentExit },
            popEnterTransition = { ScreenTransitions.zoomInTransition(duration = 400).targetContentEnter },
            popExitTransition = { ScreenTransitions.zoomOutTransition(duration = 500).initialContentExit }
        ) {
            InsightsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Add Medicines Screen - Slide from right
        composable(
            route = Routes.AddMedicines,
            enterTransition = { ScreenTransitions.slideInFromRightTransition().targetContentEnter },
            exitTransition = { 
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300, easing = EaseInCubic)
                ) + fadeOut(animationSpec = tween(durationMillis = 200))
            },
            popEnterTransition = { ScreenTransitions.slideInFromLeftTransition().targetContentEnter },
            popExitTransition = { ScreenTransitions.slideInFromRightTransition().initialContentExit }
        ) {
            AddMedicineScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Settings Screen - Slide up from bottom (modal)
        composable(
            route = Routes.Settings,
            enterTransition = { ScreenTransitions.slideUpFromBottomTransition(duration = 500).targetContentEnter },
            exitTransition = { ScreenTransitions.slideDownFromTopTransition(duration = 500).initialContentExit },
            popEnterTransition = { ScreenTransitions.slideDownFromTopTransition(duration = 400).targetContentEnter },
            popExitTransition = { ScreenTransitions.slideUpFromBottomTransition(duration = 400).initialContentExit }
        ) {
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

        // Notifications Screen - Slide up from bottom (modal)
        composable(
            route = Routes.Notification,
            enterTransition = { ScreenTransitions.slideUpFromBottomTransition().targetContentEnter },
            exitTransition = { ScreenTransitions.slideDownFromTopTransition().initialContentExit },
            popEnterTransition = { ScreenTransitions.slideDownFromTopTransition(duration = 400).targetContentEnter },
            popExitTransition = { ScreenTransitions.slideUpFromBottomTransition(duration = 400).initialContentExit }
        ) {
            NotificationsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Chat Screen - Slide up from bottom (modal)
        composable(
            route = Routes.ChatWithArg,
            arguments = listOf(
                navArgument("preset") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            enterTransition = { ScreenTransitions.slideUpFromBottomTransition().targetContentEnter },
            exitTransition = { ScreenTransitions.slideDownFromTopTransition().initialContentExit },
            popEnterTransition = { ScreenTransitions.slideDownFromTopTransition(duration = 400).targetContentEnter },
            popExitTransition = { ScreenTransitions.slideUpFromBottomTransition(duration = 400).initialContentExit }
        ) {backStackEntry ->

            val presetMessage =
                backStackEntry.arguments?.getString("preset")
            ChatScreen(
                presetMessage = presetMessage,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Medicine Scanner Screen - Bounce in
        composable(
            route = Routes.MedicineScanner,
            enterTransition = { ScreenTransitions.bounceInTransition(duration = 600).targetContentEnter },
            exitTransition = { ScreenTransitions.bounceInTransition(duration = 400).initialContentExit },
            popEnterTransition = { ScreenTransitions.bounceInTransition(duration = 500).targetContentEnter },
            popExitTransition = { ScreenTransitions.bounceInTransition(duration = 400).initialContentExit }
        ) {
            MedicineScannerScreen(
                onBack = {
                    navController.popBackStack()
                },
                onNavigateToChat = { presetMessage ->
                    navController.navigate(
                        "chat?preset=${Uri.encode(presetMessage)}"
                    )
                }
            )
        }

        // Voice Assistant Screen
        composable(route = Routes.VoiceAssistant) {
            VoiceAssistantScreen(navController = navController)
        }

        // Medicine Analysis Screen (OCR to AI Analysis Pipeline)

    }
}
