package com.runanywhere.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.EaseInBounce
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutBounce
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically

/**
 * Collection of reusable animated transitions for screen navigation
 * Provides smooth, professional animations between different screens
 */
object ScreenTransitions {

    // ==================== SLIDE TRANSITIONS ====================
    /**
     * Slide in from right with fade, slide out to left with fade
     * Best for: Forward navigation
     */
    fun slideInFromRightTransition(): ContentTransform {
        return ContentTransform(
            targetContentEnter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 400, easing = EaseOutCubic)
            ) + fadeIn(animationSpec = tween(durationMillis = 300)),
            initialContentExit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 2 },
                animationSpec = tween(durationMillis = 300, easing = EaseInCubic)
            ) + fadeOut(animationSpec = tween(durationMillis = 200)),
            targetContentZIndex = 1f
        )
    }

    /**
     * Slide in from left with fade, slide out to right with fade
     * Best for: Back navigation
     */
    fun slideInFromLeftTransition(): ContentTransform {
        return ContentTransform(
            targetContentEnter = slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth },
                animationSpec = tween(durationMillis = 400, easing = EaseOutCubic)
            ) + fadeIn(animationSpec = tween(durationMillis = 300)),
            initialContentExit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth / 2 },
                animationSpec = tween(durationMillis = 300, easing = EaseInCubic)
            ) + fadeOut(animationSpec = tween(durationMillis = 200)),
            targetContentZIndex = 1f
        )
    }

    // ==================== VERTICAL SLIDE TRANSITIONS ====================
    /**
     * Slide up from bottom with fade
     * Best for: Modal-like screens (Settings, Notifications)
     */
    fun slideUpFromBottomTransition(
        duration: Int = 500
    ): ContentTransform {
        return ContentTransform(
            targetContentEnter = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(durationMillis = duration, easing = EaseOutBounce)
            ) + fadeIn(animationSpec = tween(durationMillis = 300)),
            initialContentExit = slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(durationMillis = duration, easing = EaseInBounce)
            ) + fadeOut(animationSpec = tween(durationMillis = 200)),
            targetContentZIndex = 1f
        )
    }

    /**
     * Slide down from top with fade
     * Best for: Dismissing modal screens
     */
    fun slideDownFromTopTransition(
        duration: Int = 500
    ): ContentTransform {
        return ContentTransform(
            targetContentEnter = slideInVertically(
                initialOffsetY = { fullHeight -> -fullHeight },
                animationSpec = tween(durationMillis = duration, easing = EaseOutCubic)
            ) + fadeIn(animationSpec = tween(durationMillis = 300)),
            initialContentExit = slideOutVertically(
                targetOffsetY = { fullHeight -> -fullHeight },
                animationSpec = tween(durationMillis = duration, easing = EaseInCubic)
            ) + fadeOut(animationSpec = tween(durationMillis = 200)),
            targetContentZIndex = 1f
        )
    }

    // ==================== SCALE TRANSITIONS ====================
    /**
     * Zoom in with fade (like a pop-up)
     * Best for: Detail screens, expanded views
     */
    fun zoomInTransition(
        duration: Int = 400
    ): ContentTransform {
        return ContentTransform(
            targetContentEnter = scaleIn(
                initialScale = 0.85f,
                animationSpec = tween(durationMillis = duration, easing = EaseOutCubic)
            ) + fadeIn(animationSpec = tween(durationMillis = 300)),
            initialContentExit = scaleOut(
                targetScale = 0.85f,
                animationSpec = tween(durationMillis = 300, easing = EaseInCubic)
            ) + fadeOut(animationSpec = tween(durationMillis = 200)),
            targetContentZIndex = 1f
        )
    }

    /**
     * Zoom out with fade (like collapsing)
     * Best for: Going back from detail screens
     */
    fun zoomOutTransition(
        duration: Int = 400
    ): ContentTransform {
        return ContentTransform(
            targetContentEnter = scaleIn(
                initialScale = 1.1f,
                animationSpec = tween(durationMillis = duration, easing = EaseOutCubic)
            ) + fadeIn(animationSpec = tween(durationMillis = 300)),
            initialContentExit = scaleOut(
                targetScale = 1.1f,
                animationSpec = tween(durationMillis = 300, easing = EaseInCubic)
            ) + fadeOut(animationSpec = tween(durationMillis = 200)),
            targetContentZIndex = 1f
        )
    }

    // ==================== COMBINED TRANSITIONS ====================
    /**
     * Slide + Scale transition (Elegant forward navigation)
     * Best for: Main feature screens
     */
    fun slideScaleTransition(
        duration: Int = 500
    ): ContentTransform {
        return ContentTransform(
            targetContentEnter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = duration, easing = EaseOutCubic)
            ) + scaleIn(
                initialScale = 0.95f,
                animationSpec = tween(durationMillis = duration, easing = EaseOutCubic)
            ) + fadeIn(animationSpec = tween(durationMillis = 300)),
            initialContentExit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(durationMillis = 300, easing = EaseInCubic)
            ) + scaleOut(
                targetScale = 0.95f,
                animationSpec = tween(durationMillis = 300, easing = EaseInCubic)
            ) + fadeOut(animationSpec = tween(durationMillis = 200)),
            targetContentZIndex = 1f
        )
    }

    /**
     * Fade with subtle scale (Smooth minimal transition)
     * Best for: Similar screens, minimal disruption
     */
    fun fadeScaleTransition(
        duration: Int = 300
    ): ContentTransform {
        return ContentTransform(
            targetContentEnter = fadeIn(animationSpec = tween(durationMillis = duration)) +
                    scaleIn(
                        initialScale = 0.98f,
                        animationSpec = tween(durationMillis = duration)
                    ),
            initialContentExit = fadeOut(animationSpec = tween(durationMillis = 200)) +
                    scaleOut(
                        targetScale = 1.02f,
                        animationSpec = tween(durationMillis = 200)
                    ),
            targetContentZIndex = 1f
        )
    }

    // ==================== BOUNCE TRANSITIONS ====================
    /**
     * Bouncy entrance from right
     * Best for: Fun, interactive screens
     */
    fun bounceInTransition(
        duration: Int = 600
    ): ContentTransform {
        return ContentTransform(
            targetContentEnter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = duration, easing = EaseOutBounce)
            ) + fadeIn(animationSpec = tween(durationMillis = 300)),
            initialContentExit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 2 },
                animationSpec = tween(durationMillis = 300, easing = EaseInCubic)
            ) + fadeOut(animationSpec = tween(durationMillis = 200)),
            targetContentZIndex = 1f
        )
    }
}
