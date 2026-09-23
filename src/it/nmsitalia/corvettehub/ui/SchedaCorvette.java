package it.nmsitalia.corvettehub.ui;

import it.nmsitalia.corvettehub.detect.SaveLocator;
import it.nmsitalia.corvettehub.domain.CatalogoParti;
import it.nmsitalia.corvettehub.domain.Corvette;
import it.nmsitalia.corvettehub.domain.LayoutInventario;
import it.nmsitalia.corvettehub.domain.LettoreCorvette;
import it.nmsitalia.corvettehub.domain.LettoreInventari;
import it.nmsitalia.corvettehub.domain.SaveSlotInfo;
import it.nmsitalia.corvettehub.domain.StatisticheCorvette;
import it.nmsitalia.corvettehub.safety.ScrittoreSalvataggio;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import nomanssave.eY;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Scheda Corvette: la carta d'identita' della nave scelta.
 *
 * Mostra, tutto insieme:
 *   - le statistiche (danni, scudi, iperguida, agilita'), la classe e gli slot
 *     supercaricati;
 *   - le tecnologie installate, con l'icona del gioco;
 *   - l'inventario di bordo;
 *   - i depositi montati sulla nave, se ce ne sono.
 *
 * Sola lettura: qui non si modifica niente.
 */
public final class SchedaCorvette extends JPanel implements SchedaModificabile {

    private final JComboBox<String> sceltaCorvette = new JComboBox<String>();
    private final JPanel contenuto = new JPanel();
    private SchedaModificabile.AscoltatoreModifiche ascoltatoreModifiche;

    private SaveLocator.Rilevamento rilevamento;
    private SaveSlotInfo slot;
    private LettoreCorvette.Esito corrente;
    private LayoutInventario layoutInventario;
    private LayoutInventario layoutTecnologie;
    private boolean ultimoStatoModifiche;
    private boolean salvaInCorso;

    /** Chi vuole sapere se ci sono spostamenti da salvare. */
    public void setAscoltatoreModifiche(SchedaModificabile.AscoltatoreModifiche a) {
        this.ascoltatoreModifiche = a;
    }

    /** Vero se ci sono spostamenti non ancora salvati. */
    public boolean haModifiche() {
        return !salvaInCorso
                && ((layoutInventario != null && layoutInventario.modificato())
                || (layoutTecnologie != null && layoutTecnologie.modificato()));
    }

    /** Quanti oggetti si sono spostati in tutto. */
    private int quantiSpostati() {
        int n = 0;
        if (layoutInventario != null) {
            n += layoutInventario.quantiSpostati();
        }
        if (layoutTecnologie != null) {
            n += layoutTecnologie.quantiSpostati();
        }
        return n;
    }

    /** Salva la disposizione in lavorazione. */
    public void salvaModifiche() {
        salvaInventario();
    }

    /** Butta via gli spostamenti non salvati. */
    public void annullaModifiche() {
        if (layoutInventario != null) {
            layoutInventario.annulla();
        }
        if (layoutTecnologie != null) {
            layoutTecnologie.annulla();
        }
        mostra();
    }

    public SchedaCorvette() {
        setBackground(Theme.SFONDO);
        setLayout(new BorderLayout());

        JPanel testata = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        testata.setOpaque(false);
        testata.setBorder(BorderFactory.createEmptyBorder(4, 8, 0, 8));
        JLabel l = new JLabel("Corvette:");
        l.setForeground(Theme.TESTO_TENUE);
        sceltaCorvette.setPreferredSize(new Dimension(320, 28));
        sceltaCorvette.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // cambiando nave si rilegge la disposizione di quella nuova
                layoutInventario = null;
                mostra();
            }
        });
        testata.add(l);
        testata.add(sceltaCorvette);

        contenuto.setBackground(Theme.SFONDO);
        contenuto.setLayout(new BoxLayout(contenuto, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(contenuto);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(28);

        add(testata, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    // ---------------------------------------------------------------- dati

    public void aggiorna(SaveLocator.Rilevamento rilevamento, SaveSlotInfo slot,
                         LettoreCorvette.Esito esito) {
        this.rilevamento = rilevamento;
        this.slot = slot;
        this.corrente = esito;
        this.layoutInventario = null;
        this.layoutTecnologie = null;

        sceltaCorvette.removeAllItems();
        if (esito == null || esito.corvette.isEmpty()) {
            sceltaCorvette.addItem("(nessuna Corvette)");
            contenuto.removeAll();
            contenuto.add(messaggio("Questo slot non contiene Corvette."));
            contenuto.revalidate();
            contenuto.repaint();
            return;
        }
        for (int i = 0; i < esito.corvette.size(); i++) {
            Corvette c = esito.corvette.get(i);
            sceltaCorvette.addItem((c.isAttiva() ? "\u26A0 " : "") + c.getNome());
        }
        sceltaCorvette.setSelectedIndex(0);
    }

    private Corvette corvetteScelta() {
        int i = sceltaCorvette.getSelectedIndex();
        if (corrente == null || i < 0 || i >= corrente.corvette.size()) {
            return null;
        }
        return corrente.corvette.get(i);
    }

    private void mostra() {
        contenuto.removeAll();
        Corvette c = corvetteScelta();
        if (c == null || slot == null) {
            contenuto.add(messaggio("Scegli una Corvette."));
            contenuto.revalidate();
            contenuto.repaint();
            return;
        }

        StatisticheCorvette st = StatisticheCorvette.leggi(slot.getModello(), c);

        contenuto.add(intestazione(c, st));
        contenuto.add(sezione("Statistiche", pannelloStatistiche(st)));
        contenuto.add(sezione("Tecnologie installate",
                pannelloModificabile(c, "Inventory_TechOnly", true)));
        contenuto.add(sezione("Inventario di bordo",
                pannelloModificabile(c, "Inventory", false)));
        contenuto.add(sezione("Depositi montati", pannelloDepositi(st)));
        contenuto.add(Box.createVerticalStrut(16));

        contenuto.revalidate();
        contenuto.repaint();

        // accende o spegne i pulsanti in alto, ma solo quando cambia qualcosa
        boolean modificato = haModifiche();
        if (modificato != ultimoStatoModifiche) {
            ultimoStatoModifiche = modificato;
            if (ascoltatoreModifiche != null) {
                ascoltatoreModifiche.modificheCambiate(modificato);
            }
        }
    }

    private LettoreInventari.Deposito inventario(Corvette c) {
        LettoreInventari.Deposito d = new LettoreInventari.Deposito("Inventario",
                "ShipOwnership[" + c.getIndiceNave() + "].Inventory");
        nomanssave.eY radice = slot.getModello();
        if (radice == null) {
            return d;
        }
        try {
            nomanssave.eY stato = radice.H("PlayerStateData");
            nomanssave.eV navi = stato.d("ShipOwnership");
            if (c.getIndiceNave() >= 0 && c.getIndiceNave() < navi.size()) {
                nomanssave.eY nave = navi.V(c.getIndiceNave());
                d.voci.addAll(LettoreInventari.leggiVoci(nave.H("Inventory")));
            }
        } catch (Throwable t) {
            // inventario non leggibile
        }
        return d;
    }

    // ------------------------------------------------------------ pannelli

    /**
     * L'inventario di bordo, con la possibilita' di spostare le cose col mouse.
     *
     * Lo spostamento cambia solo la COORDINATA degli oggetti: non se ne crea e
     * non se ne distrugge nessuno. Le modifiche restano in memoria finche' non
     * si preme Salva, che scrive sul salvataggio con le stesse cautele delle
     * altre scritture.
     */
    /**
     * Una griglia modificabile: le tecnologie oppure l'inventario.
     *
     * Lo spostamento cambia solo la COORDINATA degli oggetti: non se ne crea e
     * non se ne distrugge nessuno. Le modifiche restano in memoria finche' non
     * si preme Salva, che scrive sul salvataggio con le stesse cautele delle
     * altre scritture.
     */
    private JPanel pannelloModificabile(Corvette c, String campo, boolean tecnologia) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        nomanssave.eY inv = inventarioDi(c, campo);
        if (inv == null) {
            p.add(testo("Questo salvataggio non espone questa parte della nave."));
            return p;
        }
        // La disposizione si legge UNA VOLTA SOLA. Se la rileggessi a ogni
        // ridisegno, lo spostamento appena fatto verrebbe cancellato: il
        // modello non e' ancora stato scritto, quindi rileggendolo si torna
        // alla disposizione di partenza.
        if (tecnologia) {
            if (layoutTecnologie == null) {
                layoutTecnologie = LayoutInventario.leggi(inv, "Tecnologie installate");
            }
        } else {
            if (layoutInventario == null) {
                layoutInventario = LayoutInventario.leggi(inv, "Inventario di bordo");
            }
        }
        final LayoutInventario lay = tecnologia ? layoutTecnologie : layoutInventario;

        // la fascia di stato sta SOPRA la griglia: la griglia e' alta piu' di
        // 700 pixel e una fascia messa sotto finirebbe fuori dalla vista
        p.add(fascia(lay, tecnologia));

        JPanel riga = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        riga.setOpaque(false);
        riga.setAlignmentX(Component.LEFT_ALIGNMENT);
        riga.add(new GrigliaModuli(lay, new GrigliaModuli.AscoltatoreSpostamento() {
            @Override
            public void spostato(int daX, int daY, int aX, int aY) {
                if (lay.sposta(daX, daY, aX, aY)) {
                    mostra();
                }
            }
        }));
        p.add(riga);
        return p;
    }

    /** La fascia che dice se ci sono spostamenti da salvare. */
    private JPanel fascia(LayoutInventario lay, boolean tecnologia) {
        JPanel b = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        b.setOpaque(true);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel scritta = new JLabel();
        if (lay.modificato()) {
            int n = lay.quantiSpostati();
            b.setBackground(Theme.AVVISO_SCURO);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.AVVISO),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            scritta.setText(n + (n == 1 ? " oggetto spostato" : " oggetti spostati")
                    + ": usa Salva le modifiche nella barra in alto");
            scritta.setForeground(Theme.TESTO);
            scritta.setFont(new Font("Segoe UI", Font.BOLD, 12));
        } else {
            b.setBackground(Theme.SFONDO);
            b.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
            scritta.setText("Trascina un oggetto per spostarlo   ·   "
                    + lay.getNumeroPezzi() + " su "
                    + (lay.getLarghezza() * lay.getAltezza()) + " celle"
                    + (tecnologia
                    ? "   ·   spostare le tecnologie cambia i bonus di adiacenza" : ""));
            scritta.setForeground(Theme.TESTO_TENUE);
            scritta.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        }
        b.add(scritta);
        return b;
    }

    /** Per le prove automatiche: la disposizione in lavorazione. */
    LayoutInventario layoutCorrente() {
        return layoutInventario;
    }

    /** Per le prove automatiche: la griglia dell'inventario (quella modificabile). */
    GrigliaModuli grigliaInventario() {
        return trovaGriglia(this, true);
    }

    /**
     * Cerca una griglia nell'albero dei componenti.
     *
     * @param soloModificabile se true si cerca quella dell'inventario, che e'
     *                         l'unica con i gestori del mouse
     */
    private static GrigliaModuli trovaGriglia(java.awt.Container c, boolean soloModificabile) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component f = c.getComponent(i);
            if (f instanceof GrigliaModuli) {
                GrigliaModuli g = (GrigliaModuli) f;
                if (!soloModificabile || g.getMouseListeners().length > 0) {
                    return g;
                }
            }
            if (f instanceof java.awt.Container) {
                GrigliaModuli g = trovaGriglia((java.awt.Container) f, soloModificabile);
                if (g != null) {
                    return g;
                }
            }
        }
        return null;
    }

    /** Il nodo dell'inventario di bordo della nave. */
    private nomanssave.eY inventarioDi(Corvette c, String campo) {
        nomanssave.eY radice = slot == null ? null : slot.getModello();
        if (radice == null) {
            return null;
        }
        try {
            nomanssave.eY stato = radice.H("PlayerStateData");
            nomanssave.eV navi = stato.d("ShipOwnership");
            if (c.getIndiceNave() < 0 || c.getIndiceNave() >= navi.size()) {
                return null;
            }
            return navi.V(c.getIndiceNave()).H(campo);
        } catch (Throwable t) {
            return null;
        }
    }

    // ------------------------------------------------------------ salvataggio

    /** Scrive sul salvataggio la nuova disposizione dell'inventario. */
    private void salvaInventario() {
        if (layoutInventario == null || rilevamento == null || slot == null) {
            return;
        }
        if (!layoutInventario.modificato()) {
            return;
        }
        if (ScrittoreSalvataggio.giocoInEsecuzione()) {
            JOptionPane.showMessageDialog(this,
                    "No Man's Sky e' in esecuzione.\n\nChiudi il gioco prima di salvare, "
                            + "altrimenti il gioco potrebbe sovrascrivere le modifiche.",
                    "Gioco aperto", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int r = JOptionPane.showConfirmDialog(this,
                "Sto per salvare la nuova disposizione.\n\n"
                        + quantiSpostati() + " oggetti spostati.\n"
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
        final int spostati = quantiSpostati();
        final LayoutInventario layout = layoutInventario;
        final LayoutInventario layoutTech = layoutTecnologie;
        final int indiceNave = corvetteScelta() == null ? -1 : corvetteScelta().getIndiceNave();
        salvaInCorso = true;
        if (ascoltatoreModifiche != null) {
            ascoltatoreModifiche.modificheCambiate(false);
        }

        new javax.swing.SwingWorker<ScrittoreSalvataggio.Esito, Void>() {
            @Override
            protected ScrittoreSalvataggio.Esito doInBackground() {
                return ScrittoreSalvataggio.scrivi(rl, sl,
                        new ScrittoreSalvataggio.Modifica() {
                            @Override
                            public String descrizione() {
                                return "spostamento di " + spostati
                                        + " oggetti (inventario e tecnologie)";
                            }

                            @Override
                            public void applica(eY radice) {
                                // Il modello che arriva qui e' letto FRESCO dal
                                // file: e' un'istanza diversa da quella su cui
                                // abbiamo lavorato, quindi lo spostamento va
                                // riapplicato. Si riconoscono gli oggetti dalla
                                // loro posizione di partenza.
                                eY stato = radice.H("PlayerStateData");
                                nomanssave.eV navi = stato.d("ShipOwnership");
                                if (navi == null || indiceNave < 0 || indiceNave >= navi.size()) {
                                    throw new IllegalStateException(
                                            "La nave " + indiceNave + " non c'e' piu'.");
                                }
                                eY nave = navi.V(indiceNave);
                                if (layout != null && layout.modificato()) {
                                    layout.applica(nave.H("Inventory"));
                                }
                                if (layoutTech != null && layoutTech.modificato()) {
                                    layoutTech.applica(nave.H("Inventory_TechOnly"));
                                }
                            }
                        });
            }

            @Override
            protected void done() {
                salvaInCorso = false;
                if (ascoltatoreModifiche != null) {
                    ascoltatoreModifiche.modificheCambiate(haModifiche());
                }
                try {
                    ScrittoreSalvataggio.Esito e = get();
                    JOptionPane.showMessageDialog(SchedaCorvette.this,
                            (e.dettaglio == null ? e.messaggio : e.dettaglio),
                            e.riuscito ? "Salvato" : "Non riuscito",
                            e.riuscito ? JOptionPane.INFORMATION_MESSAGE
                                    : JOptionPane.ERROR_MESSAGE);
                    if (e.riuscito) {
                        // si rilegge tutto dal modello aggiornato
                        layoutInventario = null;
                        layoutTecnologie = null;
                        mostra();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(SchedaCorvette.this,
                            "Errore inatteso:\n" + ex, "Errore",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private JPanel intestazione(Corvette c, StatisticheCorvette st) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(12, 16, 6, 16));

        JLabel nome = new JLabel(c.getNome());
        nome.setFont(new Font("Segoe UI", Font.BOLD, 20));
        nome.setForeground(Theme.ACCENTO);
        nome.setAlignmentX(Component.LEFT_ALIGNMENT);

        StringBuilder riga = new StringBuilder();
        riga.append("Classe ").append(st.getClasse());
        if (!st.getModelloBreve().isEmpty()) {
            riga.append("   ·   ").append(st.getModelloBreve());
        }
        riga.append("   ·   ").append(st.getModuliTotali()).append(" moduli (")
                .append(st.getTipiDistinti()).append(" tipi)");
        riga.append("   ·   nave indice ").append(st.getIndiceNave());
        if (c.isAttiva()) {
            riga.append("   ·   IN USO");
        }

        JLabel info = new JLabel(riga.toString());
        info.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        info.setForeground(Theme.TESTO_TENUE);
        info.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(nome);
        p.add(Box.createVerticalStrut(4));
        p.add(info);
        return p;
    }

    private JPanel pannelloStatistiche(StatisticheCorvette st) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        p.setOpaque(false);

        List<StatisticheCorvette.Statistica> s = st.getStatistiche();
        if (s.isEmpty()) {
            p.add(testo("Questo salvataggio non espone le statistiche della nave."));
        } else {
            for (int i = 0; i < s.size(); i++) {
                StatisticheCorvette.Statistica x = s.get(i);
                if (x.disponibile()) {
                    p.add(valore(x.nome, String.valueOf((long) x.valore)));
                } else {
                    p.add(valore(x.nome, "—"));
                }
            }
        }
        if (st.getSlotSupercaricati() > 0) {
            p.add(valore("Slot supercaricati", String.valueOf(st.getSlotSupercaricati())));
        }
        if (st.getColonne() > 0) {
            p.add(valore("Griglia tecnologie", st.getColonne() + " x " + st.getRighe()));
        }
        return p;
    }

    private JPanel pannelloDepositi(StatisticheCorvette st) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        List<String> d = st.getDepositiMontati();
        if (d.isEmpty()) {
            p.add(testo("Nessun deposito montato su questa Corvette."));
            p.add(Box.createVerticalStrut(4));
            p.add(testo("I moduli di deposito sono quelli della categoria Magazzini; "
                    + "il deposito della Stazione Spaziale sta nella scheda Deposito."));
            return p;
        }
        for (int i = 0; i < d.size(); i++) {
            p.add(testo("· " + SchedaDeposito.nomeLeggibile(d.get(i))));
        }
        return p;
    }

    /** Un riquadro con il numero grande e l'etichetta sotto. */
    private JPanel valore(String etichetta, String numero) {
        JPanel p = new JPanel();
        p.setOpaque(true);
        p.setBackground(Theme.SUPERFICIE);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDO),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));

        JLabel n = new JLabel(numero);
        n.setFont(new Font("Segoe UI", Font.BOLD, 22));
        n.setForeground(Theme.ACCENTO);
        n.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel e = new JLabel(etichetta);
        e.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        e.setForeground(Theme.TESTO_TENUE);
        e.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(n);
        p.add(e);
        return p;
    }

    private JPanel sezione(String titolo, JPanel corpo) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(10, 16, 4, 16));

        JLabel t = new JLabel(titolo);
        t.setFont(Theme.sezione());
        t.setForeground(Theme.TESTO_TENUE);
        t.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        p.add(t, BorderLayout.NORTH);
        p.add(corpo, BorderLayout.CENTER);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    /**
     * Una griglia di moduli con le dimensioni dell'inventario nel salvataggio,
     * come le mostra il gioco: le caselle libere si vedono.
     */
    private JPanel griglia(LettoreInventari.Deposito d, int[] dim, String vuoto) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        if (d.voci.isEmpty()) {
            p.add(testo(vuoto));
            return p;
        }
        p.add(new GrigliaModuli(dim[0], dim[1], d.voci));
        return p;
    }

    private JLabel testo(String s) {
        JLabel l = new JLabel("<html><body style='width:900px'>" + s + "</body></html>");
        l.setForeground(Theme.TESTO_TENUE);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return l;
    }

    private JLabel messaggio(String s) {
        JLabel l = new JLabel(s);
        l.setForeground(Theme.TESTO_TENUE);
        l.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        return l;
    }
}
