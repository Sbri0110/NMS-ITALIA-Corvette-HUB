package it.nmsitalia.corvettehub.ui;

import it.nmsitalia.corvettehub.detect.SaveLocator;
import it.nmsitalia.corvettehub.domain.CatalogoParti;
import it.nmsitalia.corvettehub.domain.Compatibilita;
import it.nmsitalia.corvettehub.domain.Corvette;
import it.nmsitalia.corvettehub.domain.LettoreCorvette;
import it.nmsitalia.corvettehub.domain.SaveSlotInfo;
import it.nmsitalia.corvettehub.domain.WrapperBuild;
import it.nmsitalia.corvettehub.safety.ScrittoreSalvataggio;
import nomanssave.eV;
import nomanssave.eY;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Scheda Importa: porta un progetto dentro una Corvette del salvataggio.
 *
 * Percorso (specifica R4):
 *   1. si trascina il file nella finestra, oppure lo si sceglie;
 *   2. si vede l'anteprima: nome build, autore, numero di parti;
 *   3. controllo di compatibilita' PRIMA di scrivere, con l'elenco di cosa
 *      manca; si procede solo con conferma esplicita;
 *   4. backup verificato di entrambi i file dello slot;
 *   5. scrittura della nuova lista Objects sulla Corvette scelta;
 *   6. rilettura di conferma;
 *   7. riepilogo in chiaro.
 *
 * Si toccano SOLO il nome e la lista Objects della Corvette scelta. Non si
 * sblocca niente: le parti mancanti restano mancanti, e la build risultera'
 * incompleta finche' il giocatore non le ottiene in gioco.
 */
public final class SchedaImporta extends JPanel {

    private final JLabel zonaRilascio = new JLabel();
    private final JButton scegliFile = new JButton("Scegli file...");
    private final JTextArea anteprima = new JTextArea();
    private final JComboBox<String> sceltaCorvette = new JComboBox<String>();
    private final JCheckBox usaNomeBuild =
            new JCheckBox("Applica anche il nome della build", true);
    private final JTextArea compatibilita = new JTextArea();
    private final JTextArea esito = new JTextArea();
    private final JButton importa = new JButton("Importa");

    private SaveLocator.Rilevamento rilevamento;
    private SaveSlotInfo slot;
    private LettoreCorvette.Esito corrente;
    private WrapperBuild build;
    private File fileProgetto;

    public SchedaImporta() {
        setBackground(Theme.SFONDO);
        setLayout(new BorderLayout());
        add(costruisciSinistra(), BorderLayout.WEST);
        add(costruisciDestra(), BorderLayout.CENTER);
        add(costruisciPiede(), BorderLayout.SOUTH);
        aggiorna(null, null, null);
    }

    // ------------------------------------------------------------- struttura

    private JPanel costruisciSinistra() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(Scala.bordo(16, 16, 16, 10));
        p.setPreferredSize(Scala.dim(420, 100));

        JLabel t = new JLabel("Progetto da importare");
        t.setFont(Scala.font(Font.BOLD, 14));
        t.setForeground(Theme.TESTO);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);

        zonaRilascio.setText("Trascina qui il file .json del progetto");
        zonaRilascio.setHorizontalAlignment(JLabel.CENTER);
        zonaRilascio.setVerticalAlignment(JLabel.CENTER);
        zonaRilascio.setPreferredSize(Scala.dim(380, 84));
        zonaRilascio.setMaximumSize(Scala.dimLarga(84));
        zonaRilascio.setMinimumSize(Scala.dim(200, 84));
        zonaRilascio.setOpaque(true);
        zonaRilascio.setBackground(Theme.SUPERFICIE);
        zonaRilascio.setForeground(Theme.TESTO_TENUE);
        zonaRilascio.setBorder(BorderFactory.createDashedBorder(
                Theme.BORDO, 8, 5, 1.5f, true));
        zonaRilascio.setAlignmentX(Component.LEFT_ALIGNMENT);
        abilitaRilascio();

        scegliFile.setAlignmentX(Component.LEFT_ALIGNMENT);
        scegliFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                scegliFile();
            }
        });

        anteprima.setEditable(false);
        anteprima.setBackground(Theme.SUPERFICIE);
        anteprima.setForeground(Theme.TESTO);
        anteprima.setFont(Theme.monospazio());
        anteprima.setBorder(Scala.bordo(10, 12, 10, 12));
        JScrollPane scrollAnteprima = new JScrollPane(anteprima);
        scrollAnteprima.setBorder(BorderFactory.createLineBorder(Theme.BORDO));
        scrollAnteprima.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollAnteprima.setPreferredSize(Scala.dim(380, 260));
        scrollAnteprima.setMaximumSize(Scala.dimLarga(400));

        p.add(t);
        p.add(Box.createVerticalStrut(10));
        p.add(zonaRilascio);
        p.add(Box.createVerticalStrut(8));
        p.add(scegliFile);
        p.add(Box.createVerticalStrut(14));
        p.add(etichetta("Anteprima del progetto"));
        p.add(Box.createVerticalStrut(4));
        p.add(scrollAnteprima);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private Component costruisciDestra() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(Scala.bordo(16, 6, 0, 16));

        JLabel t = new JLabel("Corvette di destinazione");
        t.setFont(Scala.font(Font.BOLD, 14));
        t.setForeground(Theme.TESTO);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);

        sceltaCorvette.setAlignmentX(Component.LEFT_ALIGNMENT);
        sceltaCorvette.setMaximumSize(Scala.dimLarga(28));
        sceltaCorvette.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                calcolaCompatibilita();
            }
        });

        usaNomeBuild.setOpaque(false);
        usaNomeBuild.setForeground(Theme.TESTO_TENUE);
        usaNomeBuild.setFont(Scala.font(Font.PLAIN, 12));
        usaNomeBuild.setAlignmentX(Component.LEFT_ALIGNMENT);

        compatibilita.setEditable(false);
        compatibilita.setBackground(Theme.SUPERFICIE);
        compatibilita.setForeground(Theme.TESTO);
        compatibilita.setFont(Theme.monospazio());
        compatibilita.setBorder(Scala.bordo(10, 12, 10, 12));
        JScrollPane scrollCompat = new JScrollPane(compatibilita);
        scrollCompat.setBorder(BorderFactory.createLineBorder(Theme.BORDO));
        scrollCompat.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(t);
        p.add(Box.createVerticalStrut(6));
        p.add(sceltaCorvette);
        p.add(Box.createVerticalStrut(8));
        p.add(usaNomeBuild);
        p.add(Box.createVerticalStrut(14));
        p.add(etichetta("Controllo di compatibilita'"));
        p.add(Box.createVerticalStrut(4));
        p.add(scrollCompat);
        p.add(Box.createVerticalStrut(12));
        p.add(etichetta("Esito"));
        p.add(Box.createVerticalStrut(4));

        esito.setEditable(false);
        esito.setBackground(Theme.SUPERFICIE);
        esito.setForeground(Theme.TESTO);
        esito.setFont(Theme.monospazio());
        esito.setBorder(Scala.bordo(10, 12, 10, 12));
        JScrollPane scrollEsito = new JScrollPane(esito);
        scrollEsito.setBorder(BorderFactory.createLineBorder(Theme.BORDO));
        scrollEsito.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollEsito.setPreferredSize(Scala.dim(400, 150));
        p.add(scrollEsito);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JPanel costruisciPiede() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        p.setOpaque(false);
        p.setBorder(Scala.bordo(8, 16, 14, 16));

        importa.setFont(Scala.font(Font.BOLD, 13));
        importa.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                importaOra();
            }
        });
        p.add(importa);
        return p;
    }

    private JLabel etichetta(String testo) {
        JLabel l = new JLabel(testo);
        l.setForeground(Theme.TESTO_TENUE);
        l.setFont(Scala.font(Font.PLAIN, 12));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    // ------------------------------------------------------------ rilascio

    private void abilitaRilascio() {
        zonaRilascio.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                try {
                    Object dati = support.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    if (dati instanceof List) {
                        List<?> l = (List<?>) dati;
                        if (!l.isEmpty() && l.get(0) instanceof File) {
                            caricaFile((File) l.get(0));
                            return true;
                        }
                    }
                } catch (Exception e) {
                    // il file non e' trascinabile: si ignora
                }
                return false;
            }
        });
    }

    private void scegliFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Scegli il progetto Corvette");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Progetto Corvette (*.json)", "json"));
        File libreria = new File(ScrittoreSalvataggio.cartellaProgramma(), "Builds");
        if (libreria.isDirectory()) {
            fc.setCurrentDirectory(libreria);
        }
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            caricaFile(fc.getSelectedFile());
        }
    }

    private void caricaFile(File f) {
        if (f == null) {
            return;
        }
        try {
            build = WrapperBuild.leggi(f);
            fileProgetto = f;
        } catch (Exception e) {
            build = null;
            fileProgetto = null;
            anteprima.setText("Non riesco a leggere questo file:\n\n" + e.getMessage());
            compatibilita.setText("Questo file non e' un progetto Corvette valido.\n"
                    + "Attesi i campi 'format' e 'objects'.");
            importa.setEnabled(false);
            return;
        }
        zonaRilascio.setText("Progetto caricato");
        zonaRilascio.setBorder(BorderFactory.createDashedBorder(
                Theme.OK, 8, 5, 1.5f, true));

        StringBuilder b = new StringBuilder();
        b.append("PROGETTO\n\n");
        b.append("File       : ").append(f.getName()).append('\n');
        b.append("Nome build : ").append(build.getNome()).append('\n');
        b.append("Autore     : ").append(build.getAutore().isEmpty()
                ? "(non indicato)" : build.getAutore()).append('\n');
        b.append("Data UTC   : ").append(build.getDataUtc().isEmpty()
                ? "(non indicata)" : build.getDataUtc()).append('\n');
        b.append("Formato    : ").append(build.getFormatoDichiarato()).append('\n');
        b.append("Moduli     : ").append(build.getNumeroModuli()).append('\n');
        b.append("Tipi       : ").append(build.getPartiRichieste().size()).append('\n');

        b.append("\nCATEGORIE\n");
        java.util.Map<String, Integer> perCat = new java.util.TreeMap<String, Integer>();
        for (String id : build.getPartiRichieste()) {
            String c = CatalogoParti.categoria(id);
            Integer n = perCat.get(c);
            perCat.put(c, n == null ? 1 : n + 1);
        }
        for (java.util.Map.Entry<String, Integer> e : perCat.entrySet()) {
            b.append(String.format("   %-24s %d%n", e.getKey(), e.getValue()));
        }
        anteprima.setText(b.toString());
        anteprima.setCaretPosition(0);

        calcolaCompatibilita();
    }

    // ---------------------------------------------------------------- dati

    public void aggiorna(SaveLocator.Rilevamento rilevamento, SaveSlotInfo slot,
                         LettoreCorvette.Esito esitoSlot) {
        this.rilevamento = rilevamento;
        this.slot = slot;
        this.corrente = esitoSlot;

        sceltaCorvette.removeAllItems();
        if (esitoSlot == null || esitoSlot.corvette.isEmpty()) {
            sceltaCorvette.addItem("(nessuna Corvette in questo slot)");
            importa.setEnabled(false);
            compatibilita.setText("Questo slot non contiene Corvette.\n\n"
                    + "Le Corvette sono basi di tipo PlayerShipBase: se non ne\n"
                    + "compare nessuna, in questo salvataggio non ne hai ancora una.");
            return;
        }
        for (int i = 0; i < esitoSlot.corvette.size(); i++) {
            Corvette c = esitoSlot.corvette.get(i);
            sceltaCorvette.addItem((c.isAttiva() ? "\u26A0 " : "")
                    + c.getNome() + "   (" + c.getNumeroModuli() + " moduli)");
        }
        sceltaCorvette.setSelectedIndex(0);
        calcolaCompatibilita();
    }

    private Corvette corvetteScelta() {
        int i = sceltaCorvette.getSelectedIndex();
        if (corrente == null || i < 0 || i >= corrente.corvette.size()) {
            return null;
        }
        return corrente.corvette.get(i);
    }

    private void calcolaCompatibilita() {
        Corvette c = corvetteScelta();
        if (build == null) {
            importa.setEnabled(false);
            return;
        }
        if (c == null || slot == null) {
            importa.setEnabled(false);
            compatibilita.setText("Scegli prima la Corvette di destinazione.");
            return;
        }
        if (c.isAttiva()) {
            importa.setEnabled(false);
            compatibilita.setText("Questa e' la Corvette che stai usando in gioco.\n\n"
                    + "Il gioco la ricarica in memoria e sovrascriverebbe la build.\n"
                    + "Scegline un'altra.");
            return;
        }
        importa.setEnabled(true);

        Set<String> richieste = build.getPartiRichieste();
        Compatibilita comp = Compatibilita.calcola(richieste, slot.getModello());

        StringBuilder b = new StringBuilder();
        b.append("Destinazione: ").append(c.getNome()).append('\n');
        b.append("Moduli attuali: ").append(c.getNumeroModuli())
                .append("   ->   dopo l'importazione: ").append(build.getNumeroModuli())
                .append("\n\n");
        b.append("NEL DEPOSITO   ").append(comp.getNelDeposito()).append('\n');
        b.append("SBLOCCATE      ").append(comp.getSbloccate()).append('\n');
        b.append("MANCANTI       ").append(comp.getMancanti()).append("\n\n");

        if (comp.isDatiIncompleti()) {
            b.append("Attenzione: ").append(comp.getNota()).append('\n');
        } else if (comp.isCompleta()) {
            b.append("Tutte le parti sono almeno sbloccate: la build potra'\n");
            b.append("essere costruita per intero.\n");
        } else {
            b.append("Mancano ").append(comp.getMancanti()).append(" parti.\n");
            b.append("Puoi importare lo stesso, ma la Corvette potrebbe\n");
            b.append("risultare incompleta.\n");
        }

        List<String> mancanti = comp.getPartiMancanti();
        if (!mancanti.isEmpty()) {
            b.append("\nPARTI MANCANTI\n");
            for (int i = 0; i < mancanti.size(); i++) {
                b.append("   ").append(mancanti.get(i))
                        .append("   [").append(CatalogoParti.categoria(mancanti.get(i)))
                        .append("]\n");
            }
        }
        compatibilita.setText(b.toString());
        compatibilita.setCaretPosition(0);
    }

    // -------------------------------------------------------------- azione

    private void importaOra() {
        final Corvette destinazione = corvetteScelta();
        if (destinazione == null || build == null || rilevamento == null || slot == null) {
            return;
        }
        if (destinazione.isAttiva()) {
            JOptionPane.showMessageDialog(this,
                    "Questa e' la Corvette che stai usando in gioco.\n"
                            + "Scegline un'altra: il gioco la ricaricherebbe "
                            + "sovrascrivendo la build.",
                    "Corvette in uso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (ScrittoreSalvataggio.giocoInEsecuzione()) {
            JOptionPane.showMessageDialog(this,
                    "No Man's Sky e' in esecuzione.\n\nChiudi il gioco prima di "
                            + "importare, altrimenti il gioco potrebbe sovrascrivere "
                            + "le modifiche.",
                    "Gioco aperto", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Set<String> richieste = build.getPartiRichieste();
        Compatibilita comp = Compatibilita.calcola(richieste, slot.getModello());
        StringBuilder domanda = new StringBuilder();
        domanda.append("Sto per sostituire la build di una Corvette.\n\n");
        domanda.append("Corvette:      ").append(destinazione.getNome()).append('\n');
        domanda.append("Progetto:      ").append(build.getNome()).append('\n');
        domanda.append("Moduli:        ").append(destinazione.getNumeroModuli())
                .append("  ->  ").append(build.getNumeroModuli()).append('\n');
        if (usaNomeBuild.isSelected()) {
            domanda.append("Nome:          ").append(destinazione.getNome())
                    .append("  ->  ").append(build.getNome()).append('\n');
        }
        domanda.append('\n');
        if (comp.isCompleta()) {
            domanda.append("Tutte le parti sono disponibili.\n");
        } else {
            domanda.append("ATTENZIONE: mancano ").append(comp.getMancanti())
                    .append(" parti richieste. La Corvette potrebbe\n");
            domanda.append("risultare incompleta. L'elenco e' nel pannello.\n");
        }
        domanda.append("\nVerranno scritti ENTRAMBI i file dello slot, dopo una copia\n");
        domanda.append("di sicurezza verificata. Procedo?");

        int r = JOptionPane.showConfirmDialog(this, domanda.toString(),
                "Conferma importazione", JOptionPane.YES_NO_OPTION,
                comp.isCompleta() ? JOptionPane.QUESTION_MESSAGE
                        : JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }

        final int indiceBase = destinazione.getIndiceBase();
        final boolean cambiaNome = usaNomeBuild.isSelected();
        final String nomeBuild = build.getNome();
        final eV oggetti = build.getOggetti();

        importa.setEnabled(false);
        esito.setText("Scrivo...\n\nQuesta operazione richiede qualche secondo: "
                + "converte il salvataggio, fa il backup, riscrive e rilegge.");
        esito.setCaretPosition(0);

        final SaveLocator.Rilevamento rl = rilevamento;
        final SaveSlotInfo sl = slot;

        new SwingWorker<ScrittoreSalvataggio.Esito, Void>() {
            @Override
            protected ScrittoreSalvataggio.Esito doInBackground() {
                return ScrittoreSalvataggio.scrivi(rl, sl,
                        new ScrittoreSalvataggio.Modifica() {
                            @Override
                            public String descrizione() {
                                return "importazione della build \"" + nomeBuild + "\"";
                            }

                            @Override
                            public void applica(eY radice) {
                                eY stato = radice.H("PlayerStateData");
                                eV basi = stato.d("PersistentPlayerBases");
                                eY base = basi.V(indiceBase);
                                // SOLO la lista oggetti, e il nome se richiesto.
                                // Niente altro: nessuno sblocco di parti.
                                base.b("Objects", oggetti);
                                if (cambiaNome) {
                                    base.b("Name", nomeBuild);
                                }
                                base.b("LastUpdateTimestamp",
                                        Integer.valueOf((int) (System.currentTimeMillis() / 1000L)));
                            }
                        });
            }

            @Override
            protected void done() {
                importa.setEnabled(true);
                try {
                    ScrittoreSalvataggio.Esito e = get();
                    StringBuilder b = new StringBuilder();
                    b.append(e.riuscito ? "IMPORTAZIONE RIUSCITA\n\n"
                            : "IMPORTAZIONE NON RIUSCITA\n\n");
                    b.append(e.messaggio).append("\n\n");
                    if (e.dettaglio != null) {
                        b.append(e.dettaglio);
                    }
                    esito.setText(b.toString());
                    esito.setCaretPosition(0);
                    if (!e.riuscito) {
                        JOptionPane.showMessageDialog(SchedaImporta.this, e.messaggio,
                                "Non riuscito", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    esito.setText("Errore inatteso:\n" + ex);
                }
            }
        }.execute();
    }
}
