# Revisione 1.2.0 — 9 ottobre 2026

Revisione dei sorgenti Java, del percorso di avvio e della compilazione;
restyle completo delle due schermate e delle sette schede. Il JAR nella
cartella del programma è stato ricompilato alla versione 1.2.0.

## Interfaccia

- Palette blu notte e oro, testata persistente, icone vettoriali, navigazione
  laterale, statistiche a carte, griglie e anteprime coordinate.
- Schermata iniziale ridisegnata con introduzione e selezione della partita.
- Pulsante **Discord NMS ITALIA** visibile in entrambe le schermate:
  `https://discord.gg/uvDTR3wRMg`. Apre il browser solo dopo il clic;
  se il browser non è disponibile, mostra un indirizzo selezionabile.
- Rilevamento, lettura e ricaricamento dei salvataggi fuori dal thread grafico.
- Comandi bloccati durante lettura e scrittura; avviso prima di abbandonare
  modifiche non salvate cambiando nave, scheda, partita o chiudendo.
- Libreria collegata alla scheda Importa, anteprime centrate e disegno della
  griglia limitato anche per coordinate anomale.

## Bug corretti

- Il cambio di Corvette ora ricrea sia l'inventario sia le tecnologie:
  la disposizione della nave precedente non viene riutilizzata.
- Rinomina ed Esporta si abilitano correttamente all'arrivo dei dati e
  rispettano il blocco della Corvette in uso.
- I callback delle schede inattive non modificano i pulsanti Salva e Annulla.
- Le modalità di gioco sono mappate correttamente dai nomi dell'enumerazione;
  campi mancanti non diventano automaticamente l'indice della nave zero.
- Importazione: copia degli oggetti, controllo della destinazione, validazione
  di formato/versione, vettori e coordinate, limite di dimensione, BOM UTF-8
  e supporto degli array grezzi. Nomi di esportazione compatibili con Windows.
- Inventari: controllo di celle valide, duplicati, dimensioni, identità e
  quantità; una lista esplicitamente vuota non sblocca le celle.
- Un salvataggio modificato sul disco dopo il caricamento viene rifiutato
  prima di applicare nuove modifiche; dopo una scrittura il modello si rilegge.
- File scritti attraverso sostituzione atomica, temporanei unici, sincronizzazione
  e tentativi limitati per i blocchi transitori di Windows.
- Xbox WGS: solo nomi GUID esadecimali; contenitori Auto/Manual identici
  distinti; aggiornamento della dimensione nel record dell'indice associato
  al GUID, conservazione di descrittori, terminatori e alias del parser.
- Backup WGS completo di tutti i contenitori insieme all'indice globale,
  SHA-256, nome univoco e marcatore di completamento. Il ripristino rifiuta
  snapshot parziali o contenitori assenti nella destinazione.
- Rollback degli errori verificato sui file e sull'indice; gli errori di
  ripristino vengono segnalati con la posizione del backup.
- Il controllo del processo di gioco ha un timeout: se non è disponibile,
  la scrittura viene bloccata senza un'attesa infinita.
- Avvio: verifica del runtime incluso, prosecuzione della ricerca dopo
  candidati non funzionanti, scelta coerente di javaw e codice di errore corretto.

## Verifiche

**79 prove backend superate**, incluse prove di integrazione su copie Steam
e Xbox: modifica e rilettura dal disco, invalidazione cache, rifiuto del
modello obsoleto, backup completo, errore iniettato durante la transazione,
rollback e ripristino confrontati byte per byte. Sono state usate copie
ulteriormente isolate dentro `build/test-data/`; i salvataggi originali
non sono stati modificati.

**12 prove funzionali UI superate a ciascuna scala 100%, 175% e 200%**:
blocco e riabilitazione dei comandi, cambio nave, griglie ricreate, pulsanti
Salva/Rinomina/Esporta. Prodotte 30 schermate di componenti Swing reali
su dati sintetici, incluse finestre compatte 1040 × 720.

Compilazione Java 8 riuscita. Il pacchetto locale include runtime e librerie;
esclude codice sorgente, test, salvataggi, backup, progetti personali,
configurazione e log. JAR e ZIP controllati; hash SHA-256 accanto allo ZIP.

## Limiti della verifica

Le prove di scrittura usano copie di salvataggi disponibili, non tutte le
combinazioni di versioni e piattaforme del gioco. Non è stato eseguito un
collaudo dentro No Man's Sky dopo le modifiche. Le schermate sono renderizzate
dai componenti reali fuori schermo: non certificano ogni comportamento del
desktop Windows, il trascinamento con il mouse o l'apertura del browser.
Il parser incluso può emettere avvisi per chiavi di gioco non mappate.

Le correzioni coprono i problemi individuati durante la revisione; non
costituiscono una garanzia di assenza di qualunque bug. I backup restano
necessari prima di modificare una partita.

## Riprodurre i controlli

```powershell
.\tools\compila.ps1
.\tools\verifica.ps1 -Anteprime
# Per le prove di integrazione, aggiungere entrambe le fixture:
.\tools\verifica.ps1 -FixtureSteam 'copia-steam' -FixtureXbox 'copia-xbox' -Anteprime
.\tools\compila.ps1 -Pacchetto
```
