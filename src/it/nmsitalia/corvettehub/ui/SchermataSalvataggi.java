package it.nmsitalia.corvettehub.ui;

import it.nmsitalia.corvettehub.Main;
import it.nmsitalia.corvettehub.config.AppConfig;
import it.nmsitalia.corvettehub.detect.SaveLocator;
import it.nmsitalia.corvettehub.domain.SaveSlotInfo;
import nomanssave.ft;

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
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Prima schermata: la scelta del salvataggio.
 *
 * Qui l'utente non vede nulla delle Corvette. Vede solo dove il tool ha
 * trovato i salvataggi e quali slot esistono, con i dati che servono a
 * riconoscere il proprio. Quando sceglie e conferma, si passa alla schermata
 * della Corvette.
 */
public final class SchermataSalvataggi extends JPanel {

    /** Chi vuole essere avvisato quando l'utente sceglie uno slot. */
    public interface Ascoltatore {
        void salvataggioScelto(SaveLocator.Rilevamento rilevamento, SaveSlotInfo slot);
    }

    private final AppConfig config;
    private final Ascoltatore ascoltatore;

    private final JLabel stato = new JLabel("Cerco i salvataggi di No Man's Sky...");
    private final JLabel sottotitolo = new JLabel();
    private final JLabel messaggio = new JLabel(" ");
    private final DefaultListModel<SaveSlotInfo> modello = new DefaultListModel<SaveSlotInfo>();
    private final JList<SaveSlotInfo> lista = new JList<SaveSlotInfo>(modello) {
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
    };
    private final JButton continua = new JButton("Continua  \u2192");
    private final Manifesto introduzione = new Manifesto();
    private final JLabel passoSelezione = new JLabel("WORKSPACE   /   01 — SALVATAGGIO");
    private final javax.swing.JTextArea titoloSelezione = new javax.swing.JTextArea();
    private JPanel testataSelezione;
    private int modoSelezione = -1;
    private JPanel rigaCorpo;
    /** Il guscio che tiene la lista alta quanto le sue righe. */
    private JPanel guscioLista;

    private SaveLocator.Rilevamento rilevamento;
    private int generazione;

    public SchermataSalvataggi(AppConfig config, Ascoltatore ascoltatore) {
        this(config, ascoltatore, true);
    }

    SchermataSalvataggi(AppConfig config, Ascoltatore ascoltatore, boolean rileva) {
        this.config = config;
        this.ascoltatore = ascoltatore;
        setBackground(Theme.SFONDO);

        // Impaginazione a quattro fasce: testata, corpo, fascia elastica,
        // piede. Non uso un BoxLayout perche' dentro un BorderLayout
        // distribuirebbe lo spazio residuo anche a testata e corpo, spingendo
        // tutto verso il basso e aprendo una fascia vuota in alto. Con il
        // GridBag il peso lo ha solo la fascia elastica: testata e corpo
        // stanno in cima e il piede cade subito sotto la lista.
        JPanel testata = costruisciTestata();
        JPanel corpo = costruisciCorpo();
        JPanel piede = costruisciPiede();

        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;

        gc.gridy = 0; gc.weighty = 0; add(testata, gc);
        gc.gridy = 1; gc.weighty = 0; add(corpo, gc);
        // La fascia elastica e' l'unica a peso 1: assorbe tutto lo spazio che
        // avanza e tiene i pulsanti attaccati sotto l'ultimo slot.
        JPanel fascia = new JPanel();
        fascia.setOpaque(false);
        gc.gridy = 2;
        gc.weighty = 1;
        add(fascia, gc);
        gc.gridy = 3;
        gc.weighty = 0;
        add(piede, gc);

        Theme.rifinisci(this);
        if (rileva) avvia();
    }

    private JPanel costruisciTestata() {
        JPanel p = Theme.colonnaFluida(); p.setOpaque(false);testataSelezione=p;
        p.setBorder(Scala.bordo(20, 22, 16, 22));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel passo = passoSelezione;
        passo.setFont(Theme.etichetta()); passo.setForeground(Theme.ACCENTO);
        javax.swing.JTextArea titolo = titoloSelezione;
        titolo.setText("La tua flotta. Un nuovo orizzonte.");
        titolo.setEditable(false);titolo.setOpaque(false);titolo.setLineWrap(true);titolo.setWrapStyleWord(true);titolo.setRows(1);
        titolo.setFont(Theme.titolo()); titolo.setForeground(Theme.TESTO);
        sottotitolo.setText("Scegli la partita e gestisci le tue Corvette in un unico spazio.");
        sottotitolo.setForeground(Theme.TESTO_TENUE); sottotitolo.setFont(Theme.sezione());
        p.add(passo); p.add(Box.createVerticalStrut(Scala.px(7))); p.add(titolo);
        p.add(Box.createVerticalStrut(Scala.px(6))); p.add(sottotitolo);
        return p;
    }

    private JPanel costruisciCorpo() {
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setCellRenderer(new RendererSlot());
        Theme.altezzaRiga(lista, 68);
        lista.setOpaque(false);
        lista.setBackground(new Color(0, 0, 0, 0));
        lista.setBorder(Scala.bordo(4, 2, 4, 2));
        // La lista dichiara l'altezza del suo contenuto (righe + bordi) invece
        // di stirarsi: con pochi slot riempiva tutta la finestra e sotto
        // restava una voragine scura.

        lista.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                SaveSlotInfo s = lista.getSelectedValue();
                boolean utilizzabile = s != null && !s.isVuoto() && s.getModello() != null;
                continua.setEnabled(utilizzabile);
                if (s != null && s.isVuoto()) {
                    messaggio.setText("Lo slot " + s.getNumero()
                            + " e' libero: scegline uno occupato.");
                }
            }
        });

        JScrollPane scroll = new JScrollPane(lista) {
            @Override public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                // Alta quanto le righe che contiene, mai di piu': cosi' la
                // lista non lascia spazio vuoto quando gli slot sono pochi.
                int righe = Math.max(1, modello.getSize());
                int utile = righe * Scala.px(68) + Scala.px(10);
                int massimo = getParent() == null ? utile
                        : Math.max(Scala.px(120), getParent().getHeight() - Scala.px(150));
                d.height = Math.min(utile, massimo);
                return d;
            }
        };
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(Scala.px(24));

        JLabel t = Theme.titoloCarta("Slot disponibili");
        t.setBorder(Scala.bordo(0, 0, 8, 0));

        JPanel testa = new JPanel(new BorderLayout());
        testa.setOpaque(false);
        testa.add(t, BorderLayout.NORTH);
        stato.setForeground(Theme.TESTO_DEBOLE);
        stato.setFont(Scala.font(Font.PLAIN, 11));
        testa.add(stato, BorderLayout.SOUTH);

        // La lista dichiara l'altezza del suo contenuto (righe + bordi) e sta
        // in un guscio ad altezza fissa: cosi' non si stira e sotto non resta
        // una voragine quando gli slot sono pochi.
        int righe = Math.max(1, modello.getSize());
        guscioLista = new JPanel(new BorderLayout());
        guscioLista.setOpaque(false);
        guscioLista.add(scroll, BorderLayout.NORTH);
        aggiornaAltezzaLista();

        JPanel elenco = new JPanel(new BorderLayout(0, Scala.px(10)));
        elenco.setOpaque(false);
        elenco.add(testa, BorderLayout.NORTH);
        elenco.add(guscioLista, BorderLayout.CENTER);

        // L'introduzione sta a sinistra, l'elenco a destra. Entrambi ancorati
        // in alto.
        JPanel colonnaIntro = new JPanel(new BorderLayout());
        colonnaIntro.setOpaque(false);
        colonnaIntro.add(introduzione, BorderLayout.NORTH);

        JPanel colonnaElenco = new JPanel(new BorderLayout());
        colonnaElenco.setOpaque(false);
        colonnaElenco.add(elenco, BorderLayout.NORTH);

        rigaCorpo = new JPanel(new BorderLayout(Scala.px(22), 0));
        rigaCorpo.setOpaque(false);
        rigaCorpo.setBorder(Scala.bordo(8, 22, 0, 22));
        rigaCorpo.add(colonnaIntro, BorderLayout.WEST);
        rigaCorpo.add(colonnaElenco, BorderLayout.CENTER);
        // Il corpo e' solo la riga intro + lista: niente glue qui dentro, perche'
        // l'elasticita' e' gia' gestita dal contenuto esterno. Se la glue stesse
        // anche qui, il corpo si prenderebbe tutto lo spazio e il piede
        // tornerebbe in fondo.
        rigaCorpo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel corpo = new JPanel();
        corpo.setOpaque(false);
        corpo.setLayout(new BoxLayout(corpo, BoxLayout.Y_AXIS));
        corpo.add(rigaCorpo);
        corpo.setAlignmentX(Component.LEFT_ALIGNMENT);
        return corpo;
    }

    private JPanel costruisciPiede() {
        JPanel p = new JPanel(new BorderLayout(Scala.px(18), 0));
        p.setOpaque(false);
        p.setBorder(Scala.bordo(10, 22, 12, 22));

        messaggio.setForeground(Theme.TESTO_TENUE);
        messaggio.setFont(Theme.corpo());
        messaggio.setBorder(Scala.bordo(0, 2, 0, 0));

        JButton cambia = new JButton("Cambia cartella\u2026");
        Theme.secondaria(cambia);
        cambia.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                scegliCartella();
            }
        });

        continua.setEnabled(false);
        Theme.primaria(continua);
        continua.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SaveSlotInfo s = lista.getSelectedValue();
                if (s != null && rilevamento != null) {
                    ascoltatore.salvataggioScelto(rilevamento, s);
                }
            }
        });

        // I pulsanti stanno a destra, il messaggio a sinistra: e' la lettura
        // naturale (prima si legge lo stato, poi si agisce) e non lascia
        // spazio morto in mezzo.
        JPanel destra = new JPanel(new LayoutFluido(Scala.px(8), Scala.px(4)));
        destra.setOpaque(false);
        destra.add(cambia);
        destra.add(continua);

        p.add(messaggio, BorderLayout.CENTER);
        p.add(destra, BorderLayout.EAST);
        return p;
    }

    // ------------------------------------------------------------ ricerca

    /**
     * Tiene la lista alta quanto le sue righe.
     *
     * Il numero di slot si sa solo dopo la lettura, quindi la misura va rifatta
     * ogni volta che il modello cambia: una lista stirata a tutta finestra
     * lascerebbe sotto l'ultima riga un rettangolo vuoto.
     */
    private void aggiornaAltezzaLista() {
        if (guscioLista == null) return;
        int righe = Math.max(1, modello.getSize());
        guscioLista.setPreferredSize(Scala.dim(righe * Scala.px(68) + Scala.px(10),
                righe * Scala.px(68) + Scala.px(10)));
        guscioLista.revalidate();
        guscioLista.repaint();
    }

    public void ricarica() { avvia(); }

    @Override public void doLayout() {
        boolean mostra=getWidth()>=Scala.px(960) && getHeight()>=Scala.px(600);
        if(introduzione.isVisible()!=mostra) introduzione.setVisible(mostra);
        boolean compatto=getWidth()<Scala.px(600) || getHeight()<Scala.px(440);
        if(modoSelezione!=(compatto?1:0)) {
            modoSelezione=compatto?1:0;
            passoSelezione.setVisible(!compatto);sottotitolo.setVisible(!compatto);
            titoloSelezione.setText(compatto?"Seleziona la partita":"La tua flotta. Un nuovo orizzonte.");
            titoloSelezione.setFont(Scala.font(Font.BOLD,compatto?18:22));
            testataSelezione.setBorder(compatto?Scala.bordo(8,16,8,16):Scala.bordo(16,20,14,20));
        }
        super.doLayout();
    }

    private void avvia() {
        final int richiesta = ++generazione;
        modello.clear(); continua.setEnabled(false);
        stato.setText("Ricerca dei salvataggi…");
        new SwingWorker<SaveLocator.Rilevamento, Void>() {
            @Override protected SaveLocator.Rilevamento doInBackground() {
                String ricordata = config.cartellaSalvataggi();
                if (ricordata != null) {
                    SaveLocator.Rilevamento r = SaveLocator.apri(new File(ricordata), config.tipoStorage());
                    if (r != null && SaveLocator.contaSlotPieni(r.storage) > 0) return r;
                }
                return SaveLocator.rileva();
            }
            @Override protected void done() {
                if (richiesta != generazione) return;
                try {
                    SaveLocator.Rilevamento r = get();
                    if (r == null) {
                        stato.setText("Nessun salvataggio trovato");
                        messaggio.setText("Usa Cambia cartella per indicare i tuoi salvataggi.");
                    } else applica(r, "rilevamento automatico");
                } catch (Exception e) { stato.setText("Ricerca non riuscita: " + e.getMessage()); }
            }
        }.execute();
    }

    private void applica(SaveLocator.Rilevamento r, String come) {
        this.rilevamento = r;
        stato.setText("Salvataggi trovati: " + r.tipo + " · " + r.cartella.getAbsolutePath());
        messaggio.setText("Trovati per " + come + ". Scegli lo slot e premi Continua.");
        config.ricorda(r.cartella.getAbsolutePath(), r.tipo);
        caricaSlot(r);
    }

    private void caricaSlot(final SaveLocator.Rilevamento r) {
        final int richiesta = ++generazione;
        modello.clear(); continua.setEnabled(false);
        new SwingWorker<List<SaveSlotInfo>, Void>() {
            @Override
            protected List<SaveSlotInfo> doInBackground() {
                List<SaveSlotInfo> out = new ArrayList<SaveSlotInfo>();
                ft[] slots = r.storage.bU();
                for (int i = 0; i < slots.length; i++) {
                    // si elencano SOLO gli slot occupati: mostrare i liberi
                    // riempie la lista di righe inutili
                    if (slots[i] != null && !slots[i].isEmpty()) {
                        SaveSlotInfo info = new SaveSlotInfo(slots[i]);
                        info.getModello(); // Conversione fuori dal renderer e dall'EDT.
                        out.add(info);
                    }
                }
                return out;
            }

            @Override
            protected void done() {
                try {
                    if (richiesta != generazione) return;
                    List<SaveSlotInfo> l = get();
                    for (int i = 0; i < l.size(); i++) {
                        modello.addElement(l.get(i));
                    }
                    aggiornaAltezzaLista();
                    int pieni = 0;
                    for (int i = 0; i < l.size(); i++) {
                        if (!l.get(i).isVuoto()) {
                            pieni++;
                        }
                    }
                    if (pieni == 0) {
                        messaggio.setText("Nessuno slot occupato in questa cartella.");
                    } else {
                        // si posiziona sul primo slot occupato
                        for (int i = 0; i < l.size(); i++) {
                            if (!l.get(i).isVuoto()) {
                                lista.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    messaggio.setText("Errore nella lettura degli slot: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void scegliCartella() {
        File partenza = rilevamento != null ? rilevamento.cartella : null;
        File scelta = SelettoreFile.cartella(this,
                "Indica la cartella dei salvataggi di No Man's Sky", partenza);
        if (scelta == null || !scelta.isDirectory()) {
            return;
        }
        final File sceltaFinale = scelta;
        final int richiesta = ++generazione;
        modello.clear(); continua.setEnabled(false); stato.setText("Verifico la cartella…");
        new SwingWorker<SaveLocator.Rilevamento, Void>() {
            @Override protected SaveLocator.Rilevamento doInBackground() { return SaveLocator.apri(sceltaFinale); }
            @Override protected void done() {
                if (richiesta != generazione) return;
                try { cartellaScelta(get()); }
                catch (Exception e) { messaggio.setText("Cartella non leggibile: " + e.getMessage()); }
            }
        }.execute();
    }

    private void cartellaScelta(SaveLocator.Rilevamento r) {
        if (r == null) {
            JOptionPane.showMessageDialog(this,
                    "In questa cartella non ho trovato un salvataggio leggibile.\n\n"
                            + "Mi aspetto:\n"
                            + "   · file save*.hg             (Steam, GOG, Epic)\n"
                            + "   · un file containers.index  (Xbox app su PC)",
                    "Cartella non valida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        applica(r, "scelta manuale");
    }

    /** Rende leggibile uno slot: una carta con i dati utili e niente rumore. */
    private final class RendererSlot extends JPanel implements ListCellRenderer<SaveSlotInfo> {

        private final Theme.SegnoNumero numero = new Theme.SegnoNumero("00");
        private final JLabel riga1 = new JLabel();
        private final JLabel riga2 = new JLabel();
        private boolean selezionato;

        RendererSlot() {
            setLayout(new BorderLayout(Scala.px(13), 0));
            setOpaque(false);
            numero.setPreferredSize(Scala.dim(34, 34));
            numero.setMinimumSize(Scala.dim(34, 34));
            numero.setMaximumSize(Scala.dim(34, 34));
            numero.setFont(Scala.font(Font.PLAIN, 13));

            JPanel testi = new JPanel();
            testi.setOpaque(false);
            testi.setLayout(new BoxLayout(testi, BoxLayout.Y_AXIS));
            riga1.setFont(Theme.corpo());
            riga2.setFont(Theme.etichetta());
            testi.add(Box.createVerticalGlue());
            testi.add(riga1);
            testi.add(Box.createVerticalStrut(Scala.px(3)));
            testi.add(riga2);
            testi.add(Box.createVerticalGlue());

            add(numero, BorderLayout.WEST);
            add(testi, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends SaveSlotInfo> list,
                                                      SaveSlotInfo s, int index,
                                                      boolean selezionato, boolean fuoco) {
            this.selezionato = selezionato;

            if (s.isVuoto()) {
                numero.spento();
                numero.setText(String.format("%02d", s.getNumero()));
                riga1.setFont(Theme.corpo());
                riga1.setText("Slot " + s.getNumero() + "   \u00b7   libero");
                riga2.setText("nessun salvataggio in questo slot");
                riga1.setForeground(Theme.TESTO_DEBOLE);
                riga2.setForeground(Theme.TESTO_DEBOLE);
                setBorder(Scala.bordo(10, 14, 10, 14));
                revalidate();
                return this;
            }

            numero.attivo();
            numero.setText(String.format("%02d", s.getNumero()));
            riga1.setFont(Theme.corpo());
            riga1.setText(s.getModalita() + "   \u00b7   " + s.getNomeSalvataggio());
            riga2.setText(s.getOreFormattate() + "   \u00b7   " + s.getDataFormattata());

            riga1.setForeground(selezionato ? Theme.TESTO : Theme.TESTO);
            riga2.setForeground(selezionato ? Theme.TESTO_TENUE : Theme.TESTO_TENUE);
            setBorder(Scala.bordo(10, 14, 10, 14));
            revalidate();
            return this;
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int pad = Scala.px(4);
            int r = Scala.px(12);
            if (selezionato) {
                Theme.ombra(p, pad, pad, getWidth() - pad * 2, getHeight() - pad * 2, r, 0.55f);
                p.setColor(Theme.SUPERFICIE_ALTA);
                p.fillRoundRect(pad, pad, getWidth() - pad * 2 - 1, getHeight() - pad * 2 - 1, r, r);
                p.setColor(Theme.ACCENTO);
                p.setStroke(new BasicStroke(Scala.px(1.4f)));
                p.drawRoundRect(pad, pad, getWidth() - pad * 2 - 1, getHeight() - pad * 2 - 1, r, r);
            } else {
                p.setColor(Theme.SUPERFICIE);
                p.fillRoundRect(pad, pad, getWidth() - pad * 2 - 1, getHeight() - pad * 2 - 1, r, r);
                p.setColor(Theme.BORDO);
                p.drawRoundRect(pad, pad, getWidth() - pad * 2 - 1, getHeight() - pad * 2 - 1, r, r);
            }
            p.dispose();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return Scala.dim(920, 620);
    }
}
