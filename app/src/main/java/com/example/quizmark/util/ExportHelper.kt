package com.example.quizmark.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.quizmark.data.model.HistoryRecord
import org.apache.poi.ss.usermodel.Sheet
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
            val summarySheet = workbook.createSheet("Bảng Điểm Tổng Hợp")
            writeScoreSheet(summarySheet, records)

            // Nếu trong danh sách điểm có nhiều lớp, tự động tạo thêm từng Sheet riêng cho mỗi lớp
            val byClass = records
                .filter { it.className.isNotBlank() }
                .groupBy { it.className.trim() }

            val usedSheetNames = mutableSetOf("Bảng Điểm Tổng Hợp")
            byClass.forEach { (className, classRecords) ->
                val rawSheetName = "Lớp $className"
                    .replace(Regex("[\\\\/:*?\"<>|\\[\\]]"), "_")
                    .take(28)
                var finalSheetName = rawSheetName
                var suffix = 2
                while (finalSheetName in usedSheetNames) {
                    finalSheetName = "${rawSheetName}_$suffix"
                    suffix++
                }
                usedSheetNames.add(finalSheetName)
                val classSheet = workbook.createSheet(finalSheetName)
                writeScoreSheet(classSheet, classRecords)
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

    private fun writeScoreSheet(sheet: Sheet, records: List<HistoryRecord>) {
        sheet.setColumnWidth(0, 15 * 256) // Cột SBD
        sheet.setColumnWidth(1, 28 * 256) // Cột Tên
        sheet.setColumnWidth(2, 16 * 256) // Cột Lớp
        sheet.setColumnWidth(3, 12 * 256) // Cột Mã đề
        sheet.setColumnWidth(4, 12 * 256) // Cột Điểm

        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("SBD")
        headerRow.createCell(1).setCellValue("Họ và Tên")
        headerRow.createCell(2).setCellValue("Lớp")
        headerRow.createCell(3).setCellValue("Mã đề")
        headerRow.createCell(4).setCellValue("Điểm")

        records.forEachIndexed { index, record ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(record.studentCode.ifBlank { "Trống" })
            row.createCell(1).setCellValue(record.studentName.ifBlank { "Ẩn danh" })
            row.createCell(2).setCellValue(record.className.ifBlank { "-" })
            row.createCell(3).setCellValue(record.examCode.ifBlank { "-" })
            row.createCell(4).setCellValue(record.score)
        }
    }
}
