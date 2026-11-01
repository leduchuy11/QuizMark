package com.example.quizmark.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.quizmark.data.model.HistoryRecord
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

object ExportHelper {
    fun exportToExcelAndShare(
        context: Context,
        records: List<HistoryRecord>,
        emailTo: String,
        rosterName: String
    ) {
        try {
            // 1. Tạo file Excel
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Bảng Điểm")

            // SET ĐỘ RỘNG CỘT (Đơn vị là 1/256 ký tự)
            sheet.setColumnWidth(0, 15 * 256) // Cột SBD: rộng ~15 ký tự
            sheet.setColumnWidth(1, 30 * 256) // Cột Tên: rộng ~30 ký tự
            sheet.setColumnWidth(2, 12 * 256) // Cột Điểm: rộng ~12 ký tự

            val headerRow = sheet.createRow(0)
            headerRow.createCell(0).setCellValue("SBD")
            headerRow.createCell(1).setCellValue("Họ và Tên")
            headerRow.createCell(2).setCellValue("Điểm")

            records.forEachIndexed { index, record ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(if (record.studentCode.isNotBlank()) record.studentCode else "Trống")
                row.createCell(1).setCellValue(if (record.studentName.isNotBlank()) record.studentName else "Ẩn danh")
                row.createCell(2).setCellValue(record.score)
            }

            // 2. Lưu vào Cache
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            // ĐẶT TÊN FILE
            val safeRosterName = rosterName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val fileName = "Bảng điểm $safeRosterName.xlsx"

            val excelFile = File(exportDir, fileName)
            FileOutputStream(excelFile).use { fos ->
                workbook.write(fos)
            }
            workbook.close()

            // 3. Tạo URI an toàn bằng FileProvider
            val authority = "${context.packageName}.provider"
            val uri = FileProvider.getUriForFile(context, authority, excelFile)

            // 4. Kích hoạt Bảng chia sẻ
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)

                if (emailTo.isNotBlank()) {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(emailTo))
                    putExtra(Intent.EXTRA_SUBJECT, "Bảng điểm xuất từ ứng dụng QuizMark")
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Chia sẻ bảng điểm qua...")
            context.startActivity(chooserIntent)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}