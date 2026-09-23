package com.example.quizmark.ui.main.account

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.quizmark.R
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.ColorText2
import com.example.quizmark.ui.theme.PrimaryDarkBlue
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.text.style.TextAlign
import androidx.core.os.LocaleListCompat
import com.example.quizmark.ui.theme.BackgroundScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.quizmark.ui.auth.CustomToastUI
import com.example.quizmark.ui.auth.ToastType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    viewModel: AccountViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToEditProfile: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showLanguageSheet by remember { mutableStateOf(false) }
    val currentLocaleTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    var currentLanguage by remember { mutableStateOf(if (currentLocaleTag.startsWith("en")) "English" else "Tiếng Việt") }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    val userProfile by viewModel.userProfile.collectAsState()
    val scrollState = rememberScrollState()

    // Khai báo State cho Custom Toast
    var toastMessage by remember { mutableStateOf("") }
    var toastType by remember { mutableStateOf(ToastType.SUCCESS) }
    var showToast by remember { mutableStateOf(false) }

    LaunchedEffect(showToast) {
        if (showToast) {
            delay(3000)
            showToast = false
        }
    }

    // Bọc toàn bộ vào Box để căn lề Toast xuống dưới cùng
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundScreen)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Custom Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_avatar_app),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(4.dp, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "QuizMark",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ColorText
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 1. Thẻ Hồ sơ
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(20.dp),
                            spotColor = Color(0x33000000),
                            ambientColor = Color(0x1A000000)
                        )
                        .background(Color.White, RoundedCornerShape(20.dp))
                        .clickable { onNavigateToEditProfile() }
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .border(4.dp, Color(0xFFFFD700), CircleShape)
                            .background(Color(0xFFCCDCF5), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_user_avatar),
                            contentDescription = "User Avatar",
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        val displayName = if (userProfile.fullName.isNotBlank()) {
                            userProfile.fullName
                        } else {
                            viewModel.getUserEmail().substringBefore("@")
                        }

                        Text(
                            text = displayName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorText
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stringResource(id = R.string.account_edit_hint),
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = "Edit",
                        tint = Color(0xFF8A93A6),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 2. Cá nhân hoá
                SectionTitle(title = stringResource(id = R.string.section_personalization))
                SettingsGroup {
                    SettingItem(
                        icon = R.drawable.ic_language,
                        title = stringResource(id = R.string.setting_language),
                        trailingText = currentLanguage,
                        onClick = { showLanguageSheet = true }
                    )
                }

                // 3. Đồng bộ hoá
                SectionTitle(title = stringResource(id = R.string.section_sync))
                SettingsGroup {
                    SettingItem(
                        icon = R.drawable.ic_sync,
                        title = stringResource(id = R.string.setting_sync_app),
                        onClick = { /* TODO */ }
                    )
                }

                // 4. Về ứng dụng
                SectionTitle(title = stringResource(id = R.string.section_about))
                SettingsGroup {
                    SettingItem(
                        icon = R.drawable.ic_info,
                        title = stringResource(id = R.string.setting_version),
                        trailingText = "1.0.1",
                        showArrow = false,
                        onClick = {}
                    )
                    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                    SettingItem(
                        icon = R.drawable.ic_lock,
                        title = stringResource(id = R.string.setting_privacy),
                        onClick = { /* TODO */ }
                    )
                    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                    SettingItem(
                        icon = R.drawable.ic_support,
                        title = stringResource(id = R.string.setting_contact),
                        onClick = { /* TODO */ }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 5. Nút Đăng xuất
                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFFFF0F0),
                        contentColor = Color(0xFFE53935)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935))
                ) {
                    Text(text = stringResource(id = R.string.btn_logout), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 6. Nút Xóa tài khoản
                OutlinedButton(
                    onClick = { showDeleteAccountDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFFFF0F0),
                        contentColor = ColorText
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935))
                ) {
                    Text(text = stringResource(id = R.string.btn_delete_account), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        } // Kết thúc Column chính

        // Dialog Đăng xuất
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text(text = stringResource(id = R.string.dialog_logout_title), fontWeight = FontWeight.Bold, color = ColorText) },
                text = { Text(text = stringResource(id = R.string.dialog_logout_desc), color = ColorText) },
                confirmButton = {
                    TextButton(onClick = {
                        showLogoutDialog = false
                        viewModel.logout(onSuccess = {
                            toastMessage = context.getString(R.string.logout_success)
                            toastType = ToastType.SUCCESS
                            showToast = true
                            coroutineScope.launch {
                                delay(1000)
                                onNavigateToLogin()
                            }
                        })
                    }) {
                        Text(text = stringResource(id = R.string.dialog_confirm), color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text(text = stringResource(id = R.string.dialog_cancel), color = Color.Gray)
                    }
                },
                containerColor = Color.White
            )
        }

        // Dialog Xóa tài khoản
        if (showDeleteAccountDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAccountDialog = false },
                title = { Text(text = stringResource(id = R.string.dialog_delete_title), fontWeight = FontWeight.Bold, color = ColorText) },
                text = { Text(text = stringResource(id = R.string.dialog_delete_desc), color = ColorText) },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteAccountDialog = false
                        viewModel.deleteAccount(
                            onSuccess = {
                                toastMessage = context.getString(R.string.delete_account_success)
                                toastType = ToastType.SUCCESS
                                showToast = true
                                coroutineScope.launch {
                                    delay(1000)
                                    onNavigateToLogin()
                                }
                            },
                            onError = { err ->
                                toastMessage = err
                                toastType = ToastType.ERROR
                                showToast = true
                            }
                        )
                    }) {
                        Text(text = stringResource(id = R.string.dialog_confirm), color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAccountDialog = false }) {
                        Text(text = stringResource(id = R.string.dialog_cancel), color = Color.Gray)
                    }
                },
                containerColor = Color.White
            )
        }

        // Bottom Sheet chọn ngôn ngữ
        if (showLanguageSheet) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { showLanguageSheet = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    Text(
                        text = stringResource(id = R.string.choose_language),
                        fontSize = 18.sp,
                        color = ColorText,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )

                    LanguageOption(title = stringResource(id = R.string.lang_vi), isSelected = currentLanguage == "Tiếng Việt") {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("vi"))
                        currentLanguage = "Tiếng Việt"
                        showLanguageSheet = false
                    }

                    LanguageOption(title = stringResource(id = R.string.lang_en), isSelected = currentLanguage == "English") {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                        currentLanguage = "English"
                        showLanguageSheet = false
                    }
                }
            }
        }

        // Giao diện Toast trượt lên
        AnimatedVisibility(
            visible = showToast,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            CustomToastUI(message = toastMessage, type = toastType)
        }
    }
}

// --- CÁC COMPONENT TÁI SỬ DỤNG CHO GIAO DIỆN ---

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = ColorText2,
        modifier = Modifier.padding(top = 28.dp, bottom = 12.dp, start = 8.dp)
    )
}

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x26000000),
                ambientColor = Color(0x14000000)
            )
            .background(Color.White, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
        content = content
    )
}

@Composable
fun SettingItem(
    @DrawableRes icon: Int,
    title: String,
    trailingText: String? = null,
    showArrow: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = PrimaryDarkBlue,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            fontSize = 15.sp,
            color = ColorText,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Medium
        )

        if (trailingText != null) {
            Text(text = trailingText, fontSize = 15.sp, color = Color(0xFF9CA3AF))
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (showArrow) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = Color(0xFF999999),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun LanguageOption(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = if (isSelected) PrimaryDarkBlue else Color.Black,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        if (isSelected) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check),
                contentDescription = null,
                tint = PrimaryDarkBlue,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}