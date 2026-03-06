package com.bordrotakip.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.CircularProgressIndicator
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bordrotakip.ui.calendar.CalendarScreen
import com.bordrotakip.ui.onboarding.OnboardingGateViewModel
import com.bordrotakip.ui.onboarding.OnboardingScreen
import com.bordrotakip.ui.payroll.PayrollDetailScreen
import com.bordrotakip.ui.payroll.PayrollEditorScreen
import com.bordrotakip.ui.payroll.PayrollListScreen
import com.bordrotakip.ui.payroll.PayrollSummaryScreen
import com.bordrotakip.ui.salary.SalaryPredictionScreen
import com.bordrotakip.ui.settings.SettingsScreen

/**
 * Navigation rotaları
 */
object Routes {
    const val CALENDAR = "calendar"
    const val SALARY_PREDICTION = "salary_prediction"
    const val PAYROLL_LIST = "payroll_list"
    const val PAYROLL_SUMMARY = "payroll_summary"
    const val PAYROLL_DETAIL = "payroll_detail/{payrollId}"
    const val PAYROLL_ADD = "payroll_add"
    const val SETTINGS = "settings"
    const val ONBOARDING = "onboarding"
    
    fun payrollDetail(payrollId: Long) = "payroll_detail/$payrollId"
}

@Composable
fun BordroNavHost(
    navController: NavHostController = rememberNavController(),
    openShiftPromptEpochDay: Long? = null,
    onConsumeOpenShiftPrompt: () -> Unit = {},
    openSalaryPrediction: Boolean = false,
    onConsumeOpenSalaryPrediction: () -> Unit = {}
) {
    val gateViewModel: OnboardingGateViewModel = hiltViewModel()
    val gateState by gateViewModel.uiState.collectAsState()

    if (!gateState.isReady) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val backStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = backStackEntry?.destination?.route

    var isBackNavigationInProgress by remember { mutableStateOf(false) }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect {
            isBackNavigationInProgress = false
        }
    }

    val safePopBackStack: () -> Unit = pop@{
        if (isBackNavigationInProgress) return@pop
        // Check if we can actually pop (prevent popping beyond start destination)
        if (navController.previousBackStackEntry == null) return@pop
        isBackNavigationInProgress = true
        val didPop = navController.popBackStack()
        if (!didPop) {
            isBackNavigationInProgress = false
        }
    }

    val isOnboardingCompleted = !gateState.shouldShowOnboarding

    LaunchedEffect(openShiftPromptEpochDay, currentRoute, isOnboardingCompleted) {
        if (!isOnboardingCompleted) return@LaunchedEffect
        if (openShiftPromptEpochDay == null) return@LaunchedEffect
        if (currentRoute == null) return@LaunchedEffect
        if (currentRoute == Routes.CALENDAR) return@LaunchedEffect

        navController.navigate(Routes.CALENDAR) {
            popUpTo(Routes.CALENDAR) { inclusive = false }
            launchSingleTop = true
        }
    }

    LaunchedEffect(openSalaryPrediction, currentRoute, openShiftPromptEpochDay, isOnboardingCompleted) {
        if (!isOnboardingCompleted) return@LaunchedEffect
        if (!openSalaryPrediction) return@LaunchedEffect
        if (openShiftPromptEpochDay != null) return@LaunchedEffect
        if (currentRoute == null) return@LaunchedEffect
        if (currentRoute == Routes.SALARY_PREDICTION) {
            onConsumeOpenSalaryPrediction()
            return@LaunchedEffect
        }

        navController.navigate(Routes.SALARY_PREDICTION) {
            launchSingleTop = true
        }
        onConsumeOpenSalaryPrediction()
    }

    NavHost(
        navController = navController,
        startDestination = if (gateState.shouldShowOnboarding) Routes.ONBOARDING else Routes.CALENDAR
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.CALENDAR) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.CALENDAR) {
            CalendarScreen(
                onNavigateToSalary = { navController.navigate(Routes.SALARY_PREDICTION) },
                onNavigateToPayrolls = { navController.navigate(Routes.PAYROLL_LIST) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                openDayDetailEpochDay = openShiftPromptEpochDay,
                onOpenDayDetailConsumed = onConsumeOpenShiftPrompt
            )
        }
        
        composable(Routes.SALARY_PREDICTION) {
            SalaryPredictionScreen(
                onNavigateBack = safePopBackStack
            )
        }
        
        composable(Routes.PAYROLL_LIST) {
            PayrollListScreen(
                onNavigateBack = safePopBackStack,
                onNavigateToSummary = { navController.navigate(Routes.PAYROLL_SUMMARY) },
                onNavigateToDetail = { payrollId ->
                    navController.navigate(Routes.payrollDetail(payrollId))
                },
                onNavigateToAdd = { navController.navigate(Routes.PAYROLL_ADD) }
            )
        }

        composable(Routes.PAYROLL_SUMMARY) {
            PayrollSummaryScreen(
                onNavigateBack = safePopBackStack
            )
        }

        composable(
            route = Routes.PAYROLL_DETAIL,
            arguments = listOf(
                navArgument("payrollId") { type = NavType.LongType }
            )
        ) {
            PayrollDetailScreen(
                onNavigateBack = safePopBackStack
            )
        }

        composable(Routes.PAYROLL_ADD) {
            PayrollEditorScreen(
                onNavigateBack = safePopBackStack
            )
        }
        
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = safePopBackStack
            )
        }
    }
}
