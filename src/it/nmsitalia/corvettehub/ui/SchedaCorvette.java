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
 * Le statistiche sono in sola lettura; inventario e tecnologie si riordinano.
 */
public final class SchedaCorvette extends JPanel implements SchedaModificabile {

    private final JComboBox<String> sceltaCorvette = new JComboBox<String>();
    private final JPanel contenuto = Theme.colonnaFluida();
    private SchedaModificabile.AscoltatoreModifiche ascoltatoreModifiche;

    private SaveLocator.Rilevamento rilevamento;
    private SaveSlotInfo slot;
    private LettoreCorvette.Esito corrente;
    private LayoutInventario layoutInventario;
    private LayoutInventario layoutTecnologie;
    private boolean ultimoStatoModifiche;
    private boolean salvaInCorso;
    private boolean aggiornaScelta;
    private int navePrecedente = -1;
    private AscoltatoreSelezione ascoltatoreSelezione;

    /** Chi vuole sapere quale Corvette e' a schermo. */
    public interface AscoltatoreSelezione {
        void selezioneCambiata(int indice, LettoreCorvette.Esito esito);
    }

    /**
     * La scheda "Deposito Corvette" deve mostrare i depositi della nave che
     * l'utente sta guardando, quindi ha bisogno di sapere quand'e' che cambia.
     */
    public void setAscoltatoreSelezione(AscoltatoreSelezione a) {
        this.ascoltatoreSelezione = a;
    }

    /** L'indice della Corvette a schermo, oppure -1 se non ce n'e' nessuna. */
    public int getIndiceSelezionato() {
        if (corrente == null || corrente.corvette.isEmpty()) {
            return -1;
        }
        return sceltaCorvette.getSelectedIndex();
    }

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

        JPanel testata = new JPanel(new LayoutFluido(Scala.px(8),Scala.px(4)));
        testata.setOpaque(false);
        testata.setBorder(Scala.bordo(8, 10, 4, 10));
        JLabel l = new JLabel("Corvette");
        l.setForeground(Theme.TESTO_DEBOLE);
        l.setFont(Theme.etichetta());
        sceltaCorvette.setPreferredSize(Scala.dim(260, 30));
        sceltaCorvette.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (aggiornaScelta) return;
                int nuova = sceltaCorvette.getSelectedIndex();
                if (nuova == navePrecedente) return;
                if (haModifiche()) {
                    int scelta = JOptionPane.showConfirmDialog(SchedaCorvette.this,
                            "Ci sono spostamenti non salvati. Vuoi abbandonarli e cambiare Corvette?",
                            "Modifiche non salvate", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (scelta != JOptionPane.YES_OPTION) {
                        aggiornaScelta = true;
                        try { sceltaCorvette.setSelectedIndex(navePrecedente); }
                        finally { aggiornaScelta = false; }
                        return;
                    }
                }
                layoutInventario = null;
                layoutTecnologie = null;
                navePrecedente = nuova;
                mostra();
                if (ascoltatoreSelezione != null) {
                    ascoltatoreSelezione.selezioneCambiata(nuova, corrente);
                }
            }
        });
        testata.add(l);
        testata.add(sceltaCorvette);

        contenuto.setBackground(Theme.SFONDO);

        JScrollPane scroll = new JScrollPane(contenuto);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(Scala.px(28));

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

        aggiornaScelta = true;
        sceltaCorvette.removeAllItems();
        if (esito == null || esito.corvette.isEmpty()) {
            sceltaCorvette.addItem("(nessuna Corvette)");
            contenuto.removeAll();
            contenuto.add(messaggio("Questo slot non contiene Corvette."));
            Theme.rifinisci(contenuto);
        contenuto.revalidate();
            contenuto.repaint();
            navePrecedente = 0;
            aggiornaScelta = false;
            return;
        }
        for (int i = 0; i < esito.corvette.size(); i++) {
            Corvette c = esito.corvette.get(i);
            sceltaCorvette.addItem((c.isAttiva() ? "\u26A0 " : "") + c.getNome());
        }
        sceltaCorvette.setSelectedIndex(0);
        navePrecedente = 0;
        aggiornaScelta = false;
        mostra();
        if (ascoltatoreSelezione != null) {
            ascoltatoreSelezione.selezioneCambiata(0, corrente);
        }
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
            Theme.rifinisci(contenuto);
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
        // I depositi montati hanno una scheda tutta loro ("Deposito Corvette"):
        // qui si dice solo dove trovarli, cosi' non si legge due volte la
        // stessa cosa e la scheda Corvette resta corta.
        contenuto.add(sezione("Depositi montati", rimandoDepositi(c, st)));
        contenuto.add(Box.createVerticalStrut(16));

        Theme.rifinisci(contenuto);
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
        JPanel p = Theme.colonnaFluida();
        p.setOpaque(false);

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

        JPanel riga = new JPanel(new BorderLayout());
        riga.setOpaque(false);
        riga.setAlignmentX(Component.LEFT_ALIGNMENT);
        riga.add(new GrigliaModuli(lay, new GrigliaModuli.AscoltatoreSpostamento() {
            @Override
            public void spostato(int daX, int daY, int aX, int aY) {
                if (lay.sposta(daX, daY, aX, aY)) {
                    mostra();
                }
            }
        }),BorderLayout.NORTH);
        p.add(riga);
        return p;
    }

    /** La fascia che dice se ci sono spostamenti da salvare. */
    private JPanel fascia(LayoutInventario lay, boolean tecnologia) {
        JPanel b = new JPanel(new BorderLayout());
        b.setOpaque(true);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);

        javax.swing.JTextArea scritta = new javax.swing.JTextArea();
        scritta.setEditable(false);scritta.setOpaque(false);scritta.setLineWrap(true);scritta.setWrapStyleWord(true);scritta.setRows(1);
        if (lay.modificato()) {
            int n = lay.quantiSpostati();
            b.setBackground(Theme.AVVISO_SCURO);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.AVVISO),
                    Scala.bordo(7, 11, 7, 11)));
            scritta.setText(n + (n == 1 ? " oggetto spostato" : " oggetti spostati")
                    + ": usa Salva le modifiche nella barra in alto");
            scritta.setForeground(Theme.TESTO);
            scritta.setFont(Scala.font(Font.BOLD, 12));
        } else {
            b.setBackground(Theme.SUPERFICIE);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.BORDO),
                    Scala.bordo(7, 11, 7, 11)));
            scritta.setText("Trascina per spostare   \u00b7   "
                    + lay.getNumeroPezzi() + " su "
                    + (lay.getLarghezza() * lay.getAltezza()) + " celle"
                    + (tecnologia
                    ? "   \u00b7   i bonus di adiacenza possono cambiare" : ""));
            scritta.setForeground(Theme.TESTO_TENUE);
            scritta.setFont(Theme.corpo());
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

        Theme.lavora(this, true);
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
                Theme.lavora(SchedaCorvette.this, false);
                salvaInCorso = false;
                if (ascoltatoreModifiche != null) {
                    ascoltatoreModifiche.modificheCambiate(haModifiche());
                }
                try {
                    ScrittoreSalvataggio.Esito e = get();
                    JOptionPane.showMessageDialog(SchedaCorvette.this,
                            (e.dettaglio == null || e.dettaglio.isEmpty() ? e.messaggio : e.dettaglio),
                            e.riuscito ? "Salvato" : "Non riuscito",
                            e.riuscito ? JOptionPane.INFORMATION_MESSAGE
                                    : JOptionPane.ERROR_MESSAGE);
                    if (e.riuscito) {
                        // si rilegge tutto dal modello aggiornato
                        layoutInventario = null;
                        layoutTecnologie = null;
                        Theme.dopoScrittura(SchedaCorvette.this);
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
        p.setBorder(Scala.bordo(14, 16, 8, 16));

        JPanel rigaNome = new JPanel(new LayoutFluido(Scala.px(9), Scala.px(4)));
        rigaNome.setOpaque(false);
        rigaNome.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel nome = new JLabel(c.getNome());
        nome.setFont(Scala.font(Font.PLAIN, 19));
        nome.setForeground(Theme.ACCENTO);
        rigaNome.add(nome);
        if (c.isAttiva()) {
            Theme.Pill inUso = new Theme.Pill("IN USO", Theme.OK, Theme.OK_FONDO);
            inUso.setFont(Theme.etichetta());
            rigaNome.add(inUso);
        }

        StringBuilder riga = new StringBuilder();
        riga.append("Classe ").append(st.getClasse());
        if (!st.getModelloBreve().isEmpty()) {
            riga.append("   \u00b7   ").append(st.getModelloBreve());
        }
        riga.append("   \u00b7   ").append(st.getModuliTotali()).append(" moduli (")
                .append(st.getTipiDistinti()).append(" tipi)");

        JLabel info = new JLabel(riga.toString());
        info.setFont(Theme.sezione());
        info.setForeground(Theme.TESTO_TENUE);
        info.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(rigaNome);
        p.add(Box.createVerticalStrut(Scala.px(6)));
        p.add(info);
        return p;
    }

    private JPanel pannelloStatistiche(StatisticheCorvette st) {
        JPanel p = new JPanel(new LayoutFluido(Scala.px(8),Scala.px(8)));
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

    /**
     * I depositi montati sulla Corvette.
     *
     * Un deposito e' un modulo della categoria Magazzini: nell'elenco Objects
     * della Corvette compare come ^CONTAINER0 .. ^CONTAINER9 oppure come
     * ^MAGICCONTAINER. A ciascuno corrisponde un inventario proprio del
     * giocatore (Chest1Inventory .. ). Se il modulo c'e' ma l'inventario non
     * e' ancora stato creato, il gioco lo crea alla prima apertura: qui si
     * dice "vuoto" e non "assente", perche' sono due cose diverse.
     *
     * @param c la Corvette scelta
     * @param st le sue statistiche (per l'elenco dei moduli di deposito)
     */
    private JPanel rimandoDepositi(Corvette c, StatisticheCorvette st) {
        JPanel p = Theme.colonnaFluida();
        p.setOpaque(false);

        List<String> montati = st.getDepositiMontati();
        if (montati.isEmpty()) {
            JPanel vuoto = Theme.colonnaFluida();
            vuoto.setOpaque(false);
            vuoto.setBorder(Scala.bordo(6, 12, 6, 12));
            vuoto.add(testo("Nessun modulo di deposito e' montato su questa Corvette."));
            vuoto.add(Box.createVerticalStrut(Scala.px(6)));
            vuoto.add(testo("I moduli di deposito sono quelli della categoria Magazzini "
                    + "(^CONTAINER0 .. ^CONTAINER9 e i contenitori magici). "
                    + "Puoi aggiungerli montandoli sulla Corvette in gioco: qui "
                    + "compaiono da soli alla prossima lettura del salvataggio."));
            p.add(vuoto);
            return p;
        }

        // Qui si da' solo il colpo d'occhio: il contenuto di ogni contenitore,
        // con la griglia per riordinarlo, sta nella scheda "Deposito Corvette".
        eY radice = slot == null ? null : slot.getModello();
        int conInventario = 0;
        int vuoti = 0;
        int pezzi = 0;
        for (int i = 0; i < montati.size(); i++) {
            String campo = LettoreInventari.campoInventario(montati.get(i));
            eY inventario = campo == null ? null : inventarioDelGiocatore(radice, campo);
            if (inventario == null) {
                continue;
            }
            conInventario++;
            List<LettoreInventari.Voce> lette = LettoreInventari.leggiVoci(inventario);
            if (lette.isEmpty()) {
                vuoti++;
            }
            for (int k = 0; k < lette.size(); k++) {
                pezzi += lette.get(k).quantita;
            }
        }

        JPanel riepilogo = new JPanel(new LayoutFluido(Scala.px(8), Scala.px(6)));
        riepilogo.setOpaque(false);
        riepilogo.setAlignmentX(Component.LEFT_ALIGNMENT);
        riepilogo.add(new Theme.Pill(montati.size()
                + (montati.size() == 1 ? " contenitore" : " contenitori"),
                Theme.ACCENTO, Theme.SUPERFICIE_ALTA));
        riepilogo.add(new Theme.Pill(pezzi + " pezzi in tutto",
                Theme.TESTO_TENUE, Theme.SUPERFICIE_ALTA));
        if (vuoti > 0) {
            riepilogo.add(new Theme.Pill(vuoti
                    + (vuoti == 1 ? " vuoto" : " vuoti"),
                    Theme.TESTO_TENUE, Theme.SUPERFICIE_ALTA));
        }
        p.add(riepilogo);
        p.add(Box.createVerticalStrut(Scala.px(10)));

        JPanel nota = Theme.colonnaFluida();
        nota.setOpaque(false);
        nota.setAlignmentX(Component.LEFT_ALIGNMENT);
        nota.setBorder(Scala.bordo(0, 2, 0, 2));
        nota.add(testo("Il contenuto di questi contenitori, con la griglia per "
                + "riordinarlo, e' nella scheda Deposito Corvette."));
        if (conInventario < montati.size()) {
            nota.add(Box.createVerticalStrut(Scala.px(4)));
            nota.add(testo((montati.size() - conInventario)
                    + (montati.size() - conInventario == 1
                    ? " contenitore non ha ancora un inventario: "
                    : " contenitori non hanno ancora un inventario: ")
                    + "non e' un errore, il gioco lo crea alla prima apertura in gioco."));
        }
        p.add(nota);

        return p;
    }

    /** L'inventario di un contenitore del giocatore, con il suo nome nel salvataggio. */
    private eY inventarioDelGiocatore(eY radice, String campo) {
        if (radice == null || campo == null) {
            return null;
        }
        try {
            return radice.H("PlayerStateData").H(campo);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Una carta statistica: numero grande in oro, etichetta in maiuscoletto sotto. */
    private JPanel valore(String etichetta, String numero) {
        Theme.Carta p = new Theme.Carta();
        p.riempi(Theme.SUPERFICIE);
        p.raggio(Scala.px(12));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(Scala.bordo(12, 14, 12, 14));
        p.setPreferredSize(Scala.dim(136, 74));

        JLabel n = new JLabel(numero);
        n.setFont(Theme.numero());
        n.setForeground(Theme.ACCENTO);
        n.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel e = new JLabel(etichetta.toUpperCase(java.util.Locale.ITALY));
        e.setFont(Theme.etichetta());
        e.setForeground(Theme.TESTO_DEBOLE);
        e.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(Box.createVerticalGlue());
        p.add(n);
        p.add(Box.createVerticalStrut(Scala.px(2)));
        p.add(e);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JPanel sezione(String titolo, JPanel corpo) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(Scala.bordo(9, 12, 4, 12));

        JLabel t = Theme.intestazione(titolo);
        t.setBorder(Scala.bordo(0, 0, 7, 0));

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

    private javax.swing.JTextArea testo(String s) {
        javax.swing.JTextArea l = new javax.swing.JTextArea(s);
        l.setEditable(false);
        l.setOpaque(false);
        l.setLineWrap(true);
        l.setWrapStyleWord(true);
        l.setRows(s.length() > 90 ? 2 : 1);
        l.setColumns(36);
        l.setForeground(Theme.TESTO_TENUE);
        l.setFont(Theme.corpo());
        l.setMinimumSize(new Dimension(0, l.getPreferredSize().height));
        return l;
    }

    private JLabel messaggio(String s) {
        JLabel l = new JLabel(s);
        l.setForeground(Theme.TESTO_TENUE);
        l.setFont(Theme.corpo());
        l.setBorder(Scala.bordo(28, 24, 28, 24));
        return l;
    }
}
