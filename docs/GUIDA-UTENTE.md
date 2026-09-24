# Guida all'uso

## Come è organizzato: due schermate

Il programma non mette tutto in una schermata sola. Prima si decide **su quale
partita** si lavora, poi si lavora.

**Schermata 1 — Scelta del salvataggio.** Non si vede nulla delle Corvette:
solo dove il tool ha trovato i salvataggi e quali slot esistono, con modalità,
nome, ore giocate e data. Si sceglie lo slot e si preme **Continua**.

**Schermata 2 — Hub della Corvette.** Sette schede:

| Scheda | Cosa fa |
|---|---|
| **Corvette** | statistiche, tecnologie installate, inventario, depositi montati |
| **Importa** | porta un progetto dentro una Corvette, con controllo di compatibilità |
| **Esporta** | estrae una Corvette in un progetto condivisibile, con anteprima |
| **Rinomina** | cambia il nome di una Corvette, con backup e verifica |
| **Elimina** | toglie una Corvette e restituisce i moduli al deposito della Stazione |
| **Deposito** | moduli della Stazione Spaziale, a griglia, con icone e filtri |
| **Libreria** | i progetti salvati, con anteprima, ricerca ed eliminazione |

In alto resta sempre scritto su quale slot si sta lavorando, e il pulsante
**Cambia salvataggio** riporta alla prima schermata.

---

## La scheda Corvette

Mostra tutto quello che riguarda la nave scelta:

- **Statistiche**: danni, scudi, iperguida, agilità, più classe e slot
  supercaricati. Nel salvataggio non sono campi della nave: stanno in
  `Inventory_TechOnly.BaseStatValues`.
- **Tecnologie installate**, con l'icona ufficiale del gioco.
- **Inventario di bordo**, con gli oggetti spostabili col mouse.
- **Depositi montati**, se la nave ne ha.

La griglia delle tecnologie è 10 × 6 = 60 celle, e in un salvataggio maturo
sono tutte occupate. Sotto una tecnologia **non c'è la quantità**: una
tecnologia installata non ha un "x100". Il numero nel salvataggio è la carica
(100 su 100), non un numero di pezzi.

### Spostare gli oggetti

Si trascina in tre posti: **l'inventario di bordo**, le **tecnologie
installate** e il **deposito**. Mentre muovi, l'icona segue il puntatore. Se la
cella è occupata, i due oggetti si scambiano di posto. Le celle che il gioco
blocca non accettano niente.

Quando sposti qualcosa compare una fascia gialla che dice quanti oggetti hai
mosso, e **in alto a destra si accendono due pulsanti**: *Salva le modifiche* e
*Annulla*. Sono lì e non nella scheda perché le griglie sono alte più di 700
pixel: messi sotto finirebbero fuori dalla vista. Funzionano con la scheda che
stai guardando, deposito compreso.

Finché non premi *Salva* non viene scritto niente: puoi provare le disposizioni
quanto vuoi, e *Annulla* rimette tutto com'era.

**Nessun oggetto viene creato o perso.** Spostare significa cambiare la
*coordinata* dell'oggetto, non toglierlo e rimetterlo: il tool controlla che il
numero di oggetti prima e dopo sia identico e, se non lo fosse, non scrive.

Due avvertenze:

- **Spostare le tecnologie cambia i bonus di adiacenza**, quindi anche le
  statistiche della nave. È una modifica con effetti sul gioco.
- Nel **deposito**, se hai un filtro attivo (una categoria o una ricerca) la
  griglia non è trascinabile: le posizioni che vedi non sarebbero quelle vere.
  Togli il filtro per spostare.

Il salvataggio passa dalle stesse cautele delle altre scritture: gioco chiuso,
backup verificato, entrambi i file dello slot, rilettura di conferma.

---

## Eliminare una Corvette

La scheda **Elimina** toglie una Corvette dal salvataggio. Funziona al
contrario di come si aspetta: **prima smonta, poi cancella.**

1. I moduli da Corvette (`^B_...`) tornano nel **deposito della Stazione
   Spaziale**, impilati per tipo.
2. Poi la Corvette e la sua nave vengono rimosse dal salvataggio.
3. Prima di scrivere, la Corvette viene **esportata in `Builds/`**: il progetto
   resta recuperabile anche dopo l'eliminazione.

### Il deposito non si allarga

Il deposito della Stazione ha una capienza **fissa**: 10 × 16 = **160 celle**.
Il tool non la aumenta, perché sarebbe barare. Quando apri la scheda, il
riquadro *Cosa succede* ti dice esattamente:

- quanti moduli ha la Corvette e di quanti tipi;
- quanti di quei tipi sono già nel deposito;
- **quante celle servono** e **quante ne sono libere**.

Se le celle libere non bastano, il pulsante resta spento e il tool ti dice
quante liberarne. Le liberi in gioco (spostando o vendendo moduli), poi torni
qui e riprovi.

### Le decorazioni

Su una Corvette possono esserci parti che non sono moduli da Corvette: luci,
corridoi, porte. Il deposito della Stazione accetta **solo** `^B_...`, quindi
quelle spariscono con la nave — esattamente come quando si elimina una base nel
gioco. Il tool te le conta prima di procedere.

### La conferma

Per confermare devi **scrivere il nome della Corvette**. Non è una formalità:
l'operazione non si annulla, e il nome va scritto per essere sicuri di aver
capito quale nave si sta eliminando. Se il nome non corrisponde, non succede
nulla.

### Quando non si può

- **La Corvette in uso** non si elimina: il gioco la tiene in memoria e
  sovrascriverebbe la modifica. Cambia nave in gioco, salva, e riprova.
- **Il gioco deve essere chiuso**, come per tutte le scritture.
- **Il deposito deve avere posto** per tutti i moduli (vedi sopra).

### Cosa cambia nel salvataggio

L'eliminazione tocca quattro cose, e nient'altro:

| Cosa | Prima | Dopo |
|---|---|---|
| `PersistentPlayerBases` | contiene la Corvette | la Corvette non c'è più |
| `ShipOwnership` | contiene la nave della Corvette | la nave non c'è più |
| `ShipUsesLegacyColours` | un valore per nave | resta lungo quanto le navi |
| `PrimaryShip` | indice della nave in uso | **rimappato** se era oltre quella rimossa |

E se nel salvataggio ci sono altre Corvette, i loro riferimenti alla nave
vengono spostati di conseguenza. Dopo la scrittura il tool **rilegge tutto e
verifica**: se un solo riferimento fosse rimasto rotto, rimetterebbe a posto il
backup da solo.

---

## Il deposito a griglia

I moduli si mostrano come riquadri con l'icona grande, il nome e la quantità —
non come righe di tabella: le icone del gioco sono quadrate e a 128 pixel, e in
una riga alta 30 verrebbero tagliate.

Le categorie sono pulsanti cliccabili con il numero di moduli che contengono,
così si vede subito com'è distribuito il deposito.

### Il contatore: 661, non 664

Il catalogo delle parti costruibili su una Corvette si ricava da
`basebuildingobjectstable` del gioco:

| | |
|---|---|
| costruibili come **strutturali** | 622 |
| costruibili come **decorative** | 42 |
| presenti in **entrambi** gli elenchi | 3 |
| **totale distinto** | **661** |

Sommare 622 e 42 darebbe 664, ma conterebbe tre parti due volte. Il contatore
del deposito usa **661** come denominatore e tiene la distinzione strutturale /
decorativa.

---

## Il controllo di compatibilità

È il punto di forza del tool. Prima di importare, per ogni parte richiesta dal
progetto dice in quale dei tre stati si trova:

| Stato | Significato |
|---|---|
| **nel deposito** | ce l'hai, pronta da montare |
| **sbloccata** | la conosci ma non è in deposito: dovrai procurartela |
| **mancante** | non l'hai mai sbloccata: la build risulterà incompleta |

Si procede solo con una conferma esplicita, più insistente se mancano parti.
**Niente viene sbloccato**: le parti mancanti restano mancanti.

---

## Il backup e il ripristino

Prima di ogni scrittura il tool copia i file dello slot in
`Backup/AAAA-MM-GG_HH-MM-SS/`, con le impronte SHA-256 annotate in
`backup.txt`. Se la copia non riesce, l'operazione si interrompe.

Il pulsante **Ripristina backup...** nella barra in alto rimette a posto i file
da un backup scelto. Anche il ripristino, prima di toccare qualcosa, salva lo
stato attuale: se il backup fosse vecchio si può tornare indietro.

**Limite noto**: il ripristino riconosce i contenitori dal nome della cartella.
Il gioco, quando salva, tiene le cartelle ma può cambiare i nomi dei file
dentro. In quel caso i file vecchi vengono rimessi accanto ai nuovi: lo stato
torna comunque quello del backup, perché viene ripristinato anche
`containers.index` e il gioco segue quello, ma nella cartella restano dei file
che il gioco non usa più.

---

## I file che il programma crea

Accanto al programma:

| File | Cosa contiene |
|---|---|
| `CorvetteHUB.conf` | la cartella dei salvataggi scelta, e la scala dell'interfaccia |
| `Builds/` | i progetti esportati |
| `Backup/` | i backup, con data e ora |
| `CorvetteHUB.log` | cosa è stato fatto e com'è finita |
| `CorvetteHUB-avvio.log` | quale runtime Java è stato usato |
| `CorvetteHUB-console.log` | messaggi del programma, per diagnosi |

I log non contengono dati personali oltre al nome del giocatore, che è già
dentro il salvataggio.

---

## Se il testo è troppo piccolo (monitor 4K)

Il programma legge il **DPI dello schermo** all'avvio e ingrandisce di
conseguenza testi, pulsanti, griglie e margini. Su un 4K con l'ingrandimento di
Windows al 150% o al 200% non devi fare niente: si adatta da solo.

Se l'ingrandimento non ti convince, apri `CorvetteHUB.conf` con Blocco note e
aggiungi una riga:

```
UiScale=1.5
```

I valori utili sono `1.25`, `1.5`, `1.75`, `2`, `2.5`, `3`. Per tornare
all'automatico, cancella la riga. La scala si applica alla riapertura del
programma.

La finestra si **ridimensiona trascinando i bordi**, come qualunque altro
programma. La dimensione di partenza si adatta allo schermo: su un 4K non
parte mai più grande del monitor.
