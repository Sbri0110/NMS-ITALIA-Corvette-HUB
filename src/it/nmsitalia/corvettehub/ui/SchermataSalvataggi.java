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
import javax.swing.JFileChooser;
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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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

    private SaveLocator.Rilevamento rilevamento;
    private int generazione;

    public SchermataSalvataggi(AppConfig config, Ascoltatore ascoltatore) {
        this(config, ascoltatore, true);
    }

    SchermataSalvataggi(AppConfig config, Ascoltatore ascoltatore, boolean rileva) {
        this.config = config;
        this.ascoltatore = ascoltatore;
        setBackground(Theme.SFONDO);
        setLayout(new BorderLayout());
        add(costruisciTestata(), BorderLayout.NORTH);
        add(costruisciCorpo(), BorderLayout.CENTER);
        add(costruisciPiede(), BorderLayout.SOUTH);
        Theme.rifinisci(this);
        if (rileva) avvia();
    }

    private JPanel costruisciTestata() {
        JPanel p = Theme.colonnaFluida(); p.setOpaque(false);testataSelezione=p;
        p.setBorder(Scala.bordo(16, 20, 14, 20));
        JLabel passo = passoSelezione;
        passo.setFont(Scala.font(Font.BOLD, 11)); passo.setForeground(Theme.ACCENTO);
        javax.swing.JTextArea titolo = titoloSelezione;
        titolo.setText("La tua flotta. Un nuovo orizzonte.");
        titolo.setEditable(false);titolo.setOpaque(false);titolo.setLineWrap(true);titolo.setWrapStyleWord(true);titolo.setRows(1);
        titolo.setFont(Scala.font(Font.BOLD, 22)); titolo.setForeground(Theme.TESTO);
        sottotitolo.setText("Scegli la partita e gestisci le tue Corvette in un unico spazio.");
        sottotitolo.setForeground(Theme.TESTO_TENUE); sottotitolo.setFont(Scala.font(Font.PLAIN, 12));
        p.add(passo); p.add(Box.createVerticalStrut(Scala.px(6))); p.add(titolo);
        p.add(Box.createVerticalStrut(Scala.px(5))); p.add(sottotitolo);
        return p;
    }

    private JPanel costruisciCorpo() {
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setCellRenderer(new RendererSlot());
        lista.setFixedCellHeight(Scala.px(62));
        lista.setBackground(Theme.SUPERFICIE);
        lista.setBorder(Scala.bordo(6, 6, 6, 6));
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

        JScrollPane scroll = new JScrollPane(lista);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDO));

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(Scala.bordo(0, 20, 0, 20));

        JLabel t = new JLabel("Slot disponibili");
        t.setFont(Theme.sezione());
        t.setForeground(Theme.TESTO_TENUE);
        t.setBorder(Scala.bordo(0, 0, 8, 0));

        JPanel elenco = new JPanel(new BorderLayout(0, Scala.px(10))); elenco.setOpaque(false);
        JPanel testa = new JPanel(new BorderLayout()); testa.setOpaque(false);
        testa.add(t, BorderLayout.NORTH);
        stato.setForeground(Theme.TESTO_TENUE); stato.setFont(Scala.font(Font.PLAIN, 11));
        testa.add(stato, BorderLayout.SOUTH);
        elenco.add(testa, BorderLayout.NORTH); elenco.add(scroll, BorderLayout.CENTER);
        p.setLayout(new BorderLayout(Scala.px(16), 0));
        p.add(introduzione, BorderLayout.WEST);
        p.add(elenco, BorderLayout.CENTER);
        return p;
    }

    private JPanel costruisciPiede() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(Scala.bordo(12, 20, 14, 20));

        messaggio.setForeground(Theme.TESTO_TENUE);
        messaggio.setFont(Scala.font(Font.PLAIN, 12));

        JButton cambia = new JButton("Cambia cartella...");
        cambia.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                scegliCartella();
            }
        });

        continua.setEnabled(false);
        continua.setFont(Scala.font(Font.BOLD, 13));
        continua.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SaveSlotInfo s = lista.getSelectedValue();
                if (s != null && rilevamento != null) {
                    ascoltatore.salvataggioScelto(rilevamento, s);
                }
            }
        });

        JPanel destra = new JPanel(new LayoutFluido(Scala.px(6),Scala.px(4)));
        destra.setOpaque(false);
        destra.add(cambia);
        destra.add(continua);

        p.add(messaggio, BorderLayout.NORTH);
        p.add(destra, BorderLayout.CENTER);
        return p;
    }

    // ------------------------------------------------------------ ricerca

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
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Indica la cartella dei salvataggi di No Man's Sky");
        if (rilevamento != null) {
            fc.setCurrentDirectory(rilevamento.cartella);
        }
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        final File scelta = fc.getSelectedFile();
        final int richiesta = ++generazione;
        modello.clear(); continua.setEnabled(false); stato.setText("Verifico la cartella…");
        new SwingWorker<SaveLocator.Rilevamento, Void>() {
            @Override protected SaveLocator.Rilevamento doInBackground() { return SaveLocator.apri(scelta); }
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

    /** Rende leggibile uno slot: due righe, dati utili e niente rumore. */
    private static final class RendererSlot extends JPanel implements ListCellRenderer<SaveSlotInfo> {

        private final JLabel riga1 = new JLabel();
        private final JLabel riga2 = new JLabel();

        RendererSlot() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(Scala.bordo(18, 20, 18, 20));
            riga1.setFont(Scala.font(Font.BOLD, 14));
            riga2.setFont(Scala.font(Font.PLAIN, 12));
            add(riga1);
            add(Box.createVerticalStrut(3));
            add(riga2);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends SaveSlotInfo> list,
                                                      SaveSlotInfo s, int index,
                                                      boolean selezionato, boolean fuoco) {
            if (s.isVuoto()) {
                riga1.setText("Slot " + s.getNumero() + "   ·   libero");
                riga2.setText("nessun salvataggio in questo slot");
                riga1.setFont(Scala.font(Font.PLAIN, 14));
                if (selezionato) {
                    setBackground(Theme.ACCENTO_SCURO);
                    riga1.setForeground(Color.WHITE);
                    riga2.setForeground(Theme.TESTO_TENUE);
                } else {
                    setBackground(Theme.SUPERFICIE);
                    riga1.setForeground(Theme.TESTO_TENUE);
                    riga2.setForeground(new Color(0x6A, 0x68, 0x72));
                }
                return this;
            }

            riga1.setFont(Scala.font(Font.BOLD, 14));
            riga1.setText(String.format("%02d   /   %s", s.getNumero(), s.getModalita()));
            String nome = s.getNomeSalvataggio();
            String ore = s.getOreFormattate();
            riga2.setText((nome == null ? "(nome non leggibile)" : nome)
                    + "   ·   " + ore
                    + "   ·   " + s.getDataFormattata()
);

            if (selezionato) {
                setBackground(Theme.ACCENTO_SCURO);
                riga1.setForeground(Color.WHITE);
                riga2.setForeground(Theme.TESTO_TENUE);
            } else {
                setBackground(Theme.SUPERFICIE);
                riga1.setForeground(Theme.TESTO);
                riga2.setForeground(Theme.TESTO_TENUE);
            }
            return this;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return Scala.dim(920, 620);
    }
}
