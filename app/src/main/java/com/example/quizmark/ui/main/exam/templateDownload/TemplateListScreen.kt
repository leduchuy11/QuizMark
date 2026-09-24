package com.example.quizmark.ui.main.exam.templateDownload

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quizmark.R
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.PrimaryDarkBlue

// Data class đại diện cho một mẫu phiếu
data class OmrTemplate(
    val id: Int,
    val name: String,
    val questionCount: String,
    val optionsFormat: String,
    val paperSize: String,
    val imageRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDownload: (Int) -> Unit
) {
    // Khởi tạo danh sách 5 phiếu cố định
    val templates = listOf(
        OmrTemplate(1, "Phiếu THPT 2025 (bản chuẩn)", "54 câu", "A-D", "A4", R.drawable.form_2025),
        OmrTemplate(2, "Phiếu 20 câu (bản chuẩn)", "20 câu", "A-D", "A4", R.drawable.form_20),
        OmrTemplate(3, "Phiếu 40 câu (bản chuẩn)", "40 câu", "A-D", "A4", R.drawable.form_40),
        OmrTemplate(4, "Phiếu 50 câu (bản chuẩn)", "50 câu", "A-D", "A4", R.drawable.form_50),
        OmrTemplate(5, "Phiếu 120 câu (bản chuẩn)", "120 câu", "A-D", "A4", R.drawable.form_120)
    )

    Scaffold(
        containerColor = BackgroundScreen,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        spotColor = Color(0x1A000000),
                        ambientColor = Color.Transparent
                    )
                    .background(Color.White)
                    .padding(vertical = 6.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ColorText
                    )
                }

                Text(
                    text = stringResource(id = R.string.template_list_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorText,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(templates) { template ->
                TemplateItemCard(
                    template = template,
                    onClick = { onNavigateToDownload(template.id) }
                )
            }
        }
    }
}

@Composable
fun TemplateItemCard(
    template: OmrTemplate,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp), spotColor = Color(0x1A000000))
            .background(Color.White, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        // Phần hình ảnh
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color(0xFFF6CFCC)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = template.imageRes),
                contentDescription = template.name,
                modifier = Modifier
                    .fillMaxHeight(0.85f)
                    .shadow(2.dp),
                contentScale = ContentScale.Fit
            )
        }

        // Phần thông tin bên dưới
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = template.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ColorText,
                maxLines = 2, // Giới hạn 2 dòng để lưới không bị xô lệch
                modifier = Modifier.heightIn(min = 40.dp) // Ép chiều cao tối thiểu để các thẻ bằng nhau
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Hàng chứa các tag thông số (Số câu, Format, Khổ giấy)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TemplateTag(text = template.questionCount)
                TemplateTag(text = template.optionsFormat)
                TemplateTag(text = template.paperSize)
            }
        }
    }
}

// Component nhỏ để vẽ các cục Tag thông số (xám nhạt, chữ nhỏ)
@Composable
fun TemplateTag(text: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF64748B) // Màu xám xanh
        )
    }
}