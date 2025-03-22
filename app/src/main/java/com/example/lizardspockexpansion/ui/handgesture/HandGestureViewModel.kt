package com.example.lizardspockexpansion.ui.handgesture

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.AndroidViewModel
import com.example.lizardspockexpansion.ui.models.HandGestureUiState
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max

class HandGestureViewModel(application: Application) : AndroidViewModel(application) {
    val uiState = MutableStateFlow(HandGestureUiState())

    private val gestureRecognizer by lazy {
        val baseOptionsBuilder = BaseOptions.builder().setModelAssetPath("game.task")
        val baseOptions = baseOptionsBuilder.build()

        val optionsBuilder =
            GestureRecognizer.GestureRecognizerOptions.builder()
                .setBaseOptions(baseOptions)
                .setResultListener { result, _ -> handleGestureRecognizerResult(result) }
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(2)

        val options = optionsBuilder.build()
        GestureRecognizer.createFromOptions(getApplication(), options)
    }

    private fun handleGestureRecognizerResult(result: GestureRecognizerResult) {
        val detectedGestures =
            result.gestures().mapNotNull { it.maxByOrNull { category -> category.score() } }

        // Convert to list of gesture names
        val gestures = detectedGestures.map { it.categoryName() }

        // Update UI state to reflect both gestures
        if (gestures.isNotEmpty()) {
            uiState.update { it.copy(mostRecentGesture = gestures) }
        }
    }


    val imageAnalyzer = ImageAnalysis.Analyzer { image ->
        val imageBitmap = image.toBitmap()
        val scale = 500f / max(image.width, image.height)
        // Create a bitmap that's scaled as needed for the model, and rotated as needed to match display orientation
        val scaleAndRotate = Matrix().apply {
            postScale(scale, scale)
            postRotate(image.imageInfo.rotationDegrees.toFloat())
        }
        val scaledAndRotatedBmp =
            Bitmap.createBitmap(
                imageBitmap,
                0,
                0,
                image.width,
                image.height,
                scaleAndRotate,
                true
            )

        image.close()

        gestureRecognizer.recognizeAsync(
            BitmapImageBuilder(scaledAndRotatedBmp).build(),
            System.currentTimeMillis()
        )
    }
}