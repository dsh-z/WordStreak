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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wordstreak.ui.calendar.CalendarScreen
import com.wordstreak.ui.home.HomeScreen
import com.wordstreak.ui.review.ReviewScreen
import com.wordstreak.ui.settings.SettingsScreen
import com.wordstreak.ui.study.StudyScreen
import com.wordstreak.ui.vocabulary.VocabularyScreen
import com.wordstreak.ui.vocabulary.WordListScreen

object Routes {
    const val HOME = "home"
    const val VOCABULARY = "vocabulary"
    const val BOOK_DETAIL = "vocabulary/{bookId}"
    const val STUDY = "study"
    const val REVIEW = "review"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"
}

private data class TopLevelRoute(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
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
    androidx.compose.material3.Scaffold(bottomBar = {
        NavigationBar {
            val entry by navController.currentBackStackEntryAsState()
            val destination = entry?.destination
            topLevelRoutes.forEach { item ->
                val selected = destination?.hierarchy?.any {
                    it.route == item.route ||
                        it.route?.startsWith("${item.route}/") == true ||
                        (item.route == Routes.STUDY && it.route == Routes.REVIEW)
                } == true
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
    }) { innerPadding ->
        NavHost(navController, startDestination = Routes.HOME, modifier = Modifier.padding(innerPadding)) {
            composable(Routes.HOME) {
                HomeScreen(
                    onStartStudy = {
                        navController.navigate(Routes.STUDY) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onGoReview = {
                        navController.navigate(Routes.REVIEW) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Routes.VOCABULARY) {
                VocabularyScreen(onOpenBook = { id -> navController.navigate("vocabulary/$id") })
            }
            composable(Routes.BOOK_DETAIL, arguments = listOf(navArgument("bookId") { type = NavType.LongType })) { entry ->
                WordListScreen(bookId = entry.arguments?.getLong("bookId") ?: 0L, onBack = { navController.popBackStack() })
            }
            composable(Routes.STUDY) { StudyScreen(onOpenReview = { navController.navigate(Routes.REVIEW) }) }
            composable(Routes.REVIEW) { ReviewScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.CALENDAR) { CalendarScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
        }
    }
}
