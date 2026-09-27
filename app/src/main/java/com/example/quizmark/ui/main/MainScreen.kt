package com.example.quizmark.ui.main

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.quizmark.ui.main.account.AccountScreen
import com.example.quizmark.ui.main.account.AccountViewModel
import com.example.quizmark.ui.main.account.ProfileEditScreen
import com.example.quizmark.ui.main.exam.ExamScreen
import com.example.quizmark.ui.main.exam.addExam.CreateExamScreen
import com.example.quizmark.ui.main.exam.addExam.InputThptAnswersScreen
import com.example.quizmark.ui.main.exam.addExam.SetupAiExamScreen
import com.example.quizmark.ui.main.exam.addExam.SetupBasicExamScreen
import com.example.quizmark.ui.main.exam.addExam.SetupThptExamScreen
import com.example.quizmark.ui.main.exam.allExam.AllExamsScreen
import com.example.quizmark.ui.main.exam.editExam.EditAiAnswersScreen
import com.example.quizmark.ui.main.exam.editExam.EditBasicAnswersScreen
import com.example.quizmark.ui.main.exam.editExam.EditExamScreen
import com.example.quizmark.ui.main.exam.editExam.EditThptAnswersScreen
import com.example.quizmark.ui.main.exam.editExam.EditThptExamScreen
import com.example.quizmark.ui.main.exam.templateDownload.TemplateDetailScreen
import com.example.quizmark.ui.main.exam.templateDownload.TemplateListScreen
import com.example.quizmark.ui.main.examList.roster.AddRosterScreen
import com.example.quizmark.ui.main.examList.ExamListScreen
import com.example.quizmark.ui.main.examList.roster.EditRosterScreen
import com.example.quizmark.ui.main.examList.roster.RosterDetailScreen
import com.example.quizmark.ui.main.examList.student.AddStudentScreen
import com.example.quizmark.ui.main.examList.student.EditStudentScreen

import com.example.quizmark.ui.theme.DarkBlue
import com.example.quizmark.ui.theme.PrimaryDarkBlue

@Composable
fun MainScreen(onNavigateToLogin: () -> Unit,
               sharedAccountViewModel: AccountViewModel = hiltViewModel()) {
    val bottomNavController = rememberNavController()

    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        BottomNavItem.Home.route,
        BottomNavItem.Exam.route,
        BottomNavItem.Scan.route,
        BottomNavItem.ExamList.route,
        BottomNavItem.Account.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                CustomBottomNavigationBar(bottomNavController)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            val activity = LocalContext.current as? Activity

            NavHost(
                navController = bottomNavController,
                startDestination = BottomNavItem.Scan.route
            ) {
                composable(BottomNavItem.Home.route) {
                    BackHandler { activity?.finish() }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        Alignment.Center
                    ) { Text("Màn hình Trang chủ") }
                }
                composable(BottomNavItem.Exam.route) {
                    BackHandler { activity?.finish() }
                    ExamScreen(
                        onNavigateToTemplates = { bottomNavController.navigate("template_list") },
                        onNavigateToCreateExam = { bottomNavController.navigate("create_exam") },
                        onNavigateToAllExams = { bottomNavController.navigate("all_exams") },
                        onNavigateToEditExam = { exam ->
                            val codesStr = exam.codes.joinToString(",")
                            val customQ = if (exam.templateId == 6) exam.questionCount else 0
                            bottomNavController.navigate("edit_exam/${exam.id}/${exam.name}/${exam.templateId}/$codesStr/$customQ")
                        }
                    )
                }
                composable(BottomNavItem.Scan.route) {
                    BackHandler { activity?.finish() }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        Alignment.Center
                    ) { Text("Màn hình Quét") }
                }
                composable(BottomNavItem.ExamList.route) {
                    BackHandler { activity?.finish() }
                    ExamListScreen(
                        onNavigateToAddRoster = { bottomNavController.navigate("add_roster") },
                        onNavigateToAddStudent = { bottomNavController.navigate("add_student") },
                        onNavigateToRosterDetail = { rosterId ->
                            bottomNavController.navigate("roster_detail/$rosterId")
                        },
                        onNavigateToEditStudent = { studentId -> bottomNavController.navigate("edit_student/$studentId") }
                    )
                }
                composable(BottomNavItem.Account.route) {
                    BackHandler { activity?.finish() }
                    AccountScreen(
                        viewModel = sharedAccountViewModel,
                        onNavigateToLogin = onNavigateToLogin,
                        onNavigateToEditProfile = {
                            bottomNavController.navigate("profile_edit")
                        }
                    )
                }
                composable(
                    route = "profile_edit",
                    enterTransition = {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
                    },
                    exitTransition = {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
                    }
                ) {
                    ProfileEditScreen(
                        viewModel = sharedAccountViewModel,
                        onNavigateBack = { bottomNavController.popBackStack() }
                    )
                }

                composable(
                    route = "template_list",
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) {
                    TemplateListScreen(
                        onNavigateBack = { bottomNavController.popBackStack() },
                        onNavigateToDownload = { templateId ->
                            bottomNavController.navigate("template_detail/$templateId")
                        }
                    )
                }

                composable(
                    route = "template_detail/{templateId}",
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) { backStackEntry ->
                    val templateId = backStackEntry.arguments?.getString("templateId")?.toIntOrNull() ?: 1

                    TemplateDetailScreen(
                        templateId = templateId,
                        onNavigateBack = { bottomNavController.popBackStack() }
                    )
                }

                composable(
                    route = "create_exam",
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) {
                    CreateExamScreen(
                        onNavigateBack = { bottomNavController.popBackStack() },
                        onNavigateToSetup = { templateId, examName, examCodes, customQuestionCount ->
                            val codesString = examCodes.joinToString(",")

                            if (templateId == 1) {
                                bottomNavController.navigate("setup_thpt_exam/$templateId/$examName/$codesString")
                            } else if (templateId == 6) {
                                bottomNavController.navigate("setup_ai_exam/$templateId/$examName/$codesString/$customQuestionCount")
                            } else {
                                bottomNavController.navigate("setup_basic_exam/$templateId/$examName/$codesString")
                            }
                        }
                    )
                }

                composable(
                    route = "setup_basic_exam/{templateId}/{examName}/{examCodesStr}",
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) { backStackEntry ->
                    val templateId = backStackEntry.arguments?.getString("templateId")?.toIntOrNull() ?: 2
                    val examName = backStackEntry.arguments?.getString("examName") ?: ""
                    val examCodesStr = backStackEntry.arguments?.getString("examCodesStr") ?: ""

                    val examCodes = examCodesStr.split(",").filter { it.isNotBlank() }

                    SetupBasicExamScreen(
                        templateId = templateId,
                        examName = examName,
                        examCodes = examCodes,
                        onNavigateBack = { bottomNavController.popBackStack() },
                        onNavigateToExamHome = {
                            bottomNavController.popBackStack(BottomNavItem.Exam.route, inclusive = false)
                        }
                    )
                }

                composable(
                    route = "setup_thpt_exam/{templateId}/{examName}/{examCodesStr}",
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) { backStackEntry ->
                    val templateId = backStackEntry.arguments?.getString("templateId")?.toIntOrNull() ?: 1
                    val examName = backStackEntry.arguments?.getString("examName") ?: ""
                    val examCodesStr = backStackEntry.arguments?.getString("examCodesStr") ?: ""
                    val examCodes = examCodesStr.split(",").filter { it.isNotBlank() }

                    SetupThptExamScreen(
                        templateId = templateId,
                        examName = examName,
                        examCodes = examCodes,
                        onNavigateBack = { bottomNavController.popBackStack() },
                        onNavigateToNext = { p1Q, p1S, p2Q, p3Q, p3S ->
                            bottomNavController.navigate("input_thpt_answers/$templateId/$examName/$p1Q/$p1S/$p2Q/$p3Q/$p3S/$examCodesStr")
                        }
                    )
                }

                composable(
                    route = "input_thpt_answers/{templateId}/{examName}/{p1Q}/{p1S}/{p2Q}/{p3Q}/{p3S}/{examCodesStr}"
                ) { backStackEntry ->
                    val templateId = backStackEntry.arguments?.getString("templateId")?.toIntOrNull() ?: 1
                    val examName = backStackEntry.arguments?.getString("examName") ?: ""
                    val p1Q = backStackEntry.arguments?.getString("p1Q")?.toIntOrNull() ?: 0
                    val p1S = backStackEntry.arguments?.getString("p1S")?.toFloatOrNull() ?: 0f
                    val p2Q = backStackEntry.arguments?.getString("p2Q")?.toIntOrNull() ?: 0
                    val p3Q = backStackEntry.arguments?.getString("p3Q")?.toIntOrNull() ?: 0
                    val p3S = backStackEntry.arguments?.getString("p3S")?.toFloatOrNull() ?: 0f
                    val examCodesStr = backStackEntry.arguments?.getString("examCodesStr") ?: ""
                    val examCodes = examCodesStr.split(",").filter { it.isNotBlank() }

                    InputThptAnswersScreen(
                        templateId = templateId,
                        examName = examName,
                        p1Q = p1Q,
                        p1S = p1S,
                        p2Q = p2Q,
                        p3Q = p3Q,
                        p3S = p3S,
                        examCodes = examCodes,
                        onNavigateBack = { bottomNavController.popBackStack() },
                        onNavigateToExamHome = {
                            bottomNavController.popBackStack(BottomNavItem.Exam.route, inclusive = false)
                        }
                    )
                }

                composable(
                    route = "setup_ai_exam/{templateId}/{examName}/{examCodesStr}/{customQuestionCount}",
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) { backStackEntry ->
                    val templateId = backStackEntry.arguments?.getString("templateId")?.toIntOrNull() ?: 6
                    val examName = backStackEntry.arguments?.getString("examName") ?: ""
                    val examCodesStr = backStackEntry.arguments?.getString("examCodesStr") ?: ""
                    val customQuestionCount = backStackEntry.arguments?.getString("customQuestionCount")?.toIntOrNull() ?: 0
                    val examCodes = examCodesStr.split(",").filter { it.isNotBlank() }

                    SetupAiExamScreen(
                        templateId = templateId,
                        examName = examName,
                        examCodes = examCodes,
                        customQuestionCount = customQuestionCount,
                        onNavigateBack = { bottomNavController.popBackStack() },
                        onNavigateToExamHome = {
                            bottomNavController.popBackStack(BottomNavItem.Exam.route, inclusive = false)
                        }
                    )
                }

                composable(
                    route = "all_exams",
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
                ) {
                    AllExamsScreen(
                        onNavigateBack = { bottomNavController.popBackStack() },
                        onNavigateToEditExam = { exam ->
                            val codesStr = exam.codes.joinToString(",")
                            val customQ = if (exam.templateId == 6) exam.questionCount else 0
                            bottomNavController.navigate("edit_exam/${exam.id}/${exam.name}/${exam.templateId}/$codesStr/$customQ")
                        }
                    )
                }

                composable(
                    route = "edit_exam/{examId}/{examName}/{templateId}/{examCodesStr}/{customQ}",
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) { backStackEntry ->
                    val examId = backStackEntry.arguments?.getString("examId") ?: ""
                    val examName = backStackEntry.arguments?.getString("examName") ?: ""
                    val templateId = backStackEntry.arguments?.getString("templateId")?.toIntOrNull() ?: 1
                    val examCodesStr = backStackEntry.arguments?.getString("examCodesStr") ?: ""
                    val customQ = backStackEntry.arguments?.getString("customQ")?.toIntOrNull() ?: 0
                    val examCodes = examCodesStr.split(",").filter { it.isNotBlank() }

                    EditExamScreen(
                        examId = examId,
                        initialExamName = examName,
                        templateId = templateId,
                        initialExamCodes = examCodes,
                        initialCustomQuestionCount = customQ,
                        onNavigateBack = { bottomNavController.popBackStack() },
                        onNavigateToEditAnswers = { tempId, name, codes, qCount ->
                            val codesStr = codes.joinToString(",")
                            // Phân luồng điều hướng tùy theo loại phiếu
                            if (tempId == 1) {
                                bottomNavController.navigate("edit_thpt_exam/$examId/$name/$tempId/$codesStr")
                            } else if (tempId == 6) {
                                bottomNavController.navigate("edit_ai_answers/$examId/$name/$tempId/$codesStr/$qCount")
                            } else {
                                bottomNavController.navigate("edit_basic_answers/$examId/$tempId/$name/$codesStr")
                            }
                        },
                        onExamDeleted = {
                            bottomNavController.popBackStack()
                        }
                    )
                }

                composable(
                    route = "edit_basic_answers/{examId}/{templateId}/{examName}/{examCodesStr}",
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) { backStackEntry ->
                    val examId = backStackEntry.arguments?.getString("examId") ?: ""
                    val templateId = backStackEntry.arguments?.getString("templateId")?.toIntOrNull() ?: 2
                    val examName = backStackEntry.arguments?.getString("examName") ?: ""
                    val examCodesStr = backStackEntry.arguments?.getString("examCodesStr") ?: ""
                    val examCodes = examCodesStr.split(",").filter { it.isNotBlank() }

                    EditBasicAnswersScreen(
                        examId = examId,
                        templateId = templateId,
                        examName = examName,
                        examCodes = examCodes,
                        onNavigateBack = { bottomNavController.popBackStack() },
                        onNavigateToExamHome = {
                            bottomNavController.popBackStack(BottomNavItem.Exam.route, inclusive = false)
                        }
                    )
                }

                composable(
                    route = "edit_ai_answers/{examId}/{examName}/{templateId}/{examCodesStr}/{customQ}",
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) { backStackEntry ->
                    val examId = backStackEntry.arguments?.getString("examId") ?: ""
                    val examName = backStackEntry.arguments?.getString("examName") ?: ""
                    val templateId = backStackEntry.arguments?.getString("templateId")?.toIntOrNull() ?: 6
                    val examCodesStr = backStackEntry.arguments?.getString("examCodesStr") ?: ""
                    val customQ = backStackEntry.arguments?.getString("customQ")?.toIntOrNull() ?: 0
                    val examCodes = examCodesStr.split(",").filter { it.isNotBlank() }

                    EditAiAnswersScreen(
                        examId = examId,
                        templateId = templateId,
                        examName = examName,
                        examCodes = examCodes,
                        customQuestionCount = customQ,
                        onNavigateBack = { bottomNavController.popBackStack() },
                        onNavigateToExamHome = {
                            bottomNavController.popBackStack(BottomNavItem.Exam.route, inclusive = false)
                        }
                    )
                }

                composable(
                    route = "edit_thpt_exam/{examId}/{examName}/{templateId}/{examCodesStr}",
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) { backStackEntry ->
                    val examId = backStackEntry.arguments?.getString("examId") ?: ""
                    val examName = backStackEntry.arguments?.getString("examName") ?: ""
                    val templateId = backStackEntry.arguments?.getString("templateId")?.toIntOrNull() ?: 1
                    val examCodesStr = backStackEntry.arguments?.getString("examCodesStr") ?: ""
                    val examCodes = examCodesStr.split(",").filter { it.isNotBlank() }

                    EditThptExamScreen(
                        examId = examId,
                        templateId = templateId,
                        examName = examName,
                        examCodes = examCodes,
                        onNavigateBack = { bottomNavController.popBackStack() },
                        onNavigateToNext = { p1Q, p1S, p2Q, p3Q, p3S ->
                            bottomNavController.navigate("edit_thpt_answers/$examId/$examName/$templateId/$p1Q/$p1S/$p2Q/$p3Q/$p3S/$examCodesStr")
                        }
                    )
                }

                composable(
                    route = "edit_thpt_answers/{examId}/{examName}/{templateId}/{p1Q}/{p1S}/{p2Q}/{p3Q}/{p3S}/{examCodesStr}"
                ) { backStackEntry ->
                    val examId = backStackEntry.arguments?.getString("examId") ?: ""
                    val examName = backStackEntry.arguments?.getString("examName") ?: ""
                    val templateId = backStackEntry.arguments?.getString("templateId")?.toIntOrNull() ?: 1
                    val p1Q = backStackEntry.arguments?.getString("p1Q")?.toIntOrNull() ?: 0
                    val p1S = backStackEntry.arguments?.getString("p1S")?.toFloatOrNull() ?: 0f
                    val p2Q = backStackEntry.arguments?.getString("p2Q")?.toIntOrNull() ?: 0
                    val p3Q = backStackEntry.arguments?.getString("p3Q")?.toIntOrNull() ?: 0
                    val p3S = backStackEntry.arguments?.getString("p3S")?.toFloatOrNull() ?: 0f
                    val examCodesStr = backStackEntry.arguments?.getString("examCodesStr") ?: ""
                    val examCodes = examCodesStr.split(",").filter { it.isNotBlank() }

                    EditThptAnswersScreen(
                        examId = examId,
                        templateId = templateId,
                        examName = examName,
                        p1Q = p1Q,
                        p1S = p1S,
                        p2Q = p2Q,
                        p3Q = p3Q,
                        p3S = p3S,
                        examCodes = examCodes,
                        onNavigateBack = { bottomNavController.popBackStack() },
                        onNavigateToExamHome = {
                            bottomNavController.popBackStack(BottomNavItem.Exam.route, inclusive = false)
                        }
                    )
                }

                composable(
                    route = "add_roster",
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, androidx.compose.animation.core.tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, androidx.compose.animation.core.tween(300)) }
                ) {
                    AddRosterScreen(
                        onNavigateBack = { bottomNavController.popBackStack() }
                    )
                }

                composable(
                    route = "roster_detail/{rosterId}",
                    arguments = listOf(navArgument("rosterId") { type = NavType.StringType }),
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) { backStackEntry ->
                    val rosterId = backStackEntry.arguments?.getString("rosterId") ?: ""
                    RosterDetailScreen(
                        rosterId = rosterId,
                        onNavigateBack = { bottomNavController.popBackStack() },
                        onNavigateToAddStudent = { id ->
                            // TODO: Chuyển sang màn thêm học sinh
                        },
                        onNavigateToEditRoster = { id ->
                            bottomNavController.navigate("edit_roster/$id")
                        }
                    )
                }

                // Màn hình Sửa danh sách thi
                composable(
                    route = "edit_roster/{rosterId}",
                    arguments = listOf(navArgument("rosterId") { type = NavType.StringType }),
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) { backStackEntry ->
                    val rosterId = backStackEntry.arguments?.getString("rosterId") ?: ""
                    EditRosterScreen(
                        rosterId = rosterId,
                        onNavigateBack = { bottomNavController.popBackStack() }
                    )
                }

                composable(
                    route = "add_student",
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) {
                    AddStudentScreen(
                        onNavigateBack = { bottomNavController.popBackStack() }
                    )
                }

                composable(
                    route = "edit_student/{studentId}",
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) { backStackEntry ->
                    val studentId = backStackEntry.arguments?.getString("studentId") ?: ""

                    EditStudentScreen(
                        studentId = studentId,
                        onNavigateBack = { bottomNavController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomBottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Exam,
        BottomNavItem.Scan,
        BottomNavItem.ExamList,
        BottomNavItem.Account
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 16.dp, spotColor = Color.LightGray)
            .background(Color.White)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            val isScanButton = item == BottomNavItem.Scan
            val hasCircleAndBlueBg = isSelected && isScanButton

            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            val targetScale = when {
                isPressed -> 1.15f
                isSelected && isScanButton -> 1.25f
                isSelected && !isScanButton -> 1.35f
                else -> 1.0f
            }
            val scale by animateFloatAsState(targetValue = targetScale, label = "scale")

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .size(48.dp)
                        .shadow(
                            elevation = if (hasCircleAndBlueBg) 8.dp else 0.dp,
                            shape = CircleShape,
                            spotColor = PrimaryDarkBlue,
                            ambientColor = PrimaryDarkBlue
                        )
                        .background(
                            color = if (hasCircleAndBlueBg) DarkBlue else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = item.iconResId),
                        contentDescription = stringResource(id = item.titleResId),
                        tint = if (hasCircleAndBlueBg) Color.White else if (isSelected) DarkBlue else Color.Gray,
                        modifier = Modifier.size(if (item == BottomNavItem.ExamList) 34.dp else 25.dp)
                    )
                }

                Text(
                    text = stringResource(id = item.titleResId),
                    fontSize = 11.sp,
                    color = if (isSelected) DarkBlue else Color.Gray,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}