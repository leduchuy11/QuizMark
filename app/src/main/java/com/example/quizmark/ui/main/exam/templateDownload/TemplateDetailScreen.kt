package com.example.quizmark.ui.main.exam.templateDownload

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.quizmark.R
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue
import java.io.File
import java.io.FileOutputStream

// Data class cho chi tiết phiếu
data class TemplateDetail(
    val id: Int,
    val title: String,
    val imageRes: Int,
    val rawPdfRes: Int,
    val pdfFileName: String,
    val questionCount: String,
    val paperSize: String,
    val orientation: String,
    val type: String,
    val hasExamCode: String,
    val hasStudentCode: String,
    val dimensions: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDetailScreen(
    templateId: Int,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val template = getTemplateDetailById(templateId)

    Scaffold(
        containerColor = BackgroundScreen,
        // 1. TOOLBAR
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        spotColor = Color(0x1A000000),
                        ambientColor = Color.Transparent
                    )
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.btn_back_text),
                        tint = ColorText
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Tiêu đề phiếu
                Text(
                    text = template.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(16.dp))
            }
        },
        bottomBar = {
            // Nút Tải mẫu phiếu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundScreen)
                    .padding(20.dp)
            ) {
                Button(
                    onClick = {
                        sharePdfFromRaw(context, template.rawPdfRes, template.pdfFileName)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.btn_download_template),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 2. Hình ảnh phiếu to ở giữa
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color(0x1A000000))
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = template.imageRes),
                    contentDescription = template.title,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 3. Tiêu đề "Thông số"
            Text(
                text = stringResource(id = R.string.section_parameters),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ColorText,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Lưới thông số
            val params = listOf(
                Pair(stringResource(R.string.param_question_count), template.questionCount),
                Pair(stringResource(R.string.param_paper_size), template.paperSize),
                Pair(stringResource(R.string.param_orientation), template.orientation),
                Pair(stringResource(R.string.param_type), template.type),
                Pair(stringResource(R.string.param_exam_code), template.hasExamCode),
                Pair(stringResource(R.string.param_student_code), template.hasStudentCode),
                Pair(stringResource(R.string.param_dimensions), template.dimensions)
            )

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                params.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { item ->
                            ParameterCard(
                                modifier = Modifier.weight(1f),
                                label = item.first,
                                value = item.second
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ParameterCard(modifier: Modifier = Modifier, label: String, value: String) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = ColorText
        )
    }
}

// --- HÀM XỬ LÝ DỮ LIỆU & CHIA SẺ FILE ---

fun getTemplateDetailById(id: Int): TemplateDetail {
    return when(id) {
        1 -> TemplateDetail(1, "Phiếu THPT 2025 (bản chuẩn)", R.drawable.form_2025, R.raw.form_thpt_2025, "Phieu_THPT_2025.pdf", "54 câu", "A4", "Dọc", "THPT MỚI", "Có", "Có", "605x835")
        2 -> TemplateDetail(2, "Phiếu 20 câu (bản chuẩn)", R.drawable.form_20, R.raw.form_20_cau, "Phieu_20_cau.pdf", "20 câu", "A4", "Dọc", "CƠ BẢN", "Có", "Có", "605x835")
        3 -> TemplateDetail(3, "Phiếu 40 câu (bản chuẩn)", R.drawable.form_40, R.raw.form_40_cau, "Phieu_40_cau.pdf", "40 câu", "A4", "Dọc", "CƠ BẢN", "Có", "Có", "605x835")
        4 -> TemplateDetail(4, "Phiếu 50 câu (bản chuẩn)", R.drawable.form_50, R.raw.form_50_cau, "Phieu_50_cau.pdf", "50 câu", "A4", "Dọc", "CƠ BẢN", "Có", "Có", "605x835")
        5 -> TemplateDetail(5, "Phiếu 120 câu (bản chuẩn)", R.drawable.form_120, R.raw.form_120_cau, "Phieu_120_cau.pdf", "120 câu", "A4", "Dọc", "CƠ BẢN", "Có", "Có", "605x835")
        else -> TemplateDetail(1, "Phiếu THPT 2025 (bản chuẩn)", R.drawable.form_2025, R.raw.form_thpt_2025, "Phieu_THPT_2025.pdf", "54 câu", "A4", "Dọc", "THPT MỚI", "Có", "Có", "605x835")
    }
}

fun sharePdfFromRaw(context: Context, rawResId: Int, fileName: String) {
    try {
        val file = File(context.cacheDir, fileName)

        context.resources.openRawResource(rawResId).use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserTitle = context.getString(R.string.share_chooser_title)
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))

    } catch (e: Exception) {
        e.printStackTrace()
    }
}