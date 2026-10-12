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
import nomanssave.eY;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;import java.util.List;

/**
 * Scheda "Deposito Corvette": i depositi MONTATI sulla Corvette.
 *
 * I contenitori di deposito (^CONTAINER0 .. ^CONTAINER9 e i due contenitori
 * magici) non sono moduli da costruzione: si installano sulla Corvette e hanno
 * un inventario tutto loro. Questa scheda mostra SOLO quelli, con il contenuto
 * di ciascuno e la possibilità di riordinarne gli oggetti.
 *
 * Il deposito della Stazione Spaziale, che non è montato sulla nave, resta
 * nella scheda "Deposito": sono due cose diverse e vanno tenute separate.
 *
 * Quello che si può fare qui:
 *   - vedere quali contenitori sono montati, uno per uno, con il loro stato;
 *   - vedere cosa contengono, contenitore per contenitore;
 *   - spostare gli oggetti DENTRO un contenitore e salvare la disposizione.
 *
 * Quello che NON si può fare (di proposito): spostare oggetti da un
 * contenitore all'altro. Nel gioco ogni contenitore è un inventario chiuso e
 * lo script non deve inventarsi un travaso che il gioco non prevede.
 */
public final class SchedaDepositoCorvette extends JPanel implements SchedaModificabile {

    private final JPanel contenuto = Theme.colonnaFluida();
    private final JList<Contenitore> elenco = new JList<Contenitore>(
            new DefaultListModel<Contenitore>());
    private final JPanel pannelloGriglia = new JPanel(new BorderLayout());
    private final JLabel titoloGriglia = new JLabel(" ");
    private final JLabel contatoreGriglia = new JLabel(" ");
    private final JPanel riepilogo = new JPanel(new LayoutFluido(Scala.px(8), Scala.px(6)));
    private final JButton salvaGriglia = new JButton("Salva la disposizione");

    /** Un contenitore montato sulla Corvette, con il suo stato. */
    private static final class Contenitore {
        final String id;
        final String campo;
        final String categoria;
        eY inventario;
        List<LettoreInventari.Voce> voci = new ArrayList<LettoreInventari.Voce>();
        LayoutInventario layout;
        int[] dimensioni = new int[]{10, 16};

        Contenitore(String id, String campo) {
            this.id = id;
            this.campo = campo;
            this.categoria = CatalogoParti.categoria(id);
        }

        String leggibile() {
            return SchedaDeposito.nomeLeggibile(id);
        }

        /** Vero quando il gioco ha già creato l'inventario di questo contenitore. */
        boolean esposto() {
            return inventario != null;
        }

        boolean vuoto() {
            return voci.isEmpty();
        }

        int pezzi() {
            int n = 0;
            for (int i = 0; i < voci.size(); i++) {
                n += voci.get(i).quantita;
            }
            return n;
        }

        boolean modificato() {
            return layout != null && layout.modificato();
        }
    }

    private SaveLocator.Rilevamento rilevamento;
    private SaveSlotInfo slot;
    private int indiceCorvette = -1;
    private final List<Contenitore> contenitori = new ArrayList<Contenitore>();

    private SchedaModificabile.AscoltatoreModifiche ascoltatoreModifiche;
    private boolean ultimoStatoModifiche;
    private boolean salvaInCorso;
    private boolean aggiornaSelezione;
    private Contenitore corrente;

    public SchedaDepositoCorvette() {
        setBackground(Theme.SFONDO);
        setLayout(new BorderLayout());

        JPanel testata = costruisciTestata();
        JPanel corpo = costruisciCorpo();
        JPanel pagina = Theme.colonnaFluida();
        pagina.setOpaque(false);
        pagina.add(testata);
        pagina.add(corpo);

        JScrollPane scroll = new JScrollPane(pagina);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(Scala.px(24));
        add(scroll, BorderLayout.CENTER);
    }

    // ------------------------------------------------------------- struttura

    private JPanel costruisciTestata() {
        JPanel p = Theme.colonnaFluida();
        p.setOpaque(false);
        p.setBorder(Scala.bordo(8, 12, 4, 12));

        // Stessa intestazione delle altre schede: stesso font e stesso bordo
        // sinistro, cosi' il titolo cade sulla stessa verticale di tutte.
        JLabel titolo = new JLabel("Deposito Corvette");
        titolo.setFont(Scala.font(Font.BOLD, 14));
        titolo.setForeground(Theme.TESTO);
        titolo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel nota = new JLabel();
        nota.setFont(Theme.etichetta());
        nota.setForeground(Theme.TESTO_DEBOLE);
        nota.setAlignmentX(Component.LEFT_ALIGNMENT);
        nota.setText("I contenitori di deposito montati sulla Corvette selezionata. "
                + "Il deposito della Stazione e' nella scheda Deposito.");

        riepilogo.setOpaque(false);
        riepilogo.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(titolo);
        p.add(Box.createVerticalStrut(Scala.px(3)));
        p.add(nota);
        p.add(Box.createVerticalStrut(Scala.px(8)));
        p.add(riepilogo);
        return p;
    }

    private JPanel costruisciCorpo() {
        elenco.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        elenco.setCellRenderer(new RendererContenitore());
        Theme.altezzaRiga(elenco, Scala.px(46));
        elenco.setOpaque(false);
        elenco.setBackground(new java.awt.Color(0, 0, 0, 0));
        elenco.setBorder(Scala.bordo(4, 2, 4, 2));
        elenco.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (aggiornaSelezione) return;
                mostraContenitore(elenco.getSelectedValue());
            }
        });

        JScrollPane scrollElenco = new JScrollPane(elenco);
        scrollElenco.setOpaque(false);
        scrollElenco.getViewport().setOpaque(false);
        scrollElenco.setBorder(BorderFactory.createEmptyBorder());
        scrollElenco.getVerticalScrollBar().setUnitIncrement(Scala.px(24));

        JPanel sinistra = new JPanel(new BorderLayout(0, Scala.px(8)));
        sinistra.setOpaque(false);
        sinistra.setBorder(Scala.bordo(0, 10, 0, 6));

        JLabel t = Theme.intestazione("Contenitori montati");
        sinistra.add(t, BorderLayout.NORTH);
        sinistra.add(scrollElenco, BorderLayout.CENTER);

        // ---- destra: la griglia del contenitore scelto
        titoloGriglia.setFont(Theme.sezione());
        titoloGriglia.setForeground(Theme.TESTO);
        contatoreGriglia.setFont(Theme.etichetta());
        contatoreGriglia.setForeground(Theme.TESTO_DEBOLE);

        JPanel testaGriglia = new JPanel(new BorderLayout(0, Scala.px(2)));
        testaGriglia.setOpaque(false);
        testaGriglia.add(titoloGriglia, BorderLayout.NORTH);
        testaGriglia.add(contatoreGriglia, BorderLayout.CENTER);

        pannelloGriglia.setOpaque(false);
        pannelloGriglia.setBorder(Scala.bordo(6, 10, 6, 10));

        salvaGriglia.setEnabled(false);
        Theme.primaria(salvaGriglia);
        salvaGriglia.setToolTipText("Scrive sul salvataggio la nuova "
                + "disposizione di questo contenitore");
        salvaGriglia.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                salvaModifiche();
            }
        });

        // Il pulsante sta a sinistra, sotto la griglia, non centrato: cosi'
        // cade sulla stessa verticale del titolo e dei testi.
        JPanel piedeGriglia = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        piedeGriglia.setOpaque(false);
        piedeGriglia.add(salvaGriglia);

        // La griglia e il pulsante se ne stanno in alto, uno sotto l'altro:
        // il glue in coda assorbe l'altezza avanzata, cosi' "Salva" non finisce
        // a meta' schermo e la destra non resta mezza vuota.
        JPanel destra = new JPanel();
        destra.setOpaque(false);
        destra.setLayout(new BoxLayout(destra, BoxLayout.Y_AXIS));
        testaGriglia.setAlignmentX(Component.LEFT_ALIGNMENT);
        pannelloGriglia.setAlignmentX(Component.LEFT_ALIGNMENT);
        piedeGriglia.setAlignmentX(Component.LEFT_ALIGNMENT);
        destra.add(testaGriglia);
        destra.add(Box.createVerticalStrut(Scala.px(8)));
        destra.add(pannelloGriglia);
        destra.add(Box.createVerticalStrut(Scala.px(8)));
        destra.add(piedeGriglia);
        destra.add(Box.createVerticalGlue());

        JSplitPane split = new Theme.Divisione(JSplitPane.HORIZONTAL_SPLIT, sinistra, destra);
        Theme.adattaSplit(split, 0.34, 760, true);

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(Scala.bordo(0, 12, 10, 12));
        p.add(split, BorderLayout.CENTER);
        return p;
    }

    // ---------------------------------------------------------------- dati

    /**
     * Ricarica i contenitori montati sulla Corvette scelta.
     *
     * @param esito la lettura delle Corvette dello slot: la scheda segue la
     *              Corvette che l'utente sta guardando
     */
    public void aggiorna(SaveLocator.Rilevamento rilevamento, SaveSlotInfo slot,
                         LettoreCorvette.Esito esito, int indiceCorvette) {
        this.rilevamento = rilevamento;
        this.slot = slot;
        this.indiceCorvette = indiceCorvette;
        contenitori.clear();
        corrente = null;

        Corvette c = null;
        if (esito != null && indiceCorvette >= 0 && indiceCorvette < esito.corvette.size()) {
            c = esito.corvette.get(indiceCorvette);
        }

        eY radice = slot == null ? null : slot.getModello();
        if (c != null && radice != null) {
            StatisticheCorvette st = StatisticheCorvette.leggi(radice, c);
            List<String> montati = st.getDepositiMontati();
            for (int i = 0; i < montati.size(); i++) {
                String id = montati.get(i);
                String campo = LettoreInventari.campoInventario(id);
                Contenitore k = new Contenitore(id, campo);
                if (campo != null) {
                    k.inventario = inventarioDelGiocatore(radice, campo);
                    // Un contenitore puo' esistere sulla nave senza che il gioco
                    // abbia ancora creato il suo inventario: in quel caso qui non
                    // c'e' niente da leggere e nemmeno da riordinare.
                    if (k.inventario != null) {
                        k.voci = LettoreInventari.leggiVoci(k.inventario);
                        k.dimensioni = LettoreInventari.dimensioni(k.inventario);
                        k.layout = LayoutInventario.leggi(k.inventario, k.leggibile());
                    }
                }
                contenitori.add(k);
            }
        }

        aggiornaElenco();
        aggiornaRiepilogo();
        if (!contenitori.isEmpty()) {
            elenco.setSelectedIndex(0);
        } else {
            mostraContenitore(null);
        }
        avvisaModifiche();
    }

    /** Chiamato quando l'utente cambia Corvette nella scheda Corvette. */
    public void cambiaCorvette(int indice, LettoreCorvette.Esito esito) {
        if (slot == null || rilevamento == null) {
            return;
        }
        aggiorna(rilevamento, slot, esito, indice);
    }

    private void aggiornaElenco() {
        DefaultListModel<Contenitore> m = new DefaultListModel<Contenitore>();
        for (int i = 0; i < contenitori.size(); i++) {
            m.addElement(contenitori.get(i));
        }
        aggiornaSelezione = true;
        try {
            elenco.setModel(m);
        } finally {
            aggiornaSelezione = false;
        }
        elenco.revalidate();
        elenco.repaint();
    }

    private void aggiornaRiepilogo() {
        riepilogo.removeAll();
        int conInventario = 0;
        int vuoti = 0;
        int tipi = 0;
        int pezzi = 0;
        java.util.Set<String> idVisti = new java.util.LinkedHashSet<String>();
        for (int i = 0; i < contenitori.size(); i++) {
            Contenitore k = contenitori.get(i);
            if (k.esposto()) {
                conInventario++;
            }
            if (k.esposto() && k.vuoto()) {
                vuoti++;
            }
            for (int v = 0; v < k.voci.size(); v++) {
                idVisti.add(k.voci.get(v).id);
            }
            pezzi += k.pezzi();
        }
        tipi = idVisti.size();

        riepilogo.add(new Theme.Pill(contenitori.size()
                + (contenitori.size() == 1 ? " contenitore" : " contenitori"),
                Theme.ACCENTO, Theme.SUPERFICIE_ALTA));
        riepilogo.add(new Theme.Pill(tipi
                + (tipi == 1 ? " tipo di oggetto" : " tipi di oggetto"),
                Theme.TESTO, Theme.SUPERFICIE_ALTA));
        riepilogo.add(new Theme.Pill(pezzi + " pezzi in tutto",
                Theme.TESTO_TENUE, Theme.SUPERFICIE_ALTA));
        if (vuoti > 0) {
            riepilogo.add(new Theme.Pill(vuoti
                    + (vuoti == 1 ? " contenitore vuoto" : " contenitori vuoti"),
                    Theme.TESTO_TENUE, Theme.SUPERFICIE_ALTA));
        }
        riepilogo.revalidate();
        riepilogo.repaint();
    }

    /** Mostra il contenuto di un contenitore: la griglia con gli oggetti. */
    private void mostraContenitore(Contenitore k) {
        corrente = k;
        pannelloGriglia.removeAll();

        if (k == null) {
            titoloGriglia.setText("Nessun contenitore scelto");
            contatoreGriglia.setText(" ");
            pannelloGriglia.add(messaggio(contenitori.isEmpty()
                    ? "Questa Corvette non ha contenitori di deposito montati."
                    : "Scegli un contenitore dall'elenco."), BorderLayout.NORTH);
            salvaGriglia.setEnabled(false);
            pannelloGriglia.revalidate();
            pannelloGriglia.repaint();
            return;
        }

        titoloGriglia.setText(k.leggibile() + "   \u00b7   " + k.categoria);
        int totali = k.dimensioni[0] * k.dimensioni[1];
        contatoreGriglia.setText(k.campo == null
                ? "modulo non gestito dal tool"
                : (!k.esposto()
                ? "inventario non ancora creato dal gioco"
                : k.voci.size() + " caselle occupate su " + totali
                  + "   \u00b7   " + k.pezzi() + " pezzi"));

        if (k.campo == null) {
            pannelloGriglia.add(messaggio("Questo contenitore non ha un "
                    + "inventario associato nel salvataggio: non c'e' niente da "
                    + "mostrare ne' da riordinare."), BorderLayout.NORTH);
        } else if (!k.esposto()) {
            pannelloGriglia.add(messaggio("Il gioco non ha ancora creato "
                    + "l'inventario di questo contenitore. Non e' un errore: lo fa "
                    + "la prima volta che lo apri in gioco. Da qui non e' possibile "
                    + "crearlo."), BorderLayout.NORTH);
        } else if (k.vuoto()) {
            pannelloGriglia.add(messaggio("Questo contenitore e' vuoto."),
                    BorderLayout.NORTH);
        } else {
            final Contenitore scelto = k;
            JPanel riga = new JPanel(new BorderLayout());
            riga.setOpaque(false);
            if (k.layout != null) {
                riga.add(new GrigliaModuli(k.layout, new GrigliaModuli.AscoltatoreSpostamento() {
                    @Override
                    public void spostato(int daX, int daY, int aX, int aY) {
                        if (scelto.layout.sposta(daX, daY, aX, aY)) {
                            mostraContenitore(scelto);
                            avvisaModifiche();
                        }
                    }
                }), BorderLayout.CENTER);
            } else {
                riga.add(new GrigliaModuli(k.dimensioni[0], k.dimensioni[1], k.voci),
                        BorderLayout.CENTER);
            }
            pannelloGriglia.add(riga, BorderLayout.NORTH);
        }

        Theme.rifinisci(pannelloGriglia);
        salvaGriglia.setEnabled(k.modificato() && !salvaInCorso);
        pannelloGriglia.revalidate();
        pannelloGriglia.repaint();
    }

    // ------------------------------------------------------------ modifica

    @Override
    public void setAscoltatoreModifiche(SchedaModificabile.AscoltatoreModifiche a) {
        this.ascoltatoreModifiche = a;
    }

    @Override
    public boolean haModifiche() {
        if (salvaInCorso) {
            return false;
        }
        for (int i = 0; i < contenitori.size(); i++) {
            if (contenitori.get(i).modificato()) {
                return true;
            }
        }
        return false;
    }

    private void avvisaModifiche() {
        boolean modificato = haModifiche();
        salvaGriglia.setEnabled(corrente != null && corrente.modificato() && !salvaInCorso);
        if (modificato != ultimoStatoModifiche) {
            ultimoStatoModifiche = modificato;
            if (ascoltatoreModifiche != null) {
                ascoltatoreModifiche.modificheCambiate(modificato);
            }
        }
    }

    @Override
    public void annullaModifiche() {
        for (int i = 0; i < contenitori.size(); i++) {
            Contenitore k = contenitori.get(i);
            if (k.layout != null) {
                k.layout.annulla();
            }
        }
        mostraContenitore(corrente);
        avvisaModifiche();
    }

    @Override
    public void salvaModifiche() {
        if (rilevamento == null || slot == null) {
            return;
        }
        // Si salvano insieme tutti i contenitori toccati: sono tutti nello
        // stesso file, e scrivere due volte di seguito sarebbe solo piu' lento.
        final List<Contenitore> toccati = new ArrayList<Contenitore>();
        int spostati = 0;
        for (int i = 0; i < contenitori.size(); i++) {
            Contenitore k = contenitori.get(i);
            if (k.modificato()) {
                toccati.add(k);
                spostati += k.layout.quantiSpostati();
            }
        }
        if (toccati.isEmpty()) {
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
                "Sto per salvare la nuova disposizione dei depositi montati.\n\n"
                        + toccati.size() + (toccati.size() == 1
                        ? " contenitore, " : " contenitori, ")
                        + spostati + " oggetti spostati.\n"
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
        salvaInCorso = true;
        avvisaModifiche();

        Theme.lavora(this, true);
        Theme.fase(this, "Preparo la copia di sicurezza\u2026");
        new javax.swing.SwingWorker<ScrittoreSalvataggio.Esito, Void>() {
            @Override
            protected ScrittoreSalvataggio.Esito doInBackground() {
                Theme.fase(SchedaDepositoCorvette.this,
                        "Scrivo la nuova disposizione dei depositi\u2026");
                return ScrittoreSalvataggio.scrivi(rl, sl,
                        new ScrittoreSalvataggio.Modifica() {
                            @Override
                            public String descrizione() {
                                return "spostamento di oggetti in "
                                        + toccati.size() + " depositi montati";
                            }

                            @Override
                            public void applica(eY radice) {
                                eY stato = radice.H("PlayerStateData");
                                for (int i = 0; i < toccati.size(); i++) {
                                    Contenitore k = toccati.get(i);
                                    eY inv = stato.H(k.campo);
                                    if (inv != null && k.layout != null) {
                                        k.layout.applica(inv);
                                    }
                                }
                            }
                        });
            }

            @Override
            protected void done() {
                Theme.lavora(SchedaDepositoCorvette.this, false);
                Theme.finito(SchedaDepositoCorvette.this);
                salvaInCorso = false;
                try {
                    ScrittoreSalvataggio.Esito e = get();
                    JOptionPane.showMessageDialog(SchedaDepositoCorvette.this,
                            (e.dettaglio == null || e.dettaglio.isEmpty()
                                    ? e.messaggio : e.dettaglio),
                            e.riuscito ? "Salvato" : "Non riuscito",
                            e.riuscito ? JOptionPane.INFORMATION_MESSAGE
                                    : JOptionPane.ERROR_MESSAGE);
                    if (e.riuscito) {
                        Theme.dopoScrittura(SchedaDepositoCorvette.this);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(SchedaDepositoCorvette.this,
                            "Errore inatteso:\n" + ex, "Errore",
                            JOptionPane.ERROR_MESSAGE);
                }
                avvisaModifiche();
            }
        }.execute();
    }

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

    private JLabel messaggio(String testo) {
        JLabel l = new JLabel("<html><body style='width:420px'>" + testo + "</body></html>");
        l.setForeground(Theme.TESTO_TENUE);
        l.setFont(Theme.corpo());
        l.setBorder(Scala.bordo(10, 2, 10, 2));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    @Override public Dimension getPreferredSize() {
        return Scala.dim(900, 560);
    }

    /** Una riga dell'elenco: nome, categoria e stato del contenitore. */
    private final class RendererContenitore extends JPanel
            implements ListCellRenderer<Contenitore> {

        private final JLabel nome = new JLabel();
        private final JLabel dettaglio = new JLabel();
        private final Theme.Pill stato = new Theme.Pill(" ");
        /** La pill non deve stirarsi in altezza: avrebbe forma di cerchio. */
        private final JPanel guscioStato = new JPanel();
        private boolean selezionato;

        RendererContenitore() {
            setLayout(new BorderLayout(Scala.px(10), 0));
            setOpaque(false);

            nome.setFont(Theme.corpo());
            dettaglio.setFont(Theme.etichetta());

            // Nome e dettaglio impilati, allineati a sinistra. Il guscio li
            // centra in verticale dentro la riga: senza, resterebbero appiccicati
            // in alto e sotto si vedrebbe un vuoto.
            JPanel testi = new JPanel();
            testi.setOpaque(false);
            testi.setLayout(new BoxLayout(testi, BoxLayout.Y_AXIS));
            nome.setAlignmentX(Component.LEFT_ALIGNMENT);
            dettaglio.setAlignmentX(Component.LEFT_ALIGNMENT);
            // I due glue tengono il blocco in mezzo alla riga: senza, nome e
            // dettaglio restano appiccicati in cima e sotto si apre un vuoto.
            testi.add(Box.createVerticalGlue());
            testi.add(nome);
            testi.add(Box.createVerticalStrut(Scala.px(2)));
            testi.add(dettaglio);
            testi.add(Box.createVerticalGlue());

            JPanel guscioTesti = new JPanel(new BorderLayout());
            guscioTesti.setOpaque(false);
            // Un margine interno: senza, il nome tocca il bordo del riquadro e
            // sembra scritto sopra la cornice.
            guscioTesti.setBorder(Scala.bordo(0, 12, 0, 8));
            guscioTesti.add(testi, BorderLayout.CENTER);

            // La pill sta in un guscio che la tiene alta quanto serve e la
            // centra: messa direttamente a destra verrebbe stirata in altezza
            // e diventerebbe un cerchio. Il guscio usa GridBagLayout, che con
            // "anchor" la lascia alla sua misura naturale.
            guscioStato.setOpaque(false);
            guscioStato.setLayout(new java.awt.GridBagLayout());
            guscioStato.setBorder(Scala.bordo(0, 0, 0, 10));
            java.awt.GridBagConstraints gc = new java.awt.GridBagConstraints();
            gc.anchor = java.awt.GridBagConstraints.EAST;
            gc.weightx = 1;
            guscioStato.add(stato, gc);

            add(guscioTesti, BorderLayout.CENTER);
            add(guscioStato, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Contenitore> list,
                                                      Contenitore k, int index,
                                                      boolean selezionato, boolean fuoco) {
            this.selezionato = selezionato;
            nome.setText(k.leggibile());
            nome.setForeground(selezionato ? Theme.ACCENTO : Theme.TESTO);

            if (k.campo == null) {
                dettaglio.setText(k.categoria + " \u00b7 non gestito");
                dettaglio.setForeground(Theme.TESTO_DEBOLE);
                stato("non gestito", Theme.TESTO_DEBOLE, Theme.SUPERFICIE_ALTA);
            } else if (!k.esposto()) {
                dettaglio.setText(k.categoria + " \u00b7 da aprire in gioco");
                dettaglio.setForeground(Theme.TESTO_DEBOLE);
                stato("da aprire", Theme.AVVISO, Theme.AVVISO_SCURO);
            } else if (k.vuoto()) {
                dettaglio.setText(k.categoria + " \u00b7 vuoto");
                dettaglio.setForeground(Theme.TESTO_TENUE);
                stato("vuoto", Theme.TESTO_TENUE, Theme.SUPERFICIE_ALTA);
            } else {
                int totali = k.dimensioni[0] * k.dimensioni[1];
                dettaglio.setText(k.categoria + " \u00b7 " + k.voci.size()
                        + " su " + totali + " caselle");
                dettaglio.setForeground(Theme.TESTO_TENUE);
                stato("in uso", Theme.OK, Theme.OK_FONDO);
            }
            revalidate();
            return this;
        }

        /**
         * Aggiorna la pill di stato.
         *
         * Pill e' una JLabel: si cambia con setText/setForeground, mentre
         * sfondo e bordo si toccano con riempi(...) e borda(...). Il bordo
         * viene ricalcolato dal colore del testo, come nel costruttore a tre
         * argomenti, altrimenti resterebbe quello del contenitore precedente.
         */
        private void stato(String testo, java.awt.Color testoColore, java.awt.Color fondo) {
            // "in uso" e "vuoto" sono la stessa cosa vista da due lati: il
            // colore basta, il testo no. Una pill sola per tutte le righe
            // tiene le scritte allineate, perche' la colonna di destra resta
            // larga uguale anche quando il contenitore cambia stato.
            stato.setText(testo);
            stato.setForeground(testoColore);
            stato.riempi(fondo);
            stato.borda(Theme.mescola(testoColore, fondo, 0.55f));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int pad = Scala.px(3);
            int r = Scala.px(10);
            if (selezionato) {
                p.setColor(Theme.SUPERFICIE_ALTA);
                p.fillRoundRect(pad, pad, getWidth() - pad * 2 - 1,
                        getHeight() - pad * 2 - 1, r, r);
                p.setColor(Theme.ACCENTO);
                p.setStroke(new java.awt.BasicStroke(Scala.px(1.3f)));
                p.drawRoundRect(pad, pad, getWidth() - pad * 2 - 1,
                        getHeight() - pad * 2 - 1, r, r);
            } else {
                p.setColor(Theme.SUPERFICIE);
                p.fillRoundRect(pad, pad, getWidth() - pad * 2 - 1,
                        getHeight() - pad * 2 - 1, r, r);
                p.setColor(Theme.BORDO);
                p.drawRoundRect(pad, pad, getWidth() - pad * 2 - 1,
                        getHeight() - pad * 2 - 1, r, r);
            }
            p.dispose();
        }
    }
}
