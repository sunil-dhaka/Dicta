package com.example.dicta.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.dicta.presentation.history.HistoryScreen
import com.example.dicta.presentation.history.HistoryViewModel
import com.example.dicta.presentation.home.HomeScreen
import com.example.dicta.presentation.home.HomeViewModel
import com.example.dicta.presentation.onboarding.OnboardingScreen
import com.example.dicta.presentation.onboarding.OnboardingViewModel
import com.example.dicta.presentation.settings.SettingsScreen
import com.example.dicta.presentation.settings.SettingsViewModel

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object History : Screen("history")
    data object Settings : Screen("settings")
}

@Composable
fun DictaNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) {
            val viewModel: OnboardingViewModel = hiltViewModel()
            OnboardingScreen(
                viewModel = viewModel,
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(viewModel = viewModel)
        }

        composable(Screen.History.route) {
            val viewModel: HistoryViewModel = hiltViewModel()
            HistoryScreen(viewModel = viewModel)
        }

        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(viewModel = viewModel)
        }
    }
}
