package com.mcc.mycounter.ui.navigation

/**
 * Elenco centralizzato delle route di navigazione.
 */
object Routes {
    /** Schermata principale: il TAP del counter selezionato. */
    const val TAP = "tap"

    /** Configuratore contatori: lista + crea/duplica/cancella. */
    const val COUNTER_LIST = "counter_list"

    /** Editor del singolo contatore. counterId = -1 → nuovo. */
    const val COUNTER_EDITOR = "counter_editor?counterId={counterId}"

    /** Storico tap del counter selezionato. */
    const val HISTORY = "history"

    /** Statistiche del counter selezionato. */
    const val STATS = "stats"

    /** Impostazioni globali (tema, palette, ecc.). */
    const val SETTINGS = "settings"

    fun counterEditor(counterId: Long? = null): String =
        if (counterId == null) "counter_editor?counterId=-1"
        else "counter_editor?counterId=$counterId"
}
