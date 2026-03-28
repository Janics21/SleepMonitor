package com.example.sleepmonitor

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sleepmonitor.navigation.AppScreen
import com.example.sleepmonitor.ui.AppViewModelFactory
import com.example.sleepmonitor.ui.auth.DeleteAccountScreen
import com.example.sleepmonitor.ui.auth.ForgotPasswordScreen
import com.example.sleepmonitor.ui.auth.LoginScreen
import com.example.sleepmonitor.ui.auth.RegisterScreen
import com.example.sleepmonitor.ui.home.HomeScreen
import com.example.sleepmonitor.ui.profile.ProfileScreen
import com.example.sleepmonitor.ui.profile.ProfileViewModel
import com.example.sleepmonitor.ui.recommendations.RecommendationsScreen
import com.example.sleepmonitor.ui.recommendations.RecommendationsViewModel
import com.example.sleepmonitor.ui.sleep.SleepSessionScreen
import com.example.sleepmonitor.ui.sleep.SleepSessionViewModel
import com.example.sleepmonitor.ui.utils.SessionManager

@Composable
fun SleepMonitorApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val factory = remember { AppViewModelFactory(context) }
    val sessionManager = remember { SessionManager(context) }
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    val startDestination = if (sessionManager.isLoggedIn()) {
        AppScreen.Dashboard.route
    } else {
        AppScreen.Login.route
    }

    val showBottomBar = AppScreen.bottomBarItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    AppScreen.bottomBarItems.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                if (currentRoute != destination.route) {
                                    navController.navigate(destination.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                        popUpTo(AppScreen.Dashboard.route) { saveState = true }
                                    }
                                }
                            },
                            icon = { Text(destination.label.take(1)) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            composable(AppScreen.Login.route) {
                val vm = viewModel<com.example.sleepmonitor.ui.auth.LoginViewModel>(factory = factory)
                LoginScreen(
                    viewModel = vm,
                    onGoToRegister = { navController.navigate(AppScreen.Register.route) },
                    onGoToForgotPassword = { navController.navigate(AppScreen.ForgotPassword.route) },
                    onLoginSuccess = {
                        navController.navigate(AppScreen.Dashboard.route) {
                            popUpTo(AppScreen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(AppScreen.Register.route) {
                val vm = viewModel<com.example.sleepmonitor.ui.auth.RegisterViewModel>(factory = factory)
                RegisterScreen(
                    viewModel = vm,
                    onBackToLogin = { navController.popBackStack() },
                    onRegisterSuccess = {
                        navController.navigate(AppScreen.Dashboard.route) {
                            popUpTo(AppScreen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(AppScreen.ForgotPassword.route) {
                val vm = viewModel<com.example.sleepmonitor.ui.auth.ForgotPasswordViewModel>(factory = factory)
                ForgotPasswordScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(AppScreen.Dashboard.route) {
                HomeScreen(
                    username = sessionManager.getUsername().orEmpty(),
                    onGoToSleep = { navController.navigate(AppScreen.SleepSession.route) },
                    onGoToRecommendations = { navController.navigate(AppScreen.Recommendations.route) },
                    onGoToProfile = { navController.navigate(AppScreen.Profile.route) }
                )
            }

            composable(AppScreen.SleepSession.route) {
                val vm = viewModel<SleepSessionViewModel>(factory = factory)
                SleepSessionScreen(
                    viewModel = vm,
                    onGoBackHome = { navController.navigate(AppScreen.Dashboard.route) }
                )
            }

            composable(AppScreen.Recommendations.route) {
                val vm = viewModel<RecommendationsViewModel>(factory = factory)
                RecommendationsScreen(
                    viewModel = vm,
                    onBack = { navController.navigate(AppScreen.Dashboard.route) }
                )
            }

            composable(AppScreen.Profile.route) {
                val vm = viewModel<ProfileViewModel>(factory = factory)
                ProfileScreen(
                    viewModel = vm,
                    onDeleteAccount = { navController.navigate(AppScreen.DeleteAccount.route) },
                    onLogout = {
                        sessionManager.clearSession()
                        navController.navigate(AppScreen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(AppScreen.DeleteAccount.route) {
                val vm = viewModel<com.example.sleepmonitor.ui.auth.DeleteAccountViewModel>(factory = factory)
                DeleteAccountScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onDeleted = {
                        navController.navigate(AppScreen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
