package com.avtpro.translator.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.avtpro.translator.ui.screens.AudioDubbingScreen
import com.avtpro.translator.ui.screens.HistoryScreen
import com.avtpro.translator.ui.screens.HomeScreen
import com.avtpro.translator.ui.screens.ProcessingScreen
import com.avtpro.translator.ui.screens.ResultScreen
import com.avtpro.translator.ui.screens.SettingsScreen
import com.avtpro.translator.ui.screens.SubtitleStudioScreen
import com.avtpro.translator.ui.screens.VideoTranslationScreen
import com.avtpro.translator.viewmodel.HistoryViewModel
import com.avtpro.translator.viewmodel.SettingsViewModel
import com.avtpro.translator.viewmodel.TranslationViewModel

@Composable
fun AppNavHost(
    settingsViewModel: SettingsViewModel,
    translationViewModel: TranslationViewModel,
    historyViewModel: HistoryViewModel
) {
    val navController = rememberNavController()
    val settings by settingsViewModel.settings.collectAsState()
    val navigate: (String) -> Unit = { route -> navController.navigate(route) }
    val back: () -> Unit = { navController.popBackStack() }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                settings = settings,
                translationViewModel = translationViewModel,
                onNavigate = navigate
            )
        }
        composable(Routes.TRANSLATION) {
            VideoTranslationScreen(
                settings = settings,
                translationViewModel = translationViewModel,
                onNavigate = navigate,
                onBack = back
            )
        }
        composable(Routes.AUDIO_DUBBING) {
            AudioDubbingScreen(
                settingsViewModel = settingsViewModel,
                onBack = back
            )
        }
        composable(
            route = Routes.SUBTITLE_STUDIO,
            arguments = listOf(
                navArgument("projectId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId")
            SubtitleStudioScreen(
                projectId = projectId?.takeIf { it.isNotBlank() },
                historyViewModel = historyViewModel,
                onBack = back
            )
        }
        composable(
            route = Routes.PROCESSING,
            arguments = listOf(
                navArgument("jobId") { type = NavType.StringType },
                navArgument("projectId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            ProcessingScreen(
                jobId = jobId,
                projectId = projectId,
                settings = settings,
                translationViewModel = translationViewModel,
                historyViewModel = historyViewModel,
                onNavigate = navigate,
                onBack = back
            )
        }
        composable(
            route = Routes.RESULT,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            ResultScreen(
                projectId = projectId,
                historyViewModel = historyViewModel,
                onNavigate = navigate,
                onBack = back
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                historyViewModel = historyViewModel,
                translationViewModel = translationViewModel,
                onNavigate = navigate,
                onBack = back
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                onBack = back
            )
        }
    }
}
