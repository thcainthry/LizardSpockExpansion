package com.example.lizardspockexpansion.ui.models

import android.graphics.Rect

data class HandGestureUiState(
    val mostRecentGesture: List<String>? = emptyList(),
    val handBoundingBoxes: List<Rect> = emptyList(),
)
