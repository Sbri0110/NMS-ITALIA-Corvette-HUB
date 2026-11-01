# Revisione 1.2.4 — 1 novembre 2026

Il selettore di file diventa una finestra vera, con la navigazione di Esplora
file. Cambia anche il nome del server Discord della community. Nessuna modifica
al formato dei salvataggi: le scritture e le cautele restano quelle della 1.2.2.

## Selettore di file riscritto

La 1.2.3 aveva sostituito il `JFileChooser` con una finestra a tema, ma per
muoversi c'erano solo l'elenco e un campo di testo con il percorso. Chi doveva
cambiare cartella doveva riscriverla a mano. Ora si naviga:

- **Barra con indietro, avanti, su e aggiorna.** Indietro e avanti usano uno
  storico vero, non solo la cartella superiore.
- **Percorso a pezzi cliccabili.** Ogni tacca del percorso riporta alla
  cartella che rappresenta; i pezzi che non ci stanno diventano puntini di
  sospensione. La matita a destra (o `Ctrl+L`) trasforma il percorso in un
  campo di testo, per incollare un indirizzo.
- **Barra laterale** con i *Preferiti* (Desktop, Documenti, Download, Utente) e
  le *Unità* del computer. Si nasconde da sola quando la finestra si stringe,
  per lasciare spazio all'elenco.
- **Elenco a riga singola**: icona, nome e dimensione a destra. I nomi lunghi
  vengono accorciati con i puntini invece di sbordare, e la riga sotto il mouse
  si illumina.
- **Salva con nome**: il nome del file ha una riga sua, separata dal percorso,
  e prima di sostituire un file esistente il programma chiede conferma.

Le scorciatoie da tastiera: `Invio` apre la cartella scelta o conferma,
`Backspace` sale di un livello, `F5` aggiorna, `Ctrl+L` modifica il percorso,
`Alt+Freccia sinistra` e `Alt+Freccia destra` scorrono lo storico, `Esc`
annulla.

Se la cartella di partenza non esiste più (un salvataggio spostato, una chiavetta
tolta) la finestra non si apre più vuota: risale al primo antenato che c'è.

## Il server Discord cambia nome

Il server della community non si chiama più "NMS ITALIA" ma
**[AGIT] Alleanza Galattica Italiana**. Il pulsante nella testata mostra il nome
nuovo; nelle finestre strette si riduce a "Discord" per non sbordare. Aggiornati
anche il suggerimento del pulsante, la finestra di ripiego e il README.

## Verifiche

`tools/compila.ps1` verde e `tools/verifica.ps1 -Anteprime` superato alle scale
100%, 175% e 200%.

- 56 regressioni di sicurezza con copie Steam/Xbox
- 65 prove della gestione Corvette
- 81 prove dei pulsanti a scala 100% (66 a 175%, 56 a 200%)
- 108 prove del componente fluido e 12 prove UI, per ciascuna scala
- 7502 / 7235 / 7065 ridimensionamenti dell'interfaccia, per scala

Le anteprime in `docs/img/premium/` sono state rigenerate: la testata mostra il
nome nuovo del server. Il selettore è stato controllato a parte alle tre scale,
nei modi file, cartella e salva.

Il collaudo nel gioco resta escluso: le prove usano modelli sintetici e copie
dei file, senza modificare i salvataggi personali.
