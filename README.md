# myCounter by MCC

App Android per creare e gestire **contatori personalizzati** (es. "Conta Sigarette", "Caffè", ...).
Costruita con lo **stesso layout, stile, pattern di navigazione, gestione temi** dell'app
[`MyVote_by_mcc`](../MyVote_by_mcc): Kotlin + Jetpack Compose + Material 3, single-Activity con
NavHost, Room come database locale, DataStore per le preferenze, MPAndroidChart per le statistiche
e Glance per il widget Android.

All'apertura, l'app va **direttamente alla schermata TAP** dell'ultimo contatore selezionato.

## Funzionalità principali

- **Contatori multipli**: ogni contatore è una entità configurabile (`Counter`).
- **Configurazione per contatore**:
  - Nome
  - Tap da (incremento per tap, configurabile)
  - Partenza (valore iniziale)
  - Conteggio a rovescia (boolean) — se `true` il TAP DECREMENTA
  - Obiettivo (numero) — quando il valore arriva all'80% entra in **zona calda**, al 100% mostra "Obiettivo raggiunto"
  - Costo per tap — il totale `currentValue * costPerTap` è mostrato in TapScreen e Statistiche
  - Colore TAP — il pulsante principale assume questo colore (e il gradient di sfondo si adatta)
  - Immagine TAP — opzionale, viene mostrata dentro il pulsante
  - Periodicità: Giornaliera / Mensile / Annuale (la giornaliera attiva il prompt di consolidamento)
- **TapScreen**: pulsante centrale grande, valore corrente, hot zone animata, totale costo, accessi rapidi a Lista/Statistiche/Storico/Impostazioni.
- **Configuratore contatori**: lista contatori con select/edit/duplica/elimina.
- **Storico**: tutti i tap effettuati con data/ora, valore prima/dopo e nota opzionale.
- **Statistiche**: grafico a barre per giorno/mese/anno + line chart cumulato (MPAndroidChart).
- **Consolida**: salva uno snapshot del periodo e azzera il counter; lo storico resta consultabile.
- **Reset**: cancella storico e statistiche del contatore (abilitato **solo dopo** un consolidamento).
- **Widget Android (Glance)**: pulsante TAP rapido per il counter selezionato, icona dell'app che apre Statistiche+Storico.

## Stack

| Componente | Tecnologia |
|------------|------------|
| Linguaggio | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 (BOM 2025.04.00) |
| Build | AGP 9.1.1 / Gradle 9.3.1 |
| DI | Manuale via `MyCounterApplication` (no Hilt/Koin) |
| DB | Room 2.7.2 (KSP) |
| Settings | DataStore Preferences |
| Charts | MPAndroidChart (PhilJay v3.1.0) |
| Widget | Glance 1.1.1 |
| Image | Coil 2.7.0 |
| minSdk / targetSdk | 26 / 36 |

## Struttura del progetto

```
myCounter/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   ├── wrapper/gradle-wrapper.properties
│   └── gradle-daemon-jvm.properties
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/mcc/mycounter/
        │   │   ├── MyCounterApplication.kt
        │   │   ├── MainActivity.kt
        │   │   ├── data/
        │   │   │   ├── AppDatabase.kt
        │   │   │   ├── dao/{CounterDao,TapEventDao,ConsolidationDao}.kt
        │   │   │   ├── entities/{Counter,TapEvent,Consolidation}.kt
        │   │   │   ├── preferences/SettingsManager.kt
        │   │   │   └── repository/CounterRepository.kt
        │   │   ├── ui/
        │   │   │   ├── components/{GradientBackground,SectionCard,PrimaryActionButton,TapColorPicker}.kt
        │   │   │   ├── navigation/{Routes,NavGraph}.kt
        │   │   │   ├── screens/{TapScreen,CounterListScreen,CounterEditorScreen,HistoryScreen,StatsScreen,SettingsScreen}.kt
        │   │   │   ├── theme/{Color,Theme,Type}.kt
        │   │   │   └── util/Formatters.kt
        │   │   ├── viewmodel/{CounterViewModel,SettingsViewModel,ViewModelFactory}.kt
        │   │   └── widget/{CounterWidget,CounterWidgetReceiver,TapWidgetAction,WidgetUpdater}.kt
        │   └── res/
        │       ├── drawable/{ic_launcher_background,ic_launcher_foreground,widget_preview}.xml
        │       ├── layout/glance_default_loading_layout.xml
        │       ├── mipmap-*/{ic_launcher,ic_launcher_round}.xml
        │       ├── values/{colors,strings,themes}.xml
        │       ├── values-night/themes.xml
        │       └── xml/{counter_widget_info,backup_rules,data_extraction_rules}.xml
        └── test/java/com/mcc/mycounter/data/CounterMathTest.kt
```

## Istruzioni di build

### Prerequisiti
- **Android Studio Narwhal+** (compatibile con AGP 9.x, Kotlin 2.2.x).
- **JDK 17** (gestito automaticamente via Foojay/`gradle-daemon-jvm.properties`).
- **Android SDK 36** installato (da SDK Manager).

### Steps
1. Apri Android Studio → *Open* → seleziona la cartella `myCounter/`.
2. Lascia che Gradle scarichi le dipendenze (la prima volta scarica anche MPAndroidChart da JitPack — già configurato in `settings.gradle.kts`).
3. In `local.properties` decommenta la riga `sdk.dir=` e inserisci il path della tua Android SDK (di solito Android Studio lo fa da solo al primo open).
4. Selezionа un emulatore o device fisico (API 26+) e premi **Run ▶**.

### Build da CLI
```bash
cd myCounter
./gradlew :app:assembleDebug         # APK debug
./gradlew :app:assembleRelease       # APK release (firma da configurare)
./gradlew :app:test                  # Unit test JVM
```
L'APK debug si trova in `app/build/outputs/apk/debug/app-debug.apk`.

## Come aggiungere nuovi contatori

Da app:
1. Apri il **Configuratore contatori** (icona lista nella TopAppBar della TapScreen).
2. Tocca il FAB **"Nuovo contatore"** in basso a destra.
3. Compila i campi: nome, step (`Tap da`), partenza, eventuali obiettivo / costo / colore / immagine, periodicità.
4. Tocca **"Crea contatore"**: il nuovo contatore viene salvato in Room e selezionato automaticamente.

Da codice (per i seed o test programmatici):
```kotlin
val app = context.applicationContext as MyCounterApplication
app.repository.upsert(
    Counter(
        name = "Allenamenti",
        step = 1,
        dailyTarget = 5,
        costPerTap = 0.0,
        tapColorArgb = 0xFF2E7D32.toInt(),
        periodicity = Periodicity.MONTHLY.name
    )
)
```
(Vedi `MyCounterApplication.seedSampleDataIfEmpty()` per l'esempio integrato).

## Widget

L'app installa automaticamente un *widget provider* `CounterWidget`. Per aggiungerlo:
1. Long-press sulla home Android → **Widget**.
2. Cerca **myCounter by MCC** → trascina la mini-anteprima sulla home.
3. Il widget mostra il contatore selezionato globalmente nell'app. Tap sul cerchio = TAP, tap sull'icona dell'app = apre Statistiche+Storico.

Per pinnare istanze del widget collegate a counter diversi (uno per ciascuno), si può estendere con una `WidgetConfigurationActivity` (vedi sezione *Miglioramenti futuri*).

## Suggerimenti per miglioramenti futuri

1. **Widget per-contatore**: aggiungere una `ConfigurationActivity` lanciata al pin del widget, salvando una mappa `widgetId -> counterId` in DataStore così che ogni istanza del widget sia legata al proprio counter.
2. **Notifiche di obiettivo**: usare `WorkManager` per notificare al raggiungimento dell'obiettivo o per ricordare il consolidamento mensile/annuale.
3. **Esportazione CSV/JSON**: bottone in Storico per esportare i tap del contatore (utile per analisi esterne).
4. **Sincronizzazione cloud (opzionale)**: integrare Firebase Firestore o un backend custom per il backup/sync multi-dispositivo.
5. **Grafici aggiuntivi**: pie chart per la ripartizione dei tap per fascia oraria; heatmap stile GitHub contributions.
6. **Multi-step / quantità arbitraria**: dialog per "aggiungi N tap insieme" (es. + 5).
7. **Templates**: come MyVote ha i template, qui si potrebbero introdurre "preset di contatore" (es. preset *Salute*, *Studio*, *Lavoro*).
8. **Dark UI per il widget**: oggi Glance segue il tema sistema; aggiungere palette mirate al widget.
9. **Lock-screen widget** (Android 12L / 13+).
10. **Accessibility**: contentDescription estesi su pulsanti grandi e supporto TalkBack per il valore corrente.

## Licenza

Vedi LICENSE nella root del repo.
