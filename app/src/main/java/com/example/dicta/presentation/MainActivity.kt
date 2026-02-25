package com.example.dicta.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.dicta.data.preferences.UserPreferences
import com.example.dicta.presentation.navigation.DictaNavHost
import com.example.dicta.presentation.navigation.Screen
import com.example.dicta.presentation.theme.DictaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Record", Icons.Default.Mic),
    BottomNavItem(Screen.History, "History", Icons.Default.History),
    BottomNavItem(Screen.Settings, "Settings", Icons.Default.Settings)
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DictaTheme {
                val navController = rememberNavController()
                val onboardingCompleted by userPreferences.onboardingCompleted.collectAsState(initial = null)
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                onboardingCompleted?.let { completed ->
                    val startDestination = if (completed) {
                        Screen.Home.route
                    } else {
                        Screen.Onboarding.route
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            AnimatedVisibility(
                                visible = currentRoute != Screen.Onboarding.route,
                                enter = slideInVertically { it },
                                exit = slideOutVertically { it }
                            ) {
                                NavigationBar {
                                    bottomNavItems.forEach { item ->
                                        NavigationBarItem(
                                            selected = currentRoute == item.screen.route,
                                            onClick = {
                                                navController.navigate(item.screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = item.icon,
                                                    contentDescription = item.label
                                                )
                                            },
                                            label = { Text(item.label) }
                                        )
                                    }
                                }
                            }
                        }
                    ) { paddingValues ->
                        DictaNavHost(
                            navController = navController,
                            startDestination = startDestination,
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
                }
            }
        }
    }
}
