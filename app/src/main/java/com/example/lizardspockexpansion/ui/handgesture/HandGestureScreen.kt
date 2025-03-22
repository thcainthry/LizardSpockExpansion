package com.example.lizardspockexpansion.ui.handgesture

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lizardspockexpansion.ui.components.CameraPreview

@SuppressLint("NewApi", "UseOfNonLambdaOffsetOverload")
@Composable
fun HandGestureScreen(
    modifier: Modifier = Modifier,
    viewModel: HandGestureViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val width by viewModel.imageWidth.collectAsState()
    val height by viewModel.imageHeight.collectAsState()
    val isHandDetected by viewModel.isHandDetected.collectAsState()

    val context = LocalContext.current

    val imageAnalysisUseCase = remember {
        ImageAnalysis.Builder().build().apply {
            setAnalyzer(context.mainExecutor, viewModel.imageAnalyzer)
        }
    }
    var canvasSize by remember { mutableStateOf(IntSize(0, 0)) }
    Box(modifier = modifier) {
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            imageAnalysisUseCase = imageAnalysisUseCase
        )
        if (isHandDetected) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { layoutCoordinates ->
                    canvasSize = layoutCoordinates.size
                }) {
                uiState.handBoundingBoxes.forEach { rect ->
                    drawRect(
                        color = Color(0xFF0DFF25),
                        topLeft = Offset(
                            (rect.left.toFloat() / width) * size.width,
                            (rect.top.toFloat() / height) * size.height
                        ),
                        size = Size(
                            (rect.width().toFloat() / width) * size.width,

                            (rect.height().toFloat() / height) * size.height
                        ),
                        style = Stroke(width = 10f)
                    )
                }
            }
            uiState.handBoundingBoxes.zip(uiState.mostRecentGesture.orEmpty())
                .forEach { (rect, gesture) ->
                    Box(
                        modifier = Modifier
                            .offset(
                                x = ((rect.left.toFloat() / canvasSize.width) * canvasSize.width).dp,
                                y = (((rect.top.toFloat() / canvasSize.height) * (canvasSize.height * 1.3))).dp
                            )
                            .background(
                                color = Color.Black.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Text(
                            text = gesture,
                            fontSize = 24.sp,
                            color = Color.White
                        )
                    }
                }
        }
    }
}