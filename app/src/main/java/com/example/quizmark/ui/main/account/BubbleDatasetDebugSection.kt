package com.example.quizmark.ui.main.account

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.quizmark.R
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.PrimaryDarkBlue
import com.example.quizmark.util.scanner.BubbleDatasetDumper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Mục CHỈ HIỆN Ở BẢN DEBUG trong màn Tài khoản (công cụ cho nhà phát triển, nên chữ viết thẳng, không qua strings.xml):
 * bật/tắt thu dữ liệu ô tô, xuất zip lên Google Drive, xoá dữ liệu.
 * Dữ liệu dùng để train model phân loại ô trong repo quizmark-ml.
 *
 * @param onMessage hiện thông báo: (nội dung, có phải lỗi không)
 */
@Composable
fun BubbleDatasetDebugSection(onMessage: (String, Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var enabled by remember { mutableStateOf(BubbleDatasetDumper.isEnabled()) }
    var sampleCount by remember { mutableStateOf<Int?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    fun refreshCount() {
        scope.launch {
            sampleCount = withContext(Dispatchers.IO) { BubbleDatasetDumper.countSamples() }
        }
    }

    // Đếm lại mỗi khi quay về màn Tài khoản (sau khi đi quét phiếu)
    LaunchedEffect(Unit) { refreshCount() }

    SectionTitle(title = "Công cụ dữ liệu (chỉ bản debug)")
    SettingsGroup {
        // Công tắc thu dữ liệu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_scanner),
                contentDescription = null,
                tint = PrimaryDarkBlue,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Thu dữ liệu ô tô",
                    fontSize = 15.sp,
                    color = ColorText,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = sampleCount?.let { "Đã thu: $it ô" } ?: "Đang đếm…",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    BubbleDatasetDumper.setEnabled(it)
                },
                colors = SwitchDefaults.colors(checkedTrackColor = PrimaryDarkBlue)
            )
        }

        HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)

        // Xuất zip -> hộp thoại chia sẻ -> chọn Google Drive
        SettingItem(
            icon = R.drawable.ic_sync,
            title = "Xuất dữ liệu (zip) lên Drive",
            trailingText = if (isExporting) "Đang nén…" else null,
            onClick = {
                if (isExporting) return@SettingItem
                isExporting = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { BubbleDatasetDumper.exportZip() } }
                    isExporting = false
                    result.onSuccess { zipFile ->
                        if (zipFile == null) {
                            onMessage("Chưa có dữ liệu để xuất", true)
                            return@onSuccess
                        }
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", zipFile)
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/zip"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Lưu dữ liệu ô tô (chọn Google Drive)"))
                    }.onFailure {
                        onMessage("Xuất dữ liệu lỗi: ${it.message}", true)
                    }
                }
            }
        )

        HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)

        SettingItem(
            icon = R.drawable.ic_delete,
            title = "Xoá dữ liệu đã thu",
            showArrow = false,
            onClick = { showClearDialog = true }
        )
    }

    Text(
        text = "Cách dùng: bật công tắc → quét phiếu mẫu 1–5 như bình thường → \"Xuất dữ liệu\" → " +
                "chọn Google Drive, lưu vào thư mục quizmark-ml/data.",
        fontSize = 12.sp,
        color = Color.Gray,
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp)
    )

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(text = "Xoá dữ liệu đã thu?", fontWeight = FontWeight.Bold, color = ColorText) },
            text = {
                Text(
                    text = "Xoá ${sampleCount ?: 0} ảnh ô trên máy. Hãy chắc chắn bạn đã xuất và lưu lên Drive trước.",
                    color = ColorText
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    scope.launch {
                        withContext(Dispatchers.IO) { BubbleDatasetDumper.clear() }
                        refreshCount()
                        onMessage("Đã xoá dữ liệu", false)
                    }
                }) {
                    Text(text = "Xoá", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(text = "Huỷ", color = Color.Gray)
                }
            },
            containerColor = Color.White
        )
    }
}
