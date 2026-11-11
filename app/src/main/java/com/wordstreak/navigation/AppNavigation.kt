package com.wordstreak.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wordstreak.ui.calendar.CalendarScreen
import com.wordstreak.ui.home.HomeScreen
import com.wordstreak.ui.settings.SettingsScreen
import com.wordstreak.ui.study.StudyScreen
import com.wordstreak.ui.vocabulary.VocabularyScreen

object Routes {
    const val HOME = "home"
    const val VOCABULARY = "vocabulary"
    const val STUDY = "study"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"
}

private data class TopLevelRoute(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val topLevelRoutes = listOf(
    TopLevelRoute(Routes.HOME, "首页", Icons.Filled.Home),
    TopLevelRoute(Routes.VOCABULARY, "词库", Icons.Filled.List),
    TopLevelRoute(Routes.STUDY, "学习", Icons.Filled.PlayArrow),
    TopLevelRoute(Routes.CALENDAR, "日历统计", Icons.Filled.DateRange),
    TopLevelRoute(Routes.SETTINGS, "设置", Icons.Filled.Settings)
)

@Composable
fun WordStreakNavHost() {
    val navController = rememberNavController()

    androidx.compose.material3.Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                topLevelRoutes.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) { HomeScreen() }
            composable(Routes.VOCABULARY) { VocabularyScreen() }
            composable(Routes.STUDY) { StudyScreen() }
            composable(Routes.CALENDAR) { CalendarScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
        }
    }
}
