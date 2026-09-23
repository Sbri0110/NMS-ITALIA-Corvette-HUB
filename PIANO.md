# NMS ITALIA Corvette HUB — Piano di realizzazione

> Derivato da `PROMPT-MASTER.md`. Le decisioni qui elencate sono state confermate
> da Sbri il 2026-09-23. La ricognizione tecnica (percorsi, formato, campo del
> nome) è stata eseguita sulla macchina di sviluppo e i risultati sono citati
> con il percorso o il comando che li prova.

---

## 1. Decisioni bloccate

| Decisione | Scelta | Motivo |
|---|---|---|
| Stack | **Java 8 + Swing + FlatLaf** | Riuso diretto del parser `nomanssave` di Atlante; stesso look & feel; competenza già presente |
| Sede del progetto | cartella di lavoro locale dello sviluppatore | Scelta di Sbri |
| Distribuzione | **JAR portabile + `.bat`** | Nessuna installazione, nessun registro, coerente con Atlante |
| Piattaforme | **Solo PC**: Steam, GOG, Epic, Xbox app su PC | PS4 e Switch fuori perimetro |
| Compilatore | `tools/ecj.jar` (ECJ, compatibile Java 8) | Sulla macchina c'è solo il JRE 1.8.0_503, non `javac` |
| Runtime target | Java 8 (JRE 1.8.0_503) | Come Atlante |

---

## 2. Fatti verificati sul campo

### 2.1 Percorsi dei salvataggi

| Piattaforma | Percorso | Stato |
|---|---|---|
| Steam / GOG / Epic | `%APPDATA%\HelloGames\NMS\<profilo>\save*.hg` | percorso noto; sul PC di sviluppo esiste ma contiene solo `cache` |
| **Xbox app su PC** | `%LOCALAPPDATA%\Packages\HelloGames.NoMansSky_bs190hzg1sesy\SystemAppData\wgs\<id>\<container>\<guid>` | **verificato, popolato** |
| Xbox app su PC (alternativo) | `...\SystemAppData\xgs\<id>\Slot{N}Manual\|Auto\data` | **verificato** |

Prova: i payload iniziano con `e5 a1 ed fe` (= `0xFEEDA1E5`), il magic dei
salvataggi NMS, e misurano 1.743.323 e 1.743.369 byte. **Sono `.hg` validi in
chiaro**, non compressi: solo con nome file GUID e incapsulati nei contenitori.

Sul PC di sviluppo: **slot 3 popolato** (`Slot3Manual` + `Slot3Auto`),
gioco installato in `C:\XboxGames\No Man's Sky\Content\Binaries\NMS.exe`.

### 2.2 Il processo di gioco

`NMS.exe`. Da rilevare con `tasklist` / `ProcessHandle` prima di ogni scrittura.

### 2.3 Campo del nome della Corvette

La Corvette è una **base** in `PlayerStateData.PersistentPlayerBases[]` con
`BaseType.PersistentBaseTypes == "PlayerShipBase"`; `UserData` è l'indice in
`ShipOwnership[]`. Il nome sta in **`base.Name`**, replicato in
`ShipOwnership[i].Name`. Modello navi: `gL.rE` =
`MODELS/COMMON/SPACECRAFT/BIGGS/BIGGS.SCENE.MBIN`.
Fonte: `_work/src_patch/nomanssave/gz.java`, righe 1059–1146.
**Da confermare a video in gioco.**

### 2.4 Catalogo dei moduli

`CORVETTE_STATION_PARTS` in `gz.java` (righe 861–892) contiene **140 moduli**:

| Categoria | N. | Prefissi |
|---|---|---|
| Cockpit | 3 | `^B_COK_A/B/D` |
| Hab, walkway, access bay | 10 | `^B_HAB*`, `^B_ALK_*` |
| Landing gear | 4 | `^B_LAN_B`, `^B_LND_A/B/C` |
| Wing, motori, booster | 20 | `^B_WNG_*` |
| Thruster posteriori | 8 | `^B_TRU_A..H` |
| Armi | 6 | `^B_TUR_A..E`, `^B_DECO_I` |
| Scudi | 5 | `^B_SHL_A..E` |
| Reattori | 4 | `^B_GEN_0..3` |
| Connettori e giunti | 20 | `^B_CON*`, `^B_CON2_*` |
| Decori scafo | 19 | `^B_DECO_A..T` |
| Corazze e profili | 41 | `^B_STR_*` |

**Da chiarire**: il documento parla di 159 moduli. 159 è probabilmente il
catalogo completo del terminale (Corvette + base + stazione), non i soli moduli
Corvette. Decidere quale denominatore mostrare nel contatore.

---

## 3. Architettura

```
corvette-hub/
  src/it/nmsitalia/corvettehub/
    app/      MainFrame, tema FlatLaf, icone, barra di stato
    detect/   rilevamento candidati + validazione della cartella
    save/     storage provider: Steam/GOG/Epic, Xbox WGS
    conv/     conversione .hg <-> modello   [da nomanssave, sola lettura]
    domain/   Corvette, Parte, Deposito, Compatibilita
    build/    lettura/scrittura del wrapper JSON di progetto
    safety/   backup, scrittura atomica, verifica in rilettura
    ui/       le 5 schede: Importa, Esporta, Rinomina, Deposito, Libreria
  lib/        corvette-parser.jar
  Builds/     libreria locale dei progetti
  Backup/     backup automatici con data e ora
  tools/      ecj.jar, cfr.jar
```

### 3.1 Il parser condiviso

Estratto da `Atlante.jar` in `lib/corvette-parser.jar`, **sola lettura/scrittura
del formato**. Classi incluse (≈65 KB di bytecode + 2,27 MB di dati):

| Classe | Ruolo |
|---|---|
| `eY` | modello JSON dell'albero di salvataggio |
| `eV` | array JSON |
| `eC`, `hh` | SpookyHash V2 base-68: chiavi offuscate |
| `hm` | formato `HGSAVEV2\0` |
| `gX` | stream LZ4 (magic `0xFEEDA1E5`) |
| `ha` | formato legacy |
| `fq_0` | interfaccia storage |
| `fJ` | storage Steam / GOG / Epic |
| `fT` | storage WGS (Xbox app su PC) |
| `db/jsonmap.txt` | 28 KB, mappa chiavi → nomi |
| `db/jsonmapac.txt` | 3,8 KB, mappa per `accountdata` |
| `db/items.xml` | 2,2 MB, nomi e categorie degli oggetti |

**Esclusa di proposito**: `gz` (il controller dell'editor), perché contiene
`populateStationStorageParts()`, `unlockAllStationAndBaseProducts()` e la
versione di `importCorvette` che **sblocca** le parti. Il nuovo tool non deve
poter compilare nessuna di quelle funzioni: se la classe non è nel JAR, il
divieto della sezione 2 della specifica è garantito dal compilatore, non dalla
buona volontà.

### 3.2 Perché l'import va riscritto

`gz.importCorvetteToPlayerState()` scrive `Objects`, `Name`,
`LastUpdateTimestamp`, **e anche** aggiunge le parti mancanti a
`SeenBaseBuildingObjects` e modifica `ShipOwnership`. Le ultime due sono
esattamente il comportamento vietato. Si riscrive un import che tocca **solo**
`base.Name` e `base.Objects` della Corvette scelta.

---

## 4. Fasi

### Fase 1 — Fondamenta (sola lettura, zero rischio)

1. Estrazione di `lib/corvette-parser.jar` da `Atlante.jar`.
2. Rilevamento automatico: candidati Steam/GOG/Epic e WGS, **validati** provando
   a convertire un file e riconoscendo la struttura (non solo l'esistenza della
   cartella).
3. Elenco slot: nome giocatore, data e ora, ore di gioco, modalità, ed
   eventuale "corrotto/non leggibile".
4. Elenco Corvette dello slot, con etichetta chiara su quella **attiva**.
5. Fallback manuale con scelta memorizzata in `CorvetteHUB.conf`.

**Consegna**: un JAR che si apre, trova da solo il salvataggio slot 3 su Xbox,
e mostra le Corvette. Nessuna scrittura, nessun rischio.
**Test**: sul salvataggio reale MS Store (`Slot3Manual` / `Slot3Auto`).

### Fase 2 — Funzioni non distruttive

1. Export: estrazione di `Objects[]` + compilazione del wrapper ufficiale
   (`format`, `version`, `name`, `author`, `created_utc`).
2. Libreria locale `Builds\` con elenco, ricerca per nome, eliminazione con
   conferma.
3. Scheda Deposito: lettura di `CorvetteStorageInventory` +
   `CorvetteStorageLayout` + `ValidSlotIndices`, raggruppamento per categoria,
   contatore, filtro e ricerca.
4. Esportazione dell'elenco del deposito in testo o CSV.

**Consegna**: export e deposito funzionanti, ancora zero scritture.

### Fase 3 — Le scritture (la fase delicata)

1. **Rinomina** per prima: è la scrittura più semplice e collauda il ciclo
   backup → scrittura atomica → verifica in rilettura.
2. **Import** con il controllo di compatibilità a tre stati (nel deposito /
   sbloccata ma non in deposito / mancante), calcolato incrociando il deposito
   con `KnownProducts` e `SeenBaseBuildingObjects`.
3. Blocco se `NMS.exe` è in esecuzione; blocco della Corvette attiva.
4. Backup obbligatorio: se fallisce, l'operazione si interrompe.
5. Entrambi i file dello slot aggiornati insieme, o nessuno.

**Consegna**: ciclo di scrittura completo, con ripristino backup funzionante.

### Fase 4 — Rifinitura

Tema FlatLaf premium, icone dei moduli, anteprime disegnate delle Corvette
(vista semplificata dei moduli nello spazio), log testuale, ripristino backup
con elenco, limite di conservazione configurabile.

### Fase 5 — Distribuzione

JAR + `.bat`, README in italiano, pagina di rilascio GitHub, coerenza con la
pubblicazione di Atlante.

---

## 5. Punti ancora aperti

| # | Punto | Come si chiude |
|---|---|---|
| 1 | 140 o 159 moduli nel contatore? | Decisione di Sbri |
| 2 | `base.Name` è davvero il campo che il gioco mostra? | Rinominare una Corvette usa e getta e riaprire il gioco |
| 3 | `KnownProducts` e `SeenBaseBuildingObjects`: array di stringhe o di oggetti? | Ispezione diretta del salvataggio reale |
| 4 | Colori e posizioni dei moduli: quali campi contengono `Position`, `Forward`, `Up`? | Ispezione di una build esportata reale |
| 5 | Esiste una Corvette nel salvataggio attuale, per fare da cavia? | Fase 1 |

---

## 6. Criteri di accettazione

Quelli della sezione 9 di `PROMPT-MASTER.md`, più il vincolo architetturale
introdotto qui: **nel JAR del tool non deve esistere alcuna funzione che
sblocchi, aggiunga o riempia moduli** — verificabile elencando le classi del
JAR compilato.

---

## 7. Requisito aggiunto da Sbri (2026-09-23)

Oltre al deposito dei moduli della Stazione Spaziale, il tool deve mostrare:

1. le **tecnologie installate**;
2. i **depositi delle basi**;
3. il **deposito della Corvette**.

E la grafica deve usare le **icone del gioco per tutto**: potenziamenti,
tecnologie, materiali, elementi, moduli.

Tutti e tre sono già leggibili e implementati (classe
`LettoreInventari`). Le icone sono disponibili: vedi sezione 9.

---

## 8. Fase 1 — CONCLUSA (2026-09-23)

Consegna: applicazione che si avvia, rileva da sola il salvataggio Xbox, elenca
slot e Corvette, e mostra tecnologie e depositi. **Nessuna scrittura.**

### Cosa è stato verificato eseguendo davvero il codice

Sul salvataggio Xbox `Slot3Manual` / `Slot3Auto`:

| Dato | Valore misurato |
|---|---|
| Piattaforma | Xbox Game Pass |
| Slot | 3 (due file: Manual e Auto) |
| Modalità | NORMAL |
| Nome salvataggio | NEMESI OSCURA |
| Ore giocate | 669511 secondi = 185 ore e 58 minuti |
| `PrimaryShip` | 6 (la nave in uso NON è la Corvette) |
| Corvette trovate | 1 |
| Corvette | `#30 Crimson Executioner`, base #16, nave #0, 1581 moduli, 63 distinti |
| Deposito Corvette | 141 slot occupati, moduli a stack da 500 |
| Tecnologie | 44 moduli `Technology` su `Inventory_TechOnly` della nave #0 |
| Navi in `ShipOwnership` | 12 |
| Basi in `PersistentPlayerBases` | 34 |

### Correzioni rispetto a quanto scritto sopra

- **Le ore di gioco NON stanno in `PlayerStateData.TotalPlayTime`** (che
  restituisce 0) ma in **`CommonStateData.TotalPlayTime`**, in secondi.
- **Le classi del parser non sono 10 ma 85** (73 `nomanssave` + 12 LZ4): la
  chiusura va calcolata seguendo TUTTE le dipendenze, non solo quelle dirette.
- **Serve un segnaposto `nomanssave.Application`**: vedi sezione 10.
- **I nomi delle classi decompilate non sono affidabili**: il decompilatore
  aggiunge il suffisso `_0` per evitare collisioni (`fq_0` è in realtà `fq`,
  `ft_0` è `ft`, `fn_0` è `fn`, `fs_0` è `fs`). Usare sempre i nomi del JAR.

### Sul numero dei moduli (questione aperta, ora con più dati)

Ci sono **quattro** numeri diversi, per quattro cose diverse:

| Numero | Cos'è | Fonte |
|---|---|---|
| 140 | moduli Corvette nell'array dell'editor | `CORVETTE_STATION_PARTS` in `gz.java` |
| 141 | stack presenti nel deposito di questo salvataggio | misurato su `CorvetteStorageInventory` |
| 159 | catalogo del Terminale di Personalizzazione | messaggio dell'interfaccia di Atlante |
| 664 | parti costruibili su una nave (622 strutturali + 42 decorative) | `parts_catalog.json` da `basebuildingobjectstable` |
| 757 | tutte le voci `B_*` del catalogo basi | `parts_catalog.json` |

**Decisione da prendere**: quale denominatore mostrare nel contatore del
deposito. Proposta: **664**, perché è il numero di parti che il gioco accetta
davvero su una Corvette, e distinguere le strutturali dalle decorative.

---

## 9. Icone del gioco (scoperta)

Dentro `Atlante.jar` ci sono **3211 icone PNG** degli oggetti, in
`nomanssave/icons/`, nominate `<TIPO>-<ID>.PNG`:

| Prefisso | N. | Contenuto |
|---|---|---|
| `PRODUCT-` | 2806 | prodotti, moduli di costruzione, parti di Corvette |
| `TECHNOLOGY-` | 247 | tecnologie e potenziamenti |
| `SUBSTANCE-` | 100 | elementi e materiali |
| `TECHBOX-` | 24 | moduli tecnologici |
| `UI-` | 33 | interfaccia |

Coprono **tutto** ciò che serve: moduli Corvette (`PRODUCT-B_COK_A.PNG`),
tecnologie (`TECHNOLOGY-LAUNCHER.PNG`), elementi (`SUBSTANCE-OXYGEN.PNG`).
Nessun problema di licenza: sono già parte del progetto di Sbri.

Peso: 66 MB. Sono in `lib/nms-icons.jar`, separato dal parser, così una
distribuzione "leggera" può ometterlo.

---

## 10. Trappole incontrate (da non ripetere)

1. **208 coppie di classi che differiscono solo per maiuscole** nel JAR di
   Atlante (`a.class` e `A.class` sono classi diverse). Estrarre il JAR su
   Windows le sovrascrive e si ottengono classi sbagliate. Sintomo:
   `eV.class` pesa 765 byte invece di 5090. **Costruire le librerie leggendo
   gli archivi direttamente, mai passando dal filesystem.**

2. **`nomanssave.eC` ha una dipendenza nell'inizializzatore statico** da
   `Application.class`, usata per risolvere il percorso di `db/jsonmap.txt`.
   Senza, la conversione fallisce con `NoClassDefFoundError` su qualunque
   salvataggio. Rimedio: `src-stub/nomanssave/Application.java`, un segnaposto
   minimo che non contiene nulla dell'editor.

3. **FlatLaf carica le proprie UI per nome, dentro i file `.properties`.**
   Un'analisi delle dipendenze dal bytecode non le vede. Va incluso **tutto**
   `com/formdev/`, classi e risorse: 393 voci, non 141.

4. **Il parser cattura le eccezioni e le silenzia.** Per vedere gli stack trace
   serve `hc.k(fileDiLog)`: senza, l'errore è solo "a read error".

5. **Le classi della serie `f` decompilate hanno nomi diversi da quelli reali.**
   Compilare con `fq_0`/`ft_0`/`fs_0`/`fn_0` produce errori di risoluzione:
   i nomi veri sono `fq`/`ft`/`fs`/`fn`.

---

## 11. Fase 2 — CONCLUSA (2026-09-23)

Funzioni non distruttive. **Nessuna scrittura sul salvataggio.**

### Interfaccia ristrutturata in due schermate

Su richiesta di Sbri: tutto in una schermata era confuso.

1. **Schermata 1 — Scelta del salvataggio.** Nessun riferimento alle Corvette.
   Elenco degli slot con modalità, nome, ore e data. Pulsante *Continua*.
2. **Schermata 2 — Hub della Corvette.** Cinque schede (Importa, Esporta,
   Rinomina, Deposito, Libreria). In alto resta sempre lo slot di lavoro, con
   il pulsante *Cambia salvataggio* per tornare indietro.

### Cosa è stato implementato

| Componente | Ruolo |
|---|---|
| `domain/CatalogoParti` | Carica `res/parts_catalog.json`, 661 parti, 20 categorie |
| `domain/WrapperBuild` | Legge e scrive il wrapper ufficiale, con scrittura atomica |
| `domain/Compatibilita` | Il controllo a tre stati |
| `ui/Icone` | Carica le 3211 icone ufficiali, con ripiego disegnato |
| `ui/Anteprima` | Disegna la Corvette dalle posizioni reali dei moduli |
| `ui/SchermataSalvataggi` | Prima schermata |
| `ui/SchermataCorvette` | Seconda schermata, con le cinque schede |
| `ui/SchedaEsporta` | Export con anteprima |
| `ui/SchedaDeposito` | Deposito con icone, filtro, ricerca, compatibilità, CSV |
| `ui/SchedaLibreria` | Progetti con anteprima, ricerca, eliminazione |

### Verifiche eseguite sul salvataggio reale

| Prova | Esito |
|---|---|
| Catalogo | 661 totali = 622 strutturali + 42 decorative − 3 doppioni |
| Icone ufficiali | `^B_COK_A`, `^LAUNCHER`, `^OXYGEN` → 128×128 dalla libreria |
| Ripiego | `^UP_LAUN4#67946` e identificativi inesistenti → icona disegnata 24×24 |
| Export | 1581 moduli → 591 KB, wrapper `NMS-CorvetteBuild` v1 completo |
| Rilettura | formato, versione, nome, autore, data e 1581 moduli ritrovati |
| Compatibilità | 63 parti richieste → 16 nel deposito, 46 sbloccate, 1 mancante |
| Build della community | `#30 Crimson Executioner - Construction Data.json` letto: 1583 moduli, 65 tipi |
| Seconda schermata | Costruita, disposta e aggiornata senza errori |

### Formato del file esportato (verificato)

```json
{
	"format": "NMS-CorvetteBuild",
	"version": 1,
	"name": "#30 Crimson Executioner",
	"author": "Sbri",
	"created_utc": "2026-09-22T23:54:28Z",
	"objects": [ { "Timestamp", "ObjectID", "UserData", "Position", "Up", "At" } ]
}
```

### Decisione presa sul contatore

**661**, con distinzione fra strutturali (622) e decorative (42). Sbri ha
scelto questa strada: è il numero di parti che il gioco accetta davvero su una
Corvette, e permette di separare le due nature.

---

## 12. Fase 3 — RINOMINA (implementata il 2026-09-23)

La prima operazione che **scrive** sul salvataggio. Implementata e verificata su
copie; il salvataggio vero non è mai stato toccato.

### Il blocco trovato prima di scrivere

La scrittura della libreria di conversione **non è utilizzabile**: riscrive il
descrittore del contenitore in una forma diversa da quella del gioco (scrive la
dimensione compressa invece della decompressa, e perde 80 byte di coda che
contengono la difficoltà di gioco). Tutto documentato in
`docs/SCRITTURA-WGS.md`, con le prove su tre fonti indipendenti.

### Lo scrittore corretto

`src/it/nmsitalia/corvettehub/safety/ScrittoreSalvataggio.java`

1. Controlla che il gioco sia chiuso.
2. Per **entrambi** i file dello slot: legge il modello, lo serializza nella
   forma offuscata (`fj.g`) e ne ricava la forma esatta del payload.
3. **Identifica il contenitore dal contenuto**, non dal nome: i due file di uno
   slot hanno lo stesso nome salvataggio e la stessa descrizione, quindi la
   ricerca per nome cade sul file sbagliato.
4. Backup completo con verifica SHA-256 di ogni file; se fallisce, si ferma.
5. Modifica, riserializza, ricomprime con lo scrittore LZ4 della libreria.
6. Scrive il payload con file temporaneo e sostituzione.
7. **Descrittore: parte da quello originale a 360 byte** e ne cambia solo il
   campo dimensione a offset 16. La coda di 80 byte resta intatta.
8. **`containers.index`: cambia solo la dimensione totale** del contenitore.
9. Rilegge e confronta byte per byte; se qualcosa non torna, ripristina il
   backup da solo.

### Verifiche eseguite

| Prova | Esito |
|---|---|
| Round-trip senza modifiche | 25.911.324 caratteri **identici** |
| Le 15 chiavi non mappate | **non si perdono** |
| Rinomina su copia | entrambi i file, nome riletto corretto |
| Descrittore | 360 byte, coda di 80 byte conservata |
| Altri file | **invariati**, byte per byte |
| `containers.index` | stessa dimensione, 2 campi aggiornati |
| Backup | creato e verificato con SHA-256 |
| Salvataggio vero | **mai toccato** |

### Cosa manca, e va verificato da Sbri

**La prova in gioco.** Il tool riscrive il contenitore nel formato del gioco, ma
solo il gioco può confermare che lo accetti. Procedura: aprire `CorvetteHUB.bat`,
scegliere lo slot, scheda **Rinomina**, cambiare il nome, poi aprire No Man's Sky
e controllare. Se qualcosa non va, il backup è in `Backup/`.

### Resta da fare nella Fase 3

L'**import** di un progetto, con il controllo di compatibilità già pronto e le
stesse cautele. E il ripristino di un backup dal programma.

### Trappola nota per l'import

`gz.importCorvetteToPlayerState()` di Atlante scrive anche
`SeenBaseBuildingObjects` e `ShipOwnership`, cioè **sblocca le parti**. La
specifica lo vieta. L'import va riscritto toccando **solo** `base.Name` e
`base.Objects` della Corvette scelta.

### Due nomi, una domanda aperta

La Corvette ha il nome in due posti, e sono diversi:
`PersistentPlayerBases[i].Name` = `#30 Crimson Executioner`,
`ShipOwnership[0].Name` = `CORRIDORE TRA LE STELLE`. La rinomina li cambia
**entrambi**. Resta da capire quale dei due mostra il gioco: si vede solo
provando.

