# Note tecniche

Materiale per chi vuole capire come funziona dentro, o metterci mano.

---

## I due formati di salvataggio

Il tool legge e scrive due famiglie di salvataggi, che sono fatte in modo
diverso. Riconosce da sé quale ha davanti: se nella cartella c'è
`containers.index` è il formato a contenitori, altrimenti è quello a file
singoli.

| | **File singoli** | **Contenitori** |
|---|---|---|
| Piattaforme | Steam, GOG, Epic | app Xbox per PC |
| File di uno slot | `save.hg`, `save2.hg`, … | una sottocartella per contenitore |
| Metadati | manifest `mf_save*.hg` | descrittore da 360 byte |
| Indice | non c'è | `containers.index` |
| Compressione | LZ4, magic `E5 A1 ED FE` | LZ4, stesso magic |

### Perché la scrittura è diversa nei due casi

Su **file singoli** si usa il metodo `fs.b(eY)` della libreria: riscrive il file
compresso **e aggiorna il manifest** con nome del salvataggio, ore di gioco,
dimensione e impronte. Il manifest è la parte delicata: se restasse quello
vecchio, il gioco leggerebbe metadati che non descrivono più il file, e lo slot
potrebbe risultare vuoto o incoerente.

Su **contenitori** la scrittura della libreria **non è utilizzabile**: riscrive
il descrittore in una forma diversa da quella del gioco (scrive la dimensione
compressa invece della decompressa, e perde 80 byte di coda che contengono la
difficoltà di gioco). Per questo il tool ha uno scrittore proprio,
`ScrittoreSalvataggio`, che parte dai file originali e ne cambia il minimo
indispensabile:

1. controlla che il gioco sia chiuso
2. fa un backup completo e ne verifica l'esito
3. decomprime il payload originale e ne apprende la forma esatta
4. applica la modifica al modello
5. riserializza e ricomprime
6. scrive il payload con file temporaneo e sostituzione
7. aggiorna il descrittore: **solo** il campo dimensione a offset 16,
   conservando gli 80 byte di coda
8. aggiorna `containers.index`: **solo** la dimensione totale
9. rilegge e confronta byte per byte; se non torna, ripristina il backup

Tutto ciò che non deve cambiare resta byte per byte quello che era.

Il contenitore si riconosce **dal contenuto**, non dal nome: i due file di uno
slot hanno lo stesso nome salvataggio e la stessa descrizione, quindi cercare
per nome rischia di far scrivere nel file sbagliato.

---

## Il parser condiviso

`lib/nms-parser.jar` contiene 73 classi estratte da `Atlante.jar` più 12 classi
della libreria LZ4 e le risorse `nomanssave/db/`. È la chiusura completa delle
dipendenze delle classi di conversione: nulla di più.

**Esclusa di proposito**: `nomanssave.gz`, la classe che contiene
`populateStationStorageParts()` e `unlockAllStationAndBaseProducts()`. Non
essendoci, le funzioni di sblocco non sono raggiungibili nemmeno per errore.

### Una trappola che costa tempo

Nel JAR di Atlante ci sono **208 coppie di classi che differiscono solo per
maiuscole** (`nomanssave/a.class` e `nomanssave/A.class` sono classi diverse).
Estrarre il JAR su Windows le sovrascrive a vicenda e si ottengono classi
sbagliate. Sintomo: `eV.class` pesa 765 byte invece di 5090.

**La libreria va costruita leggendo gli archivi direttamente, mai passando dal
filesystem.**

### Il segnaposto `nomanssave.Application`

La classe `nomanssave.eC`, che carica il dizionario delle chiavi di
salvataggio, risolve il percorso delle proprie risorse partendo da
`Application.class`. Senza quella classe la conversione di **qualunque**
salvataggio fallisce con `NoClassDefFoundError`.

`src-stub/nomanssave/Application.java` è un segnaposto scritto apposta per
questo: non è una copia di `Application`, non crea finestre, non contiene
nessuna scheda dell'editor. Fornisce solo ciò che serve al caricamento delle
risorse.

### Il rilevamento dei salvataggi

Il percorso è diverso per piattaforma:

| Piattaforma | Dove cerca |
|---|---|
| Steam, GOG, Epic | `%APPDATA%\HelloGames\NMS\<profilo>` |
| Xbox app su PC | `%LOCALAPPDATA%\Packages\HelloGames.NoMansSky_*\SystemAppData\wgs\*` |

Ogni candidato viene **validato provando davvero a convertire un salvataggio**,
non limitandosi a controllare che la cartella esista.

---

## Le icone del gioco

`lib/nms-icons.jar` contiene **3210 icone** estratte dall'installazione,
nominate per identificativo dell'oggetto:

| Prefisso | Quantità | Cosa sono |
|---|---|---|
| `PRODUCT-` | 2806 | prodotti, moduli di costruzione, parti di Corvette |
| `TECHNOLOGY-` | 247 | tecnologie e potenziamenti |
| `SUBSTANCE-` | 100 | elementi e materiali |
| `TECHBOX-` | 24 | moduli tecnologici |
| `UI-` | 33 | icone di interfaccia |

Si caricano come risorsa dal percorso `nomanssave/icons/PRODUCT-<ID>.PNG`, dove
`<ID>` è lo stesso identificativo che compare nel salvataggio (es.
`PRODUCT-B_COK_A.PNG` per il cockpit A).

**I moduli di potenziamento non hanno un disegno proprio.** Il gioco, per un
modulo installato, mostra il disegno della **tecnologia base** a cui appartiene:
un modulo dell'iperguida mostra i motori, uno degli scudi mostra lo scudo. Il
programma fa lo stesso, riconoscendo la famiglia dal nome del modulo
(`UP_HYP4` → iperguida, `UP_S_SHL4` → scudi).

Le icone `TECHBOX-` **non** si usano per i moduli installati: sono quelle da
negozio, prima di installare la tecnologia.

---

## Fatti verificati sul campo

Ognuno è stato misurato su un salvataggio reale, non dedotto dal codice.

- La Corvette è una **base** in `PersistentPlayerBases[]` con
  `BaseType.PersistentBaseTypes == "PlayerShipBase"`; `UserData` è l'indice in
  `ShipOwnership[]`; il nome sta in `base.Name`; i moduli in `base.Objects[]`.
- La nave in uso è `PlayerStateData.PrimaryShip`.
- **Le ore di gioco stanno in `CommonStateData.TotalPlayTime`, in secondi** —
  non in `PlayerStateData.TotalPlayTime`, che restituisce 0, come pure
  suggerirebbe il codice dell'editor.
- Le tecnologie della Corvette stanno in `ShipOwnership[i].Inventory_TechOnly`,
  con voci di tipo `Technology`.
- I depositi delle basi stanno in `Chest1Inventory` … `Chest10Inventory`, e
  nell'elenco `Objects` della base compaiono i moduli `^CONTAINER0` … `^CONTAINER9`
  che li rendono visibili in gioco.
- Il file `.hg` inizia con il magic `E5 A1 ED FE` quando è compresso LZ4.
  L'header `HGSAVEV2` **non** c'è su Steam: è del formato a contenitori.

Il resto, con il comando o il file che lo dimostra, è in `PIANO.md`.

---

## Le trappole del decompilato

I nomi delle classi decompilate **non sono affidabili**: il decompilatore
aggiunge il suffisso `_0` per evitare collisioni, e il nome del file può non
corrispondere a quello della classe contenuta.

Il modo affidabile per leggere le firme reali è `javap` sul JAR:

```bash
javap -classpath lib/nms-parser.jar nomanssave.fs
javap -classpath lib/nms-parser.jar nomanssave.fq
```

Attenzione in particolare: `fq_0` è in realtà `fq`, `ft_0` è `ft`, `fs_0` è
`fs`, `fn_0` è `fn`. Compilare con i nomi del decompilato produce errori di
risoluzione.

Alcune classi della libreria sono **package-private** (es. `fM`, `fQ`, `fI`): non
si possono usare direttamente. Si passa dalle interfacce pubbliche `fq`, `ft`,
`fs`.

---

## Compilare dai sorgenti

Serve un JDK (per `javac`/`jar`) e il compilatore `tools/ecj.jar`, che è
compatibile con Java 8:

```bash
JAVA="<percorso-jdk>/bin/java.exe"
JARX="<percorso-jdk>/bin/jar.exe"

find src -name "*.java" > sources.txt
echo "src-stub/nomanssave/Application.java" >> sources.txt

"$JAVA" -jar tools/ecj.jar -encoding UTF-8 -source 1.8 -target 1.8 -warn:none \
  -cp "lib/nms-parser.jar;lib/flatlaf.jar" -d build/classes "@sources.txt"

"$JARX" cfm NMSITALIA-CorvetteHUB.jar build/MANIFEST.MF \
  -C build/classes . -C . res
```

Il manifest dichiara `Class-Path: lib/nms-parser.jar lib/flatlaf.jar
lib/nms-icons.jar`: le tre librerie devono restare nella cartella `lib/` accanto
al JAR.

**Attenzione prima di impacchettare**: se durante una prova il programma è stato
avviato con la cartella di compilazione come "cartella del programma", dentro
`build/classes` possono essere finiti `Backup/`, `Builds/` e i log — e il JAR
diventa enorme. Controlla prima di crearlo.
