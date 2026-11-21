package com.example.quizmark.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.quizmark.R
import com.example.quizmark.data.model.HistoryRecord
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class UploadedDriveRecord(
    val record: HistoryRecord,
    val driveFileId: String,
    val driveFolderId: String,
    val driveWebViewLink: String
)

data class DriveBatchUploadSummary(
    val folderId: String,
    val folderName: String,
    val folderWebLink: String,
    val uploadedCount: Int,
    val skippedCount: Int,
    val uploadedRecords: List<UploadedDriveRecord>
)


// hỗ trợ đẩy ảnh bài chấm lên Google Drive
object GoogleDriveHelper {

    private const val ROOT_FOLDER_NAME = "QuizMark_BaiCham"
    private const val MIME_TYPE_FOLDER = "application/vnd.google-apps.folder"
    private const val MIME_TYPE_JPEG = "image/jpeg"
    private const val MIME_TYPE_ZIP = "application/zip"
    private const val GOOGLE_DRIVE_PACKAGE = "com.google.android.apps.docs"

    // nén bài và tệp csv vào zip và đẩy lên drive
    suspend fun exportZipToGoogleDriveScreen(
        context: Context,
        groupTitle: String,
        records: List<HistoryRecord>
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val exportDir = File(context.cacheDir, "exports").apply {
                if (!exists()) mkdirs()
            }

            // tranfer to valid digit
            val cleanTitle = sanitizeAsciiFileName(groupTitle.ifBlank { "BaiCham_QuizMark" })
            val dateStr = SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault()).format(Date())
            val zipFile = File(exportDir, "BaiCham_${cleanTitle}_$dateStr.zip")

            val usedEntryNames = mutableSetOf<String>()
            var addedImageCount = 0

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                // Tạo bảng điểm CSV tổng hợp ngay trong tệp zip
                val csvBuilder = StringBuilder("\uFEFFSBD,Họ và Tên,Lớp,Mã đề,Điểm,Số câu đúng,Số câu sai,Bỏ trống\n")
                records.forEach { rec ->
                    val sbd = rec.studentCode.ifBlank { "Trống" }
                    val name = rec.studentName.ifBlank { "Ẩn danh" }.replace(",", " ")
                    val cls = rec.className.ifBlank { "-" }.replace(",", " ")
                    val code = rec.examCode.ifBlank { "-" }
                    val score = String.format(Locale.US, "%.2f", rec.score)
                    csvBuilder.append("$sbd,$name,$cls,$code,$score,${rec.correctCount},${rec.wrongCount},${rec.blankCount}\n")
                }
                val csvEntry = ZipEntry("BangDiem_$cleanTitle.csv")
                zos.putNextEntry(csvEntry)
                zos.write(csvBuilder.toString().toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // Đưa từng ảnh bài đã chấm theo lớp vào tệp .zip
                val distinctClasses = records.map { it.className.trim() }.filter { it.isNotEmpty() }.distinct()
                val useClassSubfolder = distinctClasses.size > 1

                records.forEachIndexed { idx, rec ->
                    val imgPath = rec.localImagePath.ifBlank { rec.localOriginalImagePath }
                    val srcFile = File(imgPath)
                    if (!srcFile.exists()) return@forEachIndexed

                    val baseName = buildDriveImageFileName(rec)
                    val subDir = if (useClassSubfolder && rec.className.isNotBlank()) {
                        "${sanitizeAsciiFileName(rec.className)}/"
                    } else {
                        ""
                    }

                    var entryName = "$subDir$baseName"
                    if (entryName in usedEntryNames) {
                        val withoutExt = baseName.removeSuffix(".jpg")
                        entryName = "$subDir${withoutExt}_${idx + 1}.jpg"
                    }
                    usedEntryNames.add(entryName)

                    zos.putNextEntry(ZipEntry(entryName))
                    FileInputStream(srcFile).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                    addedImageCount++
                }
            }

            if (addedImageCount == 0 && records.isEmpty()) {
                throw IllegalStateException("Không có bài chấm nào để đẩy lên Google Drive")
            }

            withContext(Dispatchers.Main) {
                launchGoogleDriveSingleFileScreen(
                    context = context,
                    file = zipFile,
                    mimeType = MIME_TYPE_ZIP,
                    subjectTitle = "Bài chấm $groupTitle"
                )
            }

            zipFile
        }
    }

    // export image jpg
    suspend fun exportImagesToGoogleDriveScreen(
        context: Context,
        groupTitle: String,
        records: List<HistoryRecord>
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val exportDir = File(context.cacheDir, "exports").apply {
                if (!exists()) mkdirs()
            }

            val authority = "${context.packageName}.provider"
            val uris = ArrayList<Uri>()
            val usedNames = mutableSetOf<String>()

            records.forEachIndexed { idx, rec ->
                val imgPath = rec.localImagePath.ifBlank { rec.localOriginalImagePath }
                val srcFile = File(imgPath)
                if (!srcFile.exists()) return@forEachIndexed

                var fileName = buildDriveImageFileName(rec)
                if (fileName in usedNames) {
                    fileName = "${fileName.removeSuffix(".jpg")}_${idx + 1}.jpg"
                }
                usedNames.add(fileName)

                val destFile = File(exportDir, fileName)
                srcFile.copyTo(destFile, overwrite = true)
                uris.add(FileProvider.getUriForFile(context, authority, destFile))
            }

            if (uris.isEmpty()) {
                throw IllegalStateException("Không tìm thấy tệp ảnh bài chấm trên thiết bị")
            }

            withContext(Dispatchers.Main) {
                launchGoogleDriveMultipleFilesScreen(
                    context = context,
                    uris = uris,
                    mimeType = MIME_TYPE_JPEG,
                    subjectTitle = "Ảnh bài chấm $groupTitle"
                )
            }

            uris.size
        }
    }

    //Mở trực tiếp màn hình "Lưu vào Drive" của ứng dụng Google Drive cho 1 tệp (.zip hoặc .jpg)
    //Nếu máy chưa cài app Google Drive thì mở bảng chọn ứng dụng chia sẻ
    private fun launchGoogleDriveSingleFileScreen(
        context: Context,
        file: File,
        mimeType: String,
        subjectTitle: String
    ) {
        val authority = "${context.packageName}.provider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val driveIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subjectTitle)
            putExtra(Intent.EXTRA_TITLE, file.name)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage(GOOGLE_DRIVE_PACKAGE)
        }

        try {
            context.startActivity(driveIntent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subjectTitle)
                putExtra(Intent.EXTRA_TITLE, file.name)
                clipData = ClipData.newRawUri(file.name, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(fallbackIntent, "Lưu tệp lên Google Drive...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    // mở lưu vào drive cho tệp nhiều ảnh
    private fun launchGoogleDriveMultipleFilesScreen(
        context: Context,
        uris: ArrayList<Uri>,
        mimeType: String,
        subjectTitle: String
    ) {
        if (uris.size == 1) {
            val singleUri = uris.first()
            val driveIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, singleUri)
                putExtra(Intent.EXTRA_SUBJECT, subjectTitle)
                clipData = ClipData.newRawUri(subjectTitle, singleUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage(GOOGLE_DRIVE_PACKAGE)
            }
            try {
                context.startActivity(driveIntent)
                return
            } catch (_: Exception) {
            }
        }

        val multiDriveIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            putExtra(Intent.EXTRA_SUBJECT, subjectTitle)
            val clip = ClipData.newRawUri(subjectTitle, uris.first())
            for (i in 1 until uris.size) {
                clip.addItem(ClipData.Item(uris[i]))
            }
            clipData = clip
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage(GOOGLE_DRIVE_PACKAGE)
        }

        try {
            context.startActivity(multiDriveIntent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                putExtra(Intent.EXTRA_SUBJECT, subjectTitle)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(fallbackIntent, "Đẩy ảnh lên Google Drive...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    fun getDriveSignInIntent(context: Context): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    fun hasDrivePermission(context: Context): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return false
        return GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE))
    }

    fun buildDriveService(context: Context): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context)?.account ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE)
        ).apply {
            selectedAccount = account
        }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName(context.getString(R.string.app_name))
            .build()
    }

    private fun findOrCreateFolder(
        driveService: Drive,
        folderName: String,
        parentFolderId: String? = null
    ): String {
        val escapedName = folderName.replace("'", "\\'")
        val parentQuery = if (parentFolderId != null) {
            " and '$parentFolderId' in parents"
        } else {
            ""
        }
        val query = "mimeType = '$MIME_TYPE_FOLDER' and name = '$escapedName' and trashed = false$parentQuery"

        val existing = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .setPageSize(1)
            .execute()

        val foundId = existing.files?.firstOrNull()?.id
        if (!foundId.isNullOrBlank()) {
            return foundId
        }

        val metadata = com.google.api.services.drive.model.File().apply {
            name = folderName
            mimeType = MIME_TYPE_FOLDER
            if (parentFolderId != null) {
                parents = listOf(parentFolderId)
            }
        }

        val created = driveService.files().create(metadata)
            .setFields("id")
            .execute()

        return created.id
    }

    suspend fun uploadRecordsToDrive(
        context: Context,
        rosterName: String,
        records: List<HistoryRecord>,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<DriveBatchUploadSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val driveService = buildDriveService(context)
                ?: throw IllegalStateException("Chưa kết nối tài khoản Google Drive")

            val rootFolderId = findOrCreateFolder(driveService, ROOT_FOLDER_NAME)

            val cleanRosterFolderName = rosterName.ifBlank { "CaThi_QuizMark" }
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val targetFolderId = findOrCreateFolder(
                driveService = driveService,
                folderName = cleanRosterFolderName,
                parentFolderId = rootFolderId
            )

            val uploadedList = mutableListOf<UploadedDriveRecord>()
            var skipped = 0
            val total = records.size

            records.forEachIndexed { index, record ->
                onProgress(index + 1, total)

                val localFile = File(record.localImagePath)
                if (!localFile.exists()) {
                    skipped++
                    return@forEachIndexed
                }

                if (record.driveFileId.isNotBlank() && record.driveFolderId == targetFolderId) {
                    skipped++
                    return@forEachIndexed
                }

                val fileName = buildDriveImageFileName(record)

                val fileMetadata = com.google.api.services.drive.model.File().apply {
                    name = fileName
                    parents = listOf(targetFolderId)
                    description = "SBD: ${record.studentCode} - Học sinh: ${record.studentName} - Điểm: ${record.score}"
                }

                val mediaContent = FileContent(MIME_TYPE_JPEG, localFile)

                val uploadedFile = driveService.files()
                    .create(fileMetadata, mediaContent)
                    .setFields("id, name, webViewLink")
                    .execute()

                val fileId = uploadedFile.id.orEmpty()
                val webLink = uploadedFile.webViewLink
                    ?: "https://drive.google.com/file/d/$fileId/view"

                uploadedList.add(
                    UploadedDriveRecord(
                        record = record,
                        driveFileId = fileId,
                        driveFolderId = targetFolderId,
                        driveWebViewLink = webLink
                    )
                )
            }

            DriveBatchUploadSummary(
                folderId = targetFolderId,
                folderName = "$ROOT_FOLDER_NAME/$cleanRosterFolderName",
                folderWebLink = "https://drive.google.com/drive/folders/$targetFolderId",
                uploadedCount = uploadedList.size,
                skippedCount = skipped,
                uploadedRecords = uploadedList
            )
        }
    }

    // dạt tên jpg
    fun buildDriveImageFileName(record: HistoryRecord): String {
        val sbdPart = record.studentCode.trim().ifBlank { "Trong" }
        val namePart = sanitizeAsciiFileName(record.studentName.ifBlank { "AnDanh" })
        val codePart = record.examCode.trim().ifBlank { "000" }
        val scorePart = String.format(Locale.US, "%.1f", record.score)
        return "SBD_${sbdPart}_${namePart}_MaDe_${codePart}_${scorePart}d.jpg"
    }

    fun sanitizeAsciiFileName(input: String): String {
        val normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace('đ', 'd')
            .replace('Đ', 'D')
        return normalized
            .replace(Regex("[^a-zA-Z0-9]+"), "_")
            .trim('_')
            .ifBlank { "HocSinh" }
    }
}
