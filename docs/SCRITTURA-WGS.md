# Scrittura di un salvataggio Xbox (formato WGS)

> Ricognizione eseguita il 2026-09-23 sul salvataggio reale `Slot3Manual`,
> confrontando tre fonti indipendenti: i file scritti dal gioco, i file scritti
> dalla pipeline Python **verificata in gioco** (`corvette_project/staged_latest/`)
> e il codice della libreria di conversione.
>
> **Conclusione in una riga**: la libreria di conversione legge correttamente ma
> **scrive il descrittore del contenitore in modo diverso dal gioco**. Non va
> usata per scrivere su un salvataggio vero senza correzioni.

---

## 1. Com'è fatto un contenitore

Un contenitore sta in una cartella con nome GUID e contiene tre file:

| File | Dimensione tipica | Cos'è |
|---|---|---|
| `<guid>` (descrittore) | **360 byte** | metadati dello slot |
| `<guid>` (payload) | 1,7 MB | il salvataggio compresso |
| `container.<n>` | 328 byte | indice del contenitore |

Più `containers.index` (547 byte) nella cartella padre.

---

## 2. Il descrittore: 280 byte noti + 80 di coda

Struttura letta dal codice di conversione (classe `fS`):

| Offset | Byte | Contenuto |
|---|---|---|
| 0 | 4 | `lL` — variante del formato (`0x1081` = 4225 in questo salvataggio) |
| 4 | 4 | `version` (1) |
| 8 | 8 | **`totalPlayTime`** in secondi (669511 = 185 h 58 min) |
| 16 | 4 | **dimensione decompressa** del payload |
| 20 | 128 | nome del salvataggio, UTF-16LE a lunghezza fissa |
| 148 | 128 | descrizione dello slot (es. `In GamX | OASI`) |
| 276 | 4 | `lM` |
| 280 | **80** | **coda che la libreria non legge e non riscrive** |

### La coda di 80 byte contiene dati veri

```
de d1 c0 55 5b 61 ef 5d  a3 a2 b2 6a d4 07 00 00
00 00 00 00 00 00 00 00  ... (zeri)
```

Nel contenitore gemello (`Slot3Auto`) la stessa coda è:

```
de d1 c0 55 5b 61 ef 5d  2f 14 af 6a d4 07 00 00
50 65 72 73 6f 6e 61 6c 69 7a 7a 61 74 65 00 00
```

`50 65 72 73 6f 6e 61 6c 69 7a 7a 61 74 65` in UTF-16LE è **"Personalizzate"**:
l'etichetta della difficoltà di gioco. Nel contenitore dell'account
(`AccountData`) la coda è tutta a zeri.

**Quindi la coda non è riempimento: contiene la difficoltà.** Scartarla significa
perderla.

---

## 3. Il campo a offset 16: quale dimensione?

Confronto fra le tre fonti:

| Fonte | Valore a offset 16 | Payload reale | Rapporto |
|---|---|---|---|
| Gioco, `Slot3Auto` | 9.842.314 | 1.743.369 | 5,65 |
| Gioco, `Slot3Manual` | 9.842.410 | 1.743.323 | 5,65 |
| Gioco, `AccountData` | 82.814 | 27.977 | 2,96 |
| Pipeline Python, build Etheria | 10.254.376 | 2.732.443 | 3,75 |
| Pipeline Python, build Crimson | 14.789.460 | 4.785.356 | 3,09 |
| **Libreria Java** | **1.738.015** | 1.738.015 | **1,00** |

Il rapporto non è costante: il campo **non** è un multiplo del payload. È la
dimensione **decompressa**, che dipende da quanto il contenuto si comprime.

Lo conferma la documentazione del lavoro precedente (`RISULTATO.md`):
*"Il campo lunghezza del formato attuale è stato verificato contro entrambi i
salvataggi reali e preservato come dimensione decompressa."*

**La libreria ci scrive invece la dimensione compressa.** Nel codice:

```java
int  ch() { return this.mz; }                          // decompressa
void aj(int n) { if (this.mB != null) this.mz = n; }   // la imposta solo nella variante 1
int  ci() { return this.mA; }                          // compressa
void ak(int n) { if (this.mB == null) this.mA = n; }   // la imposta solo nella variante 0
```

In questo salvataggio `mB == null` (variante 0), quindi `h()` chiama `ak()` con
`Files.size(temp)` — la dimensione compressa — e quella finisce nel file.

---

## 4. Cosa la libreria fa bene

Da non buttare, sono verificate:

- **Lettura**: il contenuto sopravvive intatto. Ciclo letto → riscritto → riletto:
  **25.911.324 caratteri identici**, con 15 chiavi non mappate che vengono
  conservate tali e quali (il codice, quando non trova la mappatura, fa
  `string2 = string` e mette in cache l'identità).
- **Serializzazione**: l'LZ4 prodotto è valido e rileggibile.
- **Scrittura atomica**: file temporaneo nella stessa cartella + `ATOMIC_MOVE`.
- **Ripristino**: se qualcosa fallisce, i file toccati vengono ricostruiti dallo
  snapshot. **Verificato**: dopo un tentativo fallito i file erano byte per byte
  quelli di partenza.
- **Stabilità**: una seconda scrittura identica cambia solo `containers.index`,
  che contiene il timestamp di modifica.

---

## 5. Conseguenza: cosa serve per la Fase 3

Non si può usare `fs.b(modello)` per scrivere su un salvataggio vero. Serve un
procedimento che:

1. usi la libreria per **leggere e serializzare** (questa parte è corretta);
2. costruisca il nuovo payload con lo scrittore LZ4 (`gZ`), che è corretto;
3. **parta dal descrittore originale** e ne modifichi solo i campi che cambiano,
   **conservando gli 80 byte di coda**;
4. aggiorni `containers.index` allo stesso modo, senza ricostruirlo da zero;
5. scriva tutto con backup verificato, file temporanei e `ATOMIC_MOVE`.

Il riferimento è `install_corvette.py` in
`NMSSaveEditor_archivio_20260917/dati_utente/corvette_project/`: quella
procedura ha già scritto nei salvataggi veri con verifica degli hash e ricevuta.

---

## 6. La coda non contiene dimensioni: si conserva così com'è

Confronto delle code con payload di dimensioni molto diverse:

| Fonte | Payload | Coda (primi 16 byte) |
|---|---|---|
| Gioco `Slot3Auto` | 1.743.369 | `de d1 c0 55 5b 61 ef 5d` `2f 14 af 6a d4 07 00 00` |
| Gioco `Slot3Manual` | 1.743.323 | `de d1 c0 55 5b 61 ef 5d` `a3 a2 b2 6a d4 07 00 00` |
| Pipeline, build Crimson | 2.725.970 | `6e 84 96 1e 2b de f6 9f` `5a e6 a6 6a d4 07 00 00` |
| Gioco `AccountData` | 27.977 | tutti zeri |

I primi 8 byte sono un identificativo del contenitore: cambiano fra il gioco e
la pipeline Python, quindi non dipendono dal contenuto. Gli 8 successivi
contengono una parte fissa (`d4 07 00 00`) e una che varia: sembrano un
identificativo o un contatore, non una dimensione. **Nessun valore della coda
segue la dimensione del payload**, che varia di oltre un megabyte fra i casi
confrontati.

Quindi, per riscrivere un contenitore, **la coda va semplicemente copiata
dall'originale**. Non c'è nulla da ricalcolare.

---

## 7. `containers.index`: 8 byte su 547

Dopo una scrittura, `containers.index` resta lungo 547 byte e differisce
dall'originale in **8 byte** (offset 524-529 e 540-541): sono il timestamp di
modifica e i campi di dimensione. Anche qui il problema è che la dimensione
registrata è calcolata sul descrittore da 280 byte invece che da 360.

Ricalcolando la dimensione sul descrittore corretto, anche questi 8 byte
tornano a posto.

---

## 8. Procedimento corretto per scrivere

```
1. backup verificato della cartella del contenitore + containers.index
2. lettura e modifica del modello            (libreria: corretto)
3. costruzione del nuovo payload             (gZ: corretto)
4. nuovo descrittore = ORIGINALE a 360 byte, con:
       offset 16-19 = dimensione DECOMPRESSA del nuovo payload
       coda di 80 byte conservata
5. containers.index aggiornato nei campi di dimensione e timestamp
6. scrittura con file temporaneo + ATOMIC_MOVE, e rilettura di conferma
```

I punti 2 e 3 si possono delegare alla libreria, purché il descrittore e
l'indice vengano poi ricostruiti come ai punti 4 e 5.

