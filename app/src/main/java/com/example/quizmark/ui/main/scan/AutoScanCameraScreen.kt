package com.example.quizmark.ui.main.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.quizmark.R
import com.example.quizmark.ui.theme.DarkBlue
import com.example.quizmark.util.scanner.saveBitmapToGallery
import com.example.quizmark.util.scanner.ScannerCore

@Composable
fun AutoScanCameraScreen(
    examId: String,
    rosterId: String?,
    templateId: Int,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Yêu cầu quyền Camera
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
        CameraPreviewContent(templateId = templateId, onNavigateBack = onNavigateBack)
    } else {
        // UI khi chưa có quyền
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
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
                    // Nút Quay lại
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text(text = stringResource(id = R.string.btn_back_text), color = Color.White)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Nút Mở Cài Đặt trực tiếp
                    Button(
                        onClick = {
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            ).apply {
                                data = android.net.Uri.fromParts("package", context.packageName, null)
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
fun CameraPreviewContent(templateId: Int,onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Khởi tạo bộ quét và luồng chạy ngầm để không làm lag giao diện (UI)
    val scannerCore = remember { ScannerCore() }
    val analyzerExecutor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }

    // Cờ trạng thái để tránh việc quét liên tục 1 tờ giấy nhiều lần
    var isScanning by remember { mutableStateOf(true) }

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // 1. LỚP ĐÁY: CAMERA PREVIEW & ANALYSIS
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    // Cấu hình luồng xem trước (Preview)
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    // Cấu hình luồng phân tích hình ảnh (ImageAnalysis)
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setTargetResolution(android.util.Size(720, 1280))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                                if (isScanning) {
                                    // 1. Lấy góc xoay và Bitmap gốc
                                    val rotation = imageProxy.imageInfo.rotationDegrees
                                    val rawBitmap = imageProxy.toBitmap()

                                    // 2. Xoay ảnh cho đúng chiều đứng (Portrait)
                                    val rotatedBitmap = if (rotation != 0) {
                                        val matrix = android.graphics.Matrix()
                                        matrix.postRotate(rotation.toFloat())
                                        android.graphics.Bitmap.createBitmap(
                                            rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
                                        )
                                    } else {
                                        rawBitmap
                                    }

                                    // 3. Đưa ảnh đã xoay chuẩn vào ScannerCore xử lý
                                    val resultBitmap = scannerCore.processFrame(rotatedBitmap)

                                    if (resultBitmap != null) {
                                        isScanning = false

                                        // 1. GỌI BỘ NÃO ĐỌC ĐÁP ÁN TRƯỚC
                                        val processor = when (templateId) {
                                            1 -> com.example.quizmark.util.scanner.ExamThptProcessor()
                                            2 -> com.example.quizmark.util.scanner.Exam20Processor()
                                            3 -> com.example.quizmark.util.scanner.Exam40Processor()
                                            4 -> com.example.quizmark.util.scanner.Exam50Processor()
                                            5 -> com.example.quizmark.util.scanner.Exam120Processor()
                                            else -> com.example.quizmark.util.scanner.Exam20Processor() // Default nếu lỗi
                                        }
                                        val scanResult = processor.process(resultBitmap)

                                        // 2. LƯU ẢNH "ĐÃ CÓ VÒNG TRÒN MÀU" VÀO THƯ VIỆN
                                        scanResult.processedBitmap?.let { annotatedBitmap ->
                                            saveBitmapToGallery(context, annotatedBitmap)
                                        }

                                        // 3. In toàn bộ kết quả ra Logcat
                                        android.util.Log.d("OMR_RESULT", "========== KẾT QUẢ QUÉT ==========")
                                        android.util.Log.d("OMR_RESULT", "Số Báo Danh: ${scanResult.sbd}")
                                        android.util.Log.d("OMR_RESULT", "Mã Đề: ${scanResult.examCode}")

                                        if (templateId == 1) {
                                            // LOG DÀNH RIÊNG CHO PHIẾU THPT 2025
                                            android.util.Log.d("OMR_RESULT", "--- PHẦN I (Trắc nghiệm 4 lựa chọn) ---")
                                            scanResult.answers.filter { it.questionNumber in 1..40 }.forEach {
                                                android.util.Log.d("OMR_RESULT", "Câu ${it.questionNumber}: ${it.selectedAnswer ?: if (it.isError) "Lỗi tô đúp" else "Trống"}")
                                            }

                                            android.util.Log.d("OMR_RESULT", "--- PHẦN II (Đúng/Sai) ---")
                                            scanResult.answers.filter { it.questionNumber in 41..72 }.forEach {
                                                val offset = it.questionNumber - 41 // 0 đến 31
                                                val cau = (offset / 4) + 1 // Câu 1 đến 8
                                                val y = when (offset % 4) { 0 -> "a"; 1 -> "b"; 2 -> "c"; else -> "d" }
                                                android.util.Log.d("OMR_RESULT", "Câu $cau ý $y: ${it.selectedAnswer ?: if (it.isError) "Lỗi tô đúp" else "Trống"}")
                                            }

                                            android.util.Log.d("OMR_RESULT", "--- PHẦN III (Điền số) ---")
                                            scanResult.answers.filter { it.questionNumber in 73..78 }.forEach {
                                                val cau = it.questionNumber - 72
                                                android.util.Log.d("OMR_RESULT", "Câu $cau: ${it.selectedAnswer ?: if (it.isError) "Lỗi tô đúp/Sai quy tắc" else "Trống"}")
                                            }
                                        } else {
                                            // LOG DÀNH CHO CÁC PHIẾU CŨ (20, 40, 50, 120 CÂU)
                                            val answersStr = scanResult.answers.joinToString(separator = "\n") {
                                                "Câu ${it.questionNumber}: ${it.selectedAnswer ?: if (it.isError) "Lỗi tô đúp/Bỏ trống" else "Bỏ trống"}"
                                            }
                                            android.util.Log.d("OMR_RESULT", "Đáp án chi tiết:\n$answersStr")
                                        }
                                        android.util.Log.d("OMR_RESULT", "==================================")
                                        // Phát tiếng Bíp
                                        try {
                                            val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
                                            toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                                imageProxy.close()
                            }
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        // Ràng buộc cả preview và imageAnalyzer vào vòng đời Camera
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalyzer
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

        // 2. LỚP TOP: TOOLBAR QUÉT PHIẾU
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x99000000))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }

            Text(
                text = stringResource(id = R.string.camera_scan_title),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Nút bật/tắt Flash
            IconButton(onClick = { isFlashOn = !isFlashOn }) {
                Icon(
                    imageVector = if (isFlashOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                    contentDescription = "Flash",
                    tint = if (isFlashOn) Color(0xFFFFD700) else Color.White
                )
            }
        }

        // LỜI NHẮC ARUCO LƠ LỬNG
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 90.dp)
                .background(
                    color = Color(0x99000000),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = context.getString(R.string.camera_aruco_instruction),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // 3. LỚP BOTTOM: HƯỚNG DẪN & NÚT ĐÓNG
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xCC000000))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF04DC04), CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(id = R.string.camera_instruction_main),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(id = R.string.camera_instruction_sub),
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }

            Button(
                onClick = onNavigateBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.btn_close),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}