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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
                        Switch(checked = haptic, onCheckedChange = { haptic = it })
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
                        Switch(checked = confirmDec, onCheckedChange = { confirmDec = it })
                    }
                }

                PrimaryActionButton(
                    text = "Salva",
                    onClick = {
                        settingsViewModel.updateTheme(themeMode, palette)
                        settingsViewModel.updateHaptic(haptic)
                        settingsViewModel.updateConfirmDecrement(confirmDec)
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
