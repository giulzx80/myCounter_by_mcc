package com.mcc.mycounter.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mcc.mycounter.MyCounterApplication
import com.mcc.mycounter.data.entities.Counter
import com.mcc.mycounter.data.entities.Periodicity
import com.mcc.mycounter.ui.components.GradientBackground
import com.mcc.mycounter.ui.components.PrimaryActionButton
import com.mcc.mycounter.ui.components.SectionCard
import com.mcc.mycounter.ui.components.TapColorPicker
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * Editor di un contatore: nuovo (counterId == -1) o esistente.
 * Espone tutti i campi richiesti: nome, step (Tap da), partenza, reverse,
 * obiettivo, costo per tap, colore, immagine, periodicità.
 *
 * Stile coerente con SettingsScreen / TemplateEditorScreen di MyVote_by_mcc.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CounterEditorScreen(
    counterId: Long,
    counterViewModel: com.mcc.mycounter.viewmodel.CounterViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as MyCounterApplication }
    val scope = rememberCoroutineScope()

    var existing by remember { mutableStateOf<Counter?>(null) }
    LaunchedEffect(counterId) {
        existing = if (counterId > 0) app.repository.getById(counterId) else null
    }

    // Stati locali (inizializzati a partire dall'esistente quando arriva)
    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var step by remember(existing) { mutableStateOf((existing?.step ?: 1).toString()) }
    var startValue by remember(existing) { mutableStateOf((existing?.startValue ?: 0).toString()) }
    var reverse by remember(existing) { mutableStateOf(existing?.reverse ?: false) }
    var timeMode by remember(existing) { mutableStateOf(existing?.timeMode ?: false) }
    // Target in modalità classica = numero tap; in Conta Tempo = MINUTI.
    // Quando salviamo, se timeMode allora moltiplichiamo per 60 per convertirli in secondi.
    var target by remember(existing) {
        val initial = existing?.let { e ->
            if (e.timeMode) (e.dailyTarget / 60L).toString()
            else e.dailyTarget.toString()
        } ?: "0"
        mutableStateOf(initial)
    }
    var cost by remember(existing) { mutableStateOf((existing?.costPerTap ?: 0.0).toString()) }
    var color by remember(existing) { mutableStateOf(existing?.tapColorArgb ?: 0xFF52525B.toInt()) }
    var imageUri by remember(existing) { mutableStateOf(existing?.tapImageUri) }
    var periodicity by remember(existing) {
        mutableStateOf(Periodicity.fromName(existing?.periodicity).name)
    }

    // Image picker (system content picker, niente permessi runtime su Android 13+)
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // Persist read permission per riusare l'URI dopo restart
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { /* alcune sorgenti non consentono persist */ }
            imageUri = uri.toString()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(if (counterId > 0) "Modifica contatore" else "Nuovo contatore") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
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
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                SectionCard {
                    Text("Anagrafica", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome (es. Conta Sigarette)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                SectionCard {
                    Text("Modalità", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Conta Tempo (timer)",
                                style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (timeMode)
                                    "Il TAP funziona come start/stop di un cronometro. Le sessioni vengono registrate nello storico e accumulate. L'obiettivo è in MINUTI."
                                else
                                    "Modalità classica: ogni TAP incrementa/decrementa il conteggio.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Switch(checked = timeMode, onCheckedChange = { timeMode = it })
                    }
                }

                SectionCard {
                    Text("Conteggio", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = step,
                            onValueChange = { step = it.filter(Char::isDigit) },
                            label = { Text("Tap da (incremento)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            enabled = !timeMode
                        )
                        OutlinedTextField(
                            value = startValue,
                            onValueChange = { startValue = it.filter { c -> c.isDigit() || c == '-' } },
                            label = { Text("Partenza") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Conteggio a rovescia",
                                style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (reverse) "Ogni TAP DECREMENTA il valore."
                                else "Ogni TAP INCREMENTA il valore.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Switch(
                            checked = reverse,
                            onCheckedChange = { reverse = it },
                            enabled = !timeMode
                        )
                    }
                    if (timeMode) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "In modalità Conta Tempo, step e reverse sono ignorati: il TAP è start/stop del timer.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                SectionCard {
                    Text("Obiettivo & Costi", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it.filter(Char::isDigit) },
                        label = {
                            Text(
                                if (timeMode) "Obiettivo in MINUTI (0 = nessuno)"
                                else "Obiettivo (0 = nessun obiettivo)"
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cost,
                        onValueChange = {
                            cost = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
                        },
                        label = {
                            Text(
                                if (timeMode) "Costo per minuto (es. 0.50)"
                                else "Costo per tap (es. 5)"
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        if (timeMode)
                            "Il totale costo è (minuti accumulati) × (costo per minuto)."
                        else
                            "Il totale costo verrà mostrato in TapScreen e nelle statistiche.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                SectionCard {
                    Text("Aspetto", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Colore TAP", style = MaterialTheme.typography.bodyMedium)
                    TapColorPicker(selected = color, onSelect = { color = it })
                    Spacer(Modifier.height(12.dp))
                    Text("Immagine TAP (opzionale)", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { pickImage.launch("image/*") }) {
                            Text(if (imageUri == null) "Scegli immagine" else "Cambia immagine")
                        }
                        if (imageUri != null) {
                            OutlinedButton(onClick = { imageUri = null }) { Text("Rimuovi") }
                        }
                    }
                    if (imageUri != null) {
                        Text(
                            imageUri!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                SectionCard {
                    Text("Periodicità", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Periodicity.DAILY to "Giornaliera",
                            Periodicity.WEEKLY to "Settimanale",
                            Periodicity.MONTHLY to "Mensile",
                            Periodicity.YEARLY to "Annuale"
                        ).forEach { (p, label) ->
                            FilterChip(
                                selected = periodicity == p.name,
                                onClick = { periodicity = p.name },
                                label = { Text(label) }
                            )
                        }
                    }
                    Text(
                        "Giornaliera: prompt di consolidamento ogni giorno nuovo. " +
                                "Settimanale: prompt al primo accesso del lunedì (consolida la settimana precedente).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                PrimaryActionButton(
                    text = if (counterId > 0) "Salva modifiche" else "Crea contatore",
                    enabled = name.isNotBlank() && (timeMode || (step.toIntOrNull() ?: 0) > 0),
                    onClick = {
                        val src = existing
                        val parsedStart = startValue.toLongOrNull() ?: 0L
                        // Target: in time-mode è in MINUTI da convertire in secondi.
                        val parsedTarget = (target.toLongOrNull() ?: 0L).let {
                            if (timeMode) it * 60L else it
                        }
                        val toSave = (src ?: Counter(name = "")).copy(
                            id = src?.id ?: 0L,
                            name = name.trim(),
                            step = if (timeMode) 1 else (step.toIntOrNull() ?: 1),
                            startValue = parsedStart,
                            currentValue = src?.currentValue ?: parsedStart,
                            reverse = if (timeMode) false else reverse,
                            dailyTarget = parsedTarget,
                            costPerTap = cost.replace(',', '.').toDoubleOrNull() ?: 0.0,
                            tapColorArgb = color,
                            tapImageUri = imageUri,
                            periodicity = periodicity,
                            timeMode = timeMode,
                            // Quando si CAMBIA modalità, ferma eventuale timer in corso.
                            runningStartedAt = if (src?.timeMode != timeMode) null else src?.runningStartedAt
                        )
                        scope.launch {
                            counterViewModel.saveCounter(toSave) { newId ->
                                if (counterId <= 0) counterViewModel.selectCounter(newId)
                                onSaved()
                            }
                        }
                    }
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
