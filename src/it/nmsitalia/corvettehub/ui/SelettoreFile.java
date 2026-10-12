package it.nmsitalia.corvettehub.ui;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Una finestra per scegliere un file, disegnata come il resto del programma.
 *
 * Il {@code JFileChooser} di Swing usa il Look-and-Feel di sistema: sul
 * desktop di Windows esce il pannello grigio con "Risorse del computer"
 * scritto in inglese e le icone di vent'anni fa. In un programma che ha
 * un suo vestito, quella finestra sembra un'altra applicazione.
 *
 * Qui c'e' un sostituto che usa solo pezzi gia' esistenti: le listE di
 * {@link Theme}, la stessa tipografia, gli stessi pulsanti. Non e' un
 * file manager completo: fa una cosa sola, scegliere un file o una
 * cartella, e la fa restando dentro il tema.
 *
 * Uso:
 * <pre>
 *   File scelto = SelettoreFile.apri(this, "Scegli il progetto Corvette", cartella, "json");
 *   File cartella = SelettoreFile.cartella(this, "Scegli la cartella di lavoro", cartella);
 * </pre>
 */
public final class SelettoreFile {

    private SelettoreFile() { }

    // ------------------------------------------------------------ API

    /**
     * Apre il selettore per un singolo file.
     *
     * @param genitore   la finestra da cui si apre
     * @param titolo     la riga in alto
     * @param partenza   la cartella da mostrare per prima
     * @param estensione l'estensione accettata, senza punto; null per tutte
     * @return il file scelto, oppure null se si annulla
     */
    public static File apri(Component genitore, String titolo, File partenza, String estensione) {
        Dialogo d = new Dialogo(genitore, titolo, partenza, false, estensione);
        d.mostra();
        return d.getScelto();
    }

    /** Apre il selettore in modalita' cartella. */
    public static File cartella(Component genitore, String titolo, File partenza) {
        Dialogo d = new Dialogo(genitore, titolo, partenza, true, null);
        d.mostra();
        return d.getScelto();
    }

    /**
     * Costruisce il dialogo senza aprirlo.
     *
     * Serve alle prove automatiche: senza mostrare la finestra non si entra
     * nel ciclo modale, e si puo' disegnare il contenuto su un'immagine per
     * controllare che rispetti il tema.
     */
    static JComponent pannelloDiProva(Component genitore, String titolo, File partenza,
                                      boolean cartelle, String estensione, String nomeSalva) {
        Dialogo d = new Dialogo(genitore, titolo, partenza, cartelle, estensione);
        if (nomeSalva != null) d.modalitaSalva(nomeSalva);
        return d.contenutoDiProva();
    }

    /**
     * Apre il selettore per salvare un file nuovo.
     *
     * A differenza di {@link #apri} il nome si scrive a mano: il campo in
     * alto diventa il nome proposto, e si conferma con "Salva".
     */
    public static File salva(Component genitore, String titolo, File partenza, String nomeProposto) {
        Dialogo d = new Dialogo(genitore, titolo, partenza, false, null);
        d.modalitaSalva(nomeProposto);
        d.mostra();
        return d.getScelto();
    }

    // ------------------------------------------------------------ dialogo

    private static final class Dialogo {

        private final JPanel radice;
        private final boolean cartelle;
        private final String estensione;
        private final String titolo;
        private final Component genitore;
        private final DefaultListModel<Voce> modello = new DefaultListModel<Voce>();
        private final JList<Voce> lista = new JList<Voce>(modello);
        private final JTextField percorso = new JTextField();
        private final JLabel stato = new JLabel(" ");
        private final JButton apri = new JButton("Apri");
        private final JButton su = new JButton("Cartella superiore");
        private final JButton annulla = new JButton("Annulla");

        private JDialog finestra;
        private File cartellaCorrente;
        private File scelto;
        private boolean salvaCome;
        private String nomeFile;

        Dialogo(Component genitore, String titolo, File partenza, boolean cartelle, String estensione) {
            this.genitore = genitore;
            this.titolo = titolo;
            this.cartelle = cartelle;
            this.estensione = estensione == null ? null : estensione.toLowerCase(java.util.Locale.ITALY);

            radice = new JPanel(new BorderLayout(0, Scala.px(10)));
            radice.setBackground(Theme.ABISSO);
            radice.setBorder(Scala.bordo(14, 16, 14, 16));

            radice.add(costruisciTestata(titolo), BorderLayout.NORTH);
            radice.add(costruisciCentro(), BorderLayout.CENTER);
            radice.add(costruisciPiede(), BorderLayout.SOUTH);

            Theme.rifinisci(radice);
            apri.putClientProperty("JButton.buttonType", "roundRect");

            apri.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) { conferma(); }
            });
            annulla.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) { chiudi(); }
            });
            su.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) { saliDiUnLivello(); }
            });
            lista.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        Voce v = lista.getSelectedValue();
                        if (v != null) {
                            if (v.cartella || cartelle) apriVoce(v); else conferma();
                        }
                    }
                }
            });
            percorso.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) { vaiA(new File(percorso.getText().trim())); }
            });

            apri.setEnabled(false);
            vaiA(partenza);
        }

        private void chiudi() {
            if (finestra != null) finestra.dispose();
        }

        private JComponent costruisciTestata(String titolo) {
            JPanel p = new JPanel(new BorderLayout(0, Scala.px(8)));
            p.setOpaque(false);

            JLabel t = new JLabel(titolo);
            t.setFont(Theme.titoloCarta());
            t.setForeground(Theme.TESTO);

            JPanel riga = new JPanel(new BorderLayout(Scala.px(8), 0));
            riga.setOpaque(false);
            percorso.setFont(Theme.corpo());
            percorso.setBackground(Theme.SUPERFICIE_ALTA);
            percorso.setForeground(Theme.TESTO);
            percorso.setCaretColor(Theme.ACCENTO);

            JPanel conSu = new JPanel(new BorderLayout(Scala.px(6), 0));
            conSu.setOpaque(false);
            conSu.add(percorso, BorderLayout.CENTER);
            conSu.add(su, BorderLayout.EAST);

            JLabel nota = new JLabel(cartelle
                    ? "Scegli una cartella, poi conferma"
                    : "Scegli un file" + (estensione == null ? "" : " *." + estensione));
            nota.setFont(Theme.etichetta());
            nota.setForeground(Theme.TESTO_DEBOLE);

            p.add(t, BorderLayout.NORTH);
            p.add(conSu, BorderLayout.CENTER);
            p.add(nota, BorderLayout.SOUTH);
            return p;
        }

        private JComponent costruisciCentro() {
            lista.setFont(Theme.corpo());
            lista.setBackground(Theme.SUPERFICIE);
            lista.setForeground(Theme.TESTO);
            lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            lista.setCellRenderer(new Renderer());
            Theme.altezzaRiga(lista, 30);
            JScrollPane scroll = new JScrollPane(lista);
            scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDO));
            scroll.getViewport().setBackground(Theme.SUPERFICIE);
            scroll.getVerticalScrollBar().putClientProperty("JScrollBar.width", Scala.px(11));
            scroll.getVerticalScrollBar().setUnitIncrement(Scala.px(28));
            return scroll;
        }

        private JComponent costruisciPiede() {
            JPanel p = new JPanel(new BorderLayout(Scala.px(12), 0));
            p.setOpaque(false);

            stato.setFont(Theme.etichetta());
            stato.setForeground(Theme.TESTO_TENUE);

            JPanel destra = new JPanel(new FlowLayout(FlowLayout.RIGHT, Scala.px(8), 0));
            destra.setOpaque(false);
            Theme.secondaria(annulla);
            Theme.primaria(apri);
            destra.add(annulla);
            destra.add(apri);

            p.add(stato, BorderLayout.CENTER);
            p.add(destra, BorderLayout.EAST);
            return p;
        }

        // -------------------------------------------------------- navigazione

        private void vaiA(File cartella) {
            if (cartella == null || !cartella.isDirectory()) {
                if (cartella != null && cartella.isFile()) {
                    vaiA(cartella.getParentFile());
                    seleziona(cartella);
                }
                return;
            }
            cartellaCorrente = cartella;
            percorso.setText(salvaCome ? nomeFile : cartella.getAbsolutePath());
            percorso.setCaretPosition(0);
            riempi(cartella);
            apri.setEnabled(salvaCome || !cartelle);
        }

        /** Mostra anche i file in modalita' cartella? No: solo cartelle, per non confondere. */
        private void riempi(File cartella) {
            modello.clear();
            File[] figli = cartella.listFiles();
            List<Voce> voci = new ArrayList<Voce>();
            if (figli != null) {
                for (File f : figli) {
                    if (f.isDirectory()) {
                        voci.add(new Voce(f, true));
                    } else if (!cartelle && accettata(f)) {
                        voci.add(new Voce(f, false));
                    }
                }
            }
            Collections.sort(voci, new Comparator<Voce>() {
                @Override public int compare(Voce a, Voce b) {
                    if (a.cartella != b.cartella) return a.cartella ? -1 : 1;
                    return a.file.getName().compareToIgnoreCase(b.file.getName());
                }
            });
            for (Voce v : voci) modello.addElement(v);

            int cart = 0;
            for (Voce v : voci) if (v.cartella) cart++;
            stato.setText(voci.size() - cart + " file, " + cart + " cartelle");
            if (modello.getSize() > 0) lista.setSelectedIndex(0);
        }

        private boolean accettata(File f) {
            if (estensione == null) return true;
            String n = f.getName().toLowerCase(java.util.Locale.ITALY);
            return n.endsWith("." + estensione);
        }

        private void saliDiUnLivello() {
            File padre = cartellaCorrente == null ? null : cartellaCorrente.getParentFile();
            if (padre != null) vaiA(padre);
        }

        private void seleziona(File f) {
            for (int i = 0; i < modello.getSize(); i++) {
                if (modello.get(i).file.equals(f)) {
                    lista.setSelectedIndex(i);
                    lista.ensureIndexIsVisible(i);
                    return;
                }
            }
        }

        private void apriVoce(Voce v) {
            if (v.cartella) {
                vaiA(v.file);
            } else if (cartelle) {
                // in modalita' cartella un doppio clic sui file non fa nulla
            } else {
                conferma();
            }
        }

        private void conferma() {
            if (salvaCome) {
                String scritto = nomeScritto();
                if (scritto.isEmpty()) return;
                File f = new File(scritto);
                if (f.isDirectory()) { vaiA(f); return; }
                scelto = f;
            } else if (cartelle) {
                if (cartellaCorrente == null) return;
                Voce v = lista.getSelectedValue();
                scelto = (v != null && v.cartella) ? v.file : cartellaCorrente;
            } else {
                // se il percorso in alto punta a un file esistente, vale quello:
                // cosi' si puo' anche incollare un percorso e premere Apri
                File digitato = new File(percorso.getText().trim());
                if (digitato.isFile()) {
                    scelto = digitato;
                    chiudi();
                    return;
                }
                Voce v = lista.getSelectedValue();
                if (v == null || v.cartella) return;
                scelto = v.file;
            }
            chiudi();
        }

        void mostra() {
            Window proprietario = genitore == null ? null : SwingUtilities.getWindowAncestor(genitore);
            finestra = new JDialog(proprietario, titolo, JDialog.ModalityType.APPLICATION_MODAL);
            finestra.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            finestra.setLayout(new BorderLayout());
            finestra.add(radice, BorderLayout.CENTER);
            finestra.setSize(Scala.dim(760, 520));
            finestra.setMinimumSize(Scala.dim(520, 360));
            if (proprietario != null) finestra.setLocationRelativeTo(proprietario);
            if (salvaCome) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() { percorso.requestFocusInWindow(); percorso.selectAll(); }
                });
            }
            finestra.setVisible(true);
        }
        File getScelto() { return scelto; }

        /** Il pannello interno, per le prove automatiche (nessuna finestra aperta). */
        JComponent contenutoDiProva() {
            return radice;
        }

        /** Trasforma il dialogo in "salva con nome". */
        void modalitaSalva(String nomeProposto) {
            salvaCome = true;
            nomeFile = nomeProposto == null ? "" : nomeProposto;
            apri.setText("Salva");
            apri.setEnabled(true);
            percorso.setText(nomeFile);
        }

        /** Il nome scritto a mano, se siamo in modalita' salvataggio. */
        private String nomeScritto() {
            String t = percorso.getText().trim();
            if (t.isEmpty()) return "";
            File f = new File(t);
            // se l'utente ha digitato un percorso, si usa quello
            if (f.isAbsolute()) return t;
            return new File(cartellaCorrente, t).getAbsolutePath();
        }
    }

    // ------------------------------------------------------------ riga

    /** Una voce della lista: una cartella o un file. */
    private static final class Voce {
        final File file;
        final boolean cartella;
        Voce(File file, boolean cartella) { this.file = file; this.cartella = cartella; }
    }

    /**
     * La riga: un segno colorato, il nome, e per i file la dimensione.
     *
     * Il segno e' disegnato con Java2D invece di usare un'icona: cosi' la
     * dimensione segue la scala dello schermo come tutto il resto. Anche il
     * testo e' disegnato a mano, per tenere nome e dettaglio con due colori
     * diversi sulla stessa riga.
     */
    private static final class Renderer extends DefaultListCellRenderer {

        private String nome = "";
        private String dettaglio = "";
        private boolean cartella;
        private boolean scelta;

        @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i,
                boolean scelto, boolean fuoco) {
            super.getListCellRendererComponent(l, "", i, scelto, fuoco);
            Voce voce = (Voce) v;
            this.nome = voce.file.getName();
            this.cartella = voce.cartella;
            this.scelta = scelto;
            this.dettaglio = voce.cartella ? "cartella" : dimensione(voce.file);
            setText("");
            setOpaque(false);
            return this;
        }

        @Override public void paintComponent(Graphics g) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (scelta) {
                p.setColor(Theme.SUPERFICIE_ALTA);
                p.fillRoundRect(0, 0, getWidth(), getHeight() - 1, Scala.px(8), Scala.px(8));
            }

            int lato = Scala.px(11);
            int y = (getHeight() - lato) / 2;
            if (cartella) {
                p.setColor(Theme.ACCENTO);
                p.fillRoundRect(Scala.px(9), y + Scala.px(2), lato, lato - Scala.px(2), Scala.px(3), Scala.px(3));
                p.fillRoundRect(Scala.px(9), y - Scala.px(1), Scala.px(5), Scala.px(4), Scala.px(2), Scala.px(2));
            } else {
                p.setColor(Theme.TESTO_DEBOLE);
                p.setStroke(new java.awt.BasicStroke(Scala.px(1.2f)));
                p.drawRoundRect(Scala.px(9), y, Scala.px(9), lato, Scala.px(2), Scala.px(2));
                p.drawLine(Scala.px(11), y + Scala.px(4), Scala.px(16), y + Scala.px(4));
            }

            int x = Scala.px(28);
            java.awt.FontMetrics m = p.getFontMetrics(Theme.corpo());
            int base = getHeight() / 2 + (m.getAscent() - m.getDescent()) / 2 - Scala.px(2);
            p.setFont(Theme.corpo());
            p.setColor(scelta ? Theme.ACCENTO : Theme.TESTO);
            p.drawString(nome, x, base);
            p.setFont(Theme.etichetta());
            p.setColor(Theme.TESTO_DEBOLE);
            p.drawString(dettaglio, x, base + Scala.px(12));

            p.dispose();
        }

        @Override public Dimension getPreferredSize() {
            java.awt.FontMetrics m = getFontMetrics(Theme.corpo());
            int larghezza = SwingUtilities.computeStringWidth(m, nome) + Scala.px(70);
            return new Dimension(larghezza, Scala.px(30));
        }
    }

    static String dimensione(File f) {
        long b = f.length();
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format(java.util.Locale.ITALY, "%.1f KB", b / 1024.0);
        return String.format(java.util.Locale.ITALY, "%.1f MB", b / (1024.0 * 1024.0));
    }
}
