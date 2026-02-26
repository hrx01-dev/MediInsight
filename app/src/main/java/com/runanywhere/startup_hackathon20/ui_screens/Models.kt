package com.runanywhere.startup_hackathon20.ui_screens

/**
 * Common data models used across multiple screens
 */

data class Insight(
    val title: String,
    val description: String,
    val time: String,
    val category: String = "Health"
)
