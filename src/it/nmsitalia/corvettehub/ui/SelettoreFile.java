package it.nmsitalia.corvettehub.ui;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Una finestra per scegliere un file o una cartella, disegnata come il resto
 * del programma e con la navigazione di Esplora file.
 *
 * PERCHE' NON IL SELETTORE DI SISTEMA
 * -----------------------------------
 * Il {@code JFileChooser} di Swing usa il Look-and-Feel di sistema: sul
 * desktop di Windows esce il pannello grigio con le icone di vent'anni fa e
 * in un programma che ha un suo vestito sembra un'altra applicazione.
 *
 * COME E' FATTO
 * -------------
 * La struttura e' quella di Esplora file, ridotta a quello che serve qui:
 *
 * <pre>
 *   titolo                                            *.json
 *   [&lt;] [&gt;] [^] [rilancia]  Utente &gt; Desktop &gt; Builds   [matita]
 *   +----------+-------------------------------------------------+
 *   | PREFERITI|  Backup                            Cartella      |
 *   |  Desktop |  Esportazioni                      Cartella      |
 *   |  Documenti  Aurora.json                        5,7 KB       |
 *   |  Download|  ...                                            |
 *   | UNITA'   |                                                 |
 *   |  C:\     |                                                 |
 *   +----------+-------------------------------------------------+
 *   4 file, 2 cartelle                        [Annulla]  [Apri]
 * </pre>
 *
 * Il percorso in alto e' fatto di pezzi cliccabili: ogni tacca riporta alla
 * cartella che rappresenta. La matita lo trasforma in un campo di testo, per
 * incollare un indirizzo (Ctrl+L). Indietro e avanti tengono uno storico vero,
 * come il browser.
 *
 * COSA NON E'
 * -----------
 * Non e' un file manager: non copia, non sposta, non cancella, non mostra i
 * file nascosti. Sceglie un file o una cartella e restituisce il percorso.
 *
 * Uso:
 * <pre>
 *   File scelto = SelettoreFile.apri(this, "Scegli il progetto Corvette", cartella, "json");
 *   File cartella = SelettoreFile.cartella(this, "Scegli la cartella di lavoro", cartella);
 *   File nuovo = SelettoreFile.salva(this, "Salva l'elenco", cartella, "deposito.csv");
 * </pre>
 */
public final class SelettoreFile {

    /** Larghezza sotto la quale la barra laterale si nasconde. */
    private static final int SOGLIA_LATERALE = 620;

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
        d.apriFinestra();
        return d.getScelto();
    }

    /** Apre il selettore in modalita' cartella. */
    public static File cartella(Component genitore, String titolo, File partenza) {
        Dialogo d = new Dialogo(genitore, titolo, partenza, true, null);
        d.apriFinestra();
        return d.getScelto();
    }

    /**
     * Apre il selettore per salvare un file nuovo.
     *
     * A differenza di {@link #apri} c'e' una riga in piu' con il nome del file:
     * il percorso in alto resta il percorso, e il nome si scrive a parte.
     */
    public static File salva(Component genitore, String titolo, File partenza, String nomeProposto) {
        Dialogo d = new Dialogo(genitore, titolo, partenza, false, null);
        d.modalitaSalva(nomeProposto);
        d.apriFinestra();
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

    // ------------------------------------------------------------ dialogo

    private static final class Dialogo {

        // --- modello dei dati ---
        private final DefaultListModel<Voce> modello = new DefaultListModel<Voce>();
        private final JList<Voce> lista = new JList<Voce>(modello);
        private final DefaultListModel<Scorciatoia> modelloLaterale = new DefaultListModel<Scorciatoia>();
        private final JList<Scorciatoia> laterale = new JList<Scorciatoia>(modelloLaterale);

        // --- navigazione ---
        private final List<File> storicoIndietro = new ArrayList<File>();
        private final List<File> storicoAvanti = new ArrayList<File>();

        // --- pezzi dell'interfaccia ---
        private final JPanel radice;
        private final Briciole briciole = new Briciole();
        private final JTextField campoPercorso = new JTextField();
        private final JPanel campo = new Campo(new BorderLayout());
        private final JTextField nomeNuovo = new JTextField();
        private final JPanel rigaNome = new JPanel(new BorderLayout(Scala.px(10), 0));
        private final JLabel stato = new JLabel(" ");
        private final JLabel nota = new JLabel();

        private final JButton indietro;
        private final JButton avanti;
        private final JButton su;
        private final JButton aggiorna;
        private final JButton modificaPercorso;
        private final JButton apri = new JButton("Apri");
        private final JButton annulla = new JButton("Annulla");

        // --- stato ---
        private final boolean cartelle;
        private final String estensione;
        private final String titolo;
        private final Component genitore;

        private JDialog finestra;
        private File cartellaCorrente;
        private File scelto;
        private boolean salvaCome;
        private boolean modificandoPercorso;
        private String nomeFile;
        private int indiceSopra = -1;

        Dialogo(Component genitore, String titolo, File partenza, boolean cartelle, String estensione) {
            this.genitore = genitore;
            this.titolo = titolo;
            this.cartelle = cartelle;
            this.estensione = estensione == null ? null : estensione.toLowerCase(Locale.ITALY);

            indietro = pulsante("indietro", "Indietro (Alt+Freccia sinistra)");
            avanti = pulsante("avanti", "Avanti (Alt+Freccia destra)");
            su = pulsante("su", "Cartella superiore (Backspace)");
            aggiorna = pulsante("aggiorna", "Aggiorna (F5)");
            modificaPercorso = pulsante("matita", "Scrivi il percorso (Ctrl+L)");

            radice = new JPanel(new GridBagLayout());
            radice.setBackground(Theme.ABISSO);
            radice.setBorder(Scala.bordo(16, 18, 14, 18));

            componi();
            Theme.rifinisci(radice);
            Theme.primaria(apri);
            Theme.secondaria(annulla);

            // il campo del percorso sta dentro un campo gia' disegnato da noi:
            // non deve portarsi dietro anche il bordo proprio
            campoPercorso.putClientProperty("JComponent.roundRect", null);
            campoPercorso.setBorder(Scala.bordo(0, 9, 0, 9));

            apri.setText(cartelle ? "Scegli cartella" : "Apri");

            collegaEventi();
            naviga(partenza != null ? partenza : new File(System.getProperty("user.home", ".")));
        }

        // ------------------------------------------------------ montaggio

        private void componi() {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;

            c.gridy = 0;
            c.weighty = 0;
            radice.add(costruisciTestata(), c);

            c.gridy = 1;
            c.insets = new Insets(Scala.px(12), 0, 0, 0);
            radice.add(costruisciBarra(), c);

            // la fascia elastica: e' l'unica che si prende lo spazio che avanza
            c.gridy = 2;
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            radice.add(costruisciCorpo(), c);

            c.gridy = 3;
            c.weighty = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            radice.add(costruisciRigaNome(), c);

            c.gridy = 4;
            c.insets = new Insets(Scala.px(12), 0, 0, 0);
            radice.add(costruisciPiede(), c);
        }

        private JComponent costruisciTestata() {
            JPanel p = new JPanel(new BorderLayout(Scala.px(12), 0));
            p.setOpaque(false);

            JLabel t = new JLabel(titolo);
            t.setFont(Theme.titoloCarta());
            t.setForeground(Theme.TESTO);

            nota.setText(cartelle ? "Scegli una cartella"
                    : (salvaCome ? "Scrivi il nome del file"
                    : "Scegli un file" + (estensione == null ? "" : " *." + estensione)));
            nota.setFont(Theme.etichetta());
            nota.setForeground(Theme.TESTO_DEBOLE);

            JPanel destra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            destra.setOpaque(false);
            destra.add(nota);

            p.add(t, BorderLayout.WEST);
            p.add(destra, BorderLayout.EAST);
            return p;
        }

        private JComponent costruisciBarra() {
            JPanel barra = new JPanel(new BorderLayout(Scala.px(8), 0));
            barra.setOpaque(false);

            JPanel frecce = new JPanel(new FlowLayout(FlowLayout.LEFT, Scala.px(4), 0));
            frecce.setOpaque(false);
            frecce.add(indietro);
            frecce.add(avanti);
            frecce.add(su);
            frecce.add(aggiorna);

            briciole.setBorder(Scala.bordo(0, 8, 0, 8));
            briciole.setAscoltatore(new Briciole.Ascoltatore() {
                @Override public void vai(File cartella) { naviga(cartella); }
            });

            campoPercorso.setOpaque(false);
            campoPercorso.setBorder(Scala.bordo(0, 9, 0, 9));
            campoPercorso.setFont(Theme.corpo());
            campoPercorso.setForeground(Theme.TESTO);
            campoPercorso.setCaretColor(Theme.ACCENTO);

            campo.add(briciole, BorderLayout.CENTER);

            barra.add(frecce, BorderLayout.WEST);
            barra.add(campo, BorderLayout.CENTER);
            barra.add(modificaPercorso, BorderLayout.EAST);
            return barra;
        }

        private JComponent costruisciCorpo() {
            return new Corpo(costruisciLaterale(), costruisciElenco());
        }

        private JComponent costruisciLaterale() {
            JPanel p = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D d = (Graphics2D) g.create();
                    d.setColor(Theme.BORDO);
                    d.drawLine(getWidth() - 1, Scala.px(2), getWidth() - 1, getHeight() - Scala.px(2));
                    d.dispose();
                }
            };
            p.setOpaque(false);
            p.setPreferredSize(Scala.dim(168, 100));

            laterale.setFont(Theme.corpo());
            laterale.setOpaque(false);
            laterale.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            laterale.setCellRenderer(new RendererLaterale());
            Theme.altezzaRiga(laterale, 30);
            laterale.addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    int i = laterale.locationToIndex(e.getPoint());
                    if (i < 0) return;
                    Scorciatoia s = modelloLaterale.get(i);
                    laterale.clearSelection();
                    if (s != null && !s.intestazione && s.cartella != null) naviga(s.cartella);
                }
            });

            JScrollPane scroll = new JScrollPane(laterale);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            // il margine sta sul guscio: Theme.rifinisci azzera il bordo degli scorrimenti
            JPanel guscio = new JPanel(new BorderLayout());
            guscio.setOpaque(false);
            guscio.setBorder(Scala.bordo(4, 0, 4, 8));
            guscio.add(scroll, BorderLayout.CENTER);

            p.add(guscio, BorderLayout.CENTER);
            return p;
        }

        private JComponent costruisciElenco() {
            lista.setFont(Theme.corpo());
            lista.setOpaque(false);
            lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            lista.setCellRenderer(new Renderer());
            Theme.altezzaRiga(lista, 26);
            lista.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        Voce v = lista.getSelectedValue();
                        if (v != null) {
                            if (v.cartella) vaiA(v.file);
                            else if (salvaCome) {
                                nomeNuovo.setText(v.file.getName());
                                nomeNuovo.requestFocusInWindow();
                            } else if (!cartelle) conferma();
                        }
                    }
                }
            });
            lista.addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int i = lista.locationToIndex(e.getPoint());
                    if (i >= 0 && !lista.getCellBounds(i, i).contains(e.getPoint())) i = -1;
                    if (i != indiceSopra) { indiceSopra = i; lista.repaint(); }
                }
            });
            lista.addMouseListener(new MouseAdapter() {
                @Override public void mouseExited(MouseEvent e) {
                    if (indiceSopra != -1) { indiceSopra = -1; lista.repaint(); }
                }
            });
            lista.addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) return;
                aggiornaPulsanteApri();
                aggiornaStato();
            });

            JScrollPane scroll = new JScrollPane(lista);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.getVerticalScrollBar().setUnitIncrement(Scala.px(26));

            JPanel guscio = new JPanel(new BorderLayout());
            guscio.setOpaque(false);
            guscio.setBorder(Scala.bordo(6, 5, 6, 5));
            guscio.add(scroll, BorderLayout.CENTER);

            Theme.Carta carta = new Theme.Carta().riempi(Theme.SUPERFICIE).senzaOmbra().raggio(Scala.px(12));
            carta.setLayout(new BorderLayout());
            carta.add(guscio, BorderLayout.CENTER);
            return carta;
        }

        private JComponent costruisciRigaNome() {
            rigaNome.setOpaque(false);
            rigaNome.setBorder(Scala.bordo(12, 0, 0, 0));

            JLabel l = new JLabel("Nome file");
            l.setFont(Theme.etichetta());
            l.setForeground(Theme.TESTO_DEBOLE);
            l.setPreferredSize(Scala.dim(62, 20));

            nomeNuovo.setFont(Theme.corpo());
            nomeNuovo.setForeground(Theme.TESTO);
            nomeNuovo.setBackground(Theme.SUPERFICIE);
            nomeNuovo.setCaretColor(Theme.ACCENTO);

            rigaNome.add(l, BorderLayout.WEST);
            rigaNome.add(nomeNuovo, BorderLayout.CENTER);
            rigaNome.setVisible(false);
            return rigaNome;
        }

        private JComponent costruisciPiede() {
            JPanel p = new JPanel(new BorderLayout(Scala.px(12), 0));
            p.setOpaque(false);

            stato.setFont(Theme.etichetta());
            stato.setForeground(Theme.TESTO_TENUE);

            JPanel destra = new JPanel(new FlowLayout(FlowLayout.RIGHT, Scala.px(8), 0));
            destra.setOpaque(false);
            destra.add(annulla);
            destra.add(apri);

            p.add(stato, BorderLayout.CENTER);
            p.add(destra, BorderLayout.EAST);
            return p;
        }

        // --------------------------------------------------------- eventi

        private void collegaEventi() {
            indietro.addActionListener(e -> indietro());
            avanti.addActionListener(e -> avanti());
            su.addActionListener(e -> saliDiUnLivello());
            aggiorna.addActionListener(e -> {
                if (cartellaCorrente != null) vaiSenzaStorico(cartellaCorrente);
            });
            modificaPercorso.addActionListener(e -> {
                if (modificandoPercorso) disattivaModificaPercorso();
                else attivaModificaPercorso();
            });
            apri.addActionListener(e -> conferma());
            annulla.addActionListener(e -> chiudi());
            nomeNuovo.addActionListener(e -> conferma());

            campoPercorso.addActionListener(e -> {
                String testo = campoPercorso.getText().trim();
                if (testo.isEmpty()) { disattivaModificaPercorso(); return; }
                File scritto = new File(testo).getAbsoluteFile();
                // se il percorso non esiste si resta in modifica: cosi' si corregge
                if (!scritto.isDirectory() && !scritto.isFile()) return;
                disattivaModificaPercorso();
                vaiA(scritto);
            });

            collegaTastiera();
        }

        /** Le scorciatoie valgono per tutta la finestra, ma i campi di testo hanno la precedenza. */
        private void collegaTastiera() {
            InputMap tasti = radice.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap azioni = radice.getActionMap();

            tasti.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "annulla");
            azioni.put("annulla", azione(() -> chiudi()));

            tasti.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "apri");
            azioni.put("apri", azione(() -> {
                Voce v = lista.getSelectedValue();
                if (v != null && v.cartella) vaiA(v.file);
                else conferma();
            }));

            tasti.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "sali");
            azioni.put("sali", azione(() -> saliDiUnLivello()));

            tasti.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "aggiorna");
            azioni.put("aggiorna", azione(() -> {
                if (cartellaCorrente != null) vaiSenzaStorico(cartellaCorrente);
            }));

            tasti.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK), "percorso");
            azioni.put("percorso", azione(() -> attivaModificaPercorso()));

            tasti.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK), "indietro");
            azioni.put("indietro", azione(() -> indietro()));

            tasti.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK), "avanti");
            azioni.put("avanti", azione(() -> avanti()));

            campoPercorso.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "lascia");
            campoPercorso.getActionMap().put("lascia", azione(() -> disattivaModificaPercorso()));
        }

        private static Action azione(final Runnable corpo) {
            return new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { corpo.run(); }
            };
        }

        private JButton pulsante(String glifo, String suggerimento) {
            JButton b = new JButton(new Glifo(glifo, 16));
            b.setToolTipText(suggerimento);
            b.setPreferredSize(Scala.dim(30, 30));
            b.setMinimumSize(Scala.dim(30, 30));
            b.setMaximumSize(Scala.dim(30, 30));
            b.setMargin(new Insets(0, 0, 0, 0));
            b.setFocusable(false);
            b.setForeground(Theme.TESTO_TENUE);
            b.putClientProperty("JButton.buttonType", "toolBarButton");
            return b;
        }

        // ----------------------------------------------------- navigazione

        private void chiudi() {
            if (finestra != null) finestra.dispose();
        }

        /** Va a una cartella e la registra nello storico. */
        private void naviga(File destinazione) {
            File normale = normalizza(destinazione);
            if (normale == null) normale = normalizza(new File(System.getProperty("user.home", ".")));
            if (normale == null) return;
            if (cartellaCorrente != null && !cartellaCorrente.equals(normale)) {
                storicoIndietro.add(cartellaCorrente);
                storicoAvanti.clear();
            }
            vaiSenzaStorico(normale);
        }

        /** Va a una cartella senza toccare lo storico: la usano indietro e avanti. */
        private void vaiSenzaStorico(File cartella) {
            cartellaCorrente = cartella;
            riempi(cartella);
            briciole.mostra(cartella);
            aggiornaLaterale();
            aggiornaFrecce();
            aggiornaStato();
            aggiornaPulsanteApri();
            if (modificandoPercorso) disattivaModificaPercorso();
        }

        /**
         * Accetta una cartella o un file.
         *
         * Se e' un file si apre la cartella che lo contiene e lo si seleziona:
         * e' quello che serve quando si incolla un percorso intero nel campo.
         */
        private void vaiA(File destinazione) {
            if (destinazione == null) return;
            if (destinazione.isDirectory()) {
                naviga(destinazione);
                return;
            }
            if (destinazione.isFile()) {
                naviga(destinazione.getParentFile());
                seleziona(destinazione);
            }
        }

        /**
         * La prima cartella che esiste risalendo il percorso.
         *
         * Serve a non aprire una finestra vuota quando la cartella richiesta
         * non c'e' piu' (un salvataggio spostato, una chiavetta tolta): si
         * mostra il primo antenato che esiste davvero.
         */
        private File normalizza(File f) {
            if (f == null) return null;
            File assoluto = f.getAbsoluteFile();
            while (assoluto != null && !assoluto.isDirectory()) assoluto = assoluto.getParentFile();
            return assoluto;
        }

        private void indietro() {
            if (storicoIndietro.isEmpty()) return;
            File precedente = storicoIndietro.remove(storicoIndietro.size() - 1);
            if (cartellaCorrente != null) storicoAvanti.add(cartellaCorrente);
            vaiSenzaStorico(precedente);
        }

        private void avanti() {
            if (storicoAvanti.isEmpty()) return;
            File successiva = storicoAvanti.remove(storicoAvanti.size() - 1);
            if (cartellaCorrente != null) storicoIndietro.add(cartellaCorrente);
            vaiSenzaStorico(successiva);
        }

        private void saliDiUnLivello() {
            File padre = cartellaCorrente == null ? null : cartellaCorrente.getParentFile();
            if (padre != null) naviga(padre);
        }

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

            indiceSopra = -1;
            if (modello.getSize() > 0) lista.setSelectedIndex(0);
            else lista.clearSelection();
        }

        private boolean accettata(File f) {
            if (estensione == null) return true;
            String n = f.getName().toLowerCase(Locale.ITALY);
            return n.endsWith("." + estensione);
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

        // ------------------------------------------------------ aggiornamenti

        private void aggiornaFrecce() {
            indietro.setEnabled(!storicoIndietro.isEmpty());
            avanti.setEnabled(!storicoAvanti.isEmpty());
            su.setEnabled(cartellaCorrente != null && cartellaCorrente.getParentFile() != null);
            aggiorna.setEnabled(cartellaCorrente != null);
        }

        private void aggiornaPulsanteApri() {
            if (salvaCome) { apri.setEnabled(true); return; }
            if (cartelle) { apri.setEnabled(cartellaCorrente != null); return; }
            Voce v = lista.getSelectedValue();
            apri.setEnabled(v != null && !v.cartella);
        }

        private void aggiornaStato() {
            int cart = 0;
            for (int i = 0; i < modello.getSize(); i++) if (modello.get(i).cartella) cart++;
            int file = modello.getSize() - cart;

            StringBuilder s = new StringBuilder();
            s.append(file).append(" file");
            s.append(", ").append(cart).append(cart == 1 ? " cartella" : " cartelle");

            Voce v = lista.getSelectedValue();
            if (v != null && !v.cartella) {
                s.append("   \u00B7   ").append(v.file.getName())
                 .append("  ").append(dimensione(v.file));
            }
            stato.setText(s.toString());
        }

        /** Le voci della barra laterale: i preferiti che esistono e le unita' del disco. */
        private void aggiornaLaterale() {
            modelloLaterale.clear();
            modelloLaterale.addElement(Scorciatoia.intestazione("Preferiti"));

            aggiungiScorciatoia("desktop", "Desktop", cartellaPersonale("Desktop"));
            aggiungiScorciatoia("documento", "Documenti", cartellaPersonale("Documents"));
            aggiungiScorciatoia("download", "Download", cartellaPersonale("Downloads"));
            aggiungiScorciatoia("casa", "Utente", new File(System.getProperty("user.home", ".")));

            modelloLaterale.addElement(Scorciatoia.intestazione("Unit\u00E0"));
            File[] radici = File.listRoots();
            if (radici != null) {
                for (File radice : radici) {
                    if (radice == null || !radice.isDirectory()) continue;
                    String nome = radice.getPath();
                    if (nome.endsWith("\\")) nome = nome.substring(0, nome.length() - 1);
                    aggiungiScorciatoia("disco", nome, radice);
                }
            }
            laterale.repaint();
        }

        private void aggiungiScorciatoia(String glifo, String nome, File cartella) {
            if (cartella == null || !cartella.isDirectory()) return;
            modelloLaterale.addElement(Scorciatoia.voce(glifo, nome, cartella.getAbsoluteFile()));
        }

        /** La cartella dell'utente tradotta in italiano: Documents -> Documenti. */
        private File cartellaPersonale(String nomeInglese) {
            String casa = System.getProperty("user.home", ".");
            File f = new File(casa, nomeInglese);
            return f.isDirectory() ? f : null;
        }

        // ----------------------------------------------------- campo percorso

        private void attivaModificaPercorso() {
            if (modificandoPercorso) return;
            modificandoPercorso = true;
            campoPercorso.setText(cartellaCorrente == null ? "" : cartellaCorrente.getAbsolutePath());
            campo.removeAll();
            campo.add(campoPercorso, BorderLayout.CENTER);
            campo.revalidate();
            campo.repaint();
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    campoPercorso.requestFocusInWindow();
                    campoPercorso.selectAll();
                }
            });
        }

        private void disattivaModificaPercorso() {
            if (!modificandoPercorso) return;
            modificandoPercorso = false;
            campo.removeAll();
            campo.add(briciole, BorderLayout.CENTER);
            campo.revalidate();
            campo.repaint();
        }

        // ---------------------------------------------------------- conferma

        private void conferma() {
            if (salvaCome) {
                String scritto = nomeScritto();
                if (scritto.isEmpty()) { nomeNuovo.requestFocusInWindow(); return; }
                File f = new File(scritto);
                if (f.isDirectory()) { vaiA(f); return; }
                if (f.exists() && !sovrascrivere(f)) return;
                scelto = f;
            } else if (cartelle) {
                if (cartellaCorrente == null) return;
                Voce v = lista.getSelectedValue();
                scelto = (v != null && v.cartella) ? v.file : cartellaCorrente;
            } else {
                Voce v = lista.getSelectedValue();
                if (v == null || v.cartella) return;
                scelto = v.file;
            }
            chiudi();
        }

        /** Chiede conferma prima di sovrascrivere: e' l'unica scrittura che fa questa finestra. */
        private boolean sovrascrivere(File f) {
            Object[] messaggio = {
                "Esiste gia' un file con questo nome:",
                f.getAbsolutePath(),
                "Vuoi sostituirlo?"
            };
            int scelta = JOptionPane.showConfirmDialog(finestra, messaggio, "Sostituire il file?",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            return scelta == JOptionPane.YES_OPTION;
        }

        /** Il nome scritto a mano, se siamo in modalita' salvataggio. */
        private String nomeScritto() {
            String t = nomeNuovo.getText().trim();
            if (t.isEmpty()) return "";
            File f = new File(t);
            if (f.isAbsolute()) return t;
            return new File(cartellaCorrente, t).getAbsolutePath();
        }

        // ------------------------------------------------------------ finestra

        void apriFinestra() {
            Window proprietario = genitore == null ? null : SwingUtilities.getWindowAncestor(genitore);
            finestra = new JDialog(proprietario, titolo, JDialog.ModalityType.APPLICATION_MODAL);
            finestra.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            finestra.setLayout(new BorderLayout());
            finestra.add(radice, BorderLayout.CENTER);
            finestra.setSize(Scala.dim(820, 560));
            finestra.setMinimumSize(Scala.dim(520, 380));
            if (proprietario != null) finestra.setLocationRelativeTo(proprietario);
            else finestra.setLocationRelativeTo(null);

            if (salvaCome) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() { nomeNuovo.requestFocusInWindow(); nomeNuovo.selectAll(); }
                });
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() { lista.requestFocusInWindow(); }
                });
            }
            finestra.setVisible(true);
        }

        File getScelto() { return scelto; }

        /** Il pannello interno, per le prove automatiche (nessuna finestra aperta). */
        JComponent contenutoDiProva() {
            return radice;
        }

        /** Trasforma il dialogo in "salva con nome": compare la riga del nome. */
        void modalitaSalva(String nomeProposto) {
            salvaCome = true;
            nomeFile = nomeProposto == null ? "" : nomeProposto;
            apri.setText("Salva");
            apri.setEnabled(true);
            nomeNuovo.setText(nomeFile);
            rigaNome.setVisible(true);
            nota.setText("Scrivi il nome del file");
        }

        // -------------------------------------------------------- componenti

        /** Il corpo: barra laterale e elenco, con la laterale che sparisce quando la finestra e' stretta. */
        private final class Corpo extends JPanel {
            private final Component laterale;

            Corpo(Component laterale, Component elenco) {
                super(new BorderLayout(Scala.px(12), 0));
                setOpaque(false);
                this.laterale = laterale;
                add(laterale, BorderLayout.WEST);
                add(elenco, BorderLayout.CENTER);
            }

            @Override public void doLayout() {
                boolean larga = getWidth() <= 0 || getWidth() >= Scala.px(SOGLIA_LATERALE);
                if (laterale.isVisible() != larga) laterale.setVisible(larga);
                super.doLayout();
            }
        }

        /** Lo sfondo arrotondato del campo percorso. */
        private static final class Campo extends JPanel {
            Campo(LayoutManager layout) { super(layout); setOpaque(false); }

            @Override protected void paintComponent(Graphics g) {
                Graphics2D p = (Graphics2D) g.create();
                p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                p.setColor(Theme.SUPERFICIE);
                p.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Scala.px(8), Scala.px(8));
                p.setColor(Theme.BORDO);
                p.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Scala.px(8), Scala.px(8));
                p.dispose();
            }
        }

        /** La riga del file: segno, nome, e a destra la dimensione. */
        private final class Renderer extends JComponent implements ListCellRenderer<Voce> {
            private Voce voce;
            private boolean scelta;
            private int indice;

            @Override public Component getListCellRendererComponent(JList<? extends Voce> l, Voce v,
                    int i, boolean selezionato, boolean fuoco) {
                this.voce = v;
                this.scelta = selezionato;
                this.indice = i;
                return this;
            }

            @Override public Dimension getPreferredSize() {
                return new Dimension(Scala.px(240), Scala.px(26));
            }

            @Override protected void paintComponent(Graphics g) {
                if (voce == null) return;
                Graphics2D p = (Graphics2D) g.create();
                p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                p.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth(), h = getHeight();
                boolean sopra = !scelta && indice == indiceSopra;

                if (scelta) {
                    p.setColor(Theme.SUPERFICIE_ALTA);
                    p.fillRoundRect(0, 0, w - 1, h - 1, Scala.px(7), Scala.px(7));
                } else if (sopra) {
                    p.setColor(Theme.mescola(Theme.SUPERFICIE, Theme.SUPERFICIE_ALTA, 0.65f));
                    p.fillRoundRect(0, 0, w - 1, h - 1, Scala.px(7), Scala.px(7));
                }

                int lato = Scala.px(15);
                int x = Scala.px(9);
                int y = (h - lato) / 2;
                if (voce.cartella) {
                    p.setColor(Theme.ACCENTO);
                    p.fillRoundRect(x, y + Scala.px(3), lato, lato - Scala.px(3), Scala.px(3), Scala.px(3));
                    p.fillRoundRect(x, y - Scala.px(1), Scala.px(7), Scala.px(5), Scala.px(2), Scala.px(2));
                } else {
                    p.setColor(scelta ? Theme.ACCENTO : Theme.TESTO_TENUE);
                    p.setStroke(new BasicStroke(Scala.px(1.2f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int c = Scala.px(4);
                    Path2D foglio = new Path2D.Double();
                    foglio.moveTo(x, y);
                    foglio.lineTo(x + lato - c, y);
                    foglio.lineTo(x + lato, y + c);
                    foglio.lineTo(x + lato, y + lato);
                    foglio.lineTo(x, y + lato);
                    foglio.closePath();
                    p.draw(foglio);
                    p.drawLine(x + lato - c, y, x + lato - c, y + c);
                    p.drawLine(x + lato - c, y + c, x + lato, y + c);
                }

                int testoX = Scala.px(30);
                FontMetrics mc = p.getFontMetrics(Theme.corpo());
                int base = h / 2 + (mc.getAscent() - mc.getDescent()) / 2;

                String dettaglio = voce.cartella ? "Cartella" : dimensione(voce.file);
                p.setFont(Theme.etichetta());
                FontMetrics md = p.getFontMetrics();
                int larghezzaDettaglio = md.stringWidth(dettaglio);
                int destra = w - Scala.px(12) - larghezzaDettaglio;
                p.setColor(scelta ? Theme.mescola(Theme.ACCENTO, Theme.SUPERFICIE_ALTA, 0.35f) : Theme.TESTO_DEBOLE);
                p.drawString(dettaglio, destra, base);

                int spazio = Math.max(Scala.px(24), destra - testoX - Scala.px(14));
                p.setFont(Theme.corpo());
                p.setColor(scelta ? Theme.ACCENTO : Theme.TESTO);
                p.drawString(accorcia(mc, voce.file.getName(), spazio), testoX, base);

                p.dispose();
            }
        }

        /** Una riga della barra laterale: un'intestazione o una scorciatoia. */
        private final class RendererLaterale extends JComponent implements ListCellRenderer<Scorciatoia> {
            private Scorciatoia voce;

            @Override public Component getListCellRendererComponent(JList<? extends Scorciatoia> l,
                    Scorciatoia v, int indice, boolean selezionato, boolean fuoco) {
                this.voce = v;
                return this;
            }

            @Override public Dimension getPreferredSize() {
                return new Dimension(Scala.px(150), Scala.px(30));
            }

            @Override protected void paintComponent(Graphics g) {
                if (voce == null) return;
                Graphics2D p = (Graphics2D) g.create();
                p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                p.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth(), h = getHeight();

                if (voce.intestazione) {
                    p.setFont(Theme.etichetta());
                    p.setColor(Theme.TESTO_DEBOLE);
                    FontMetrics m = p.getFontMetrics();
                    String t = voce.nome.toUpperCase(Locale.ITALY);
                    p.drawString(t, Scala.px(9), h / 2 + (m.getAscent() - m.getDescent()) / 2 + Scala.px(1));
                    p.dispose();
                    return;
                }

                boolean attivo = voce.cartella != null && voce.cartella.equals(cartellaCorrente);
                if (attivo) {
                    p.setColor(Theme.SUPERFICIE_ALTA);
                    p.fillRoundRect(0, Scala.px(1), w - 1, h - Scala.px(2), Scala.px(7), Scala.px(7));
                    p.setColor(Theme.mescola(Theme.ACCENTO, Theme.SUPERFICIE_ALTA, 0.45f));
                    p.drawRoundRect(0, Scala.px(1), w - 1, h - Scala.px(2), Scala.px(7), Scala.px(7));
                }

                Color colore = attivo ? Theme.ACCENTO : Theme.TESTO_TENUE;
                Glifo.disegna(p, voce.glifo, Scala.px(10), (h - Scala.px(14)) / 2.0, Scala.px(14), colore);

                p.setFont(Theme.corpo());
                p.setColor(attivo ? Theme.ACCENTO : Theme.TESTO_TENUE);
                FontMetrics m = p.getFontMetrics();
                int base = h / 2 + (m.getAscent() - m.getDescent()) / 2;
                p.drawString(accorcia(m, voce.nome, w - Scala.px(36)), Scala.px(32), base);

                p.dispose();
            }
        }
    }

    // ------------------------------------------------------------ dati

    /** Una voce dell'elenco: una cartella o un file. */
    private static final class Voce {
        final File file;
        final boolean cartella;
        Voce(File file, boolean cartella) { this.file = file; this.cartella = cartella; }
    }

    /** Una voce della barra laterale: intestazione di sezione o cartella raggiungibile. */
    private static final class Scorciatoia {
        final String glifo;
        final String nome;
        final File cartella;
        final boolean intestazione;

        private Scorciatoia(String glifo, String nome, File cartella, boolean intestazione) {
            this.glifo = glifo;
            this.nome = nome;
            this.cartella = cartella;
            this.intestazione = intestazione;
        }

        static Scorciatoia intestazione(String nome) {
            return new Scorciatoia(null, nome, null, true);
        }

        static Scorciatoia voce(String glifo, String nome, File cartella) {
            return new Scorciatoia(glifo, nome, cartella, false);
        }
    }

    // ------------------------------------------------------- percorso a briciole

    /**
     * Il percorso disegnato a pezzi, come la barra degli indirizzi di Esplora file.
     *
     * Non usa componenti figli: i pezzi si disegnano a mano e i rettangoli
     * servono solo a capire dove si e' cliccato. Cosi' non c'e' nessun giro di
     * validazione da governare quando la finestra si stringe.
     */
    private static final class Briciole extends JComponent {

        interface Ascoltatore { void vai(File cartella); }

        private final List<File> tappe = new ArrayList<File>();
        private final List<String> nomi = new ArrayList<String>();
        private Rectangle[] zone = new Rectangle[0];
        private Rectangle zonaPuntini;
        private boolean conPuntini;
        private int sopra = -1;
        private Ascoltatore ascoltatore;

        Briciole() {
            setOpaque(false);
            setFont(Theme.corpo());
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    int i = indiceSotto(e.getPoint());
                    if (i >= 0 && ascoltatore != null) ascoltatore.vai(tappe.get(i));
                }
                @Override public void mouseExited(MouseEvent e) {
                    if (sopra != -1) { sopra = -1; repaint(); }
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int i = indiceSotto(e.getPoint());
                    if (i != sopra) { sopra = i; repaint(); }
                }
            });
        }

        void setAscoltatore(Ascoltatore a) { this.ascoltatore = a; }

        void mostra(File cartella) {
            tappe.clear();
            nomi.clear();
            List<File> catena = new ArrayList<File>();
            for (File f = cartella; f != null; f = f.getParentFile()) catena.add(f);
            Collections.reverse(catena);
            for (File f : catena) {
                tappe.add(f);
                nomi.add(nomeTappa(f));
            }
            sopra = -1;
            revalidate();
            repaint();
        }

        private static String nomeTappa(File f) {
            String nome = f.getName();
            if (nome == null || nome.isEmpty()) return f.getPath();
            return nome;
        }

        private int indiceSotto(Point punto) {
            disponi();
            for (int i = 0; i < zone.length; i++) {
                if (zone[i] != null && zone[i].contains(punto)) return i;
            }
            return -1;
        }

        /**
         * Calcola dove cadono i pezzi.
         *
         * Se non ci stanno tutti si tengono gli ultimi (quelli che contano) e i
         * primi si sostituiscono con i puntini di sospensione.
         */
        private void disponi() {
            int n = nomi.size();
            zone = new Rectangle[n];
            conPuntini = false;
            if (n == 0) return;

            Insets in = getInsets();
            FontMetrics m = getFontMetrics(Theme.corpo());
            int larghezza = Math.max(getWidth(), Scala.px(60)) - in.left - in.right - Scala.px(4);
            int altezza = Math.max(getHeight() - in.top - in.bottom, Scala.px(18));
            int gap = Scala.px(13);
            int pad = Scala.px(7);

            int[] w = new int[n];
            for (int i = 0; i < n; i++) w[i] = m.stringWidth(nomi.get(i)) + pad * 2;

            int somma = 0;
            int inizio = n;
            for (int i = n - 1; i >= 0; i--) {
                int costo = w[i] + (i < n - 1 ? gap : 0);
                if (somma + costo > larghezza && i < n - 1) break;
                somma += costo;
                inizio = i;
            }

            if (inizio > 0) {
                int larghezzaPuntini = m.stringWidth("\u2026") + pad * 2 + gap;
                while (inizio < n - 1 && somma + larghezzaPuntini > larghezza) {
                    somma -= w[inizio] + gap;
                    inizio++;
                }
                conPuntini = true;
            }

            int x = in.left;
            if (conPuntini) {
                int wp = m.stringWidth("\u2026") + pad * 2;
                zonaPuntini = new Rectangle(x, in.top, wp, altezza);
                x += wp + gap;
            }
            for (int i = inizio; i < n; i++) {
                zone[i] = new Rectangle(x, in.top, w[i], altezza);
                x += w[i];
                if (i < n - 1) x += gap;
            }
        }

        @Override protected void paintComponent(Graphics g) {
            disponi();
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            p.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            p.setFont(Theme.corpo());
            FontMetrics m = p.getFontMetrics();

            if (conPuntini && zonaPuntini != null) {
                disegnaPezzo(p, m, "\u2026", zonaPuntini, false, false);
                disegnaSeparatore(p, zonaPuntini.x + zonaPuntini.width + Scala.px(2), zonaPuntini.y, zonaPuntini.height);
            }

            for (int i = 0; i < nomi.size(); i++) {
                Rectangle r = zone[i];
                if (r == null) continue;
                boolean ultimo = i == nomi.size() - 1;
                disegnaPezzo(p, m, nomi.get(i), r, ultimo, i == sopra);
                if (!ultimo) disegnaSeparatore(p, r.x + r.width + Scala.px(2), r.y, r.height);
            }
            p.dispose();
        }

        private void disegnaPezzo(Graphics2D p, FontMetrics m, String testo, Rectangle r,
                                  boolean ultimo, boolean acceso) {
            if (acceso) {
                p.setColor(Theme.SUPERFICIE_ALTA);
                p.fillRoundRect(r.x, r.y + Scala.px(2), r.width, r.height - Scala.px(4), Scala.px(6), Scala.px(6));
            }
            p.setColor(acceso ? Theme.ACCENTO : (ultimo ? Theme.TESTO : Theme.TESTO_TENUE));
            int base = r.y + r.height / 2 + (m.getAscent() - m.getDescent()) / 2;
            p.drawString(accorcia(m, testo, r.width - Scala.px(10)), r.x + Scala.px(5), base);
        }

        private void disegnaSeparatore(Graphics2D p, int x, int y, int altezza) {
            p.setColor(Theme.TESTO_DEBOLE);
            p.setStroke(new BasicStroke(Scala.px(1.2f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = x + Scala.px(4);
            int cy = y + altezza / 2;
            p.drawLine(cx - Scala.px(2), cy - Scala.px(3), cx + Scala.px(2), cy);
            p.drawLine(cx + Scala.px(2), cy, cx - Scala.px(2), cy + Scala.px(3));
        }
    }

    // ------------------------------------------------------------- icone

    /**
     * Le icone della finestra, disegnate su una griglia di 16 punti.
     *
     * Come i {@link Segno} del resto del programma: nessuna immagine da
     * caricare, e la dimensione segue la scala dello schermo.
     */
    private static final class Glifo implements Icon {

        private final String nome;
        private final int lato;

        Glifo(String nome, int lato) { this.nome = nome; this.lato = lato; }

        public int getIconWidth() { return Scala.px(lato); }
        public int getIconHeight() { return Scala.px(lato); }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Color colore = !c.isEnabled() ? Theme.TESTO_DEBOLE
                    : (c.getForeground() == null ? Theme.TESTO_TENUE : c.getForeground());
            disegna((Graphics2D) g, nome, x, y, Scala.px(lato), colore);
        }

        static void disegna(Graphics2D g, String nome, double x, double y, double lato, Color colore) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            p.translate(x, y);
            double s = lato / 16.0;
            p.scale(s, s);
            p.setColor(colore);
            p.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            tratto(p, nome);
            p.dispose();
        }

        /**
         * Traccia il glifo nella griglia di 16 punti.
         *
         * Le forme sono tutte {@code Shape} in virgola mobile: le linee di
         * {@code Graphics} accettano solo numeri interi e sul reticolo da 16
         * punti servono i mezzi punti per non far sembrare storto il disegno.
         */
        private static void tratto(Graphics2D p, String nome) {
            if ("indietro".equals(nome)) {
                linea(p, 10, 3, 4, 8);
                linea(p, 4, 8, 10, 13);
                return;
            }
            if ("avanti".equals(nome)) {
                linea(p, 6, 3, 12, 8);
                linea(p, 12, 8, 6, 13);
                return;
            }
            if ("su".equals(nome)) {
                linea(p, 8, 13, 8, 4);
                linea(p, 4, 8, 8, 4);
                linea(p, 12, 8, 8, 4);
                return;
            }
            if ("aggiorna".equals(nome)) {
                p.draw(new Arc2D.Double(3, 3, 10, 10, 45, 280, Arc2D.OPEN));
                Path2D punta = new Path2D.Double();
                punta.moveTo(12.6, 1.8);
                punta.lineTo(13.4, 6.2);
                punta.lineTo(9.2, 5.2);
                punta.closePath();
                p.fill(punta);
                return;
            }
            if ("matita".equals(nome)) {
                Path2D m = new Path2D.Double();
                m.moveTo(3.4, 12.6);
                m.lineTo(11.2, 4.8);
                m.lineTo(13.4, 7.0);
                m.lineTo(5.6, 14.8);
                m.lineTo(2.4, 15.6);
                m.closePath();
                p.draw(m);
                return;
            }
            if ("desktop".equals(nome)) {
                p.draw(new RoundRectangle2D.Double(1.5, 2.5, 13, 9, 2, 2));
                linea(p, 6, 14, 10, 14);
                linea(p, 8, 11.5, 8, 14);
                return;
            }
            if ("documento".equals(nome)) {
                Path2D f = new Path2D.Double();
                f.moveTo(3, 1.5);
                f.lineTo(9.5, 1.5);
                f.lineTo(13, 5);
                f.lineTo(13, 14.5);
                f.lineTo(3, 14.5);
                f.closePath();
                p.draw(f);
                linea(p, 9.5, 1.5, 9.5, 5);
                linea(p, 9.5, 5, 13, 5);
                return;
            }
            if ("download".equals(nome)) {
                linea(p, 8, 2, 8, 10);
                linea(p, 4.5, 6.5, 8, 10);
                linea(p, 11.5, 6.5, 8, 10);
                linea(p, 3, 12, 3, 14);
                linea(p, 3, 14, 13, 14);
                linea(p, 13, 14, 13, 12);
                return;
            }
            if ("casa".equals(nome)) {
                linea(p, 1.5, 8, 8, 2);
                linea(p, 8, 2, 14.5, 8);
                linea(p, 3.5, 7.5, 3.5, 14.5);
                linea(p, 12.5, 7.5, 12.5, 14.5);
                linea(p, 3.5, 14.5, 12.5, 14.5);
                return;
            }
            if ("disco".equals(nome)) {
                p.draw(new RoundRectangle2D.Double(1.5, 3.5, 13, 9, 3, 3));
                linea(p, 4.5, 10, 11.5, 10);
                p.fill(new Ellipse2D.Double(11.2, 5.4, 1.7, 1.7));
                return;
            }
            // segno generico: una cartella
            p.fill(new RoundRectangle2D.Double(1.5, 4.5, 13, 9, 2, 2));
            p.fill(new RoundRectangle2D.Double(1.5, 2.5, 6.5, 4, 1.5, 1.5));
        }

        private static void linea(Graphics2D p, double x1, double y1, double x2, double y2) {
            p.draw(new Line2D.Double(x1, y1, x2, y2));
        }
    }

    // ------------------------------------------------------------ utilita'

    /** Accorcia un testo con i puntini, se non ci sta nella larghezza data. */
    private static String accorcia(FontMetrics m, String testo, int larghezza) {
        if (testo == null) return "";
        if (larghezza <= 0 || m.stringWidth(testo) <= larghezza) return testo;
        String puntini = "\u2026";
        int i = testo.length();
        while (i > 0 && m.stringWidth(testo.substring(0, i)) + m.stringWidth(puntini) > larghezza) i--;
        return testo.substring(0, Math.max(0, i)) + puntini;
    }

    static String dimensione(File f) {
        long b = f.length();
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format(Locale.ITALY, "%.1f KB", b / 1024.0);
        return String.format(Locale.ITALY, "%.1f MB", b / (1024.0 * 1024.0));
    }
}
