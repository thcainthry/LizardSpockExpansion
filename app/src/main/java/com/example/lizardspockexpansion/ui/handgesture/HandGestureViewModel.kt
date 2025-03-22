package com.example.lizardspockexpansion.ui.handgesture

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.AndroidViewModel
import com.example.lizardspockexpansion.ui.models.HandGestureUiState
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max

class HandGestureViewModel(application: Application) : AndroidViewModel(application) {
    val uiState = MutableStateFlow(HandGestureUiState())
    private val _imageWidth = MutableStateFlow<Int>(1)
    val imageWidth: StateFlow<Int> = _imageWidth
    private val _imageHeight = MutableStateFlow<Int>(1)
    val imageHeight: StateFlow<Int> = _imageHeight
    private val _isHandDetected = MutableStateFlow(false)
    val isHandDetected: StateFlow<Boolean> get() = _isHandDetected

    private val gestureRecognizer by lazy {
        val baseOptionsBuilder = BaseOptions.builder().setModelAssetPath("game.task")
        val baseOptions = baseOptionsBuilder.build()

        val optionsBuilder =
            GestureRecognizer.GestureRecognizerOptions.builder()
                .setBaseOptions(baseOptions)
                .setResultListener { result, image -> handleGestureRecognizerResult(result, image) }
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(2)

        val options = optionsBuilder.build()
        GestureRecognizer.createFromOptions(getApplication(), options)
    }

    private fun handleGestureRecognizerResult(result: GestureRecognizerResult, image: MPImage) {
        _imageWidth.value = image.width
        _imageHeight.value = image.height
        val detectedGestures =
            result.gestures().mapNotNull { it.maxByOrNull { category -> category.score() } }
        if (result.gestures().isEmpty()) {
            _isHandDetected.value = false  // No hand detected
        } else {
            _isHandDetected.value = true  // Hand detected
        }

        // Convert to list of gesture names
        val gestures = detectedGestures.map { it.categoryName() }
        result.let { gestureRecognizerResult ->
            val boundingBoxes = mutableListOf<Rect>()
            for (handIndex in gestureRecognizerResult.landmarks().indices) { // Iterate over all detected hands
                val handLandmarks =
                    gestureRecognizerResult.landmarks()[handIndex] // Get landmarks for each hand
                val boundingBox = getBoundingBox(handLandmarks)
                val scaledBoundingBox = Rect(
                    (boundingBox.minX * image.width).toInt(),
                    (boundingBox.minY * image.height).toInt(),
                    (boundingBox.maxX * image.width).toInt(),
                    (boundingBox.maxY * image.height).toInt()
                )

                // Add the scaled bounding box to the list
                boundingBoxes.add(scaledBoundingBox)
                // Update UI state to reflect both gestures
                if (gestures.isNotEmpty()) {
                    println("hello:$gestures")
                    uiState.update {
                        it.copy(
                            mostRecentGesture = gestures,
                            handBoundingBoxes = boundingBoxes,
                        )
                    }
                }
            }
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

    private fun getBoundingBox(
        handLandmarks: List<NormalizedLandmark>,
        margin: Float = 0.28f
    ): BoundingBox {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        // Find the original bounding box
        for (landmark in handLandmarks) {
            if (landmark.x() < minX) minX = landmark.x()
            if (landmark.y() < minY) minY = landmark.y()
            if (landmark.x() > maxX) maxX = landmark.x()
            if (landmark.y() > maxY) maxY = landmark.y()
        }

        // Calculate the mirrored coordinates
        val mirroredMinX = 1f - maxX
        val mirroredMaxX = 1f - minX

        // Calculate width and height of the bounding box
        val width = mirroredMaxX - mirroredMinX
        val height = maxY - minY

        // Apply the margin: double for the X-axis
        val expandedMinX = mirroredMinX - (2 * margin * width)  // Double the margin on the X-axis
        val expandedMaxX = mirroredMaxX + (2 * margin * width)  // Double the margin on the X-axis
        val expandedMinY = minY - margin * height  // Regular margin on the Y-axis
        val expandedMaxY = maxY + margin * height  // Regular margin on the Y-axis

        return BoundingBox(expandedMinX, expandedMinY, expandedMaxX, expandedMaxY)
    }


    data class BoundingBox(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float)


}