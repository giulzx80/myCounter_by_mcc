package com.mcc.mycounter.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mcc.mycounter.data.entities.Counter
import com.mcc.mycounter.data.entities.Periodicity
import com.mcc.mycounter.ui.components.GradientBackground
import com.mcc.mycounter.ui.components.SectionCard
import com.mcc.mycounter.ui.util.formatThousand
import com.mcc.mycounter.viewmodel.CounterViewModel

/**
 * Configuratore dei contatori salvati.
 * Stile coerente con TemplateListScreen di MyVote_by_mcc.
 *
 * Da qui puoi:
 *  - vedere tutti i contatori
 *  - selezionarne uno (lo apre come "current")
 *  - modificarlo
 *  - duplicarlo
 *  - cancellarlo (con conferma)
 *  - crearne uno nuovo (FAB)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterListScreen(
    counterViewModel: CounterViewModel,
    onBack: () -> Unit,
    onCreateNew: () -> Unit,
    onEdit: (Long) -> Unit,
    onSelected: () -> Unit
) {
    val state by counterViewModel.uiState.collectAsState()
    val counters = state.counters
    val selectedId = state.selected?.id ?: -1L

    var toDelete by remember { mutableStateOf<Counter?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Configuratore contatori") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateNew,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Nuovo contatore") }
            )
        }
    ) { inner ->
        GradientBackground {
            if (counters.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(inner).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Nessun contatore",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tocca il pulsante in basso per crearne uno. Ogni contatore ha " +
                                "il proprio nome, step, obiettivo, costo per tap, colore e periodicità.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(items = counters, key = { it.id }) { c ->
                        CounterRow(
                            counter = c,
                            isSelected = c.id == selectedId,
                            onSelect = {
                                counterViewModel.selectCounter(c.id)
                                onSelected()
                            },
                            onEdit = { onEdit(c.id) },
                            onDuplicate = { counterViewModel.duplicateCounter(c.id) },
                            onDelete = { toDelete = c }
                        )
                    }
                    item { Spacer(Modifier.height(96.dp)) } // margine FAB
                }
            }
        }
    }

    val toDeleteFinal = toDelete
    if (toDeleteFinal != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Elimina contatore?") },
            text = {
                Text(
                    "Eliminando \"${toDeleteFinal.name}\" verranno cancellati anche tutti " +
                            "i tap dello storico e tutti i consolidamenti. Operazione irreversibile."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    counterViewModel.deleteCounter(toDeleteFinal.id)
                    toDelete = null
                }) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Annulla") }
            }
        )
    }
}

@Composable
private fun CounterRow(
    counter: Counter,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    SectionCard(
        modifier = Modifier.clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(counter.tapColorArgb))
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        counter.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isSelected) {
                        Spacer(Modifier.size(6.dp))
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = "Selezionato",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                val periodicityLabel = when (Periodicity.fromName(counter.periodicity)) {
                    Periodicity.DAILY -> "Giornaliera"
                    Periodicity.WEEKLY -> "Settimanale"
                    Periodicity.MONTHLY -> "Mensile"
                    Periodicity.YEARLY -> "Annuale"
                }
                Text(
                    "Valore: ${counter.currentValue.formatThousand()} · Step: ${counter.step}" +
                            (if (counter.reverse) " · Reverse" else "") +
                            " · $periodicityLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (counter.dailyTarget > 0) {
                    Text(
                        "Obiettivo: ${counter.dailyTarget.formatThousand()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = "Modifica")
            }
            IconButton(onClick = onDuplicate) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = "Duplica")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Elimina",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
