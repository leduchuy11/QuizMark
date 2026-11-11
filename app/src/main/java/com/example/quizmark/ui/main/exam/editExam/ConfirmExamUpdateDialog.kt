package com.example.quizmark.ui.main.exam.editExam

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.quizmark.R

@Composable
fun ConfirmExamUpdateDialog(summary: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_update_exam_title)) },
        text = { Text(summary) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.btn_save_changes)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}
