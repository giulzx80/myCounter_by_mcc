package com.mcc.mycounter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mcc.mycounter.data.entities.Consolidation
import com.mcc.mycounter.data.entities.Counter
import com.mcc.mycounter.data.entities.TapEvent
import com.mcc.mycounter.data.preferences.SettingsManager
import com.mcc.mycounter.data.repository.CounterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Stato UI esposto dal [CounterViewModel].
 */
data class CounterUiState(
    val counters: List<Counter> = emptyList(),
    val selected: Counter? = null,
    val history: List<TapEvent> = emptyList(),
    val consolidations: List<Consolidation> = emptyList(),
    val canReset: Boolean = false,
    val pendingDailyConsolidation: Boolean = false,
    val message: String? = null
)

/**
 * ViewModel principale: gestisce la lista dei contatori, il contatore selezionato,
 * tap, consolidamenti, reset, storico e statistiche.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CounterViewModel(
    application: Application,
    private val repository: CounterRepository,
    private val settings: SettingsManager
) : AndroidViewModel(application) {

    /** Application context (per notifiche, intent email, ecc.). */
    private val appContext: android.content.Context get() = getApplication()

    /** Id del contatore selezionato. -1 = non c'è selezione (verrà calcolata). */
    private val selectedCounterId = MutableStateFlow(-1L)

    val counters: StateFlow<List<Counter>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Counter attualmente selezionato (osservato dal DB). */
    val selectedCounter: StateFlow<Counter?> = selectedCounterId
        .flatMapLatest { id ->
            if (id <= 0) flowOf(null) else repository.observeCounter(id)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Storico tap del counter selezionato. */
    val tapHistory: StateFlow<List<TapEvent>> = selectedCounterId
        .flatMapLatest { id ->
            if (id <= 0) flowOf(emptyList()) else repository.observeTapHistory(id)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Consolidazioni del counter selezionato. */
    val consolidations: StateFlow<List<Consolidation>> = selectedCounterId
        .flatMapLatest { id ->
            if (id <= 0) flowOf(emptyList()) else repository.observeConsolidations(id)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _uiState = MutableStateFlow(CounterUiState())
    val uiState: StateFlow<CounterUiState> = _uiState.asStateFlow()

    init {
        // Tieni aggiornato uiState con i flow combinati
        viewModelScope.launch {
            combine(counters, selectedCounter, tapHistory, consolidations) { c, s, h, k ->
                CounterUiState(
                    counters = c,
                    selected = s,
                    history = h,
                    consolidations = k,
                    canReset = k.isNotEmpty()
                )
            }.collect { newState ->
                _uiState.value = _uiState.value.copy(
                    counters = newState.counters,
                    selected = newState.selected,
                    history = newState.history,
                    consolidations = newState.consolidations,
                    canReset = newState.canReset
                )
            }
        }

        // Carica selezione persistita all'avvio.
        viewModelScope.launch {
            val s = settings.settingsFlow.first()
            val toUse: Long = when {
                s.selectedCounterId > 0 && repository.getById(s.selectedCounterId) != null ->
                    s.selectedCounterId
                else -> repository.getAll().firstOrNull()?.id ?: -1L
            }
            selectedCounterId.value = toUse

            // Persiste l'auto-selezione iniziale così che anche il widget (e altri
            // componenti che leggono solo dalle preferenze) abbia un fallback valido.
            if (toUse > 0 && s.selectedCounterId != toUse) {
                settings.updateSelectedCounter(toUse)
            }

            // Verifica eventuale consolidamento DAILY pendente
            if (toUse > 0) checkDailyPrompt(toUse)
        }
    }

    fun selectCounter(id: Long) {
        selectedCounterId.value = id
        viewModelScope.launch {
            settings.updateSelectedCounter(id)
            checkDailyPrompt(id)
        }
    }

    private suspend fun checkDailyPrompt(id: Long) {
        // Auto-consolidamento silenzioso del periodo precedente se necessario:
        // così "Conta Caffè" giornaliero appare già a 0 al primo accesso del
        // giorno successivo, senza chiedere conferma.
        withContext(Dispatchers.IO) { repository.autoConsolidateIfNeeded(id) }
        // Niente prompt (è già stato consolidato silenziosamente).
        _uiState.value = _uiState.value.copy(pendingDailyConsolidation = false)
    }

    /** Tap sul pulsante principale (incremento o decremento, in base a reverse). */
    fun tap() {
        val id = selectedCounterId.value
        if (id <= 0) return
        viewModelScope.launch(Dispatchers.IO) {
            val before = repository.getById(id)
            val updated = repository.applyTap(id) ?: return@launch
            // Hot-zone alert: se PRIMA del tap eravamo sotto la zona calda e ORA
            // siamo dentro (o abbiamo appena sforato un LIMIT), notifichiamo.
            val wasHot = before?.isInHotZone() == true ||
                    before?.computeGoalState() == com.mcc.mycounter.data.entities.GoalState.FAILURE
            val isHotNow = updated.isInHotZone() ||
                    updated.computeGoalState() == com.mcc.mycounter.data.entities.GoalState.FAILURE
            if (!wasHot && isHotNow) {
                com.mcc.mycounter.notify.NotificationHelper.showHotZone(appContext, updated)
            }
        }
    }

    /** Tap manuale: applica una variazione esplicita (es. -1 per Undo). */
    fun manualDelta(delta: Int, note: String? = null) {
        val id = selectedCounterId.value
        if (id <= 0) return
        viewModelScope.launch(Dispatchers.IO) { repository.applyManualDelta(id, delta, note) }
    }

    /** Annulla l'eventuale timer Conta Tempo in esecuzione (discard). */
    fun cancelRunningTimer() {
        val id = selectedCounterId.value
        if (id <= 0) return
        viewModelScope.launch(Dispatchers.IO) { repository.cancelRunningTimer(id) }
    }

    fun consolidate(onDone: (Consolidation?) -> Unit = {}) {
        val id = selectedCounterId.value
        if (id <= 0) return
        viewModelScope.launch {
            val pair = withContext(Dispatchers.IO) {
                repository.consolidateWithAchievement(id)
            }
            val cons = pair?.first
            val ach = pair?.second
            // Notifica esito + (se configurato) bozza-mail accountability + webhook.
            if (cons != null && ach != null) {
                val counter = withContext(Dispatchers.IO) { repository.getById(id) }
                if (counter != null) {
                    com.mcc.mycounter.notify.NotificationHelper.showOutcome(appContext, counter, ach)
                    val s = settings.settingsFlow.first()
                    if (s.accountabilityEmailEnabled) {
                        com.mcc.mycounter.notify.AccountabilityMailer.sendIfConfigured(appContext, counter, ach)
                    }
                    val app = appContext.applicationContext as com.mcc.mycounter.MyCounterApplication
                    com.mcc.mycounter.notify.WebhookSender.sendIfEnabled(app, counter, ach)
                }
            }
            _uiState.value = _uiState.value.copy(
                pendingDailyConsolidation = false,
                message = cons?.let { "Periodo consolidato. Counter azzerato." }
            )
            onDone(cons)
        }
    }

    fun resetHistoryAndStats(onDone: (Boolean) -> Unit = {}) {
        val id = selectedCounterId.value
        if (id <= 0) return
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { repository.resetHistoryAndStats(id) }
            _uiState.value = _uiState.value.copy(
                message = if (ok) "Storico e statistiche cancellati."
                else "Per usare il Reset esegui prima un Consolidamento."
            )
            onDone(ok)
        }
    }

    fun saveCounter(counter: Counter, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) { repository.upsert(counter) }
            // Se è il primo counter creato, selezionalo
            if (selectedCounterId.value <= 0) selectCounter(id)
            onSaved(id)
        }
    }

    fun deleteCounter(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.delete(id) }
            // Se era il selezionato, sposta su un altro
            if (selectedCounterId.value == id) {
                val next = withContext(Dispatchers.IO) { repository.getAll().firstOrNull()?.id ?: -1L }
                selectedCounterId.value = next
                settings.updateSelectedCounter(next)
            }
        }
    }

    fun duplicateCounter(id: Long, onDone: (Long?) -> Unit = {}) {
        viewModelScope.launch {
            val newId = withContext(Dispatchers.IO) { repository.duplicate(id) }
            onDone(newId)
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun dismissDailyPrompt() {
        _uiState.value = _uiState.value.copy(pendingDailyConsolidation = false)
    }
}
