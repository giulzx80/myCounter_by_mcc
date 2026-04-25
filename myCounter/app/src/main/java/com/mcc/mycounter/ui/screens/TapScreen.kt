package com.mcc.mycounter.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mcc.mycounter.ui.components.GradientBackground
import com.mcc.mycounter.ui.components.PrimaryActionButton
import com.mcc.mycounter.ui.util.formatCurrency
import com.mcc.mycounter.ui.util.formatDuration
import com.mcc.mycounter.ui.util.formatThousand
import com.mcc.mycounter.viewmodel.CounterViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * Schermata principale: pulsante TAP grande al centro, valore corrente,
 * eventuale "zona calda" e accesso rapido a Lista/Storico/Statistiche/Impostazioni.
 *
 * Lo stile (gradient background, top bar, top icon row) ricalca lo schema
 * di MyVote_by_mcc.HomeScreen / VotingScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TapScreen(
    counterViewModel: CounterViewModel,
    onOpenList: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenStats: () -> Unit,
    onCreateFirst: () -> Unit
) {
    val state by counterViewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val ctx = LocalContext.current
    val counter = state.selected

    val baseColor = counter?.let { Color(it.tapColorArgb) }

    // Prompt consolidamento di periodo (DAILY → giorno, WEEKLY → settimana).
    if (state.pendingDailyConsolidation && counter != null) {
        val periodicityKind = com.mcc.mycounter.data.entities.Periodicity.fromName(counter.periodicity)
        val (titleText, bodyText) = when (periodicityKind) {
            com.mcc.mycounter.data.entities.Periodicity.WEEKLY ->
                "Consolida la settimana precedente" to
                        "L'ultimo TAP su \"${counter.name}\" è in una settimana precedente. " +
                        "Vuoi consolidare la settimana conclusa (lunedì-domenica) e azzerare il counter? " +
                        "Le statistiche e lo storico restano disponibili."
            else ->
                "Consolida il giorno precedente" to
                        "L'ultimo TAP su \"${counter.name}\" è di un giorno precedente. " +
                        "Vuoi consolidare il periodo concluso e azzerare il counter? " +
                        "Le statistiche e lo storico restano disponibili."
        }
        AlertDialog(
            onDismissRequest = { counterViewModel.dismissDailyPrompt() },
            title = { Text(titleText) },
            text = { Text(bodyText) },
            confirmButton = {
                TextButton(onClick = { counterViewModel.consolidate() }) { Text("Consolida") }
            },
            dismissButton = {
                TextButton(onClick = { counterViewModel.dismissDailyPrompt() }) { Text("Più tardi") }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = counter?.name ?: "myCounter",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        // Esce all'home del telefono. La TapScreen è la schermata
                        // principale dell'app: la "freccia indietro" qui significa
                        // "chiudi l'app" (equivalente al tasto back di sistema).
                        (ctx as? android.app.Activity)?.finish()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Chiudi"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenList) {
                        Icon(Icons.Rounded.List, contentDescription = "Configuratore contatori")
                    }
                    IconButton(onClick = onOpenStats) {
                        Icon(Icons.Rounded.BarChart, contentDescription = "Statistiche")
                    }
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Rounded.History, contentDescription = "Storico")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Impostazioni")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { inner ->
        GradientBackground(customBaseColor = baseColor) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                // ----- HEADER (info contatore) -----
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (counter == null) {
                        // Nessun contatore: mostra branding + CTA
                        Text(
                            "myCounter",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "by MCC",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        // Timer live (in modalità Conta Tempo, quando running): scorre ogni secondo.
                        var liveElapsedSec by remember(counter.runningStartedAt) { mutableLongStateOf(0L) }
                        LaunchedEffect(counter.id, counter.runningStartedAt) {
                            val started = counter.runningStartedAt
                            if (counter.timeMode && started != null) {
                                while (true) {
                                    liveElapsedSec = ((System.currentTimeMillis() - started) / 1000L)
                                        .coerceAtLeast(0L)
                                    delay(1000L)
                                }
                            } else {
                                liveElapsedSec = 0L
                            }
                        }

                        Text(
                            text = if (counter.timeMode) {
                                if (counter.isTimerRunning()) "Timer in esecuzione…" else "Tempo accumulato"
                            } else "Valore corrente",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(4.dp))

                        // Valore principale: tempo per timeMode, numero altrimenti.
                        Text(
                            text = if (counter.timeMode) {
                                if (counter.isTimerRunning())
                                    (counter.currentValue + liveElapsedSec).formatDuration()
                                else
                                    counter.currentValue.formatDuration()
                            } else counter.currentValue.formatThousand(),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        if (counter.dailyTarget > 0) {
                            val effectiveValue = if (counter.timeMode && counter.isTimerRunning())
                                counter.currentValue + liveElapsedSec else counter.currentValue
                            val remaining = (counter.dailyTarget - effectiveValue).coerceAtLeast(0)
                            val targetReached = effectiveValue >= counter.dailyTarget
                            val targetLabel = if (counter.timeMode)
                                "${(counter.dailyTarget / 60L)} min"
                            else
                                counter.dailyTarget.formatThousand()
                            val remainingLabel = if (counter.timeMode)
                                remaining.formatDuration()
                            else
                                remaining.formatThousand()
                            Text(
                                text = if (targetReached)
                                    "Obiettivo raggiunto ($targetLabel)"
                                else
                                    "Obiettivo: $targetLabel · mancano $remainingLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (targetReached)
                                    MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.secondary
                            )
                        }
                        if (counter.costPerTap > 0) {
                            val liveTotal = if (counter.timeMode && counter.isTimerRunning())
                                ((counter.currentValue + liveElapsedSec) / 60.0) * counter.costPerTap
                            else counter.totalCost()
                            Text(
                                "Totale: ${liveTotal.formatCurrency()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                // ----- BIG TAP BUTTON -----
                if (counter == null) {
                    PrimaryActionButton(text = "Crea il tuo primo contatore", onClick = onCreateFirst)
                } else {
                    val buttonLabel = when {
                        counter.timeMode && counter.isTimerRunning() -> "STOP"
                        counter.timeMode -> "AVVIA"
                        counter.reverse -> "TAP −${counter.step}"
                        else -> "TAP +${counter.step}"
                    }
                    val buttonIcon: Int? = when {
                        counter.timeMode && counter.isTimerRunning() ->
                            com.mcc.mycounter.R.drawable.ic_widget_stop
                        counter.timeMode -> com.mcc.mycounter.R.drawable.ic_widget_play
                        else -> null
                    }
                    BigTapButton(
                        color = Color(counter.tapColorArgb),
                        label = buttonLabel,
                        imageUri = counter.tapImageUri,
                        hot = counter.isInHotZone() || counter.isTimerRunning(),
                        onTap = {
                            haptic.performHapticFeedback(
                                androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                            )
                            counterViewModel.tap()
                            com.mcc.mycounter.widget.WidgetUpdater.requestUpdateAll(ctx)
                        },
                        iconRes = buttonIcon
                    )
                    AnimatedVisibility(
                        visible = counter.isTargetReached(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            "Obiettivo raggiunto",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // ----- AZIONI BASSE -----
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (counter != null) {
                        if (counter.timeMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Modalità: Conta Tempo",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                if (counter.isTimerRunning()) {
                                    androidx.compose.material3.TextButton(
                                        onClick = { counterViewModel.cancelRunningTimer() }
                                    ) { Text("Annulla timer") }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { counterViewModel.manualDelta(if (counter.reverse) 1 else -1, "Undo") }) {
                                    Icon(Icons.Rounded.Undo, contentDescription = "Undo")
                                }
                                Text(
                                    "Step: ${counter.step}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    if (counter.reverse) "Modalità: rovescia" else "Modalità: incremento",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Pulsante TAP grande circolare. */
@Composable
private fun BigTapButton(
    color: Color,
    label: String,
    imageUri: String?,
    hot: Boolean,
    onTap: () -> Unit,
    /**
     * Se non null, viene mostrato come icona vettoriale al centro del pulsante
     * (al posto di [label]). Usato per Play/Stop in modalità Conta Tempo.
     */
    iconRes: Int? = null
) {
    val scale by animateFloatAsState(
        targetValue = if (hot) 1.05f else 1f,
        animationSpec = tween(durationMillis = 600),
        label = "tap-scale"
    )
    Box(
        modifier = Modifier
            .size(260.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.95f),
                        color.copy(alpha = 0.75f)
                    )
                )
            )
            .border(
                width = if (hot) 6.dp else 2.dp,
                color = if (hot) Color(0xFFFFC107) else Color.White.copy(alpha = 0.4f),
                shape = CircleShape
            )
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        when {
            iconRes != null -> androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(iconRes),
                contentDescription = label,
                modifier = Modifier.size(120.dp)
            )
            !imageUri.isNullOrBlank() -> AsyncImage(
                model = imageUri,
                contentDescription = null,
                modifier = Modifier.size(140.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            else -> Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
