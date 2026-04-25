package com.mcc.mycounter.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.mcc.mycounter.MyCounterApplication
import com.mcc.mycounter.data.entities.Counter
import com.mcc.mycounter.data.preferences.UserSettings
import com.mcc.mycounter.ui.components.GradientBackground
import com.mcc.mycounter.ui.components.PrimaryActionButton
import com.mcc.mycounter.ui.components.SectionCard
import com.mcc.mycounter.ui.theme.MyCounterTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Activity di configurazione del widget. Viene lanciata da Android quando
 * l'utente piazza una nuova istanza di CounterWidget sulla home.
 *
 * Ricevuta in input l'`EXTRA_APPWIDGET_ID`, mostra la lista dei contatori
 * salvati e fa scegliere quale collegare a questa istanza del widget.
 *
 * IMPORTANTE: per la convenzione widget Android, l'activity DEVE:
 *  - leggere l'appWidgetId all'avvio
 *  - settare il result a CANCELED come default (così se l'utente esce, il widget non viene aggiunto)
 *  - settare il result a OK con l'appWidgetId quando la scelta è confermata
 *  - chiamare un update del widget dopo aver salvato il binding
 */
class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Default: cancellato (se l'utente preme indietro, il widget non viene aggiunto)
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish(); return
        }

        val app = application as MyCounterApplication

        setContent {
            // Settings flow per il tema globale
            val settingsState = remember { MutableStateFlow(UserSettings()) }
            LaunchedEffect(Unit) {
                app.settingsManager.settingsFlow.first().also { settingsState.value = it }
            }
            val settings by settingsState.collectAsState()

            MyCounterTheme(settings = settings) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var counters by remember { mutableStateOf<List<Counter>>(emptyList()) }
                    LaunchedEffect(Unit) {
                        counters = app.repository.getAll()
                    }
                    PickerUi(
                        counters = counters,
                        onCancel = {
                            setResult(Activity.RESULT_CANCELED)
                            finish()
                        },
                        onConfirm = { counter -> bindAndFinish(counter) }
                    )
                }
            }
        }
    }

    private fun bindAndFinish(counter: Counter) {
        lifecycleScope.launch {
            WidgetBindings.setBinding(this@WidgetConfigActivity, appWidgetId, counter.id)
            // Forza il primo render con il counter appena scelto
            CounterWidget().updateAll(this@WidgetConfigActivity)
            // Invia anche un broadcast UPDATE per l'appwidgetId specifico
            try {
                GlanceAppWidgetManager(this@WidgetConfigActivity)
                    .getGlanceIds(CounterWidget::class.java)
                    .forEach { glanceId ->
                        CounterWidget().update(this@WidgetConfigActivity, glanceId)
                    }
            } catch (_: Throwable) { /* best-effort */ }

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerUi(
    counters: List<Counter>,
    onCancel: () -> Unit,
    onConfirm: (Counter) -> Unit
) {
    var selected by remember { mutableStateOf<Counter?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(title = { Text("Scegli il contatore per il widget") })
        }
    ) { inner ->
        GradientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                if (counters.isEmpty()) {
                    Text(
                        "Nessun contatore disponibile.\nApri l'app, crea un contatore e poi riprova ad aggiungere il widget.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(counters, key = { it.id }) { c ->
                            val isSel = selected?.id == c.id
                            SectionCard(
                                modifier = Modifier.clickable { selected = c }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(c.tapColorArgb))
                                    )
                                    Spacer(Modifier.size(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            c.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (isSel)
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Text(
                                            "Valore corrente: ${c.currentValue} · Step: ${c.step}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    if (isSel) {
                                        Text(
                                            "✓",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                PrimaryActionButton(
                    text = "Conferma",
                    enabled = selected != null,
                    onClick = { selected?.let(onConfirm) }
                )
                androidx.compose.material3.OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Annulla") }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
