package com.mcc.mycounter

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.mcc.mycounter.ui.navigation.MyCounterNavGraph
import com.mcc.mycounter.ui.navigation.Routes
import com.mcc.mycounter.ui.theme.MyCounterTheme
import com.mcc.mycounter.viewmodel.CounterViewModel
import com.mcc.mycounter.viewmodel.SettingsViewModel
import com.mcc.mycounter.viewmodel.ViewModelFactory

/**
 * Activity unica. Ospita il NavHost e passa ai ViewModel la factory che sa
 * creare le istanze con le dipendenze di [MyCounterApplication].
 *
 * All'avvio l'app va DIRETTAMENTE alla TapScreen del contatore selezionato
 * (oppure alla CounterListScreen se non esistono ancora contatori).
 *
 * Se l'app viene aperta da un deep-link del widget (mycounter://counter/<id>?screen=stats&fromWidget=1)
 * il back-stack viene resettato e l'utente atterra direttamente sulle Statistiche;
 * il pulsante "back" lì sopra esce alla home del telefono (dove è il widget),
 * NON torna alla TapScreen.
 */
class MainActivity : ComponentActivity() {

    private val counterViewModel: CounterViewModel by viewModels {
        ViewModelFactory(application as MyCounterApplication)
    }
    private val settingsViewModel: SettingsViewModel by viewModels {
        ViewModelFactory(application as MyCounterApplication)
    }

    companion object {
        private const val REQ_NOTIFICATIONS = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge: l'UI usa l'intera area schermo, coerente con il look "moderno"
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Android 13+ richiede di chiedere a runtime il permesso per le notifiche.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    REQ_NOTIFICATIONS
                )
            }
        }

        setContent {
            val settings by settingsViewModel.settings.collectAsState()
            MyCounterTheme(settings = settings) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    // Gestione deep-link "mycounter://counter/<id>?screen=stats[&fromWidget=1]"
                    var deepLinkConsumed by remember { mutableStateOf(false) }
                    LaunchedEffect(intent) {
                        if (!deepLinkConsumed) {
                            handleDeepLink(intent)?.let { dl ->
                                counterViewModel.selectCounter(dl.counterId)
                                if (dl.target == "stats") {
                                    if (dl.fromWidget) {
                                        // Back-stack pulito: STATS è l'unica destinazione
                                        // → Back esce all'home del telefono.
                                        navController.navigate(Routes.STATS) {
                                            popUpTo(Routes.TAP) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    } else {
                                        navController.navigate(Routes.STATS)
                                    }
                                }
                            }
                            deepLinkConsumed = true
                        }
                    }

                    MyCounterNavGraph(
                        navController = navController,
                        counterViewModel = counterViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Aggiorna l'intent dell'activity così il LaunchedEffect lo rilegge.
        setIntent(intent)
    }

    private data class DeepLink(val counterId: Long, val target: String, val fromWidget: Boolean)

    /**
     * Estrae i parametri da un intent con dati `mycounter://counter/<id>?screen=...&fromWidget=1`.
     */
    private fun handleDeepLink(intent: Intent?): DeepLink? {
        val data = intent?.data ?: return null
        if (data.scheme != "mycounter" || data.host != "counter") return null
        val id = data.lastPathSegment?.toLongOrNull() ?: return null
        val target = data.getQueryParameter("screen") ?: "tap"
        val fromWidget = data.getQueryParameter("fromWidget") == "1"
        return DeepLink(id, target, fromWidget)
    }
}
