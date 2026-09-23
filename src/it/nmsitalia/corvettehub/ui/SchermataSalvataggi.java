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
    private final JList<SaveSlotInfo> lista = new JList<SaveSlotInfo>(modello);
    private final JButton continua = new JButton("Continua  \u2192");

    private SaveLocator.Rilevamento rilevamento;

    public SchermataSalvataggi(AppConfig config, Ascoltatore ascoltatore) {
        this.config = config;
        this.ascoltatore = ascoltatore;
        setBackground(Theme.SFONDO);
        setLayout(new BorderLayout());
        add(costruisciTestata(), BorderLayout.NORTH);
        add(costruisciCorpo(), BorderLayout.CENTER);
        add(costruisciPiede(), BorderLayout.SOUTH);
        avvia();
    }

    private JPanel costruisciTestata() {
        JPanel p = new JPanel(new BorderLayout(20, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(32, 44, 20, 44));

        JLabel logo = new JLabel();
        try {
            logo.setIcon(Icone.logo(96));
        } catch (Throwable ignored) {
            // senza logo la schermata funziona lo stesso
        }

        JPanel testi = new JPanel();
        testi.setOpaque(false);
        testi.setLayout(new BoxLayout(testi, BoxLayout.Y_AXIS));

        JLabel titolo = new JLabel(Main.NOME);
        titolo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titolo.setForeground(Theme.ACCENTO);
        titolo.setAlignmentX(Component.LEFT_ALIGNMENT);

        sottotitolo.setText("Scegli il salvataggio da usare. Nulla viene modificato "
                + "in questa fase.");
        sottotitolo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sottotitolo.setForeground(Theme.TESTO_TENUE);
        sottotitolo.setAlignmentX(Component.LEFT_ALIGNMENT);

        stato.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        stato.setForeground(Theme.TESTO_TENUE);
        stato.setAlignmentX(Component.LEFT_ALIGNMENT);

        testi.add(Box.createVerticalGlue());
        testi.add(titolo);
        testi.add(Box.createVerticalStrut(6));
        testi.add(sottotitolo);
        testi.add(Box.createVerticalStrut(18));
        testi.add(stato);
        testi.add(Box.createVerticalGlue());

        p.add(logo, BorderLayout.WEST);
        p.add(testi, BorderLayout.CENTER);
        return p;
    }

    private JPanel costruisciCorpo() {
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setCellRenderer(new RendererSlot());
        lista.setFixedCellHeight(64);
        lista.setBackground(Theme.SUPERFICIE);
        lista.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        lista.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                SaveSlotInfo s = lista.getSelectedValue();
                boolean utilizzabile = s != null && !s.isVuoto();
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
        p.setBorder(BorderFactory.createEmptyBorder(0, 44, 0, 44));

        JLabel t = new JLabel("Slot disponibili");
        t.setFont(Theme.sezione());
        t.setForeground(Theme.TESTO_TENUE);
        t.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        p.add(t, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel costruisciPiede() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(16, 44, 30, 44));

        messaggio.setForeground(Theme.TESTO_TENUE);
        messaggio.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JButton cambia = new JButton("Cambia cartella...");
        cambia.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                scegliCartella();
            }
        });

        continua.setEnabled(false);
        continua.setFont(new Font("Segoe UI", Font.BOLD, 13));
        continua.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SaveSlotInfo s = lista.getSelectedValue();
                if (s != null && rilevamento != null) {
                    ascoltatore.salvataggioScelto(rilevamento, s);
                }
            }
        });

        JPanel destra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        destra.setOpaque(false);
        destra.add(cambia);
        destra.add(continua);

        p.add(messaggio, BorderLayout.WEST);
        p.add(destra, BorderLayout.EAST);
        return p;
    }

    // ------------------------------------------------------------ ricerca

    private void avvia() {
        String ricordata = config.cartellaSalvataggi();
        if (ricordata != null) {
            SaveLocator.Rilevamento r =
                    SaveLocator.apri(new File(ricordata), config.tipoStorage());
            if (r != null && SaveLocator.contaSlotPieni(r.storage) > 0) {
                applica(r, "cartella ricordata");
                return;
            }
        }
        new SwingWorker<SaveLocator.Rilevamento, Void>() {
            @Override
            protected SaveLocator.Rilevamento doInBackground() {
                return SaveLocator.rileva();
            }

            @Override
            protected void done() {
                try {
                    SaveLocator.Rilevamento r = get();
                    if (r == null) {
                        stato.setText("Nessun salvataggio trovato.");
                        messaggio.setText("Non ho trovato i salvataggi di No Man's Sky. "
                                + "Se il gioco e' installato, indicami la cartella "
                                + "manualmente: la ricordero'.");
                        return;
                    }
                    applica(r, "rilevamento automatico");
                } catch (Exception e) {
                    stato.setText("Rilevamento fallito: " + e.getMessage());
                }
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
        modello.clear();
        new SwingWorker<List<SaveSlotInfo>, Void>() {
            @Override
            protected List<SaveSlotInfo> doInBackground() {
                List<SaveSlotInfo> out = new ArrayList<SaveSlotInfo>();
                ft[] slots = r.storage.bU();
                for (int i = 0; i < slots.length; i++) {
                    // si elencano SOLO gli slot occupati: mostrare i liberi
                    // riempie la lista di righe inutili
                    if (slots[i] != null && !slots[i].isEmpty()) {
                        out.add(new SaveSlotInfo(slots[i]));
                    }
                }
                return out;
            }

            @Override
            protected void done() {
                try {
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
        SaveLocator.Rilevamento r = SaveLocator.apri(fc.getSelectedFile());
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
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            riga1.setFont(new Font("Segoe UI", Font.BOLD, 14));
            riga2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
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
                riga1.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                if (selezionato) {
                    setBackground(Theme.ACCENTO_SCURO);
                    riga1.setForeground(Color.WHITE);
                    riga2.setForeground(new Color(0xF0, 0xE8, 0xDA));
                } else {
                    setBackground(index % 2 == 0 ? Theme.SUPERFICIE : Theme.SUPERFICIE_ALTA);
                    riga1.setForeground(Theme.TESTO_TENUE);
                    riga2.setForeground(new Color(0x6A, 0x68, 0x72));
                }
                return this;
            }

            riga1.setFont(new Font("Segoe UI", Font.BOLD, 14));
            riga1.setText("Slot " + s.getNumero() + "   ·   " + s.getModalita());
            String nome = s.getNomeSalvataggio();
            String ore = s.getOreFormattate();
            riga2.setText((nome == null ? "(nome non leggibile)" : nome)
                    + "   ·   " + ore
                    + "   ·   " + s.getDataFormattata()
                    + "   ·   " + s.getFile().size() + " file");

            if (selezionato) {
                setBackground(Theme.ACCENTO_SCURO);
                riga1.setForeground(Color.WHITE);
                riga2.setForeground(new Color(0xF0, 0xE8, 0xDA));
            } else {
                setBackground(index % 2 == 0 ? Theme.SUPERFICIE : Theme.SUPERFICIE_ALTA);
                riga1.setForeground(Theme.TESTO);
                riga2.setForeground(Theme.TESTO_TENUE);
            }
            return this;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(920, 620);
    }
}
