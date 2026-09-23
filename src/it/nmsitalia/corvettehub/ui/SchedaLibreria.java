package it.nmsitalia.corvettehub.ui;

import it.nmsitalia.corvettehub.domain.CatalogoParti;
import it.nmsitalia.corvettehub.domain.Corvette;
import it.nmsitalia.corvettehub.domain.WrapperBuild;
import nomanssave.eV;
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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Scheda Libreria: i progetti salvati nella cartella Builds/.
 *
 * Mostra i progetti con nome, autore, data e numero di moduli, ne disegna
 * l'anteprima e permette di eliminarli con conferma. Il pulsante di
 * importazione arriva con la Fase 3, quando le scritture sul salvataggio
 * saranno implementate e collaudate.
 */
public final class SchedaLibreria extends JPanel {

    /** Chi vuole sapere quando la libreria cambia contenuto. */
    public interface Ascoltatore {
        void libreriaCambiata();
    }

    private final File cartella;
    private final Ascoltatore ascoltatore;

    private final DefaultListModel<File> modello = new DefaultListModel<File>();
    private final JList<File> lista = new JList<File>(modello);
    private final JTextField cerca = new JTextField(16);
    private final JTextArea dettaglio = new JTextArea();
    private final Anteprima anteprima = new Anteprima();
    private final JLabel stato = new JLabel(" ");

    private List<File> tutti = new ArrayList<File>();

    public SchedaLibreria(File cartella, Ascoltatore ascoltatore) {
        this.cartella = cartella;
        this.ascoltatore = ascoltatore;
        setBackground(Theme.SFONDO);
        setLayout(new BorderLayout());
        add(costruisciCorpo(), BorderLayout.CENTER);
        add(costruisciPiede(), BorderLayout.SOUTH);
        ricarica();
    }

    private Component costruisciCorpo() {
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setCellRenderer(new RendererProgetto());
        lista.setFixedCellHeight(46);
        lista.setBackground(Theme.SUPERFICIE);
        lista.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    progettoScelto();
                }
            }
        });

        cerca.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filtra();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filtra();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filtra();
            }
        });

        JPanel sinistra = new JPanel(new BorderLayout());
        sinistra.setOpaque(false);
        JPanel testaSinistra = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        testaSinistra.setOpaque(false);
        JLabel l = new JLabel("Cerca:");
        l.setForeground(Theme.TESTO_TENUE);
        testaSinistra.add(l);
        testaSinistra.add(cerca);

        JPanel titolo = new JPanel();
        titolo.setOpaque(false);
        titolo.setLayout(new BoxLayout(titolo, BoxLayout.Y_AXIS));
        JLabel t = new JLabel("Progetti nella libreria");
        t.setFont(Theme.sezione());
        t.setForeground(Theme.TESTO_TENUE);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        testaSinistra.setAlignmentX(Component.LEFT_ALIGNMENT);
        titolo.add(t);
        titolo.add(Box.createVerticalStrut(6));
        titolo.add(testaSinistra);

        JScrollPane scrollLista = new JScrollPane(lista);
        scrollLista.setBorder(BorderFactory.createLineBorder(Theme.BORDO));

        sinistra.add(titolo, BorderLayout.NORTH);
        sinistra.add(scrollLista, BorderLayout.CENTER);
        sinistra.setBorder(BorderFactory.createEmptyBorder(14, 16, 0, 8));

        dettaglio.setEditable(false);
        dettaglio.setBackground(Theme.SUPERFICIE);
        dettaglio.setForeground(Theme.TESTO);
        dettaglio.setFont(Theme.monospazio());
        dettaglio.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JScrollPane scrollDettaglio = new JScrollPane(dettaglio);
        scrollDettaglio.setBorder(BorderFactory.createLineBorder(Theme.BORDO));

        JPanel destra = new JPanel(new BorderLayout());
        destra.setOpaque(false);
        destra.setBorder(BorderFactory.createEmptyBorder(14, 8, 0, 16));

        JLabel t2 = new JLabel("Anteprima del progetto");
        t2.setFont(Theme.sezione());
        t2.setForeground(Theme.TESTO_TENUE);
        t2.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        anteprima.setPreferredSize(new Dimension(520, 230));
        scrollDettaglio.setPreferredSize(new Dimension(520, 190));

        destra.add(t2, BorderLayout.NORTH);
        destra.add(anteprima, BorderLayout.CENTER);
        destra.add(scrollDettaglio, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sinistra, destra);
        split.setDividerLocation(330);
        split.setResizeWeight(0.35);
        split.setBorder(null);
        split.setOpaque(false);
        return split;
    }

    private JPanel costruisciPiede() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(10, 16, 14, 16));

        stato.setForeground(Theme.TESTO_TENUE);
        stato.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JButton apri = new JButton("Apri cartella");
        apri.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                apriCartella();
            }
        });

        JButton elimina = new JButton("Elimina progetto");
        elimina.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                elimina();
            }
        });

        JButton importa = new JButton("Importa nel salvataggio...");
        importa.setEnabled(false);
        importa.setToolTipText("Arriva con la Fase 3, insieme alle scritture protette "
                + "da backup");

        JPanel destra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        destra.setOpaque(false);
        destra.add(apri);
        destra.add(elimina);
        destra.add(importa);

        p.add(stato, BorderLayout.WEST);
        p.add(destra, BorderLayout.EAST);
        return p;
    }

    // ---------------------------------------------------------------- dati

    public void ricarica() {
        tutti = WrapperBuild.elencoLibreria(cartella);
        filtra();
    }

    private void filtra() {
        String q = cerca.getText() == null ? "" : cerca.getText().trim().toLowerCase(Locale.ROOT);
        modello.clear();
        for (int i = 0; i < tutti.size(); i++) {
            String n = tutti.get(i).getName().toLowerCase(Locale.ROOT);
            if (q.isEmpty() || n.contains(q)) {
                modello.addElement(tutti.get(i));
            }
        }
        stato.setText(modello.size() + " progetti in " + cartella.getAbsolutePath());
        if (modello.isEmpty()) {
            dettaglio.setText("La libreria e' vuota.\n\n"
                    + "Usa la scheda Esporta per creare il primo progetto da una\n"
                    + "Corvette del tuo salvataggio.\n\n"
                    + "Cartella: " + cartella.getAbsolutePath());
            anteprima.mostra(null);
        }
    }

    private void progettoScelto() {
        File f = lista.getSelectedValue();
        if (f == null) {
            return;
        }
        try {
            WrapperBuild b = WrapperBuild.leggi(f);
            StringBuilder t = new StringBuilder();
            t.append("PROGETTO\n\n");
            t.append("Nome       : ").append(b.getNome()).append('\n');
            t.append("Autore     : ").append(b.getAutore().isEmpty()
                    ? "(non indicato)" : b.getAutore()).append('\n');
            t.append("Data UTC   : ").append(b.getDataUtc().isEmpty()
                    ? "(non indicata)" : b.getDataUtc()).append('\n');
            t.append("Formato    : ").append(b.getFormatoDichiarato());
            if (b.getVersione() > 0) {
                t.append(" v").append(b.getVersione());
            }
            t.append('\n');
            t.append("Moduli     : ").append(b.getNumeroModuli()).append('\n');
            t.append("Tipi       : ").append(b.getPartiRichieste().size()).append('\n');
            t.append("File       : ").append(f.getName()).append('\n');
            t.append("Modificato : ")
                    .append(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(f.lastModified())))
                    .append('\n');

            t.append("\nCATEGORIE\n");
            java.util.Map<String, Integer> perCat = new java.util.TreeMap<String, Integer>();
            for (String id : b.getPartiRichieste()) {
                String c = CatalogoParti.categoria(id);
                Integer n = perCat.get(c);
                perCat.put(c, n == null ? 1 : n + 1);
            }
            for (java.util.Map.Entry<String, Integer> e : perCat.entrySet()) {
                t.append(String.format("   %-24s %d%n", e.getKey(), e.getValue()));
            }

            dettaglio.setText(t.toString());
            dettaglio.setCaretPosition(0);
            anteprima.mostra(daBuild(b));
        } catch (Exception e) {
            dettaglio.setText("Non riesco a leggere questo progetto:\n\n" + e.getMessage());
            anteprima.mostra(null);
        }
    }

    /** Costruisce una Corvette fittizia solo per disegnarne l'anteprima. */
    private static Corvette daBuild(WrapperBuild b) {
        List<String> parti = new ArrayList<String>();
        List<double[]> pos = new ArrayList<double[]>();
        eV oggetti = b.getOggetti();
        for (int i = 0; i < oggetti.size(); i++) {
            eY o = oggetti.V(i);
            if (o == null) {
                continue;
            }
            String id = o.getValueAsString("ObjectID");
            if (id == null || id.isEmpty()) {
                continue;
            }
            double[] p = new double[]{0, 0, 0};
            try {
                eV v = o.d("Position");
                if (v != null && v.size() >= 3) {
                    p[0] = v.aa(0);
                    p[1] = v.aa(1);
                    p[2] = v.aa(2);
                }
            } catch (Throwable ignored) {
                // posizione non leggibile
            }
            parti.add(id);
            pos.add(p);
        }
        return new Corvette(0, -1, b.getNome(), parti, pos, false, new double[]{0, 0, 0});
    }

    // -------------------------------------------------------------- azioni

    private void apriCartella() {
        try {
            if (!cartella.isDirectory() && !cartella.mkdirs()) {
                throw new Exception("cartella non creabile");
            }
            java.awt.Desktop.getDesktop().open(cartella);
        } catch (Throwable t) {
            JOptionPane.showMessageDialog(this,
                    "Cartella dei progetti:\n" + cartella.getAbsolutePath(),
                    "Cartella", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void elimina() {
        File f = lista.getSelectedValue();
        if (f == null) {
            JOptionPane.showMessageDialog(this, "Scegli prima un progetto dall'elenco.",
                    "Nessuna selezione", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int r = JOptionPane.showConfirmDialog(this,
                "Eliminare definitivamente questo progetto?\n\n"
                        + f.getName() + "\n\n"
                        + "Il file viene cancellato dal disco. Il salvataggio di gioco "
                        + "non viene toccato.",
                "Conferma eliminazione", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }
        if (f.delete()) {
            ricarica();
            if (ascoltatore != null) {
                ascoltatore.libreriaCambiata();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Non riesco a cancellare il file.",
                    "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Rende leggibile una riga dell'elenco. */
    private static final class RendererProgetto extends JLabel
            implements javax.swing.ListCellRenderer<File> {

        RendererProgetto() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends File> list, File f,
                                                      int index, boolean sel, boolean fuoco) {
            String nome = f.getName();
            if (nome.toLowerCase(Locale.ROOT).endsWith(".json")) {
                nome = nome.substring(0, nome.length() - 5);
            }
            String data = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(f.lastModified()));
            setText("<html><b>" + escape(nome) + "</b><br><span style='font-size:10px'>"
                    + data + " · " + (f.length() / 1024) + " KB</span></html>");
            setFont(new Font("Segoe UI", Font.PLAIN, 12));
            setBackground(sel ? Theme.ACCENTO_SCURO : Theme.SUPERFICIE);
            setForeground(sel ? java.awt.Color.WHITE : Theme.TESTO);
            return this;
        }

        private static String escape(String s) {
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
