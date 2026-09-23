package com.example.quizmark.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.quizmark.R

sealed class BottomNavItem(val route: String, @StringRes val titleResId: Int, @DrawableRes val iconResId: Int) {
    object Home : BottomNavItem("home_tab", R.string.tab_home, R.drawable.ic_home)
    object Exam : BottomNavItem("exam_tab", R.string.tab_exam, R.drawable.ic_document)
    object Scan : BottomNavItem("scan_tab", R.string.tab_scan, R.drawable.ic_scan)
    object ExamList : BottomNavItem("exam_list_tab", R.string.tab_exam_list, R.drawable.ic_list_person)
    object Account : BottomNavItem("account_tab", R.string.tab_account, R.drawable.ic_profile)
}