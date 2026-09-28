package com.example.quizmark.ui.main.examList.student

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.quizmark.R
import com.example.quizmark.ui.main.examList.RosterViewModel
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue
import com.example.quizmark.util.generateSampleExcelFile
import com.example.quizmark.util.shareExcelFile
import kotlinx.coroutines.launch

@Composable
fun ImportStudentScreen(
    onNavigateBack: () -> Unit,
    onFileSelected: (Uri, String?) -> Unit,
    viewModel: RosterViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val rosterList by viewModel.rosterList.collectAsState()

    val optionNone = stringResource(id = R.string.option_none)
    var selectedRosterName by remember { mutableStateOf(optionNone) }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()


    // Trình khởi chạy bộ chọn file (Chỉ lọc file Excel)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val selectedRosterId = rosterList.find { it.name == selectedRosterName }?.id

            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)

                    val tempFile = java.io.File(context.cacheDir, "temp_import.xlsx")
                    val outputStream = java.io.FileOutputStream(tempFile)

                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()

                    val localUri = Uri.fromFile(tempFile)

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onFileSelected(localUri, selectedRosterId)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Scaffold(
        containerColor = BackgroundScreen,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 4.dp, spotColor = Color.LightGray, ambientColor = Color.Transparent)
                    .background(Color.White)
                    .padding(vertical = 6.dp)
            ) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ColorText)
                }

                Text(
                    text = stringResource(id = R.string.import_file_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorText,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 1. Ô chọn danh sách
            val rosterOptions = remember(rosterList) { listOf(optionNone) + rosterList.map { it.name } }
            ImportDropdownField(
                selectedOption = selectedRosterName,
                options = rosterOptions,
                onOptionSelected = { selectedRosterName = it }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 2. Khu vực Card nét đứt để chọn file
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .drawBehind {
                        val stroke = Stroke(
                            width = 4f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        )
                        drawRoundRect(
                            color = Color(0xFFEF827A),
                            style = stroke,
                            cornerRadius = CornerRadius(16.dp.toPx())
                        )
                    }
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        filePickerLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.ic_import),
                        contentDescription = null,
                        tint = Color(0xFFA7A7AD),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(id = R.string.choose_file_xlsx),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.drag_drop_hint),
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 3. Khối chú ý quy tắc
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.mandatory_columns),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorText
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val columns = listOf(
                        R.string.col_1, R.string.col_2, R.string.col_3, R.string.col_4
                    )
                    columns.forEach { col ->
                        Text(
                            text = "- ${stringResource(id = col)}",
                            fontSize = 14.sp,
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(id = R.string.note_format_file),
                        fontSize = 13.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = Color(0xFFDC2626)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Nút Tải file mẫu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    val sampleFile = generateSampleExcelFile(context)
                                    if (sampleFile != null) {
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            shareExcelFile(context, sampleFile)
                                        }
                                    }
                                }
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_import_file),
                            contentDescription = null,
                            tint = DarkBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.download_sample_file),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// Khối Dropdown
@Composable
fun ImportDropdownField(selectedOption: String, options: List<String>, onOptionSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    Box(
        modifier = Modifier.onGloballyPositioned { coordinates ->
            textFieldSize = coordinates.size.toSize()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                .background(Color.White, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painter = painterResource(R.drawable.ic_sum_list_exam), contentDescription = null, tint = DarkBlue, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = selectedOption,
                color = if (selectedOption == stringResource(id = R.string.option_none)) Color.Gray else ColorText,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )

            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(with(LocalDensity.current) { textFieldSize.width.toDp() })
                .background(Color.White)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option, color = ColorText) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}