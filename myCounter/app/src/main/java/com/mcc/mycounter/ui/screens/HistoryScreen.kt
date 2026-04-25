package com.mcc.mycounter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mcc.mycounter.ui.components.GradientBackground
import com.mcc.mycounter.ui.components.PrimaryActionButton
import com.mcc.mycounter.ui.components.SectionCard
import com.mcc.mycounter.ui.util.formatDateTime
import com.mcc.mycounter.ui.util.formatDurationMs
import com.mcc.mycounter.viewmodel.CounterViewModel

/**
 * Storico completo dei tap del contatore selezionato:
 * data/ora, valore prima/dopo, eventuale nota.
 *
 * Da qui si possono anche eseguire Consolida e Reset.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    counterViewModel: CounterViewModel,
    onBack: () -> Unit,
    onOpenStats: () -> Unit
) {
    val state by counterViewModel.uiState.collectAsState()
    val counter = state.selected
    val taps = state.history
    var showResetConfirm by remember { mutableStateOf(false) }
    var showConsolidateConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Storico${counter?.let { " · " + it.name } ?: ""}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenStats) {
                        Icon(Icons.Rounded.BarChart, contentDescription = "Statistiche")
                    }
                }
            )
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

                if (counter == null) {
                    Text(
                        "Nessun contatore selezionato.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    SectionCard {
                        Text("Operazioni periodo", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Consolida chiude il periodo corrente, salva uno snapshot e azzera " +
                                    "il counter. Reset cancella TUTTO lo storico/statistiche " +
                                    "(disponibile solo dopo almeno un consolidamento).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrimaryActionButton(
                                text = "Consolida",
                                onClick = { showConsolidateConfirm = true },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = { showResetConfirm = true },
                                modifier = Modifier.weight(1f),
                                enabled = state.canReset
                            ) {
                                Text(if (state.canReset) "Reset" else "Reset (consolida prima)")
                            }
                        }
                    }
                }

                if (taps.isEmpty()) {
                    Text(
                        "Nessun TAP registrato",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 24.dp).fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = taps, key = { it.id }) { ev ->
                            SectionCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        if (ev.isTimerSession()) {
                                            // Sessione cronometrata: mostra inizio, fine, durata
                                            Text(
                                                "Sessione cronometrata" +
                                                        (if (ev.fromWidget) " · widget" else ""),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                "Inizio: ${ev.sessionStartedAt!!.formatDateTime()}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                "Fine:    ${ev.timestamp.formatDateTime()}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        } else {
                                            // Tap classico
                                            Text(
                                                ev.timestamp.formatDateTime(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                "${ev.valueBefore} → ${ev.valueAfter}" +
                                                        (if (ev.fromWidget) " · widget" else ""),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        if (!ev.note.isNullOrBlank()) {
                                            Text(
                                                "Nota: ${ev.note}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                    if (ev.isTimerSession()) {
                                        Text(
                                            ev.durationMs!!.formatDurationMs(),
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            if (ev.delta >= 0) "+${ev.delta}" else "${ev.delta}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = if (ev.delta >= 0)
                                                MaterialTheme.colorScheme.tertiary
                                            else MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }

    if (showConsolidateConfirm) {
        AlertDialog(
            onDismissRequest = { showConsolidateConfirm = false },
            title = { Text("Consolidare il periodo?") },
            text = { Text("Verrà salvato lo snapshot corrente e il contatore tornerà al valore di partenza.") },
            confirmButton = {
                TextButton(onClick = {
                    showConsolidateConfirm = false
                    counterViewModel.consolidate()
                }) { Text("Consolida") }
            },
            dismissButton = {
                TextButton(onClick = { showConsolidateConfirm = false }) { Text("Annulla") }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset storico e statistiche?") },
            text = { Text("Verranno cancellati storico tap e tutti i consolidamenti del contatore. Operazione irreversibile.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    counterViewModel.resetHistoryAndStats()
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Annulla") }
            }
        )
    }
}
