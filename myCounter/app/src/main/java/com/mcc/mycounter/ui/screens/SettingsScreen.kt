package com.mcc.mycounter.ui.screens

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.mcc.mycounter.ui.components.DexSwitch
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mcc.mycounter.ui.components.GradientBackground
import com.mcc.mycounter.ui.components.PrimaryActionButton
import com.mcc.mycounter.ui.components.SectionCard
import com.mcc.mycounter.ui.theme.Palettes
import com.mcc.mycounter.viewmodel.SettingsViewModel

/**
 * Schermata Impostazioni globali: tema, palette, vibrazione, conferme.
 * Stile e organizzazione coerenti con MyVote_by_mcc.SettingsScreen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val current by settingsViewModel.settings.collectAsState()

    var themeMode by remember(current.themeMode) { mutableStateOf(current.themeMode) }
    var palette by remember(current.paletteName) { mutableStateOf(current.paletteName) }
    var haptic by remember(current.hapticFeedback) { mutableStateOf(current.hapticFeedback) }
    var confirmDec by remember(current.confirmDecrement) { mutableStateOf(current.confirmDecrement) }
    var emailAccEnabled by remember(current.accountabilityEmailEnabled) {
        mutableStateOf(current.accountabilityEmailEnabled)
    }
    var webhookEnabled by remember(current.webhookEnabled) { mutableStateOf(current.webhookEnabled) }
    var webhookUrl by remember(current.webhookUrl) { mutableStateOf(current.webhookUrl) }
    var webhookSecret by remember(current.webhookSecret) { mutableStateOf(current.webhookSecret) }
    var testMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Impostazioni") },
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
                    Text("Tema grafico", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("system" to "Sistema", "light" to "Chiaro", "dark" to "Scuro").forEach { (v, label) ->
                            FilterChip(
                                selected = themeMode == v,
                                onClick = { themeMode = v },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Palette colori", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Palettes.all.forEach { p ->
                            FilterChip(
                                selected = palette == p.name,
                                onClick = { palette = p.name },
                                label = { Text(p.label) }
                            )
                        }
                    }
                }

                SectionCard {
                    Text("Comportamento TAP", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Vibrazione al TAP", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Piccola vibrazione di feedback ogni volta che premi.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        DexSwitch(checked = haptic, onCheckedChange = { haptic = it })
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Conferma sotto zero", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Chiede conferma se un TAP fa scendere il counter sotto 0.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        DexSwitch(checked = confirmDec, onCheckedChange = { confirmDec = it })
                    }
                }

                // ===== Accountability email (master switch) =====
                SectionCard {
                    Text("Accountability via email", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Invia bozza-mail al coach", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "A fine periodo, per ogni counter con email coach configurata, " +
                                        "prepara una bozza-mail con il PDF di riepilogo. Tu confermi e invii.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        DexSwitch(checked = emailAccEnabled, onCheckedChange = { emailAccEnabled = it })
                    }
                }

                // ===== Webhook (Step E) =====
                SectionCard {
                    Text("Webhook (integrazione esterna)", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Abilita webhook", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "A fine periodo l'app fa una POST JSON al tuo endpoint con il dettaglio " +
                                        "del consolidamento. Utile per Zapier, n8n, IFTTT, server custom.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        DexSwitch(checked = webhookEnabled, onCheckedChange = { webhookEnabled = it })
                    }
                    if (webhookEnabled) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = webhookUrl,
                            onValueChange = { webhookUrl = it },
                            label = { Text("URL endpoint (https://…)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = webhookSecret,
                            onValueChange = { webhookSecret = it },
                            label = { Text("Secret HMAC (opzionale)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = {
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    com.mcc.mycounter.notify.WebhookSender.sendTestPing(
                                        webhookUrl, webhookSecret
                                    )
                                }
                                testMessage = if (ok) "✓ Test inviato con successo (HTTP 2xx)"
                                else "✗ Test fallito (verifica URL e connessione)"
                                snackbarHostState.showSnackbar(testMessage!!)
                            }
                        }) { Text("Invia ping di test") }

                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Come configurare il tuo endpoint",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Il tuo server riceverà una POST con queste caratteristiche:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "• Method: POST\n" +
                                    "• Content-Type: application/json; charset=UTF-8\n" +
                                    "• User-Agent: myCounter-by-MCC/1.0\n" +
                                    "• X-myCounter-Signature: hex(HMAC-SHA256(body, secret)) [se secret è impostato]",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Esempio di payload:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        SectionCard {
                            Text(
                                """
{
  "event": "consolidation",
  "timestamp": 1719753600000,
  "counter": {
    "id": 1,
    "name": "Conta Caffè",
    "goalType": "LIMIT",
    "periodicity": "DAILY",
    "timeMode": false,
    "startValue": 0,
    "target": 3,
    "finalValue": 4,
    "totalCost": 4.80,
    "currency": "EUR"
  },
  "achievement": {
    "outcome": "FAILURE",
    "streak": 0,
    "consolidationId": 42,
    "periodEndedAt": 1719753600000
  }
}
                                """.trim(),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Verifica server-side della firma (pseudo-codice):",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            "expected = hex(HMAC_SHA256(rawBody, secret))\n" +
                                    "if (expected != header['X-myCounter-Signature']) reject",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Compatibile con: Zapier (Catch Hook), n8n (Webhook node), IFTTT " +
                                    "(Webhooks service), Make.com, server self-hosted.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Note: best-effort, niente retry. Il consolidamento resta comunque " +
                                    "salvato in locale anche se la POST fallisce.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                PrimaryActionButton(
                    text = "Salva",
                    onClick = {
                        settingsViewModel.updateTheme(themeMode, palette)
                        settingsViewModel.updateHaptic(haptic)
                        settingsViewModel.updateConfirmDecrement(confirmDec)
                        settingsViewModel.updateAccountabilityEmailEnabled(emailAccEnabled)
                        settingsViewModel.updateWebhook(webhookEnabled, webhookUrl, webhookSecret)
                    }
                )
                OutlinedButton(
                    onClick = { settingsViewModel.reset() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Ripristina default") }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
