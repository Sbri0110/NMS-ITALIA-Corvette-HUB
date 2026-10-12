# Revisione 1.2.3 — 13 ottobre 2026

Restyling dell'interfaccia e una scheda nuova per i depositi montati sulla
Corvette. Nessuna modifica al formato dei salvataggi: le scritture e le cautele
restano quelle della 1.2.2.

## Interfaccia premium

- Nuovo sistema di design centralizzato in `ui/Theme.java`: palette a strati,
  scala tipografica, carte, pill e pulsanti `primaria()/secondaria()/distruttiva()`.
  Le schermate non usano più colori o font scritti a mano.
- Indicatore di lavoro animato (`ui/Attesa.java`): arco dorato, barra
  indeterminata e velo con titolo e fase, sopra il contenuto. Importare ed
  esportare mostrano cosa stanno facendo invece di restare fermi.
- Selettore di file e cartelle a tema (`ui/SelettoreFile.java`): ha sostituito
  ogni `JFileChooser`, compresa la finestra che sembrava di Windows XP.
- Testata ridisegnata: marchio, stato del gioco e pulsante **Discord NMS ITALIA**
  sempre visibile.

## Nuova scheda "Deposito Corvette"

Ottava scheda dell'hub. Mostra **solo i contenitori montati sulla Corvette
scelta**, non quelli delle basi o della Stazione:

- elenco dei contenitori a sinistra, con la capienza e lo stato di ciascuno;
- griglia trascinabile del contenitore scelto a destra, per riordinare gli
  oggetti;
- il pulsante **Salva la disposizione** scrive le modifiche, con le stesse
  cautele delle altre scritture (gioco chiuso, backup verificato, rilettura).

La vecchia sezione "Depositi montati" dentro la scheda Corvette resta come
rimando: in un rigo riassume i contenitori e indica dove si gestiscono.

I contenitori montati su una nave ma senza inventario creato dal gioco non
bloccano più la lettura: vengono mostrati come "da aprire".

## Correzioni di interfaccia

- **Scelta del salvataggio**: rimossa la fascia vuota fra il contenuto e il
  piede. Il piede non è più incollato al fondo della finestra ma cade subito
  sotto l'elenco degli slot.
- **Menu laterale allineato**: il contenuto delle schede è allineato a
  sinistra e le icone sono disegnate dentro una gabbia di dimensione fissa.
  Prima la voce "Deposito Corvette", con l'etichetta più lunga, spostava icona
  e testo di circa 30 px rispetto alle altre.
- **Righe dei contenitori**: altezza ridotta, testi centrati verticalmente e
  margine interno, così il nome non tocca più la cornice del riquadro.
- Aggiunta l'icona mancante della voce "Deposito Corvette" (uno scrigno).

## Verifiche

`tools/compila.ps1` verde e `tools/verifica.ps1 -Anteprime` superato alle scale
100%, 175% e 200%.

- 56 regressioni di sicurezza con copie Steam/Xbox
- 65 prove della gestione Corvette
- 81 prove dei pulsanti a scala 100% (66 a 175%, 56 a 200%)
- 108 prove del componente fluido e 12 prove UI, per ciascuna scala
- 7502 / 7235 / 7065 ridimensionamenti dell'interfaccia, per scala

Le anteprime in `docs/img/premium/` sono state rigenerate per tutte le scale e
controllate una per una.

Il collaudo nel gioco resta escluso: le prove usano modelli sintetici e copie
dei file, senza modificare i salvataggi personali.
