package com.example.quizmark.ui.main.exam

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.example.quizmark.R
import com.example.quizmark.data.model.ScanResult
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue
import com.example.quizmark.ui.theme.PrimaryDarkBlue
import com.example.quizmark.util.AnswerKeyImportSummary
import com.example.quizmark.util.AnswerKeyScanner
import com.example.quizmark.util.StaticKeyScanResult
import com.example.quizmark.util.scanner.ExamProcessorFactory
import com.example.quizmark.util.scanner.ScannerCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private enum class ScanKeyMode {
    CHOOSER,
    CAMERA,
    GALLERY_PROCESSING
}

@Composable
fun ScanAnswerKeyChipButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFE6F0FD))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_scanner),
            contentDescription = null,
            tint = PrimaryDarkBlue,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(id = R.string.btn_scan_answer_key),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryDarkBlue
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerKeyScannerHost(
    templateId: Int,
    targetCode: String?,
    onDismiss: () -> Unit,
    onScanCompleted: (targetCode: String, scanResult: ScanResult) -> Unit,
    onError: (String) -> Unit
) {
    if (targetCode == null) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by remember(targetCode) { mutableStateOf(ScanKeyMode.CHOOSER) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) {
            onDismiss()
            return@rememberLauncherForActivityResult
        }

        mode = ScanKeyMode.GALLERY_PROCESSING
        scope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    val rawBitmap = decodeSoftwareArgbBitmap(context, uri)
                    try {
                        AnswerKeyScanner.scanStaticBitmap(rawBitmap, templateId)
                    } finally {
                        rawBitmap.recycle()
                    }
                }
            }

            outcome.onSuccess { scanOutcome ->
                when (scanOutcome) {
                    is StaticKeyScanResult.Success -> {
                        onScanCompleted(targetCode, scanOutcome.scanResult)
                        onDismiss()
                    }
                    StaticKeyScanResult.CornersNotFound -> {
                        onError(context.getString(R.string.scan_key_corners_not_found))
                        onDismiss()
                    }
                    StaticKeyScanResult.UnsupportedTemplate -> {
                        onError(context.getString(R.string.scan_template_unsupported))
                        onDismiss()
                    }
                }
            }.onFailure {
                onError(context.getString(R.string.scan_key_read_failed))
                onDismiss()
            }
        }
    }

    when (mode) {
        ScanKeyMode.CHOOSER -> {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.scan_key_sheet_title, targetCode),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(id = R.string.scan_key_sheet_subtitle),
                        fontSize = 14.sp,
                        color = Color.Gray,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { mode = ScanKeyMode.CAMERA },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_camera),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(id = R.string.btn_open_camera),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, PrimaryDarkBlue)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_img),
                            contentDescription = null,
                            tint = DarkBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(id = R.string.btn_open_gallery),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkBlue
                        )
                    }
                }
            }
        }

        ScanKeyMode.GALLERY_PROCESSING -> {
            BackHandler(enabled = true) { /* Chặn thoát khi đang xử lý ảnh */ }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(5f)
                    .background(Color(0x99000000))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = DarkBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(id = R.string.scan_key_processing),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorText
                        )
                    }
                }
            }
        }

        ScanKeyMode.CAMERA -> {
            AnswerKeyCameraOverlay(
                templateId = templateId,
                targetCode = targetCode,
                onDismiss = onDismiss,
                onScanned = { scanResult ->
                    onScanCompleted(targetCode, scanResult)
                    onDismiss()
                },
                onError = { message ->
                    onError(message)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun AnswerKeyCameraOverlay(
    templateId: Int,
    targetCode: String,
    onDismiss: () -> Unit,
    onScanned: (ScanResult) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    BackHandler { onDismiss() }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(5f)
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            AnswerKeyCameraPreview(
                templateId = templateId,
                targetCode = targetCode,
                onDismiss = onDismiss,
                onScanned = onScanned,
                onError = onError
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(id = R.string.error_camera_permission),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text(text = stringResource(id = R.string.btn_back_text), color = Color.White)
                    }
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                    ) {
                        Text(text = stringResource(id = R.string.btn_open_settings), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerKeyCameraPreview(
    templateId: Int,
    targetCode: String,
    onDismiss: () -> Unit,
    onScanned: (ScanResult) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val scannerCore = remember { ScannerCore() }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val isScanning = remember { AtomicBoolean(true) }
    val isDisposed = remember { AtomicBoolean(false) }

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var boundCameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            isDisposed.set(true)
            isScanning.set(false)
            try {
                cameraControl?.enableTorch(false)
                boundCameraProvider?.unbindAll()
            } catch (_: Exception) {
            }
            analyzerExecutor.shutdown()
        }
    }

    LaunchedEffect(isFlashOn) {
        cameraControl?.enableTorch(isFlashOn)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    if (isDisposed.get()) return@addListener

                    val cameraProvider = cameraProviderFuture.get()
                    boundCameraProvider = cameraProvider

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setTargetResolution(Size(720, 1280))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                                try {
                                    if (isScanning.get() && !isDisposed.get()) {
                                        val rotation = imageProxy.imageInfo.rotationDegrees
                                        val rawBitmap = imageProxy.toBitmap()
                                        val rotatedBitmap = if (rotation != 0) {
                                            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                                            Bitmap.createBitmap(
                                                rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
                                            ).also { rawBitmap.recycle() }
                                        } else {
                                            rawBitmap
                                        }

                                        val warpedBitmap = try {
                                            scannerCore.processFrame(rotatedBitmap)
                                        } finally {
                                            rotatedBitmap.recycle()
                                        }

                                        if (warpedBitmap != null && isScanning.compareAndSet(true, false)) {
                                            try {
                                                val processor = ExamProcessorFactory.create(templateId)
                                                if (processor == null) {
                                                    ContextCompat.getMainExecutor(ctx).execute {
                                                        if (!isDisposed.get()) {
                                                            onError(ctx.getString(R.string.scan_template_unsupported))
                                                        }
                                                    }
                                                } else {
                                                    val rawResult = processor.process(warpedBitmap)
                                                    rawResult.processedBitmap?.takeIf { it !== warpedBitmap }?.recycle()
                                                    val cleanResult = rawResult.copy(processedBitmap = null, originalBitmap = null)

                                                    try {
                                                        ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                                                            .startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                                                    } catch (_: Exception) {
                                                    }

                                                    ContextCompat.getMainExecutor(ctx).execute {
                                                        if (!isDisposed.get()) {
                                                            onScanned(cleanResult)
                                                        }
                                                    }
                                                }
                                            } finally {
                                                warpedBitmap.recycle()
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    imageProxy.close()
                                }
                            }
                        }

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
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

        // Top toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x99000000))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
            Text(
                text = stringResource(id = R.string.scan_key_camera_title, targetCode),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { isFlashOn = !isFlashOn }) {
                Icon(
                    imageVector = if (isFlashOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                    contentDescription = "Flash",
                    tint = if (isFlashOn) Color(0xFFFFD700) else Color.White
                )
            }
        }

        // Bottom instruction bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xCC000000))
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(10.dp).background(Color(0xFF04DC04), CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(id = R.string.scan_key_camera_instruction),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(id = R.string.scan_key_camera_sub),
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
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

@Composable
fun AnswerKeyScanSummaryDialog(
    summary: AnswerKeyImportSummary,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                text = stringResource(id = R.string.scan_key_summary_title, summary.targetCode),
                fontWeight = FontWeight.Bold,
                color = ColorText
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(
                        id = R.string.scan_key_summary_filled,
                        summary.filledCount,
                        summary.totalCount
                    ),
                    color = ColorText,
                    fontWeight = FontWeight.SemiBold
                )
                if (summary.hasCodeMismatch) {
                    Text(
                        text = stringResource(
                            id = R.string.scan_key_summary_code_mismatch,
                            summary.scannedExamCode.orEmpty(),
                            summary.targetCode
                        ),
                        color = Color(0xFFD97706),
                        fontSize = 14.sp
                    )
                }
                if (summary.blankLabels.isNotEmpty()) {
                    Text(
                        text = stringResource(
                            id = R.string.scan_key_summary_blank,
                            summary.blankLabels.size,
                            AnswerKeyScanner.formatLabelPreview(summary.blankLabels)
                        ),
                        color = Color(0xFFDC2626),
                        fontSize = 14.sp
                    )
                }
                if (summary.errorLabels.isNotEmpty()) {
                    Text(
                        text = stringResource(
                            id = R.string.scan_key_summary_error,
                            summary.errorLabels.size,
                            AnswerKeyScanner.formatLabelPreview(summary.errorLabels)
                        ),
                        color = Color(0xFFDC2626),
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = stringResource(id = R.string.scan_key_summary_check_hint),
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.btn_understood),
                    color = DarkBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

private fun decodeSoftwareArgbBitmap(context: Context, uri: Uri): Bitmap {
    val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = true
        }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }

    return if (decoded.config == Bitmap.Config.ARGB_8888 && decoded.isMutable) {
        decoded
    } else {
        decoded.copy(Bitmap.Config.ARGB_8888, true).also {
            if (it !== decoded) decoded.recycle()
        }
    }
}
