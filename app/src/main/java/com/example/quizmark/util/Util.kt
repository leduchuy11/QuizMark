package com.example.quizmark.util

import java.text.Normalizer
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

// Hàm mở rộng giúp xóa dấu tiếng Việt
fun String.unAccent(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    val regex = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    return regex.replace(temp, "")
        .replace("đ", "d")
        .replace("Đ", "D")
}

// Hàm dùng POI tạo ra file Excel mẫu ngay trong Cache
fun generateSampleExcelFile(context: Context): File? {
    return try {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Mau_Nhap_Hoc_Sinh")

        val centerStyle = workbook.createCellStyle().apply {
            alignment = org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER
            verticalAlignment = org.apache.poi.ss.usermodel.VerticalAlignment.CENTER
        }

        val headerStyle = workbook.createCellStyle().apply {
            alignment = org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER
            verticalAlignment = org.apache.poi.ss.usermodel.VerticalAlignment.CENTER
            val font = workbook.createFont().apply {
                bold = true // In đậm
            }
            setFont(font)
        }

        sheet.setColumnWidth(0, 15 * 256)
        sheet.setColumnWidth(1, 30 * 256)
        sheet.setColumnWidth(2, 20 * 256)
        sheet.setColumnWidth(3, 15 * 256)

        val headerRow = sheet.createRow(0)
        val headers = listOf("SBD", "Họ và tên", "Ngày sinh", "Giới tính")
        headers.forEachIndexed { index, title ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
        }

        val sampleRow = sheet.createRow(1)
        val sampleData = listOf("12345", "Nguyễn Văn A", "15/08/2005", "Nam")
        sampleData.forEachIndexed { index, value ->
            val cell = sampleRow.createCell(index)
            cell.setCellValue(value)
            cell.cellStyle = centerStyle
        }

        val file = File(context.cacheDir, "File_Mau_Nhap_Hoc_Sinh.xlsx")
        val fos = FileOutputStream(file)
        workbook.write(fos)

        fos.close()
        workbook.close()
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Hàm Gọi Intent chia sẻ file
fun shareExcelFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Chia sẻ file mẫu qua..."))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}