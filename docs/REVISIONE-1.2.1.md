# Revisione 1.2.1 — 9 ottobre 2026

Correzione della densità e del ridimensionamento dell'interfaccia in seguito
al feedback sulla versione 1.2.0. Il programma mantiene le funzioni e le
protezioni dei salvataggi della revisione precedente.

## Dimensioni e comportamento

- Corretta la doppia applicazione della scala DPI nelle dimensioni delle
  celle: `lato()` era già scalato e veniva passato nuovamente a `Scala.dim()`.
- Celle da 44 a 28 pixel logici, icone fino a 28, spaziatura di 3. Il layout
  mantiene le celle quadrate, le coordinate dell'inventario e il bersaglio
  del trascinamento dopo il ridimensionamento.
- Numeri delle statistiche da 28 a 20 punti; carte da 100 a 66 pixel logici
  di altezza, disposte su più righe quando serve. Nessun allungamento a
  rettangoli per riempire la finestra.
- Pulsanti, testata, selezione della partita e navigazione più compatti.
  I quattro comandi della testata sono allineati a destra, accanto alle
  informazioni dello slot, senza una riga separata nelle finestre ampie.
  La navigazione passa in alto nelle finestre strette. I comandi secondari
  sono nel menu **Azioni...**; Salva e Annulla compaiono quando ci sono
  modifiche da salvare.
- La pagina iniziale accorcia l'intestazione e nasconde il pannello
  introduttivo quando manca spazio. La lista segue la larghezza disponibile.
- Barre di comandi e filtri vanno a capo dichiarando l'altezza necessaria.
  I pannelli affiancati adottano proporzioni coerenti e si impilano dove
  utile. Contenuti e form rimangono raggiungibili con scorrimento verticale.
- Il deposito conserva tutte le coordinate; la sua testata scorre con il
  contenuto nelle finestre basse; le categorie diventano un selettore
  compatto. Nessuna modifica ai dati di gioco.
- Nella libreria il conteggio passa nel titolo quando manca spazio.
  Le righe hanno due linee senza andare a capo: i testi lunghi si accorciano
  e i dettagli completi restano disponibili nel suggerimento.
- La barra di stato riserva spazio ai due messaggi e accorcia i testi lunghi
  senza sovrapporli. La finestra conserva un'altezza minima utile anche al
  DPI alto: **1000 × 680 a scala 100%**, adattati alla scala e limitati
  allo spazio dello schermo.

## Verifica del ridimensionamento

La suite ripete la sequenza **1440 → 800 → 960 → 1280 → 1440 pixel reali**
per la selezione dei salvataggi e tutte le sette schede, alle scale
**100%, 175% e 200%**. Lo spazio della finestra non viene moltiplicato per
la scala: i controlli diventano più grandi dentro la stessa finestra.

I controlli verificano pulsanti e carte dentro i loro contenitori,
assenza di overflow orizzontale nei viewport, pannelli dello split visibili,
almeno una riga completa nelle liste, area utile per la scheda, celle
quadrate e target del trascinamento corretto. Identità, quantità e
coordinate degli inventari restano identiche durante i cambi di dimensione.
Il ritorno alla larghezza iniziale elimina le righe aggiuntive.

La stessa sequenza viene ripetuta con modifiche dell'inventario pendenti:
**45 ridimensionamenti per scala, 135 in totale**, con Salva e Annulla
visibili e abilitati, e ripristino dei dati originali dopo Annulla.
La verifica comprende **54 regressioni**, **12 prove UI per scala** e
**108 verifiche isolate del componente di layout per scala**.
Sono verificati anche i due messaggi completi della barra di stato a
800 e 960 pixel, alle tre scale, senza sovrapposizioni o uscita dai bordi.

Le anteprime sono prodotte da componenti Swing reali su dati sintetici.
Il ridimensionamento viene verificato geometricamente e tramite ispezione
delle immagini; resta escluso il collaudo interattivo dentro No Man's Sky.

## Compilazione

```powershell
.\tools\compila.ps1
.\tools\verifica.ps1 -Anteprime
.\tools\compila.ps1 -Pacchetto
```

Il JAR aggiornato rimane nella cartella principale. L'archivio portabile
1.2.1 e il relativo SHA-256 sono in `dist/`; i dati personali, il codice
sorgente e i test restano esclusi dal pacchetto.
