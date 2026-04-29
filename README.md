# myCounter by MCC

**myCounter** è un'app Android per creare e gestire **contatori personalizzati** che ti aiutano a monitorare abitudini, consumi, attività ricorrenti e a raggiungere obiettivi. Pensata per essere veloce da usare nella vita quotidiana — un solo TAP per registrare un evento — con un widget sulla home, statistiche dettagliate, report PDF condivisibili, un sistema di obiettivi differenziato che funziona sia per limiti da non superare (es. caffè) sia per traguardi da raggiungere (es. bicchieri d'acqua), notifiche locali, accountability via email a uno o più coach e un webhook globale opzionale per integrarsi con Zapier, n8n, IFTTT o un server custom.

L'app è gemella di [`MyVote_by_mcc`](../MyVote_by_mcc) e ne condivide design, palette ed estetica generale (DexHub design system).

---

## In una riga

> Conta velocemente quello che ti interessa, definisci se vuoi **superare** o **non superare** un obiettivo, e l'app ti accompagna con avvisi, premi, notifiche al coach via email e — se vuoi — eventi automatici verso il tuo workflow tramite webhook.

---

## A chi serve

Pensa alle situazioni in cui vorresti tenere il conto di qualcosa **senza dover aprire una chat con te stesso**. Esempi:

- **Conta caffè / sigarette / drink**: vuoi restare sotto un *limite massimo* giornaliero. → modalità **LIMIT**.
- **Bicchieri d'acqua / passi / push-up / minuti di studio**: vuoi raggiungere un *traguardo minimo*. → modalità **TARGET**.
- **Tempo passato in macchina / al telefono / in pausa**: vuoi cronometrare quanto dura un'attività e sommarla nella giornata. → modalità **Conta Tempo** + obiettivo TARGET o LIMIT in *minuti*.
- **Spesa giornaliera / euro persi al gratta-e-vinci**: counter classico con costo per tap.
- **Quanti caffè ho preso oggi vs. il mio coach mi ha detto di non superarne 3?** → counter LIMIT + email accountability che a fine giornata invia il riepilogo PDF al coach (e magari anche a tua mamma).
- **Voglio che ogni "sforamento" del limite caffè finisca su un foglio Google e che ogni obiettivo "acqua" raggiunto attivi una luce verde in casa** → webhook + tag per-counter → Zapier/n8n routing.

---

## Funzionalità principali

### 1. Contatori multipli
Crea quanti contatori vuoi, ognuno con identità visiva (colore, immagine opzionale sul TAP) e proprie regole (step, partenza, reverse, target, costo, periodicità, modalità).

### 2. TAP rapido
La schermata principale è dominata da un grande pulsante centrale: un tap = un evento. Vibrazione opzionale, contatore live, badge che cambia in base allo stato dell'obiettivo.

### 3. Modalità Conta Tempo
Trasforma il counter in un cronometro: il primo TAP avvia il timer, il secondo lo ferma. Ogni sessione viene salvata nello storico (inizio, fine, durata) e il tempo totale del periodo si accumula. L'obiettivo è espresso in **minuti**. Esempi:
- "Tempo in auto" — obiettivo LIMIT 60 min/giorno
- "Studio profondo" — obiettivo TARGET 90 min/giorno

### 4. Sistema GOAL (tipi di obiettivo)
Ogni counter ha un **`goalType`** che ne definisce la semantica:

| Tipo | Significato | Esempio | SUCCESS quando | FAILURE quando |
|---|---|---|---|---|
| **TARGET** | traguardo da raggiungere | bicchieri d'acqua, passi, allenamenti | `valore ≥ obiettivo` | `valore < obiettivo` a fine periodo |
| **LIMIT**  | tetto da non superare | caffè, sigarette, calorie, soldi spesi | `valore ≤ obiettivo` | `valore > obiettivo` (anche durante il periodo) |
| **NONE**   | nessun obiettivo | semplice conteggio | n/a | n/a |

Ogni TAP ricalcola lo "stato dell'obiettivo" (`GoalState`) tra:
- **PROGRESS** — sotto la soglia di hot zone
- **HOT_ZONE** — 80%-99% del target (incoraggiamento per TARGET, warning per LIMIT)
- **SUCCESS** — target raggiunto (TARGET) oppure ancora dentro il limite (LIMIT)
- **FAILURE** — solo LIMIT, quando si supera il tetto

In TapScreen e nel widget i colori e i messaggi si adattano automaticamente:
- TARGET raggiunto → 🏆 *"Obiettivo raggiunto"*
- LIMIT rispettato → ✓ *"Sotto il limite"*
- LIMIT vicino → ⚠ *"Attenzione: vicino al limite"*
- LIMIT sforato → ⚠ *"Limite superato"* (badge rosso)

### 5. Periodicità & auto-consolidamento
Ogni counter ha una periodicità: **Giornaliera / Settimanale / Mensile / Annuale**. Per Giornaliera e Settimanale, l'app **auto-consolida silenziosamente** il periodo precedente:
- All'avvio dell'app
- A ogni refresh del widget
- A ogni TAP fatto sul widget
- All'apertura di un counter

Il consolidamento crea uno **snapshot** del periodo (con valore finale, n. tap/sessioni, costo, esito) e azzera il counter così la nuova giornata/settimana parte pulita. Lo storico resta intatto e visibile nelle statistiche.

### 6. Achievements & streak
Ad ogni consolidamento viene generato automaticamente un **Achievement** che cattura l'esito (SUCCESS / FAILURE / NEUTRAL) e una **streak** crescente di SUCCESS consecutivi.

Es: *"Caffè — limite rispettato 5 giorni di fila 🔥"*. La streak si azzera al primo FAILURE.

Gli Achievements sono visibili nella card "Streak & ultimo esito" delle Statistiche.

### 7. Notifiche locali
- **Hot-zone alert**: appena entri nell'80% del limite/target dopo un TAP, ricevi una notifica ("Stai per superare il limite di caffè", oppure "Ci sei quasi! 8/10 bicchieri").
- **Notifica esito periodo**: al consolidamento ricevi una notifica con il bilancio (premio o avviso) e la streak corrente.
- **Reminder giornaliero**: alle 22:00 una notifica per ogni counter ti ricorda lo stato di avanzamento ("Mancano 3 bicchieri al tuo obiettivo!").

Le notifiche sono interamente **locali**: niente server, niente account, zero traffico dati. Su Android 13+ l'app chiede il permesso `POST_NOTIFICATIONS` al primo avvio.

### 8. Accountability via email — uno o più destinatari
Ogni counter può avere **una o più email accountability** opzionali (es. il tuo coach, un amico, un genitore). Si configurano nell'editor del counter scrivendo gli indirizzi nel campo "Email coach", separati da **virgola, punto-e-virgola o spazio**. Esempio:
```
coach@example.com, mamma@example.com, fratello@example.com
```

A fine periodo (consolidamento manuale o automatico), per ogni counter con almeno un'email configurata, l'app:
1. Genera un **PDF di riepilogo** del periodo
2. Mostra una notifica "📧 Riepilogo da inviare a 3 destinatari"
3. Tap sulla notifica → apre il client mail di sistema con TUTTI gli indirizzi pre-popolati nel campo "A:", oggetto, corpo e PDF già allegato. **Tu confermi e invii.**

Niente backend, niente login: tu controlli sempre cosa parte.

La funzione ha un **master switch globale** nelle Impostazioni → "Accountability via email" che permette di disabilitarla per tutti i counter (utile, ad esempio, in vacanza) senza dover svuotare il campo email su ognuno.

### 9. Webhook (integrazione esterna automatica)
Per chi vuole automatizzare ulteriormente, l'app supporta un **webhook globale** completamente opzionale e disattivato di default. Quando attivato dalle Impostazioni:

- A fine periodo (consolidamento manuale o automatico) l'app fa una **POST `application/json`** all'endpoint configurato
- Payload con: dati counter, **tag logico** opzionale, tipo obiettivo, valore finale, target, esito, streak, costi, ecc.
- Header `X-myCounter-Signature` con HMAC-SHA256 del body se è impostato un secret (per validazione server-side)
- Compatibile con Zapier, n8n, IFTTT, Make.com, webhook generici, server self-hosted

#### Architettura webhook a due livelli
- **Globale** (Impostazioni → "Webhook"): UN solo endpoint per tutta l'app, attivabile, con eventuale secret HMAC.
- **Per-counter** (Editor counter → "Webhook tag"): un *tag* logico opzionale (es. `vices`, `health`, `work`) che viene incluso nel payload come `counter.tag`. Permette al tuo server downstream di **instradare/filtrare** gli eventi senza dover decodificare l'id o il nome.

#### Configurazione globale
Impostazioni → "Webhook (integrazione esterna)":
1. Attiva il toggle "Abilita webhook"
2. Inserisci l'URL del tuo endpoint (`https://...`)
3. (Opzionale) inserisci un **secret** per la firma HMAC
4. Premi **"Invia ping di test"** per verificare che l'endpoint risponda
5. Salva

#### Configurazione per-counter
Editor del singolo counter → "Webhook tag":
- Inserisci una stringa breve (es. `vices`) per categorizzare il counter
- Lascia vuoto se non ti serve il routing per quel counter
- Più counter possono condividere lo stesso tag (es. `vices` per Caffè, Sigarette, Drink)

#### Payload di esempio
```json
{
  "event": "consolidation",
  "timestamp": 1719753600000,
  "counter": {
    "id": 1,
    "name": "Conta Caffè",
    "tag": "vices",
    "goalType": "LIMIT",
    "periodicity": "DAILY",
    "timeMode": false,
    "startValue": 0,
    "target": 3,
    "finalValue": 4,
    "totalCost": 4.80,
    "currency": "EUR",
    "accountabilityEmails": ["coach@example.com", "mamma@example.com"]
  },
  "achievement": {
    "outcome": "FAILURE",
    "streak": 0,
    "consolidationId": 42,
    "periodEndedAt": 1719753600000
  }
}
```
- Il campo `tag` è presente solo se hai impostato un tag per quel counter.
- Il campo `accountabilityEmails` è presente solo se il counter ha almeno un indirizzo configurato.

#### Headers inviati
- `Content-Type: application/json; charset=UTF-8`
- `User-Agent: myCounter-by-MCC/1.0`
- `X-myCounter-Signature: <hex(HMAC-SHA256(body, secret))>` (solo se secret è impostato)

#### Verifica server-side della firma (pseudo-codice)
```
expected = hex(HMAC_SHA256(rawBody, secret))
if (expected != header['X-myCounter-Signature']) reject
```

#### Esempio di routing per tag (n8n)
```
IF $json.counter.tag == 'vices'    → Slack channel #vizi
IF $json.counter.tag == 'health'   → Google Sheets append
IF $json.counter.tag == 'work'     → Notion database insert
ELSE                                → no-op
```

#### Caratteristiche
- *Best-effort*: nessun retry, niente coda persistente. Se il server è offline il payload è perso ma il consolidamento resta comunque registrato in locale.
- *Privacy-first*: niente account, niente backend di terze parti gestito da noi, niente analytics. Tu controlli URL, secret e tag.
- *Indipendente* dall'email accountability: puoi avere uno, l'altro, entrambi o nessuno.

#### Casi d'uso tipici
- Webhook → Zapier → Google Sheets per loggare automaticamente i tuoi consolidamenti su un foglio
- Webhook → n8n self-hosted → notifica Telegram al tuo gruppo di accountability
- Webhook → server custom → dashboard web personalizzata
- Webhook → IFTTT → "ogni volta che sforo il limite caffè (`tag=vices`), accendi le luci rosse"
- Webhook → Make.com → split per `tag` → 3 flussi diversi (vices/health/work) gestiti indipendentemente

### 10. Storico completo
Ogni TAP è registrato come `TapEvent` (timestamp, valore prima/dopo, eventuale nota, se viene dal widget). Per il Conta Tempo le sessioni mostrano inizio, fine e durata HH:MM:SS.

### 11. Statistiche con grafici interattivi
Schermata Statistiche con:
- **Riepilogo KPI** adattato al tipo di counter (numerico o tempo): valore corrente, n. tap/sessioni, sessione media/max, totale costi, % obiettivo
- **Card "Streak & ultimo esito"** con badge SUCCESS/FAILURE
- **Bar chart** dei tap (o minuti, per Conta Tempo) raggruppati per **Ora (oggi) / Giorno / Settimana / Mese / Anno**, con pinch-to-zoom e scroll orizzontale
- **Line chart** cumulato dell'andamento (in unità adeguata: tap o minuti)
- Pulsante **Report PDF** che apre un dialog con **Salva** (in Downloads) e **Condividi** (sheet di sistema → mail/WhatsApp/Drive/...)

### 12. Widget Android (Glance)
Pulsante TAP rapido sulla home, configurabile per ogni istanza:
- All'aggiunta del widget si apre una **schermata di scelta** del contatore da legare
- Più widget possono coesistere (uno per "Caffè", uno per "Acqua", uno per "Tempo in auto"...)
- Per i counter normali: il pulsante mostra il **valore corrente** del counter
- Per i counter Conta Tempo: il pulsante mostra **Play** ▶ (timer fermo) o **Stop** ◼ (timer in esecuzione, con effetto pulsazione lenta del background); appena premi Stop, per 3 secondi mostra la durata della sessione appena conclusa, poi torna a Play
- Sfondo trasparente per integrarsi con qualsiasi wallpaper
- Pill colorata col nome del contatore per garantire leggibilità su qualsiasi sfondo
- Icona ingranaggio sotto il TAP → apre **Statistiche** del counter (con back che esce direttamente alla home del telefono)

### 13. Consolida & Reset
- **Consolida** (manuale): chiude il periodo corrente, crea uno snapshot + Achievement, azzera. Il counter riparte da `startValue`.
- **Reset**: cancella TUTTO lo storico, le consolidazioni e gli achievements del counter. Disponibile solo dopo almeno un consolidamento (per evitare cancellazioni accidentali).

### 14. Personalizzazione
- **Palette globale** dell'app: Default / Sky / Sunset / Ocean / Forest (DexHub design system)
- **Modalità tema**: Sistema / Chiaro / Scuro
- **Colore TAP** per ogni contatore (11 colori a scelta)
- **Immagine TAP** opzionale (puoi mettere un'icona/foto nel pulsante)
- **Vibrazione al TAP** abilitabile/disabilitabile
- **Conferma sotto zero** per evitare decrementi accidentali

### 15. Schermata Impostazioni — sezioni
La schermata Impostazioni dell'app è organizzata in card:
1. **Tema grafico** — modalità Sistema/Chiaro/Scuro + scelta palette
2. **Comportamento TAP** — toggle vibrazione, toggle conferma sotto zero
3. **Accountability via email** — master switch per attivare/disattivare globalmente l'invio della bozza-mail al coach (anche se l'email è configurata sul counter, se il toggle qui è OFF non parte nulla)
4. **Webhook (integrazione esterna)** — toggle abilita + URL endpoint + secret HMAC opzionale + pulsante "Invia ping di test" + guida completa al payload, agli headers e alla verifica della firma server-side
5. **Salva** + **Ripristina default**

### 16. Schermata Editor del singolo counter — sezioni
1. **Anagrafica** — nome
2. **Modalità** — toggle Conta Tempo (cronometro)
3. **Conteggio** — step, partenza, reverse (disabilitati in Conta Tempo)
4. **Obiettivo & Costi** — target (in unità o minuti se Conta Tempo), costo per tap/minuto
5. **Aspetto** — colore TAP + immagine TAP opzionale
6. **Periodicità** — Giornaliera / Settimanale / Mensile / Annuale
7. **Tipo di obiettivo** — Traguardo (TARGET) / Limite (LIMIT) / Nessuno (NONE)
8. **Webhook tag (opzionale)** — stringa per il routing server-side
9. **Accountability (opzionale)** — una o più email del coach

---

## Architettura

| Layer | Tecnologia |
|---|---|
| Linguaggio | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 (BOM 2025.04.00) |
| Build | AGP 9.1.1 / Gradle 9.3.1 |
| DI | Manuale via `MyCounterApplication` (no Hilt/Koin) |
| Persistenza | Room 2.7.2 (KSP) — DB schema v4 con migrazioni esplicite |
| Settings | DataStore Preferences |
| Background work | WorkManager 2.10.0 (reminder giornaliero) |
| Notifiche | NotificationCompat + canale dedicato |
| Webhook | HttpURLConnection + HMAC-SHA256 (zero dipendenze esterne) |
| Charts | MPAndroidChart (PhilJay v3.1.0) via JitPack |
| Widget | Glance 1.1.1 (`glance-appwidget` + `glance-material3`) |
| Image | Coil 2.7.0 |
| Sharing | FileProvider (PDF tramite content-URI) |
| minSdk / targetSdk | 26 / 36 |

### Modello dati (Room v4)

```
counters
├── id, name, step, startValue, currentValue, reverse
├── dailyTarget, costPerTap, tapColorArgb, tapImageUri
├── periodicity (DAILY/WEEKLY/MONTHLY/YEARLY)
├── timeMode, runningStartedAt              ← Conta Tempo
├── goalType (TARGET/LIMIT/NONE)            ← sistema Goal
├── accountabilityEmail                     ← una o più email coach (csv)
├── webhookTag                              ← tag opzionale per routing webhook
├── createdAt, updatedAt, lastTapAt, lastConsolidatedAt

tap_events
├── id, counterId (FK CASCADE), timestamp, delta, valueBefore, valueAfter
├── note, fromWidget
└── durationMs, sessionStartedAt           ← per sessioni Conta Tempo

consolidations
├── id, counterId (FK CASCADE), closedAt, finalValue
├── tapsCount, totalCost, targetReached
├── periodStartedAt, periodEndedAt, periodicity

achievements
├── id, counterId (FK CASCADE), consolidationId (FK CASCADE)
├── periodEndedAt, outcome (SUCCESS/FAILURE/NEUTRAL)
├── goalType, finalValue, targetValue
├── streak (giorni consecutivi di SUCCESS)
└── createdAt
```

### Migrazioni DB
- **v1 → v2**: aggiunti campi Conta Tempo (`timeMode`, `runningStartedAt`, `durationMs`, `sessionStartedAt`)
- **v2 → v3**: aggiunto sistema Goal + Accountability + tabella achievements
- **v3 → v4**: aggiunto `webhookTag` per il routing server-side

Tutte le migrazioni sono esplicite, **nessuna perdita dati** all'aggiornamento.

---

## Struttura del progetto

```
myCounter/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
└── app/
    ├── build.gradle.kts                    (Compose, Room/KSP, Glance, WorkManager, Coil, MPAndroidChart)
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml             (POST_NOTIFICATIONS, INTERNET, FileProvider, deep-link, widget config activity)
        ├── java/com/mcc/mycounter/
        │   ├── MyCounterApplication.kt     (DI manuale, seed, auto-consolidate at startup, channel + reminder, hook notif/email/webhook)
        │   ├── MainActivity.kt             (NavHost, deep-link da widget, runtime permission notif)
        │   ├── data/
        │   │   ├── AppDatabase.kt          (Room v4, migrations 1→2, 2→3, 3→4)
        │   │   ├── dao/{CounterDao, TapEventDao, ConsolidationDao, AchievementDao}.kt
        │   │   ├── entities/{Counter, TapEvent, Consolidation, Achievement}.kt
        │   │   ├── preferences/SettingsManager.kt   (DataStore: tema, palette, haptic, accountabilityEmailEnabled, webhookEnabled/Url/Secret, ecc.)
        │   │   └── repository/CounterRepository.kt  (single source of truth + creazione achievements)
        │   ├── notify/
        │   │   ├── NotificationHelper.kt   (canale + showHotZone/showOutcome/showReminder)
        │   │   ├── DailyReminderWorker.kt  (WorkManager periodic, scatto 22:00)
        │   │   ├── AccountabilityMailer.kt (PDF + ACTION_SEND multi-destinatario via FileProvider)
        │   │   └── WebhookSender.kt        (POST JSON + HMAC-SHA256 + ping di test)
        │   ├── report/
        │   │   ├── ReportGenerator.kt      (PdfDocument nativo, multi-page, time-aware)
        │   │   └── ReportFileSharer.kt     (Save in Downloads via MediaStore + Share intent)
        │   ├── ui/
        │   │   ├── components/{GradientBackground, SectionCard, PrimaryActionButton, TapColorPicker, DexSwitch}.kt
        │   │   ├── navigation/{Routes, NavGraph}.kt
        │   │   ├── screens/{TapScreen, CounterListScreen, CounterEditorScreen, HistoryScreen, StatsScreen, SettingsScreen}.kt
        │   │   ├── theme/{Color (DexSurface ladder), Theme, Type}.kt
        │   │   └── util/Formatters.kt      (formatDuration, formatCurrency, ecc.)
        │   ├── viewmodel/{CounterViewModel (AndroidViewModel), SettingsViewModel, ViewModelFactory}.kt
        │   └── widget/
        │       ├── CounterWidget.kt        (Glance, layout, state)
        │       ├── CounterWidgetReceiver.kt
        │       ├── TapWidgetAction.kt      (gestisce TAP: applyTap, hot-zone notif, post-stop flash)
        │       ├── WidgetConfigActivity.kt (scelta counter al pinning)
        │       ├── WidgetBindings.kt       (DataStore: appWidgetId → counterId)
        │       ├── WidgetBlinker.kt        (pulsazione lenta dell'icona Stop)
        │       └── WidgetUpdater.kt
        └── res/
            ├── drawable/{ic_launcher_*, ic_widget_play, ic_widget_stop, ic_widget_stop_dim, ic_widget_stats, widget_preview}.xml
            ├── layout/glance_default_loading_layout.xml
            ├── mipmap-*/{ic_launcher, ic_launcher_round}.xml
            ├── values/{colors (DexHub palette), strings, themes}.xml
            ├── values-night/themes.xml
            └── xml/{counter_widget_info, backup_rules, data_extraction_rules, file_provider_paths}.xml
```

---

## Build & Run

### Prerequisiti
- **Android Studio Narwhal+** (compatibile AGP 9.x / Kotlin 2.2.x)
- **JDK 17** (gestito automaticamente via Foojay)
- **Android SDK 36** installata

### Steps
1. Apri Android Studio → *Open* → seleziona la sottocartella `myCounter/` (NON la root del repo)
2. Lascia scaricare le dipendenze (la prima volta scarica anche MPAndroidChart da JitPack — già configurato)
3. Selezionа un emulatore o device fisico (API 26+)
4. ▶ **Run**

### Da CLI
```bash
cd myCounter
./gradlew :app:assembleDebug         # APK debug
./gradlew :app:assembleRelease       # APK release (richiede signing)
./gradlew :app:test                  # Unit test JVM
```

---

## Come usare l'app — flussi rapidi

### Esempio 1 — "Resta sotto i 3 caffè al giorno"
1. Apri l'app → icona lista in alto a destra → FAB "Nuovo contatore"
2. Nome: `Caffè`, Step: `1`, Obiettivo: `3`, **Tipo obiettivo: Limite (non superare)**, Periodicità: `Giornaliera`, Costo per tap: `1.20`
3. (opzionale) **Email del coach**: `coach@example.com, mamma@example.com`
4. (opzionale) **Webhook tag**: `vices`
5. Salva
6. Aggiungi il widget alla home, scegli "Caffè"
7. Ogni caffè = un tap. Al 2° caffè (80% del limite) ricevi una notifica gialla.
8. Se sfori al 4°, badge rosso ⚠ "Limite superato".
9. A mezzanotte l'app consolida silenziosamente: notifica con esito + (se hai messo email) bozza-mail al coach + (se hai abilitato webhook globale) POST al tuo endpoint.
10. Domani il counter parte da 0. La streak SUCCESS cresce di giorno in giorno se rispetti il limite.

### Esempio 2 — "Bevi 10 bicchieri d'acqua al giorno"
1. Stesso flusso ma: Obiettivo `10`, **Tipo obiettivo: Traguardo (raggiungere)**.
2. (opzionale) Webhook tag: `health`
3. A 8/10 (80%) ricevi notifica incoraggiante "Ci sei quasi!"
4. Al 10° tap: badge verde 🏆 "Obiettivo raggiunto", colore primario verde.
5. Se a fine giornata non hai raggiunto 10, FAILURE → la streak si azzera, ricevi notifica motivazionale.

### Esempio 3 — "Misura il tempo perso al telefono"
1. Crea counter `Telefono`, attiva **Conta Tempo**, Obiettivo: `60` (minuti), Tipo obiettivo: **Limite**.
2. Quando sblocchi il telefono: tap Play sul widget. Quando smetti: tap Stop.
3. Il widget mostra Play/Stop con effetto pulsazione mentre il timer è attivo.
4. Stats → granularità Orario → vedi a che ore stai più al telefono.
5. Stats → Granularità Giornaliera → vedi quanti minuti per giorno.

### Esempio 4 — "Routing webhook completo (Zapier)"
1. Configura webhook globale: Impostazioni → "Webhook" → URL Zapier Catch Hook + secret
2. Nei tuoi counter:
   - `Caffè` (LIMIT 3, tag `vices`) — accountability `coach@x.com`
   - `Sigarette` (LIMIT 0, tag `vices`)
   - `Acqua` (TARGET 10, tag `health`)
   - `Pomodori` (TARGET 8, tag `work`)
3. In Zapier: Catch Hook → Filter by `counter.tag` → 3 ramificazioni:
   - `vices` → Slack #vizi + Google Sheet "Vizi 2026"
   - `health` → Apple Health + dashboard Notion "Salute"
   - `work` → calendario riepilogativo settimanale
4. A fine giornata tutti i counter consolidano automaticamente e ognuno arriva nel canale giusto.

---

## Come aggiungere nuovi contatori (programmaticamente)

```kotlin
val app = context.applicationContext as MyCounterApplication
app.repository.upsert(
    Counter(
        name = "Allenamenti",
        step = 1,
        dailyTarget = 5,
        costPerTap = 0.0,
        tapColorArgb = 0xFF10B981.toInt(),
        periodicity = Periodicity.MONTHLY.name,
        goalType = GoalType.TARGET.name,
        webhookTag = "health",
        accountabilityEmail = "coach@example.com"
    )
)
```
(Vedi `MyCounterApplication.seedSampleDataIfEmpty()` per gli esempi seed integrati.)

---

## Roadmap / miglioramenti futuri

1. **Cloud sync nativo** — backend Firebase/proprio con auth, dashboard web realtime e push al coach. Oggi l'integrazione esterna è coperta dal **webhook**: chi vuole un sync vero può collegarsi a Zapier/n8n/server custom.
2. **Widget per-contatore con icona personalizzata** — al posto del valore o del Play/Stop, l'icona del counter.
3. **Notifiche programmate avanzate** — orario reminder configurabile per counter (oggi 22:00 fisso); quiet hours.
4. **Esportazione CSV/JSON** dello storico.
5. **Heatmap stile GitHub** per visualizzare la streak su calendario.
6. **Multi-step / quantità arbitraria** ("aggiungi N tap insieme").
7. **Templates di counter** — preset Salute/Studio/Lavoro pronti da clonare.
8. **Lock-screen widget** (Android 12L/13+).
9. **Accessibility / TalkBack** per il valore corrente e i pulsanti grandi.
10. **Sincronizzazione con Health Connect** (per counter come passi, calorie, ecc.).
11. **Webhook avanzato** — retry queue persistente con WorkManager (oggi è best-effort), payload personalizzabile, eventi aggiuntivi (per-tap, hot-zone, non solo consolidation).
12. **Multi-webhook** — uno per tag invece di uno globale, per chi non vuole un router intermedio.
13. **Filtri server-side per tag direttamente nell'app** (per non inviare webhook a tag specifici se in ferie, ecc.).

---

## Privacy e dati

- **Tutti i dati sono in locale** (SQLite Room nel sandbox dell'app).
- **Nessuna telemetria, nessun analytics, nessun account.**
- Il webhook è **opt-in**: parte solo se TU lo attivi e configuri.
- L'email accountability è **opt-in**: parte solo se TU configuri l'indirizzo + master switch ON.
- Le notifiche sono interamente locali (NotificationManager + WorkManager, no FCM).
- Backup: l'app usa `allowBackup="true"` con `backup_rules.xml` per includere DB e settings nel backup di sistema.

---

## Licenza

Vedi `LICENSE` nella root del repo.

---

## Sviluppato da

**MCC** — gemello di [`MyVote_by_mcc`](../MyVote_by_mcc), parte della suite di app personali "by MCC".
