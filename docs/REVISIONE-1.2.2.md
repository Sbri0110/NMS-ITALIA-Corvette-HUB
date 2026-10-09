# Revisione 1.2.2 — 9 ottobre 2026

Rinomina, Importa, Esporta ed Elimina funzionano anche quando la Corvette è
la nave selezionata nel salvataggio. Il controllo `PrimaryShip` introdotto
nella 1.2.0 bloccava erroneamente la gestione fuori dal gioco.

- Rinomina aggiorna il nome della base e della nave, conservando gli altri dati.
- Importa aggiorna gli oggetti e, se richiesto, il nome della base. Non modifica
  statistiche, inventari, deposito o selezione della nave.
- Esporta rimane in sola lettura. Aggiorna la libreria senza ricaricare lo slot
  e senza azzerare il contenuto delle altre schede.
- Elimina recupera i moduli nel deposito e rimappa i riferimenti. Quando rimuove
  la nave selezionata, sceglie una nave rimasta e la indica nella conferma.
  Gli slot vuoti non sono candidati; l'ultima nave posseduta non viene rimossa.
- I pulsanti dei piani di eliminazione non eseguibili mostrano il motivo del
  blocco. Rimangono necessari spazio nel deposito, gioco chiuso e conferma
  digitando il nome.
- Tutte le scritture mantengono backup verificato, controllo dello stato fresco,
  rilettura e ripristino automatico in caso di errore.
- Corretto il riconoscimento dei contenitori Xbox: quando il parser serializza
  spazi o numeri diversamente dal gioco, il confronto usa il modello completo
  riletto dallo stesso parser. Contenuti diversi continuano a essere rifiutati.
  Questo problema è stato riprodotto sul salvataggio attuale e verificato su
  una sua copia, senza modificare l'originale.

Le regressioni coprono il caso di un'unica Corvette selezionata, gli eventi del
mouse dei comandi e i bersagli dei clic dopo il ridimensionamento, alle scale
100%, 175% e 200%. Le prove del modello verificano rinomina/importazione e tutte
le combinazioni di tre navi per la rimappatura dopo l'eliminazione, recupero dei
moduli, ultima nave e deposito pieno. Le prove con fixture duplicano i salvataggi
prima di qualsiasi scrittura e verificano il ripristino byte per byte.

Risultati: 81 regressioni di sicurezza con copie Steam/Xbox, 88 prove della
gestione Corvette incluse rinomina/importazione/eliminazione su disco e
ripristino, 203 prove dei pulsanti nelle tre scale. La matrice del layout
comprende 135 ridimensionamenti, 36 prove UI e 324 prove del componente fluido.

Il collaudo nel gioco resta escluso: le prove usano modelli sintetici e copie
dei file, senza modificare i salvataggi personali.
