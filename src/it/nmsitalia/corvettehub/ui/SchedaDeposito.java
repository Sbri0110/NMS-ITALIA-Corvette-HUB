package it.nmsitalia.corvettehub.ui;

import it.nmsitalia.corvettehub.detect.SaveLocator;
import it.nmsitalia.corvettehub.domain.LayoutInventario;
import it.nmsitalia.corvettehub.safety.ScrittoreSalvataggio;
import nomanssave.eY;
import it.nmsitalia.corvettehub.domain.CatalogoParti;
import it.nmsitalia.corvettehub.domain.Compatibilita;
import it.nmsitalia.corvettehub.domain.LettoreInventari;
import it.nmsitalia.corvettehub.domain.SaveSlotInfo;
import it.nmsitalia.corvettehub.domain.WrapperBuild;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Scheda Deposito: sola consultazione.
 *
 * I moduli del deposito della Stazione Spaziale si mostrano come una GRIGLIA di
 * riquadri, non come una tabella a colonne: le icone del gioco sono quadrate e
 * a 128 pixel, e in una riga alta 30 verrebbero tagliate. Nel riquadro si
 * vedono bene e si riconoscono a colpo d'occhio.
 *
 * Le categorie sono pulsanti cliccabili con il numero di moduli che contengono,
 * cosi' si vede subito com'e' distribuito il deposito.
 *
 * Non esiste alcun comando che aggiunga, sblocchi o modifichi moduli: questa
 * scheda legge e basta.
 */
public final class SchedaDeposito extends JPanel implements SchedaModificabile {

    private final JLabel intestazione = new JLabel();
    private final JLabel contatore = new JLabel();
    private final JTextField cerca = new JTextField(18);
    private final JLabel riepilogo = new JLabel(" ");
    private final JPanel barraCategorie = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
    private final JPanel griglia = new JPanel();
    private final JComboBox<String> sceltaBuild = new JComboBox<String>();
    private final JTextArea compatibilita = new JTextArea();

    private String categoriaScelta = "Tutte";
    private SaveSlotInfo slot;
    private List<LettoreInventari.Voce> voci = new ArrayList<LettoreInventari.Voce>();
    private List<File> buildDisponibili = new ArrayList<File>();
    private int larghezza = 10;
    private int altezza = 16;
    private final File cartellaLibreria;

    private SaveLocator.Rilevamento rilevamento;
    private LayoutInventario layout;
    private SchedaModificabile.AscoltatoreModifiche ascoltatoreModifiche;
    private boolean ultimoStatoModifiche;
    private boolean salvaInCorso;

    public SchedaDeposito(File cartellaLibreria) {
        this.cartellaLibreria = cartellaLibreria;
        setBackground(Theme.SFONDO);
        setLayout(new BorderLayout());
        add(costruisciTestata(), BorderLayout.NORTH);
        add(costruisciCorpo(), BorderLayout.CENTER);
        add(costruisciPiede(), BorderLayout.SOUTH);
    }

    // ------------------------------------------------------------- struttura

    private JPanel costruisciTestata() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(Scala.bordo(12, 16, 8, 16));

        intestazione.setFont(Scala.font(Font.BOLD, 14));
        intestazione.setForeground(Theme.TESTO);
        intestazione.setAlignmentX(Component.LEFT_ALIGNMENT);

        contatore.setFont(Scala.font(Font.PLAIN, 12));
        contatore.setForeground(Theme.ACCENTO);
        contatore.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel rigaCerca = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        rigaCerca.setOpaque(false);
        rigaCerca.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel l2 = new JLabel("Cerca:");
        l2.setForeground(Theme.TESTO_TENUE);
        cerca.setPreferredSize(Scala.dim(240, 26));
        rigaCerca.add(l2);
        rigaCerca.add(cerca);

        cerca.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                aggiornaFiltro();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                aggiornaFiltro();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                aggiornaFiltro();
            }
        });

        barraCategorie.setOpaque(false);
        barraCategorie.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(intestazione);
        p.add(Box.createVerticalStrut(4));
        p.add(contatore);
        p.add(Box.createVerticalStrut(10));
        p.add(rigaCerca);
        p.add(Box.createVerticalStrut(4));
        p.add(barraCategorie);
        return p;
    }

    private Component costruisciCorpo() {
        griglia.setLayout(new BoxLayout(griglia, BoxLayout.Y_AXIS));
        griglia.setBackground(Theme.SFONDO);
        griglia.setBorder(Scala.bordo(8, 8, 8, 8));

        JScrollPane scrollGriglia = new JScrollPane(griglia);
        scrollGriglia.setBorder(BorderFactory.createLineBorder(Theme.BORDO));
        scrollGriglia.getVerticalScrollBar().setUnitIncrement(Scala.px(28));

        JPanel sinistra = new JPanel(new BorderLayout());
        sinistra.setOpaque(false);
        sinistra.setBorder(Scala.bordo(0, 8, 0, 4));
        sinistra.add(scrollGriglia, BorderLayout.CENTER);

        compatibilita.setEditable(false);
        compatibilita.setBackground(Theme.SUPERFICIE);
        compatibilita.setForeground(Theme.TESTO);
        compatibilita.setFont(Theme.monospazio());
        compatibilita.setBorder(Scala.bordo(10, 12, 10, 12));

        JPanel destra = new JPanel(new BorderLayout());
        destra.setOpaque(false);
        destra.setMinimumSize(Scala.dim(320, 100));

        JPanel testaDestra = new JPanel();
        testaDestra.setOpaque(false);
        testaDestra.setLayout(new BoxLayout(testaDestra, BoxLayout.Y_AXIS));

        JLabel t2 = new JLabel("Compatibilita' con una build");
        t2.setFont(Theme.sezione());
        t2.setForeground(Theme.TESTO_TENUE);
        t2.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel rigaBuild = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        rigaBuild.setOpaque(false);
        rigaBuild.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lb = new JLabel("Build:");
        lb.setForeground(Theme.TESTO_TENUE);
        rigaBuild.add(lb);
        sceltaBuild.setPreferredSize(Scala.dim(210, 26));
        sceltaBuild.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                calcolaCompatibilita();
            }
        });
        rigaBuild.add(sceltaBuild);

        testaDestra.add(t2);
        testaDestra.add(Box.createVerticalStrut(6));
        testaDestra.add(rigaBuild);

        JScrollPane scrollCompat = new JScrollPane(compatibilita);
        scrollCompat.setBorder(BorderFactory.createLineBorder(Theme.BORDO));

        destra.add(testaDestra, BorderLayout.NORTH);
        destra.add(scrollCompat, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sinistra, destra);
        split.setDividerLocation(700);
        split.setResizeWeight(0.7);
        split.setBorder(null);
        split.setOpaque(false);
        return split;
    }

    private JPanel costruisciPiede() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(Scala.bordo(10, 16, 14, 16));

        riepilogo.setForeground(Theme.TESTO_TENUE);
        riepilogo.setFont(Scala.font(Font.PLAIN, 12));

        JButton esporta = new JButton("Esporta elenco (CSV)...");
        esporta.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                esportaElenco();
            }
        });

        JPanel destra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        destra.setOpaque(false);
        destra.add(esporta);

        p.add(riepilogo, BorderLayout.WEST);
        p.add(destra, BorderLayout.EAST);
        return p;
    }

    // ---------------------------------------------------------------- dati

    /**
     * Ricarica il deposito dallo slot scelto.
     *
     * Si mostra SOLO il deposito della Stazione Spaziale. I contenitori delle
     * basi (Chest1..Chest10) sono un'altra cosa e non stanno sulla nave: per
     * questo non compaiono qui.
     */
    public void aggiorna(SaveLocator.Rilevamento rilevamento, SaveSlotInfo slot,
                         String descrizioneSlot) {
        this.rilevamento = rilevamento;
        this.layout = null;
        this.slot = slot;
        voci = new ArrayList<LettoreInventari.Voce>();
        larghezza = 10;
        altezza = 16;
        if (slot != null) {
            LettoreInventari.Deposito d = LettoreInventari.depositoCorvette(slot.getModello());
            voci = d.voci;
            int[] dim = LettoreInventari.dimensioniDepositoCorvette(slot.getModello());
            larghezza = dim[0];
            altezza = dim[1];
            // la disposizione serve per poter trascinare i moduli
            nomanssave.eY dep = depositoDi(slot.getModello());
            if (dep != null) {
                layout = LayoutInventario.leggi(dep, "Deposito moduli");
            }
        }

        intestazione.setText("Deposito moduli  ·  Stazione Spaziale"
                + (descrizioneSlot == null ? "" : "   —   " + descrizioneSlot));

        if (slot == null) {
            contatore.setText("Nessuno slot selezionato.");
        } else {
            int distinti = new java.util.LinkedHashSet<String>(idDistinti()).size();
            int pezzi = 0;
            for (int i = 0; i < voci.size(); i++) {
                pezzi += voci.get(i).quantita;
            }
            contatore.setText(distinti + " moduli distinti  ·  " + voci.size()
                    + " caselle occupate su " + (larghezza * altezza)
                    + "  ·  " + pezzi + " pezzi totali");
        }

        CatalogoParti cat = CatalogoParti.get();
        riepilogo.setText(cat.getTotale() + " parti costruibili su una Corvette: "
                + cat.getStrutturali() + " strutturali, " + cat.getDecorative()
                + " decorative, " + cat.getInEntrambe() + " in entrambi gli elenchi.");

        aggiornaCategorie();
        aggiornaFiltro();
        ricaricaBuild();
    }

    private List<String> idDistinti() {
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < voci.size(); i++) {
            out.add(voci.get(i).id);
        }
        return out;
    }

    /**
     * Ridisegna la griglia.
     *
     * Senza filtri si mostra la griglia INTERA del deposito, caselle libere
     * comprese: e' quello che fa il gioco, e si vede a colpo d'occhio quanto
     * spazio resta. Con un filtro attivo si mostrano solo i moduli che
     * corrispondono, sempre su larghezza caselle per riga.
     */
    private void aggiornaFiltro() {
        griglia.removeAll();
        String q = cerca.getText() == null ? ""
                : cerca.getText().trim().toLowerCase(Locale.ROOT);
        boolean filtrato = !"Tutte".equals(categoriaScelta) || !q.isEmpty();

        List<LettoreInventari.Voce> mostrate = new ArrayList<LettoreInventari.Voce>();
        for (int i = 0; i < voci.size(); i++) {
            LettoreInventari.Voce v = voci.get(i);
            String c = CatalogoParti.categoria(v.id);
            if (!"Tutte".equals(categoriaScelta) && !categoriaScelta.equals(c)) {
                continue;
            }
            if (!q.isEmpty() && !v.id.toLowerCase(Locale.ROOT).contains(q)
                    && !c.toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            mostrate.add(v);
        }

        if (mostrate.isEmpty()) {
            griglia.add(messaggio(voci.isEmpty()
                    ? "Il deposito di questo slot e' vuoto."
                    : "Nessun modulo corrisponde al filtro."));
        } else {
            int righe = filtrato
                    ? (mostrate.size() + larghezza - 1) / larghezza
                    : altezza;
            // la griglia sta in un pannello a se', allineato a sinistra: cosi'
            // le caselle restano quadrate invece di allargarsi
            JPanel riga = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            riga.setOpaque(false);
            riga.setAlignmentX(Component.LEFT_ALIGNMENT);
            if (!filtrato && layout != null) {
                // griglia intera e modificabile: si trascinano i moduli
                riga.add(new GrigliaModuli(layout, new GrigliaModuli.AscoltatoreSpostamento() {
                    @Override
                    public void spostato(int daX, int daY, int aX, int aY) {
                        if (layout.sposta(daX, daY, aX, aY)) {
                            aggiornaFiltro();
                            avvisaModifiche();
                        }
                    }
                }));
            } else {
                // con un filtro attivo la griglia mostra solo i moduli trovati:
                // li' non si trascina, perche' le posizioni non sarebbero quelle vere
                riga.add(new GrigliaModuli(larghezza, righe, mostrate));
            }
            griglia.add(riga);
        }
        griglia.revalidate();
        griglia.repaint();
    }

    // ------------------------------------------------------------ modifica

    @Override
    public void setAscoltatoreModifiche(SchedaModificabile.AscoltatoreModifiche a) {
        this.ascoltatoreModifiche = a;
    }

    @Override
    public boolean haModifiche() {
        return !salvaInCorso && layout != null && layout.modificato();
    }

    private void avvisaModifiche() {
        boolean modificato = haModifiche();
        if (modificato != ultimoStatoModifiche) {
            ultimoStatoModifiche = modificato;
            if (ascoltatoreModifiche != null) {
                ascoltatoreModifiche.modificheCambiate(modificato);
            }
        }
    }

    @Override
    public void annullaModifiche() {
        if (layout != null) {
            layout.annulla();
        }
        aggiornaFiltro();
        avvisaModifiche();
    }

    @Override
    public void salvaModifiche() {
        if (layout == null || rilevamento == null || slot == null || !layout.modificato()) {
            return;
        }
        if (ScrittoreSalvataggio.giocoInEsecuzione()) {
            JOptionPane.showMessageDialog(this,
                    "No Man's Sky e' in esecuzione.\n\nChiudi il gioco prima di salvare, "
                            + "altrimenti il gioco potrebbe sovrascrivere le modifiche.",
                    "Gioco aperto", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int n = layout.quantiSpostati();
        int r = JOptionPane.showConfirmDialog(this,
                "Sto per salvare la nuova disposizione del deposito.\n\n"
                        + n + " oggetti spostati.\n"
                        + "Nessun oggetto viene creato o perso: cambiano solo le posizioni.\n\n"
                        + "Verranno scritti ENTRAMBI i file dello slot, dopo una copia di\n"
                        + "sicurezza verificata. Procedo?",
                "Conferma salvataggio", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }

        final SaveLocator.Rilevamento rl = rilevamento;
        final SaveSlotInfo sl = slot;
        final LayoutInventario lay = layout;
        salvaInCorso = true;
        avvisaModifiche();

        new javax.swing.SwingWorker<ScrittoreSalvataggio.Esito, Void>() {
            @Override
            protected ScrittoreSalvataggio.Esito doInBackground() {
                return ScrittoreSalvataggio.scrivi(rl, sl,
                        new ScrittoreSalvataggio.Modifica() {
                            @Override
                            public String descrizione() {
                                return "spostamento di " + lay.quantiSpostati()
                                        + " oggetti nel deposito";
                            }

                            @Override
                            public void applica(eY radice) {
                                // il modello arriva fresco dal file: si riapplica
                                eY dep = radice.H("PlayerStateData").H("CorvetteStorageInventory");
                                lay.applica(dep);
                            }
                        });
            }

            @Override
            protected void done() {
                salvaInCorso = false;
                try {
                    ScrittoreSalvataggio.Esito e = get();
                    JOptionPane.showMessageDialog(SchedaDeposito.this,
                            (e.dettaglio == null ? e.messaggio : e.dettaglio),
                            e.riuscito ? "Salvato" : "Non riuscito",
                            e.riuscito ? JOptionPane.INFORMATION_MESSAGE
                                    : JOptionPane.ERROR_MESSAGE);
                    if (e.riuscito) {
                        layout = null;
                        aggiorna(rilevamento, slot, null);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(SchedaDeposito.this,
                            "Errore inatteso:\n" + ex, "Errore",
                            JOptionPane.ERROR_MESSAGE);
                }
                avvisaModifiche();
            }
        }.execute();
    }

    /** Il nodo del deposito della Corvette. */
    private nomanssave.eY depositoDi(nomanssave.eY radice) {
        if (radice == null) {
            return null;
        }
        try {
            return radice.H("PlayerStateData").H("CorvetteStorageInventory");
        } catch (Throwable t) {
            return null;
        }
    }

    private JLabel messaggio(String testo) {
        JLabel l = new JLabel(testo);
        l.setForeground(Theme.TESTO_TENUE);
        l.setFont(Scala.font(Font.PLAIN, 12));
        l.setBorder(Scala.bordo(20, 8, 20, 8));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    /** Le categorie cliccabili, con quanti moduli contengono. */
    private void aggiornaCategorie() {
        barraCategorie.removeAll();

        java.util.Map<String, Integer> conteggi = new java.util.TreeMap<String, Integer>();
        for (int i = 0; i < voci.size(); i++) {
            String c = CatalogoParti.categoria(voci.get(i).id);
            Integer n = conteggi.get(c);
            conteggi.put(c, n == null ? 1 : n + 1);
        }

        barraCategorie.add(new Chip("Tutte", voci.size()));
        for (java.util.Map.Entry<String, Integer> e : conteggi.entrySet()) {
            barraCategorie.add(new Chip(e.getKey(), e.getValue().intValue()));
        }
        barraCategorie.revalidate();
        barraCategorie.repaint();
    }

    private void ricaricaBuild() {
        buildDisponibili = WrapperBuild.elencoLibreria(cartellaLibreria);
        DefaultComboBoxModel<String> m = new DefaultComboBoxModel<String>();
        m.addElement("(nessuna)");
        for (int i = 0; i < buildDisponibili.size(); i++) {
            m.addElement(buildDisponibili.get(i).getName());
        }
        String scelta = (String) sceltaBuild.getSelectedItem();
        sceltaBuild.setModel(m);
        if (scelta != null && m.getIndexOf(scelta) >= 0) {
            sceltaBuild.setSelectedItem(scelta);
        }
        calcolaCompatibilita();
    }

    /** Chiamato dalla scheda Libreria quando cambia il contenuto di Builds/. */
    public void ricaricaLibreria() {
        ricaricaBuild();
    }

    private void calcolaCompatibilita() {
        int i = sceltaBuild.getSelectedIndex();
        if (i <= 0 || i - 1 >= buildDisponibili.size() || slot == null) {
            compatibilita.setText(slot == null
                    ? "Nessuno slot selezionato."
                    : "Scegli una build per vedere la compatibilita' con questo "
                      + "salvataggio.\n\nI progetti stanno nella scheda Libreria.");
            return;
        }
        File f = buildDisponibili.get(i - 1);
        StringBuilder b = new StringBuilder();
        try {
            WrapperBuild build = WrapperBuild.leggi(f);
            Set<String> richieste = build.getPartiRichieste();
            Compatibilita c = Compatibilita.calcola(richieste, slot.getModello());

            b.append(build.getNome()).append('\n');
            if (!build.getAutore().isEmpty()) {
                b.append("di ").append(build.getAutore()).append('\n');
            }
            b.append(build.getNumeroModuli()).append(" moduli, ")
                    .append(richieste.size()).append(" tipi distinti\n\n");

            b.append("NEL DEPOSITO        ").append(c.getNelDeposito()).append('\n');
            b.append("SBLOCCATE           ").append(c.getSbloccate()).append('\n');
            b.append("MANCANTI            ").append(c.getMancanti()).append('\n');

            if (c.isDatiIncompleti()) {
                b.append("\nAttenzione: ").append(c.getNota()).append('\n');
            } else if (c.isCompleta()) {
                b.append("\nTutte le parti sono almeno sbloccate: la build puo'\n");
                b.append("essere costruita per intero.\n");
            } else {
                b.append("\nMancano ").append(c.getMancanti())
                        .append(" parti: la Corvette risultera' incompleta.\n");
            }

            List<String> mancanti = c.getPartiMancanti();
            if (!mancanti.isEmpty()) {
                b.append("\nPARTI MANCANTI\n");
                for (int k = 0; k < mancanti.size(); k++) {
                    b.append("   ").append(mancanti.get(k))
                            .append("   [")
                            .append(CatalogoParti.categoria(mancanti.get(k)))
                            .append("]\n");
                }
            }
            List<String> sbloccate = c.getPartiSbloccateNonInDeposito();
            if (!sbloccate.isEmpty()) {
                b.append("\nSBLOCCATE MA NON IN DEPOSITO\n");
                for (int k = 0; k < sbloccate.size(); k++) {
                    b.append("   ").append(sbloccate.get(k)).append('\n');
                }
            }
        } catch (Exception e) {
            b.append("Non riesco a leggere questo progetto:\n").append(e.getMessage());
        }
        compatibilita.setText(b.toString());
        compatibilita.setCaretPosition(0);
    }

    private void esportaElenco() {
        if (voci.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Il deposito e' vuoto.",
                    "Niente da esportare", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Salva l'elenco del deposito");
        fc.setSelectedFile(new File("deposito-corvette.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Writer w = null;
        try {
            w = new OutputStreamWriter(new FileOutputStream(fc.getSelectedFile()),
                    Charset.forName("UTF-8"));
            w.write("Id;Categoria;Quantita'\n");
            for (int i = 0; i < voci.size(); i++) {
                LettoreInventari.Voce v = voci.get(i);
                w.write(v.id + ";" + CatalogoParti.categoria(v.id) + ";" + v.quantita + "\n");
            }
            w.flush();
            JOptionPane.showMessageDialog(this,
                    "Elenco esportato in:\n" + fc.getSelectedFile().getAbsolutePath(),
                    "Fatto", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Errore nella scrittura:\n" + e.getMessage(),
                    "Errore", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (w != null) {
                try {
                    w.close();
                } catch (Exception ignored) {
                    // nulla da fare
                }
            }
        }
    }

    // ------------------------------------------------------------ riquadri

    /** Toglie il carattere ^: nel salvataggio c'e', ma all'utente non serve. */
    static String nomeLeggibile(String id) {
        if (id == null) {
            return "";
        }
        return id.startsWith("^") ? id.substring(1) : id;
    }


    /** Una categoria cliccabile, con il numero di moduli che contiene. */
    private final class Chip extends JButton {

        private final String categoria;
        private final boolean scelta;

        Chip(String categoria, int quanti) {
            this.categoria = categoria;
            this.scelta = categoria.equals(categoriaScelta);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setText(categoria + "  " + quanti);
            setFont(Scala.font(scelta ? Font.BOLD : Font.PLAIN, 11));
            setForeground(scelta ? Color.WHITE : Theme.TESTO);
            setMargin(new java.awt.Insets(4, 12, 4, 12));
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            setToolTipText("Mostra solo: " + categoria);
            addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    categoriaScelta = Chip.this.categoria;
                    aggiornaCategorie();
                    aggiornaFiltro();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 1;
            int h = getHeight() - 1;
            if (scelta) {
                g2.setColor(Theme.ACCENTO_SCURO);
                g2.fillRoundRect(0, 0, w, h, h, h);
                g2.setColor(Theme.ACCENTO);
                g2.setStroke(new BasicStroke(0.8f));
                g2.drawRoundRect(0, 0, w, h, h, h);
            } else if (getModel().isRollover()) {
                g2.setColor(Theme.SUPERFICIE_ALTA);
                g2.fillRoundRect(0, 0, w, h, h, h);
                g2.setColor(Theme.BORDO);
                g2.setStroke(new BasicStroke(0.6f));
                g2.drawRoundRect(0, 0, w, h, h, h);
            }
            super.paintComponent(g);
        }
    }
}
