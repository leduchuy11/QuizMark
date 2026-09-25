package com.example.quizmark.ui.main

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.quizmark.ui.main.account.AccountScreen
import com.example.quizmark.ui.main.account.AccountViewModel
import com.example.quizmark.ui.main.account.ProfileEditScreen
import com.example.quizmark.ui.main.exam.ExamScreen
import com.example.quizmark.ui.main.exam.addEditExam.CreateExamScreen
import com.example.quizmark.ui.main.exam.addEditExam.InputThptAnswersScreen
import com.example.quizmark.ui.main.exam.addEditExam.SetupBasicExamScreen
import com.example.quizmark.ui.main.exam.addEditExam.SetupThptExamScreen
import com.example.quizmark.ui.main.exam.templateDownload.TemplateDetailScreen
import com.example.quizmark.ui.main.exam.templateDownload.TemplateListScreen
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

            NavHost(
                navController = bottomNavController,
                startDestination = BottomNavItem.Scan.route
            ) {
                composable(BottomNavItem.Home.route) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        Alignment.Center
                    ) { Text("Màn hình Trang chủ") }
                }
                composable(BottomNavItem.Exam.route) {
                    ExamScreen(
                        onNavigateToTemplates = { bottomNavController.navigate("template_list") },
                        onNavigateToCreateExam = { bottomNavController.navigate("create_exam") }
                    )
                }
                composable(BottomNavItem.Scan.route) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        Alignment.Center
                    ) { Text("Màn hình Quét") }
                }
                composable(BottomNavItem.ExamList.route) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        Alignment.Center
                    ) { Text("Màn hình Danh sách thi") }
                }
                composable(BottomNavItem.Account.route) {
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
                        onNavigateToSetup = { templateId, examName, examCodes ->
                            val codesString = examCodes.joinToString(",")

                            if (templateId == 1) {
                                bottomNavController.navigate("setup_thpt_exam/$templateId/$examName/$codesString")
                            } else {
                                // Các phiếu cơ bản (20, 40, 50, 120) thì vào đây
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
                            bottomNavController.navigate("input_thpt_answers/$p1Q/$p2Q/$p3Q/$examCodesStr")
                        }
                    )
                }

                composable(
                    route = "input_thpt_answers/{p1Q}/{p2Q}/{p3Q}/{examCodesStr}",
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) { backStackEntry ->
                    val p1Q = backStackEntry.arguments?.getString("p1Q")?.toIntOrNull() ?: 0
                    val p2Q = backStackEntry.arguments?.getString("p2Q")?.toIntOrNull() ?: 0
                    val p3Q = backStackEntry.arguments?.getString("p3Q")?.toIntOrNull() ?: 0
                    val examCodesStr = backStackEntry.arguments?.getString("examCodesStr") ?: ""
                    val examCodes = examCodesStr.split(",").filter { it.isNotBlank() }

                    InputThptAnswersScreen(
                        p1Q = p1Q,
                        p2Q = p2Q,
                        p3Q = p3Q,
                        examCodes = examCodes,
                        onNavigateBack = { bottomNavController.popBackStack() },
                        onNavigateToExamHome = {
                            bottomNavController.popBackStack(BottomNavItem.Exam.route, inclusive = false)
                        }
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