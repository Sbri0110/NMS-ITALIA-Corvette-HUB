package it.nmsitalia.corvettehub.ui;

import it.nmsitalia.corvettehub.domain.CatalogoParti;
import it.nmsitalia.corvettehub.domain.LayoutInventario;
import it.nmsitalia.corvettehub.domain.LettoreInventari;

import javax.swing.Icon;
import javax.swing.JPanel;
import java.awt.Window;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 * Una griglia di moduli, come quella del gioco.
 *
 * Perche' non un FlowLayout: FlowLayout va a capo in base alla larghezza del
 * contenitore, ma dentro uno JScrollPane la larghezza disponibile non arriva al
 * layout, che quindi dispone tutto su una riga sola e i riquadri escono fuori
 * dalla vista. Il numero di colonne rimane quello del salvataggio. Il lato
 * delle celle si adatta alla larghezza disponibile senza cambiare le
 * coordinate degli oggetti; ogni casella rimane quadrata.
 *
 * Le caselle vuote si disegnano: sono gli slot liberi, e nel gioco si vedono.
 *
 * TRASCINAMENTO (modalita' modificabile)
 * --------------------------------------
 * Il trascinamento NON usa il meccanismo di Swing (TransferHandler): le caselle
 * contengono delle etichette che intercettano gli eventi del mouse, e il
 * rilascio non arrivava mai a destinazione. La griglia invece riceve tutti gli
 * eventi e cerca la casella nei suoi bounds effettivi. Gli spazi fra le
 * caselle e lo spazio libero attorno alla griglia non sono destinazioni.
 */
public final class GrigliaModuli extends JPanel {

    /**
     * Il lato massimo di una casella, in pixel gia' scalati.
     * E' un metodo e non una costante perche' deve seguire la scala dello
     * schermo: su un monitor 4K la casella e' grande il doppio.
     */
    public static int lato() {
        return Scala.px(44);
    }

    /** Il lato dell'icona dentro la casella. */
    public static int icona() {
        return Scala.px(28);
    }

    /** Il lato dell'icona che segue il puntatore mentre si trascina. */
    private static int fantasma() {
        return Scala.px(40);
    }

    private static int minimo() {
        return Math.max(1, Scala.px(28));
    }

    private static int spazio() {
        return Math.max(1, Scala.px(3));
    }

    /** Avvisato quando l'utente trascina un oggetto da una cella a un'altra. */
    public interface AscoltatoreSpostamento {
        void spostato(int daX, int daY, int aX, int aY);
    }

    private final List<Cella> celle = new ArrayList<Cella>();
    private int colonne = 1;
    private int righe = 1;
    private Cella origine;
    private Cella bersaglio;
    private Cella sopra;
    private LayoutInventario layout;
    private AscoltatoreSpostamento ascoltatore;
    private JWindow fantasma;

    /**
     * @param colonne quante caselle per riga
     * @param righe   quante righe in tutto (le eccedenti restano vuote)
     * @param voci    i moduli, nell'ordine in cui vanno riempiti
     */
    public GrigliaModuli(int colonne, int righe, List<LettoreInventari.Voce> voci) {
        if (colonne < 1) {
            colonne = 1;
        }
        int totale = Math.max(righe * colonne, voci.size());
        int righeEffettive = (totale + colonne - 1) / colonne;

        this.colonne = colonne;
        this.righe = righeEffettive;
        preparaLayout();

        for (int i = 0; i < righeEffettive * colonne; i++) {
            Cella c = i < voci.size() ? new Cella(voci.get(i)) : new Cella();
            celle.add(c);
            add(c);
        }
    }

    /**
     * Griglia modificabile: le caselle si possono trascinare fra loro.
     *
     * Usa la disposizione letta dal salvataggio, quindi le celle bloccate dal
     * gioco (quelle fuori da ValidSlotIndices) non accettano niente.
     */
    public GrigliaModuli(LayoutInventario layout, final AscoltatoreSpostamento ascoltatore) {
        this.colonne = layout.getLarghezza();
        this.righe = layout.getAltezza();
        this.layout = layout;
        this.ascoltatore = ascoltatore;

        preparaLayout();

        for (int y = 0; y < righe; y++) {
            for (int x = 0; x < colonne; x++) {
                Cella c = new Cella(x, y, layout, this);
                celle.add(c);
                add(c);
            }
        }
    }

    private void preparaLayout() {
        setOpaque(false);
        setLayout(null);
        setAlignmentX(LEFT_ALIGNMENT);
        addComponentListener(new ComponentAdapter() {
            private int ultimaLarghezza = -1;

            @Override
            public void componentResized(ComponentEvent e) {
                if (getWidth() == ultimaLarghezza) return;
                ultimaLarghezza = getWidth();
                // La larghezza nuova puo' cambiare il preferred height. Il
                // padre ricalcola cosi' anche lo spazio verticale e lo scroll.
                revalidate();
                if (getParent() != null) getParent().revalidate();
            }
        });
    }

    /** Misura naturale compatta; l'altezza segue il lato assegnato alle celle. */
    @Override
    public Dimension getPreferredSize() {
        Insets bordi = getInsets();
        int naturale = colonne * lato() + Math.max(0, colonne - 1) * spazio();
        int disponibile = naturale;
        java.awt.Container padre = getParent();
        if (padre != null && padre.getWidth() > 0) {
            Insets margini = padre.getInsets();
            disponibile = padre.getWidth() - margini.left - margini.right;
            if (padre.getLayout() instanceof FlowLayout) {
                disponibile -= 2 * ((FlowLayout) padre.getLayout()).getHgap();
            }
            disponibile -= bordi.left + bordi.right;
        } else if (getWidth() > 0) {
            disponibile = getWidth() - bordi.left - bordi.right;
        }
        return dimensione(latoPer(disponibile));
    }

    @Override
    public Dimension getMinimumSize() {
        return dimensione(minimo());
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    private Dimension dimensione(int cella) {
        Insets bordi = getInsets();
        return new Dimension(bordi.left + bordi.right
                + colonne * cella + Math.max(0, colonne - 1) * spazio(),
                bordi.top + bordi.bottom
                + righe * cella + Math.max(0, righe - 1) * spazio());
    }

    private int latoPer(int disponibile) {
        int spazioCelle = disponibile - Math.max(0, colonne - 1) * spazio();
        return Math.max(minimo(), Math.min(lato(), spazioCelle / Math.max(1, colonne)));
    }

    /** GridLayout dilaterebbe i riquadri in rettangoli. Qui rimangono quadrati. */
    @Override
    public void doLayout() {
        Insets bordi = getInsets();
        int cella = latoPer(getWidth() - bordi.left - bordi.right);
        for (int i = 0; i < celle.size(); i++) {
            Cella c = celle.get(i);
            c.setBounds(bordi.left + (i % colonne) * (cella + spazio()),
                    bordi.top + (i / colonne) * (cella + spazio()), cella, cella);
        }
    }

    // ------------------------------------------------------- trascinamento

    /**
     * La casella ha ricevuto un clic: diventa l'origine del trascinamento.
     *
     * La gestione sta QUI, sulle caselle, non sulla griglia: le caselle hanno
     * un tooltip, e per via del tooltip Swing registra su di esse un listener
     * del mouse. Siccome gli eventi vanno al componente piu' profondo che ha
     * un listener, la griglia non li riceverebbe mai. Le caselle invece li
     * ricevono di sicuro, come dimostra il fatto che i tooltip si vedono.
     */
    void premutoSu(Cella c) {
        if (!isEnabled()) return;
        if (c == null || c.id == null) {
            origine = null;
            return;
        }
        origine = c;
        c.origine = true;
        mostraFantasma(c);
        repaint();
    }

    /**
     * Mostra l'icona che segue il puntatore.
     *
     * E' una finestrella senza bordi, tenuta sopra tutto: se la disegnassi
     * dentro la griglia verrebbe tagliata dai bordi della griglia stessa
     * quando il puntatore esce.
     */
    private void mostraFantasma(Cella c) {
        nascondiFantasma();
        if (c == null || c.icona == null) {
            return;
        }
        try {
            Window padre = SwingUtilities.getWindowAncestor(this);
            fantasma = padre == null ? new JWindow() : new JWindow(padre);
            fantasma.setBackground(new Color(0, 0, 0, 0));
            Fantasma f = new Fantasma(c.icona,
                    Icone.coloreCategoria(CatalogoParti.categoria(c.id)));
            f.setSize(fantasma(), fantasma());
            fantasma.getContentPane().add(f);
            fantasma.setSize(fantasma(), fantasma());
            fantasma.setAlwaysOnTop(true);
            fantasma.setVisible(true);
        } catch (Throwable t) {
            // senza il fantasma il trascinamento funziona lo stesso
            fantasma = null;
        }
    }

    /** Sposta l'icona che segue il puntatore. */
    private void muoviFantasma(java.awt.Point puntoSullaGriglia) {
        if (fantasma == null || puntoSullaGriglia == null) {
            return;
        }
        try {
            java.awt.Point schermo = new java.awt.Point(puntoSullaGriglia);
            SwingUtilities.convertPointToScreen(schermo, this);
            fantasma.setLocation(schermo.x - fantasma() / 2, schermo.y - fantasma() / 2);
        } catch (Throwable t) {
            // se non si puo' spostare, si nasconde
            nascondiFantasma();
        }
    }

    private void nascondiFantasma() {
        if (fantasma == null) {
            return;
        }
        try {
            fantasma.setVisible(false);
            fantasma.dispose();
        } catch (Throwable ignored) {
            // nulla da fare
        }
        fantasma = null;
    }

    /** Il rilascio, con la posizione riportata alle coordinate della griglia. */
    void rilasciatoA(java.awt.Point p) {
        if (!isEnabled()) { pulisci(); return; }
        Cella a = p == null ? null : cellaA(p.x, p.y);
        Cella da = origine;
        pulisci();
        if (da == null || a == null || a == da || layout == null) {
            return;
        }
        if (!layout.valida(a.cellaX, a.cellaY)) {
            return;
        }
        ascoltatore.spostato(da.cellaX, da.cellaY, a.cellaX, a.cellaY);
    }

    /** Il trascinamento, con la posizione riportata alle coordinate della griglia. */
    void trascinatoA(java.awt.Point p) {
        if (!isEnabled()) { pulisci(); return; }
        if (origine == null || p == null) {
            return;
        }
        muoviFantasma(p);
        trascinatoSu(cellaA(p.x, p.y));
    }

    /** Il puntatore e' passato sopra una casella. */
    void sopraSu(Cella c) {
        if (sopra == c) {
            return;
        }
        if (sopra != null) {
            sopra.sopra = false;
        }
        sopra = c;
        if (sopra != null) {
            sopra.sopra = true;
        }
        repaint();
    }

    /** Il puntatore e' passato sopra una casella mentre si trascina. */
    void trascinatoSu(Cella c) {
        if (bersaglio == c) {
            return;
        }
        if (bersaglio != null) {
            bersaglio.bersaglio = false;
        }
        bersaglio = c;
        if (bersaglio != null) {
            bersaglio.bersaglio = true;
        }
        repaint();
    }

    private void pulisci() {
        nascondiFantasma();
        for (int i = 0; i < celle.size(); i++) {
            celle.get(i).origine = false;
            celle.get(i).bersaglio = false;
            celle.get(i).sopra = false;
        }
        origine = null;
        bersaglio = null;
        sopra = null;
        repaint();
    }

    /**
     * La casella sotto il puntatore.
     *
     * I bounds sono quelli assegnati dal layout: niente arrotondamenti delle
     * coordinate verso una casella vicina, nemmeno sui bordi o nei gap.
     */
    private Cella cellaA(int px, int py) {
        if (getWidth() <= 0 || getHeight() <= 0 || colonne <= 0 || righe <= 0) {
            return null;
        }
        if (px < 0 || py < 0 || px >= getWidth() || py >= getHeight()) {
            return null;
        }
        for (int i = 0; i < celle.size(); i++) {
            Cella c = celle.get(i);
            if (c.getBounds().contains(px, py)) return c;
        }
        return null;
    }

    // ------------------------------------------------------------- casella

    /**
     * Una casella della griglia.
     *
     * Tre modi di usarla:
     *   - con un modulo dentro (deposito, tecnologie, inventario in lettura);
     *   - vuota, per le celle libere;
     *   - modificabile, quando si trascinano gli oggetti da una cella all'altra.
     */
    static final class Cella extends JPanel {

        final String id;
        private final String categoria;
        private final int quantita;
        final int cellaX;
        final int cellaY;
        private final boolean modificabile;
        private final boolean utilizzabile;
        private final GrigliaModuli griglia;
        private final Icon icona;
        /**
         * Se mostrare la quantita' sotto l'icona.
         *
         * Per le TECNOLOGIE no: una tecnologia installata non ha una quantita',
         * il numero nel salvataggio e' la carica (100 su 100). Nel gioco sotto
         * una tecnologia non c'e' scritto "x100".
         */
        private final boolean mostraQuantita;

        private boolean sopra;
        boolean origine;
        boolean bersaglio;

        /** Casella vuota. */
        Cella() {
            this(null, null, 0, -1, -1, null, null);
        }

        /** Casella con un modulo, in sola lettura. */
        Cella(LettoreInventari.Voce v) {
            this(v == null ? null : v.id, v == null ? null : v.tipo,
                    v == null ? 0 : v.quantita, -1, -1, null, null);
        }

        /** Casella modificabile. */
        Cella(int x, int y, LayoutInventario layout, GrigliaModuli griglia) {
            this(layout.a(x, y) == null ? null : layout.a(x, y).id,
                    layout.a(x, y) == null ? null : layout.a(x, y).tipo,
                    layout.a(x, y) == null ? 0 : layout.a(x, y).quantita,
                    x, y, layout, griglia);
        }

        private Cella(String id, String tipo, int quantita, int x, int y,
                      LayoutInventario layout, GrigliaModuli griglia) {
            this.id = id;
            this.quantita = quantita;
            this.cellaX = x;
            this.cellaY = y;
            this.griglia = griglia;
            this.modificabile = layout != null;
            this.categoria = id == null ? null : CatalogoParti.categoria(id);
            this.utilizzabile = !modificabile || layout.valida(x, y);

            setOpaque(false);
            setPreferredSize(new Dimension(lato(), lato()));
            setMinimumSize(new Dimension(minimo(), minimo()));

            // L'icona e la quantita' NON sono etichette figlie: si disegnano
            // direttamente. Un'etichetta figlia starebbe sopra la casella e
            // intercetterebbe il clic, che invece deve arrivare alla casella.
            this.icona = id == null ? null : Icone.per(id, tipo, icona());
            this.mostraQuantita = !"Technology".equalsIgnoreCase(tipo);

            if (id != null) {
                setToolTipText("<html><b>" + SchedaDeposito.nomeLeggibile(id) + "</b><br>"
                        + categoria + "<br>" + quantita + " pezzi"
                        + (modificabile ? "<br><i>trascina per spostare</i>" : "")
                        + "</html>");
            } else if (utilizzabile) {
                setToolTipText(modificabile ? "Cella libera: puoi lasciarci qualcosa"
                        : "Slot libero");
            } else {
                setToolTipText("Cella non disponibile in questo inventario");
            }

            if (modificabile) {
                // La casella riceve di sicuro gli eventi del mouse: li riceve
                // anche il tooltip, che infatti si vede. La posizione va pero'
                // riportata alla griglia, perche' durante un trascinamento il
                // rilascio arriva sempre alla casella di partenza.
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (griglia != null && SwingUtilities.isLeftMouseButton(e)) {
                            griglia.premutoSu(Cella.this);
                            griglia.muoviFantasma(versoGriglia(e));
                        }
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        if (griglia != null && SwingUtilities.isLeftMouseButton(e)) {
                            griglia.rilasciatoA(versoGriglia(e));
                        }
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        if (griglia != null) {
                            griglia.sopraSu(Cella.this);
                        }
                        if (id != null) {
                            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        }
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        if (griglia != null) {
                            griglia.sopraSu(null);
                        }
                    }
                });
                addMouseMotionListener(new MouseMotionAdapter() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        if (griglia != null) {
                            griglia.trascinatoA(versoGriglia(e));
                        }
                    }
                });
            } else {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        sopra = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        sopra = false;
                        repaint();
                    }
                });
            }
        }

        /** Riporta la posizione del mouse dalle coordinate della casella a quelle della griglia. */
        private java.awt.Point versoGriglia(MouseEvent e) {
            if (griglia == null) {
                return e.getPoint();
            }
            return SwingUtilities.convertPoint(Cella.this, e.getPoint(), griglia);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth() - 1;
                int h = getHeight() - 1;
                if (w <= 0 || h <= 0) return;
                int angolo = Math.max(3, Math.min(Scala.px(6), Math.min(w, h) / 5));

                if (id == null) {
                    g2.setColor(utilizzabile
                            ? Theme.SUPERFICIE : Theme.SFONDO);
                    g2.fillRoundRect(0, 0, w, h, angolo, angolo);
                    if (bersaglio && utilizzabile) {
                        // dove finirebbe l'oggetto se lo lasciassi qui
                        g2.setColor(Theme.OK);
                        g2.setStroke(new BasicStroke(2.0f));
                    } else {
                        g2.setColor(Theme.BORDO);
                        g2.setStroke(new BasicStroke(0.5f));
                    }
                    g2.drawRoundRect(0, 0, w, h, angolo, angolo);
                    return;
                }

                Color c = Icone.coloreCategoria(categoria);
                if (origine) {
                    g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 70));
                } else if (bersaglio) {
                    g2.setColor(Theme.AVVISO);
                } else {
                    g2.setColor(sopra ? Theme.SUPERFICIE_ALTA : Theme.SUPERFICIE);
                }
                g2.fillRoundRect(0, 0, w, h, angolo, angolo);

                if (origine) {
                    g2.setColor(c);
                    g2.setStroke(new BasicStroke(2.0f));
                } else if (bersaglio) {
                    g2.setColor(Theme.AVVISO);
                    g2.setStroke(new BasicStroke(2.0f));
                } else {
                    g2.setColor(sopra ? c : new Color(c.getRed(), c.getGreen(), c.getBlue(), 150));
                    g2.setStroke(new BasicStroke(sopra ? 1.6f : 1.0f));
                }
                g2.drawRoundRect(0, 0, w, h, angolo, angolo);

                // l'icona del gioco, centrata
                if (icona != null) {
                    int misura = Math.min(icona(), Math.max(1,
                            Math.round(Math.min(getWidth(), getHeight()) * 28f / 44f)));
                    Graphics2D immagine = (Graphics2D) g2.create();
                    try {
                        immagine.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        immagine.translate((getWidth() - misura) / 2,
                                (getHeight() - misura) / 2 - (mostraQuantita ? Scala.px(2) : 0));
                        immagine.scale(misura / (double) icona.getIconWidth(),
                                misura / (double) icona.getIconHeight());
                        icona.paintIcon(this, immagine, 0, 0);
                    } finally {
                        immagine.dispose();
                    }
                }

                // la quantita', in basso a destra su una fascia scura: solo per gli
                // oggetti, non per le tecnologie installate
                if (mostraQuantita) {
                    String q = "x" + quantita;
                    int dimensione = Math.max(Scala.px(8), Math.min(Scala.px(10),
                            Math.round(Math.min(getWidth(), getHeight()) / 4.4f)));
                    g2.setFont(Scala.font(Font.BOLD, 10).deriveFont((float) dimensione));
                    java.awt.FontMetrics fm = g2.getFontMetrics();
                    int margine = Math.max(1, Scala.px(2));
                    int disponibile = Math.max(1, w - 2 * margine);
                    if (fm.stringWidth(q) > disponibile) {
                        float adattato = Math.max(Scala.px(6), dimensione
                                * disponibile / (float) fm.stringWidth(q));
                        g2.setFont(g2.getFont().deriveFont(adattato));
                        fm = g2.getFontMetrics();
                    }
                    int lq = fm.stringWidth(q);
                    int xq = Math.max(margine, w - lq - margine);
                    int yq = h - margine;
                    g2.setColor(new Color(0, 0, 0, 150));
                    g2.fillRect(xq - margine, yq - fm.getAscent(),
                            lq + 2 * margine, fm.getHeight() - 1);
                    g2.setColor(Color.WHITE);
                    g2.drawString(q, xq, yq);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * La piccola icona che segue il puntatore mentre si trascina.
     *
     * Vive in una finestrella senza bordi, non dentro la griglia: dentro
     * verrebbe tagliata dai bordi della griglia appena il puntatore ne esce.
     */
    private static final class Fantasma extends JPanel {

        private final Icon icona;
        private final Color colore;

        Fantasma(Icon icona, Color colore) {
            this.icona = icona;
            this.colore = colore;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 1;
            int h = getHeight() - 1;
            g2.setColor(new Color(Theme.SUPERFICIE.getRed(), Theme.SUPERFICIE.getGreen(),
                    Theme.SUPERFICIE.getBlue(), 235));
            g2.fillRoundRect(0, 0, w, h, 12, 12);
            g2.setColor(colore);
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawRoundRect(0, 0, w, h, 12, 12);
            if (icona != null) {
                icona.paintIcon(this, g2, (getWidth() - icona.getIconWidth()) / 2,
                        (getHeight() - icona.getIconHeight()) / 2);
            }
        }
    }
}
