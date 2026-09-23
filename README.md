<div align="center">

<img src="docs/img/logo.png" alt="NMS ITALIA Corvette HUB" width="230">

# NMS ITALIA Corvette HUB

**Scambia le tue Corvette con gli altri giocatori.**

Applicazione desktop per Windows, dedicata esclusivamente alle Corvette di
No Man's Sky.

[![Licenza](https://img.shields.io/badge/licenza-Apache%202.0-blue.svg)](LICENSE)
[![Versione](https://img.shields.io/badge/versione-1.0.0-orange.svg)](../../releases)
[![Piattaforma](https://img.shields.io/badge/piattaforma-Windows-0078D6.svg)](#requisiti)
[![Discord](https://img.shields.io/badge/Discord-NMS%20ITALIA-5865F2.svg?logo=discord&logoColor=white)](https://discord.gg/ZPwrQuATC4)

**[Entra nel Discord di NMS ITALIA](https://discord.gg/ZPwrQuATC4)** ·
[Scarica](../../releases/latest) ·
[Segnala un problema](../../issues)

</div>

---

> [!IMPORTANT]
> Questo progetto è sviluppato **con l'aiuto dell'intelligenza artificiale**:
> le scelte di merito sono umane, il codice è scritto in gran parte dall'IA.
> [Dettagli più sotto](#sviluppo-ia).

---

## Cos'è

Un tool che fa quattro cose e nient'altro:

| | |
|---|---|
| **Importa** | porta un progetto di Corvette dentro una Corvette del tuo salvataggio |
| **Esporta** | estrae una Corvette in un file condivisibile |
| **Rinomina** | cambia il nome di una Corvette |
| **Consultare** | i moduli della Stazione Spaziale, le tecnologie installate, i depositi delle basi |

Il deposito si **legge e basta**: il tool non sblocca, non aggiunge e non
riempie nulla. È uno scambio di progetti tra giocatori, non un editor
generalista.

---

## Screenshot

<img src="docs/img/01-scelta-salvataggio.png" alt="Scelta del salvataggio" width="100%">

<img src="docs/img/02-corvette.png" alt="Scheda Corvette" width="100%">

<img src="docs/img/06-deposito.png" alt="Deposito dei moduli" width="100%">

<img src="docs/img/03-importa.png" alt="Importazione di un progetto" width="100%">

<details>
<summary>Altre schermate</summary>

<img src="docs/img/04-esporta.png" alt="Esportazione" width="100%">

<img src="docs/img/05-rinomina.png" alt="Rinomina" width="100%">

<img src="docs/img/07-libreria.png" alt="Libreria dei progetti" width="100%">

</details>

---

## Come si avvia

1. Scarica l'ultima versione dalla pagina [Releases](../../releases/latest)
2. Scompatta l'archivio dove vuoi
3. Doppio clic su **`CorvetteHUB.bat`**

**Non devi installare Java**: il pacchetto include già il proprio runtime.

Se Windows chiede conferma (*"Windows ha protetto il PC"*), scegli **Ulteriori
informazioni** e poi **Esegui comunque**: succede con i programmi scaricati da
Internet che non hanno una firma digitale.

Se vuoi il logo anche sul Desktop, lancia una volta
`Crea collegamento sul Desktop.bat`: crea un collegamento con l'icona giusta al
posto di quella generica di Windows.

---

## Requisiti

- **Windows**
- **No Man's Sky su PC**: Steam, GOG, Epic oppure app Xbox

Il programma riconosce da solo dove sono i salvataggi. Se non li trova, il
pulsante **Cambia cartella** permette di indicarli a mano: la scelta viene
ricordata.

---

## Sicurezza dei salvataggi

Qui si scrive dentro i salvataggi delle persone, quindi le cautele sono la
parte importante del programma.

- Il gioco deve essere **chiuso**: il tool lo controlla e blocca l'operazione.
- Prima di ogni scrittura viene fatta una **copia di sicurezza verificata con
  SHA-256**. Se la copia non riesce, non viene scritto nulla.
- La Corvette **in uso** non compare nell'elenco: il gioco la ricarica in
  memoria e sovrascriverebbe la modifica.
- Dopo la scrittura il file viene **riletto e confrontato byte per byte**. Se
  qualcosa non torna, il tool rimette a posto il backup da solo.
- **Ripristina backup...** rimette a posto i file da un backup scelto, e prima
  di farlo salva lo stato attuale.

---

## Cosa il tool NON fa, per scelta

- Non sblocca moduli, prodotti o parti nella Stazione Spaziale
- Non modifica classi, statistiche, inventari, potenziamenti, proprietario o
  slot di una Corvette
- Non tocca navi che non siano Corvette
- Non modifica valuta, naniti, quicksilver, reputazione o traguardi
- Non ha nessuna forma di "unlock tutto", trainer o cheat

**Non è una promessa sulla fiducia: è un vincolo tecnico.** Le funzioni di
sblocco vivono nella classe `nomanssave.gz` dell'editor da cui deriva il
parser, e quella classe **non è inclusa** nelle librerie di questo progetto.

---

## Limiti noti

- Il ripristino riconosce i contenitori dal nome della cartella. Se il gioco ha
  cambiato i nomi dei file, i vecchi vengono rimessi accanto ai nuovi: lo stato
  torna quello del backup, ma nella cartella restano file che il gioco non usa
  più.
- Le statistiche della Corvette si leggono ma non si modificano.
- **La scrittura su Steam è nuova.** Il ciclo è stato verificato su una copia,
  ma la conferma in gioco da parte di chi gioca su Steam è ancora da fare. Se
  qualcosa non va, il backup è automatico.

---

## Approfondimenti

| Documento | Cosa c'è dentro |
|---|---|
| [`docs/GUIDA-UTENTE.md`](docs/GUIDA-UTENTE.md) | le due schermate, spostare gli oggetti, il deposito, i backup |
| [`docs/NOTE-TECNICHE.md`](docs/NOTE-TECNICHE.md) | i due formati di salvataggio, il parser, le icone, compilare dai sorgenti |
| [`PIANO.md`](PIANO.md) | piano di lavoro e fatti verificati, con le prove |

---

## Licenza

**Apache License 2.0** — vedi [LICENSE](LICENSE).

Il software è gratuito, anche per uso commerciale. In cambio **chi lo usa deve
citare l'autore**: se usi questo software o parte di esso, indica che deriva da
**NMS ITALIA Corvette HUB di Sbri**.

Le attribuzioni del materiale di terzi sono in [NOTICE](NOTICE).

---

<a id="sviluppo-ia"></a>

## Come è stato sviluppato

Questo progetto è stato sviluppato **con l'aiuto dell'intelligenza
artificiale**.

Le scelte di merito sono umane: cosa il tool deve fare e cosa non deve fare,
come trattare i salvataggi altrui, quando una scrittura è abbastanza sicura da
essere proposta a un giocatore. Il codice è stato scritto in gran parte
dall'IA, che ha anche fatto la ricognizione sul formato dei salvataggi e le
verifiche.

Niente è stato preso per buono senza controllo: ogni affermazione sul formato
dei salvataggi in `PIANO.md` ha accanto il comando o il file che la dimostra, e
le funzioni che scrivono sono state provate su copie prima di arrivare ai
salvataggi veri.

---

<div align="center">

**No Man's Sky** e tutte le sue risorse sono marchi di **Hello Games**.

Questo progetto non è affiliato a Hello Games e non è approvato da Hello Games.

<br>

**[Discord di NMS ITALIA](https://discord.gg/ZPwrQuATC4)**

</div>
