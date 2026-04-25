package com.mcc.mycounter.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mcc.mycounter.ui.screens.CounterEditorScreen
import com.mcc.mycounter.ui.screens.CounterListScreen
import com.mcc.mycounter.ui.screens.HistoryScreen
import com.mcc.mycounter.ui.screens.SettingsScreen
import com.mcc.mycounter.ui.screens.StatsScreen
import com.mcc.mycounter.ui.screens.TapScreen
import com.mcc.mycounter.viewmodel.CounterViewModel
import com.mcc.mycounter.viewmodel.SettingsViewModel

/**
 * Grafo di navigazione Compose.
 *
 * startDestination = TAP perché l'app deve aprirsi sull'ultima istanza di TAP.
 *
 * Tutti i pulsanti "back" passano dalla helper [popOrFinish] che:
 *  - se c'è un destinazione precedente → torna indietro
 *  - se la destinazione corrente è l'unica nel back-stack (es. quando si arriva
 *    dal widget direttamente su STATS) → finisce l'activity → home telefono.
 */
@Composable
fun MyCounterNavGraph(
    navController: NavHostController,
    counterViewModel: CounterViewModel,
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val popOrFinish: () -> Unit = {
        if (!navController.popBackStack()) {
            (context as? android.app.Activity)?.finish()
        }
    }

    NavHost(navController = navController, startDestination = Routes.TAP) {
        composable(Routes.TAP) {
            TapScreen(
                counterViewModel = counterViewModel,
                onOpenList = { navController.navigate(Routes.COUNTER_LIST) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenStats = { navController.navigate(Routes.STATS) },
                onCreateFirst = { navController.navigate(Routes.counterEditor(null)) }
            )
        }
        composable(Routes.COUNTER_LIST) {
            CounterListScreen(
                counterViewModel = counterViewModel,
                onBack = popOrFinish,
                onCreateNew = { navController.navigate(Routes.counterEditor(null)) },
                onEdit = { id -> navController.navigate(Routes.counterEditor(id)) },
                onSelected = { popOrFinish() }
            )
        }
        composable(
            route = Routes.COUNTER_EDITOR,
            arguments = listOf(navArgument("counterId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { entry ->
            val id = entry.arguments?.getLong("counterId") ?: -1L
            CounterEditorScreen(
                counterId = id,
                counterViewModel = counterViewModel,
                onBack = popOrFinish,
                onSaved = { popOrFinish() }
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                counterViewModel = counterViewModel,
                onBack = popOrFinish,
                onOpenStats = { navController.navigate(Routes.STATS) }
            )
        }
        composable(Routes.STATS) {
            StatsScreen(
                counterViewModel = counterViewModel,
                onBack = popOrFinish,
                onOpenHistory = { navController.navigate(Routes.HISTORY) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                onBack = popOrFinish
            )
        }
    }
}
