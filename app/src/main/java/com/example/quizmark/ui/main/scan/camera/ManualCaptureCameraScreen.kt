package com.example.quizmark.ui.main.scan.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.quizmark.R
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.DarkBlue

@Composable
fun ManualCaptureCameraScreen(
    examId: String,
    rosterId: String?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasCameraPermission = isGranted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        ManualCameraPreviewContent(onNavigateBack = onNavigateBack)
    } else {
        // UI báo lỗi quyền
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(id = R.string.error_camera_permission),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row {
                    Button(onClick = onNavigateBack, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                        Text(text = stringResource(id = R.string.btn_back_text), color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                    ) {
                        Text(text = "Mở Cài đặt", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


@Composable
fun ManualCameraPreviewContent(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }

    val imageCapture = remember { ImageCapture.Builder().build() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // 1. LỚP ĐÁY: CAMERA PREVIEW
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                        cameraControl = camera.cameraControl
                    } catch (exc: Exception) {
                        exc.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        LaunchedEffect(isFlashOn) {
            cameraControl?.enableTorch(isFlashOn)
        }

        // 2. KHUNG VIỀN ĐỎ Ở GIỮA MÀN HÌNH
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val rectWidth = canvasWidth * 0.85f
            val rectHeight = rectWidth * 1.3f

            val left = (canvasWidth - rectWidth) / 2f

            val centerY = canvasHeight / 2f
            val verticalOffset = -40.dp.toPx()

            val top = centerY - (rectHeight / 2f) + verticalOffset
            val right = left + rectWidth
            val bottom = top + rectHeight

            val cornerLength = 80f
            val strokeWidth = 8f
            val cornerColor = Color(0xFFFF4B4B)

            fun drawCorner(startX: Float, startY: Float, endX: Float, endY: Float) {
                drawLine(
                    color = cornerColor, start = Offset(startX, startY), end = Offset(endX, startY),
                    strokeWidth = strokeWidth, cap = StrokeCap.Round
                )
                drawLine(
                    color = cornerColor, start = Offset(startX, startY), end = Offset(startX, endY),
                    strokeWidth = strokeWidth, cap = StrokeCap.Round
                )
            }

            drawCorner(left, top, left + cornerLength, top + cornerLength)
            drawCorner(right, top, right - cornerLength, top + cornerLength)
            drawCorner(left, bottom, left + cornerLength, bottom - cornerLength)
            drawCorner(right, bottom, right - cornerLength, bottom - cornerLength)
        }

        // 3. LỚP TOP: TOOLBAR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x99000000))
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter)
        ) {

            Text(
                text = stringResource(id = R.string.camera_manual_instruction),
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.CenterStart)
                    .clip(CircleShape)
                    .background(Color.White, CircleShape)
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.Black
                )
            }
        }

        // 4. LỚP BOTTOM: PANEL NÚT BẤM
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(color = BackgroundScreen, shape = CurvedTopShape(30f))
                .padding(bottom = 28.dp, top = 36.dp, start = 24.dp, end = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .align(Alignment.Center)
                    .border(4.dp, Color(0xFF1D1D1E), CircleShape)
                    .padding(8.dp)
                    .background(Color(0xFF1D1D1E), CircleShape)
                    .clickable {
                        // TODO: Xử lý sự kiện chụp ảnh tại đây
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = "Capture",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            // Nút Đèn Pin
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterEnd)
                    .background(Color(0xFFF1F5F9), CircleShape)
                    .clickable { isFlashOn = !isFlashOn },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFlashOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                    contentDescription = "Flash",
                    tint = if (isFlashOn) Color(0xFFFFE200) else Color.Gray
                )
            }
        }
    }
}

class CurvedTopShape(private val curveHeightDp: Float = 30f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val curveHeight = with(density) { curveHeightDp.dp.toPx() }
        val path = Path().apply {
            moveTo(0f, curveHeight)
            quadraticBezierTo(
                size.width / 2f, -curveHeight,
                size.width, curveHeight
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}