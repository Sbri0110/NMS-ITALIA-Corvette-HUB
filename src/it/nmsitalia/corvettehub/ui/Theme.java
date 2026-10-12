package it.nmsitalia.corvettehub.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Sistema visivo dell'applicazione.
 *
 * PALETTE A STRATI
 * ----------------
 * Non piu' tutto sullo stesso piano: quattro livelli di profondita' che
 * risalgono dal fondo dello spazio (ABISSO) fino agli elementi che l'utente
 * tocca (RILIEVO), con un RILIEVO_ALTO per hover e popup. La gerarchia si
 * legge dall'elevazione, non solo dal colore.
 *
 * ACCENTO CHAMPAGNE
 * -----------------
 * L'oro resta il colore dell'azione: pulsanti primari, indicatore della
 * scheda attiva, numeri delle statistiche. Tutto il resto e' neutro, cosi'
 * l'oro significa "qui".
 *
 * FORME
 * -----
 * Raggi coerenti: 8 per i controlli, 14 per le carte, "pill" (999) per i
 * badge. Le ombre sono disegnate a mano con Java2D perche' FlatLaf non le
 * offre sui pannelli.
 *
 * VINCOLI RISPETTATI
 * ------------------
 * Java 8 (nessuna API nuova), nessuna dipendenza aggiuntiva, scala DPI
 * invariata: si cambiano solo colori, forme, ombre e tipografia.
 */
public final class Theme {

    // ------------------------------------------------------------ palette

    /** Il fondo dello spazio: la pagina. */
    public static final Color ABISSO = new Color(0x070B11);
    /** Le superfici: testate, carte, righe. */
    public static final Color SUPERFICIE = new Color(0x0D1522);
    /** Superficie piu' chiara: elementi sollevati, hover. */
    public static final Color SUPERFICIE_ALTA = new Color(0x16233A);
    /** Il gradino piu' alto: popup, elementi in evidenza. */
    public static final Color RILIEVO = new Color(0x1E2E48);
    /** Il bordo delle superfici. */
    public static final Color BORDO = new Color(0x2F4055);
    /** Bordo piu' marcato, per gli stati attivi. */
    public static final Color BORDO_FORTE = new Color(0x4A5D75);

    /** Testo principale. */
    public static final Color TESTO = new Color(0xF4F7FB);
    /** Testo secondario. */
    public static final Color TESTO_TENUE = new Color(0x8CA0BA);
    /** Testo attenuato, per i dettagli terziari. */
    public static final Color TESTO_DEBOLE = new Color(0x5E6E86);

    /** L'accento: champagne. */
    public static final Color ACCENTO = new Color(0xE8BD78);
    /** Accento chiaro, per hover. */
    public static final Color ACCENTO_CHIARO = new Color(0xF2D79E);
    /** Accento scuro, riempimento delle selezioni. */
    public static final Color ACCENTO_SCURO = new Color(0x2A2416);
    /** Fondo scuro dell'accento, per la selezione delle righe. */
    public static final Color ACCENTO_FONDO = new Color(0x1F1A10);

    public static final Color OK = new Color(0x4FD6A3);
    public static final Color OK_FONDO = new Color(0x12271F);
    public static final Color ERRORE = new Color(0xF08089);
    public static final Color ERRORE_FONDO = new Color(0x2A171A);
    public static final Color AVVISO = new Color(0xE8BD78);
    public static final Color AVVISO_SCURO = new Color(0x2A2416);

    /**
     * Alias storico del fondo pagina.
     *
     * Il vecchio sistema chiamava SFONDO quello che ora e' ABISSO. Resta qui
     * perche' le schede non ancora riviste lo usano: si eliminera' quando
     * saranno state portate tutte sul nuovo sistema.
     */
    public static final Color SFONDO = ABISSO;

    /** Il colore del gradiente della testata: blu notte profondo. */
    private static final Color TESTATA_ALTA = new Color(0x121C2E);
    private static final Color TESTATA_BASSA = new Color(0x0A111C);

    private Theme() { }

    // ------------------------------------------------------- installazione

    public static void installa() {
        // Scala gestisce gia' i pixel: evita una seconda scala FlatLaf.
        System.setProperty("flatlaf.uiScale", "1x");
        FlatDarkLaf.setup();

        UIManager.put("defaultFont", Scala.font(Font.PLAIN, 13));

        // --- finestre e pannelli ---
        UIManager.put("Panel.background", ABISSO);
        UIManager.put("Component.background", SUPERFICIE);
        UIManager.put("Component.foreground", TESTO);
        UIManager.put("Label.foreground", TESTO);
        UIManager.put("Component.borderColor", BORDO);
        UIManager.put("Component.disabledBorderColor", new Color(0x1C2839));
        UIManager.put("Component.focusedBorderColor", ACCENTO);
        UIManager.put("Component.focusColor", ACCENTO);
        UIManager.put("Component.focusWidth", Scala.px(1));
        UIManager.put("Component.innerFocusWidth", Scala.px(1));
        UIManager.put("Component.arc", Scala.px(8));
        UIManager.put("Component.arrowType", "chevron");

        // --- pulsanti: forma pulita, hover morbido ---
        UIManager.put("Button.arc", Scala.px(9));
        UIManager.put("Button.innerFocusWidth", Scala.px(1));
        UIManager.put("Button.borderWidth", 1);
        UIManager.put("Button.margin", new Insets(Scala.px(7), Scala.px(14), Scala.px(7), Scala.px(14)));
        UIManager.put("Button.background", SUPERFICIE_ALTA);
        UIManager.put("Button.foreground", TESTO);
        UIManager.put("Button.hoverBackground", RILIEVO);
        UIManager.put("Button.pressedBackground", new Color(0x22334F));
        UIManager.put("Button.disabledBackground", new Color(0x101827));
        UIManager.put("Button.disabledText", TESTO_DEBOLE);
        UIManager.put("Button.borderColor", BORDO);
        UIManager.put("Button.default.background", ACCENTO);
        UIManager.put("Button.default.foreground", ABISSO);
        UIManager.put("Button.default.hoverBackground", ACCENTO_CHIARO);
        UIManager.put("Button.default.borderColor", ACCENTO);
        UIManager.put("Button.default.focusedBorderColor", ACCENTO_CHIARO);

        // --- campi di testo ---
        UIManager.put("TextComponent.arc", Scala.px(8));
        UIManager.put("TextField.background", SUPERFICIE);
        UIManager.put("TextField.foreground", TESTO);
        UIManager.put("TextField.borderColor", BORDO);
        UIManager.put("TextField.placeholderForeground", TESTO_DEBOLE);
        UIManager.put("TextArea.background", SUPERFICIE);
        UIManager.put("TextArea.foreground", TESTO);
        UIManager.put("TextComponent.selectionBackground", new Color(0x3A4A63));
        UIManager.put("TextComponent.selectionForeground", TESTO);

        // --- tendine ---
        UIManager.put("ComboBox.background", SUPERFICIE);
        UIManager.put("ComboBox.foreground", TESTO);
        UIManager.put("ComboBox.buttonBackground", SUPERFICIE_ALTA);
        UIManager.put("ComboBox.buttonArrowColor", TESTO_TENUE);
        UIManager.put("ComboBox.selectionBackground", SUPERFICIE_ALTA);
        UIManager.put("ComboBox.selectionForeground", TESTO);
        UIManager.put("ComboBox.borderColor", BORDO);

        // --- barre di scorrimento: sottili e arrotondate ---
        UIManager.put("ScrollBar.width", Scala.px(11));
        UIManager.put("ScrollBar.thumbArc", Scala.px(999));
        UIManager.put("ScrollBar.thumbInsets", new Insets(Scala.px(2), Scala.px(2), Scala.px(2), Scala.px(2)));
        UIManager.put("ScrollBar.trackArc", Scala.px(999));
        UIManager.put("ScrollBar.track", ABISSO);
        UIManager.put("ScrollBar.thumb", BORDO);
        UIManager.put("ScrollBar.hoverThumbColor", BORDO_FORTE);
        UIManager.put("ScrollBar.pressedThumbColor", ACCENTO);
        UIManager.put("ScrollBar.showButtons", false);

        // --- liste ---
        UIManager.put("List.background", ABISSO);
        UIManager.put("List.foreground", TESTO);
        UIManager.put("List.selectionBackground", SUPERFICIE_ALTA);
        UIManager.put("List.selectionForeground", TESTO);
        UIManager.put("List.selectionInactiveBackground", SUPERFICIE);
        UIManager.put("List.selectionInactiveForeground", TESTO);

        UIManager.put("ScrollPane.background", ABISSO);
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());

        UIManager.put("SplitPane.background", ABISSO);
        UIManager.put("SplitPane.dividerSize", Scala.px(10));
        UIManager.put("SplitPaneDivider.style", "plain");

        // --- schede: la parte che piu' faceva "vecchio" ---
        UIManager.put("TabbedPane.background", ABISSO);
        UIManager.put("TabbedPane.foreground", TESTO_TENUE);
        UIManager.put("TabbedPane.selectedForeground", TESTO);
        UIManager.put("TabbedPane.selectedBackground", SUPERFICIE_ALTA);
        UIManager.put("TabbedPane.hoverColor", SUPERFICIE);
        UIManager.put("TabbedPane.underlineColor", ACCENTO);
        UIManager.put("TabbedPane.focusColor", ACCENTO);
        UIManager.put("TabbedPane.tabHeight", Scala.px(38));
        UIManager.put("TabbedPane.tabSelectionHeight", Scala.px(3));
        UIManager.put("TabbedPane.contentAreaColor", ABISSO);
        UIManager.put("TabbedPane.contentSeparatorHeight", 0);
        UIManager.put("TabbedPane.showTabSeparators", false);
        UIManager.put("TabbedPane.tabInsets", new Insets(Scala.px(6), Scala.px(14), Scala.px(6), Scala.px(14)));
        UIManager.put("TabbedPane.tabAreaInsets", new Insets(Scala.px(6), Scala.px(8), Scala.px(4), Scala.px(6)));

        // --- menu e suggerimenti ---
        UIManager.put("ToolTip.background", RILIEVO);
        UIManager.put("ToolTip.foreground", TESTO);
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDO),
                BorderFactory.createEmptyBorder(Scala.px(6), Scala.px(9), Scala.px(6), Scala.px(9))));
        UIManager.put("PopupMenu.background", RILIEVO);
        UIManager.put("PopupMenu.foreground", TESTO);
        UIManager.put("PopupMenu.borderColor", BORDO);
        UIManager.put("MenuItem.background", RILIEVO);
        UIManager.put("MenuItem.foreground", TESTO);
        UIManager.put("MenuItem.selectionBackground", SUPERFICIE_ALTA);
        UIManager.put("MenuItem.selectionForeground", ACCENTO);
        UIManager.put("MenuItem.borderPainted", false);

        // --- separatori ---
        UIManager.put("Separator.foreground", BORDO);
        UIManager.put("Separator.background", ABISSO);

        // --- etichette di stato ---
        UIManager.put("OptionPane.background", SUPERFICIE);
        UIManager.put("OptionPane.messageForeground", TESTO);
        UIManager.put("OptionPane.border", BorderFactory.createEmptyBorder(Scala.px(16), Scala.px(18), Scala.px(16), Scala.px(18)));
    }

    // ---------------------------------------------------------- tipografia

    /** Il titolo di schermata, il livello piu' alto. */
    public static Font titolo() { return Scala.font(Font.PLAIN, 19); }
    /** Il titolo di un blocco o di una carta. */
    public static Font titoloCarta() { return Scala.font(Font.PLAIN, 15); }
    /** L'intestazione di una sezione. */
    public static Font sezione() { return Scala.font(Font.PLAIN, 13); }
    /** Il testo normale. */
    public static Font corpo() { return Scala.font(Font.PLAIN, 12); }
    /** Il numero grande delle statistiche. */
    public static Font numero() { return Scala.font(Font.PLAIN, 21); }
    /** L'etichetta minuta, in maiuscoletto. */
    public static Font etichetta() { return Scala.font(Font.PLAIN, 10); }
    /** Il font a spaziatura fissa, per i riquadri di esito e i log. */
    public static Font monospazio() { return Scala.mono(12); }

    /** Il titolo di schermata, centrato verticalmente in modo ottico. */
    public static JLabel titolo(String testo) {
        JLabel l = new JLabel(testo);
        l.setFont(titolo());
        l.setForeground(TESTO);
        return l;
    }

    /** Il titolo di una carta o di una sezione interna. */
    public static JLabel titoloCarta(String testo) {
        JLabel l = new JLabel(testo);
        l.setFont(titoloCarta());
        l.setForeground(TESTO);
        return l;
    }

    /** Una piccola etichetta in maiuscoletto, come "WORKSPACE / 01". */
    public static JLabel maiuscoletto(String testo) {
        JLabel l = new JLabel(testo.toUpperCase(java.util.Locale.ITALY));
        l.setFont(etichetta());
        l.setForeground(ACCENTO);
        return l;
    }

    /** Un'etichetta di sezione, neutra. */
    public static JLabel intestazione(String testo) {
        JLabel l = new JLabel(testo);
        l.setFont(sezione());
        l.setForeground(TESTO_TENUE);
        return l;
    }

    // ------------------------------------------------------------ pulsanti

    /** Il pulsante principale: pieno d'oro, testo scuro. */
    public static void primaria(JButton b) {
        b.setBackground(ACCENTO);
        b.setForeground(ABISSO);
        b.setFont(Scala.font(Font.PLAIN, 13));
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.putClientProperty("JButton.minimumWidth", Scala.px(96));
    }

    /** Il pulsante secondario: superficie con bordo. */
    public static void secondaria(JButton b) {
        b.setBackground(SUPERFICIE_ALTA);
        b.setForeground(TESTO);
        b.putClientProperty("JButton.buttonType", "roundRect");
    }

    /** Un'azione distruttiva: testo rosso, bordo coerente. */
    public static void distruttiva(JButton b) {
        b.setForeground(ERRORE);
        b.putClientProperty("JButton.buttonType", "roundRect");
    }

    // ------------------------------------------------------- rifinitura

    public static void rifinisci(Component c) {
        if (c instanceof JScrollPane) rifinisciScroll((JScrollPane) c);
        if (c instanceof JSplitPane) rifinisciSplit((JSplitPane) c);
        if (c instanceof JTextArea) {
            JTextArea a = (JTextArea) c;
            a.setLineWrap(true);
            a.setWrapStyleWord(true);
        }
        if (c instanceof JButton) rifinisciPulsante((JButton) c);
        if (c instanceof JList) rifinisciLista((JList<?>) c);
        if (c instanceof JComponent) {
            JComponent j = (JComponent) c;
            // le tendine e i campi seguono gli stessi raggi delle carte piccole
            j.putClientProperty("JComponent.roundRect", Boolean.TRUE);
        }
        if (c instanceof Container) {
            for (Component figlio : ((Container) c).getComponents()) rifinisci(figlio);
        }
    }

    private static void rifinisciScroll(JScrollPane s) {
        s.setBorder(BorderFactory.createEmptyBorder());
        s.getViewport().setBackground(ABISSO);
        s.getVerticalScrollBar().setUnitIncrement(Scala.px(28));
        s.getHorizontalScrollBar().setUnitIncrement(Scala.px(28));
        s.getVerticalScrollBar().putClientProperty("JScrollBar.width", Scala.px(11));
        s.getHorizontalScrollBar().putClientProperty("JScrollBar.width", Scala.px(11));
    }

    private static void rifinisciSplit(JSplitPane s) {
        if (s.getClientProperty("divisione.fluida") != null) {
            aggiornaDivisione(s);
        } else {
            if (s.getLeftComponent() != null) s.getLeftComponent().setMinimumSize(new Dimension(0, 0));
            if (s.getRightComponent() != null) s.getRightComponent().setMinimumSize(new Dimension(0, 0));
        }
    }

    private static void rifinisciPulsante(JButton b) {
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        String t = b.getText();
        if (t == null) return;
        if (t.startsWith("Continua") || t.equals("Importa")
                || t.startsWith("Esporta progetto") || t.equals("Rinomina")
                || t.startsWith("Salva le modifiche") || t.equals("Salva")) {
            primaria(b);
        } else if (t.startsWith("Elimina")) {
            distruttiva(b);
        } else if (b.getClientProperty("JButton.buttonType") == null) {
            // le azioni neutre restano superficie: l'oro si guadagna il significato
            secondaria(b);
        }
    }

    private static void rifinisciLista(JList<?> l) {
        l.setSelectionBackground(SUPERFICIE_ALTA);
        l.setSelectionForeground(TESTO);
        // Un'altezza fissa gia' scelta da una schermata non si tocca: le
        // righe-carta hanno bisogno di una misura stabile, e la rifinitura
        // viene richiamata a ogni ridimensionamento.
        if (l.getClientProperty("nms.altezzaFissa") == null) l.setFixedCellHeight(-1);
    }

    /**
     * Dichiara l'altezza fissa di una riga in modo che resista a {@link #rifinisci}.
     *
     * Serve alle liste che disegnano ogni voce come una carta: senza una
     * misura esplicita il renderer verrebbe misurato a zero.
     */
    public static void altezzaRiga(JList<?> lista, int px) {
        lista.putClientProperty("nms.altezzaFissa", Boolean.TRUE);
        lista.setFixedCellHeight(Scala.px(px));
    }

    // ------------------------------------------------------ animazione hover

    /**
     * Un cambio di colore morbido al passaggio del mouse.
     *
     * Si anima solo la tinta di sfondo: nessuna API nuova, un timer da 16 ms
     * che interpola fra due colori. Se il componente non e' opaco non si fa
     * nulla, per non sovrapporsi a chi disegna da solo.
     */
    public static void animaHover(final JComponent c, final Color normale, final Color acceso) {
        c.setBackground(normale);
        c.addMouseListener(new MouseAdapter() {
            private Timer timer;
            @Override public void mouseEntered(MouseEvent e) { avvia(acceso); }
            @Override public void mouseExited(MouseEvent e) { avvia(normale); }

            private void avvia(final Color verso) {
                if (timer != null && timer.isRunning()) timer.stop();
                final Color da = c.getBackground();
                if (da == null || da.equals(verso)) return;
                final int passi = 8;
                final int[] n = {0};
                timer = new Timer(16, null);
                timer.addActionListener(new java.awt.event.ActionListener() {
                    @Override public void actionPerformed(java.awt.event.ActionEvent ev) {
                        n[0]++;
                        float f = Math.min(1f, n[0] / (float) passi);
                        c.setBackground(mescola(da, verso, f));
                        c.repaint();
                        if (n[0] >= passi) timer.stop();
                    }
                });
                timer.start();
            }
        });
    }

    /** Interpola fra due colori. t=0 -> a, t=1 -> b. */
    public static Color mescola(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    // ------------------------------------------------------------ ombre

    /**
     * Un'ombra morbida disegnata a mano.
     *
     * Java2D non ha ombre proprie: si disegnano piu' bordi arrotondati con
     * opacita' decrescente. Costa poco e fa la differenza fra "piatto" e
     * "premium".
     */
    public static void ombra(Graphics2D g, int x, int y, int w, int h, int raggio, float opacita) {
        int strati = Scala.px(6);
        for (int i = strati; i >= 1; i--) {
            float a = opacita * (1f - i / (float) strati) * 0.55f;
            g.setColor(new Color(0, 0, 0, Math.max(0, Math.round(a * 255))));
            int off = i;
            g.fillRoundRect(x - off, y - off + Scala.px(2), w + off * 2, h + off * 2, raggio + off, raggio + off);
        }
    }

    /** Un'ombra ancora piu' marcata, per gli elementi in evidenza. */
    public static void ombraForte(Graphics2D g, int x, int y, int w, int h, int raggio) {
        ombra(g, x, y, w, h, raggio, 0.55f);
    }

    // ------------------------------------------------- superfici condivise

    /**
     * Un'immagine di sfondo per un pannello, con l'ombra gia' disegnata.
     *
     * Serve a non ricalcolare l'ombra a ogni ridisegno: si prepara una volta
     * per ogni misura richiesta.
     */
    static BufferedImage sfondoCarta(int w, int h, int raggio, Color riempimento, Color bordo, boolean ombra) {
        BufferedImage img = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (ombra) ombra(g, Scala.px(4), Scala.px(4), w - Scala.px(8), h - Scala.px(8), raggio, 0.5f);
        g.setColor(riempimento);
        g.fillRoundRect(Scala.px(4), Scala.px(4), w - Scala.px(8), h - Scala.px(8), raggio, raggio);
        if (bordo != null) {
            g.setColor(bordo);
            g.drawRoundRect(Scala.px(4), Scala.px(4), w - Scala.px(8) - 1, h - Scala.px(8) - 1, raggio, raggio);
        }
        g.dispose();
        return img;
    }

    /**
     * Una carta: superficie arrotondata con bordo e ombra morbida.
     *
     * E' il mattone dell'interfaccia rinnovata. Il colore di riempimento, il
     * bordo e l'ombra si possono cambiare con i metodi sotto.
     */
    public static class Carta extends JPanel {
        private Color riempimento = SUPERFICIE;
        private Color bordo = BORDO;
        private boolean conOmbra = true;
        private int raggio = -1;

        public Carta() {
            setOpaque(false);
        }

        public Carta riempi(Color c) { this.riempimento = c; repaint(); return this; }
        public Carta borda(Color c) { this.bordo = c; repaint(); return this; }
        public Carta senzaOmbra() { this.conOmbra = false; repaint(); return this; }
        public Carta conOmbra() { this.conOmbra = true; repaint(); return this; }
        public Carta raggio(int r) { this.raggio = r; repaint(); return this; }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int r = raggio < 0 ? Scala.px(14) : raggio;
            int pad = conOmbra ? Scala.px(6) : 0;
            if (conOmbra) ombra(p, pad, pad, getWidth() - pad * 2, getHeight() - pad * 2, r, 0.5f);
            p.setColor(riempimento);
            p.fillRoundRect(pad, pad, getWidth() - pad * 2 - 1, getHeight() - pad * 2 - 1, r, r);
            p.setColor(bordo);
            p.drawRoundRect(pad, pad, getWidth() - pad * 2 - 1, getHeight() - pad * 2 - 1, r, r);
            p.dispose();
        }
    }

    /**
     * Una carta "in evidenza": bordo d'oro, ombra piu' netta.
     * E' la riga scelta, il pannello attivo.
     */
    public static class CartaAccento extends JPanel {
        private Color riempimento = SUPERFICIE_ALTA;
        private Color bordo = ACCENTO;
        private int raggio = -1;

        public CartaAccento() { setOpaque(false); }
        public CartaAccento riempi(Color c) { this.riempimento = c; repaint(); return this; }
        public CartaAccento borda(Color c) { this.bordo = c; repaint(); return this; }
        public CartaAccento raggio(int r) { this.raggio = r; repaint(); return this; }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int r = raggio < 0 ? Scala.px(14) : raggio;
            int pad = Scala.px(6);
            ombra(p, pad, pad, getWidth() - pad * 2, getHeight() - pad * 2, r, 0.6f);
            p.setColor(riempimento);
            p.fillRoundRect(pad, pad, getWidth() - pad * 2 - 1, getHeight() - pad * 2 - 1, r, r);
            p.setColor(bordo);
            p.setStroke(new BasicStroke(Scala.px(1.5f)));
            p.drawRoundRect(pad, pad, getWidth() - pad * 2 - 1, getHeight() - pad * 2 - 1, r, r);
            p.dispose();
        }
    }

    /**
     * Una pill: un'etichetta compatta con sfondo e bordo.
     * Serve per i badge (Steam, Xbox, numero di slot).
     */
    public static class Pill extends JLabel {
        private Color bordoPill;
        private Color fondoPill;

        public Pill(String testo) { this(testo, TESTO_TENUE, SUPERFICIE_ALTA, BORDO); }

        /** Una pill colorata: il bordo prende il colore del testo, smorzato. */
        public Pill(String testo, Color testoColore, Color fondo) {
            this(testo, testoColore, fondo, mescola(testoColore, fondo, 0.55f));
        }

        public Pill(String testo, Color testoColore, Color fondo, Color bordo) {
            super(testo);
            this.fondoPill = fondo;
            this.bordoPill = bordo;
            setForeground(testoColore);
            setFont(Scala.font(Font.PLAIN, 10));
            setBorder(Scala.bordo(3, 9, 3, 9));
            setOpaque(false);
        }

        public Pill riempi(Color c) { this.fondoPill = c; repaint(); return this; }
        public Pill borda(Color c) { this.bordoPill = c; repaint(); return this; }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            p.setColor(fondoPill);
            p.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            if (bordoPill != null) {
                p.setColor(bordoPill);
                p.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            }
            p.dispose();
            super.paintComponent(g);
        }
    }

    /** Un cerchio con un numero dentro: l'ordine di una tappa o di uno slot. */
    public static class SegnoNumero extends JLabel {
        private Color anello = BORDO;
        private Color fondo = SUPERFICIE_ALTA;
        private Color testo = TESTO_TENUE;

        public SegnoNumero(String n) {
            super(n, SwingConstants.CENTER);
            setFont(Scala.font(Font.PLAIN, 12));
            setForeground(testo);
            setOpaque(false);
        }

        public SegnoNumero attivo() {
            this.anello = ACCENTO; this.fondo = ACCENTO_SCURO; this.testo = ACCENTO;
            setForeground(testo); repaint(); return this;
        }

        public SegnoNumero spento() {
            this.anello = BORDO; this.fondo = SUPERFICIE_ALTA; this.testo = TESTO_TENUE;
            setForeground(testo); repaint(); return this;
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int d = Math.min(getWidth(), getHeight());
            p.setColor(fondo);
            p.fillOval((getWidth() - d) / 2, (getHeight() - d) / 2, d, d);
            p.setColor(anello);
            p.drawOval((getWidth() - d) / 2, (getHeight() - d) / 2, d - 1, d - 1);
            p.dispose();
            super.paintComponent(g);
        }
    }

    /** Una linea sottile di separazione. */
    public static JComponent separatore() {
        JPanel p = new JPanel();
        p.setBackground(BORDO);
        p.setPreferredSize(new Dimension(1, Scala.px(1)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Scala.px(1)));
        return p;
    }

    // -------------------------------------------------------- gestori eventi

    public static void dopoScrittura(Component c) {
        while (c != null && !(c instanceof SchermataCorvette)) c = c.getParent();
        if (c instanceof SchermataCorvette) ((SchermataCorvette) c).ricarica();
    }

    public static void dopoEsportazione(Component c) {
        while (c != null && !(c instanceof SchermataCorvette)) c = c.getParent();
        if (c instanceof SchermataCorvette) ((SchermataCorvette) c).aggiornaLibreria();
    }

    public static void lavora(Component c, boolean occupata) {
        while (c != null && !(c instanceof SchermataCorvette)) c = c.getParent();
        if (c instanceof SchermataCorvette) ((SchermataCorvette) c).setOccupata(occupata);
    }

    /**
     * Aggiorna la fase del velo di attesa.
     *
     * Si chiama dal filo di lavoro per raccontare a che punto si e': la
     * scrittura vera gira fuori dall'EDT, e un'etichetta che cambia e' la
     * differenza fra "sembra bloccato" e "sta lavorando".
     */
    public static void fase(Component c, final String testo) {
        while (c != null && !(c instanceof SchermataCorvette)) c = c.getParent();
        if (c == null) return;
        final SchermataCorvette sc = (SchermataCorvette) c;
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { sc.fase(testo); }
        });
    }

    /**
     * Accende il velo con un titolo e una fase, senza passare da "occupata".
     *
     * Serve ai lavori lunghi che non scrivono sul salvataggio (per esempio
     * l'esportazione di un progetto) e che quindi non devono spegnere tutto.
     */
    public static void lavoraCon(Component c, final String titolo, final String fase) {
        while (c != null && !(c instanceof SchermataCorvette)) c = c.getParent();
        if (c == null) return;
        final SchermataCorvette sc = (SchermataCorvette) c;
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { sc.lavoraCon(titolo, fase); }
        });
    }

    /** Spegne il velo di attesa. */
    public static void finito(Component c) {
        while (c != null && !(c instanceof SchermataCorvette)) c = c.getParent();
        if (c == null) return;
        final SchermataCorvette sc = (SchermataCorvette) c;
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { sc.veloOff(); }
        });
    }

    // ------------------------------------------------------------ layout

    /** Contenuto verticale senza larghezze fisse o allungamento delle carte. */
    public static JPanel colonnaFluida() { return new Colonna(); }

    public static JScrollPane scorriFluido(Component contenuto) {
        JPanel colonna = colonnaFluida();
        colonna.add(contenuto);
        JScrollPane scroll = new JScrollPane(colonna);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(Scala.px(24));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    public static void adattaSplit(final JSplitPane split, double rapporto, int soglia, boolean verticaleStretto) {
        split.putClientProperty("divisione.fluida", new DivisioneDati(rapporto, soglia, verticaleStretto, split.getOrientation()));
        split.setContinuousLayout(true);
        split.setBorder(null);
        if (split.getLeftComponent() != null) split.getLeftComponent().setMinimumSize(new Dimension(0, 0));
        if (split.getRightComponent() != null) split.getRightComponent().setMinimumSize(new Dimension(0, 0));
        split.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) { aggiornaDivisione(split); }
        });
    }

    public static void aggiornaLayout(Component c) {
        if (c instanceof JSplitPane) aggiornaDivisione((JSplitPane) c);
        if (c instanceof Container) for (Component child : ((Container) c).getComponents()) aggiornaLayout(child);
    }

    private static void aggiornaDivisione(JSplitPane split) {
        Object value = split.getClientProperty("divisione.fluida");
        if (!(value instanceof DivisioneDati)) return;
        DivisioneDati d = (DivisioneDati) value;
        int orientamento = d.verticale && split.getWidth() > 0 && split.getWidth() < Scala.px(d.soglia)
                ? JSplitPane.VERTICAL_SPLIT : d.orientamento;
        int extent = orientamento == JSplitPane.HORIZONTAL_SPLIT ? split.getWidth() : split.getHeight();
        if (extent <= 0) return;
        if (d.extent == extent && split.getOrientation() == orientamento) return;
        if (d.extent > 0 && d.orientamentoAttuale == split.getOrientation() && split.getDividerLocation() > 0) {
            d.rapporto = Math.max(0.15, Math.min(0.85, (double) split.getDividerLocation() / Math.max(1, d.extent - split.getDividerSize())));
        }
        if (split.getOrientation() != orientamento) split.setOrientation(orientamento);
        d.extent = extent;
        d.orientamentoAttuale = orientamento;
        split.setResizeWeight(d.rapporto);
        split.setDividerLocation((int) Math.round(Math.max(0, extent - split.getDividerSize()) * d.rapporto));
    }

    private static final class DivisioneDati {
        double rapporto;
        final int soglia, orientamento;
        final boolean verticale;
        int extent, orientamentoAttuale;
        DivisioneDati(double r, int s, boolean v, int o) {
            rapporto = Math.max(0.15, Math.min(.85, r));
            soglia = s;
            verticale = v;
            orientamento = o;
            orientamentoAttuale = o;
        }
    }

    public static final class Divisione extends JSplitPane {
        public Divisione(int orientation, Component a, Component b) { super(orientation, a, b); }
        @Override public void doLayout() { aggiornaDivisione(this); super.doLayout(); }
    }

    private static final class Colonna extends JPanel implements Scrollable {
        Colonna() { setOpaque(false); setLayout(new Verticale()); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return Scala.px(24); }
        public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return Math.max(Scala.px(24), r.height - Scala.px(24)); }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private static final class Verticale implements LayoutManager {
        public void addLayoutComponent(String n, Component c) { }
        public void removeLayoutComponent(Component c) { }
        public Dimension minimumLayoutSize(Container c) { return new Dimension(0, 0); }
        public Dimension preferredLayoutSize(Container c) { return misura(c, false); }
        public void layoutContainer(Container c) { misura(c, true); }
        private Dimension misura(Container c, boolean layout) {
            Insets in = c.getInsets();
            int w = c.getWidth();
            if (c.getParent() instanceof JViewport) w = c.getParent().getWidth();
            else if (c.getParent() != null && c.getParent().getLayout() instanceof BorderLayout) {
                BorderLayout layoutParent = (BorderLayout) c.getParent().getLayout();
                Object position = layoutParent.getConstraints(c);
                if (BorderLayout.NORTH.equals(position) || BorderLayout.SOUTH.equals(position) || BorderLayout.CENTER.equals(position)) {
                    Insets parentInsets = c.getParent().getInsets();
                    w = c.getParent().getWidth() - parentInsets.left - parentInsets.right;
                }
            }
            if (w <= 0) w = Scala.px(600);
            int available = Math.max(1, w - in.left - in.right), y = in.top;
            for (Component child : c.getComponents()) if (child.isVisible()) {
                if (child.getWidth() != available) { child.setSize(available, child.getHeight()); child.invalidate(); }
                int h = child.getPreferredSize().height;
                if (layout) child.setBounds(in.left, y, available, h);
                y += h;
            }
            return new Dimension(w, y + in.bottom);
        }
    }

    /** Bordo arrotondato sottile, per i riquadri che non usano Carta. */
    public static final class BordoArrotondato extends AbstractBorder {
        @Override public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); }
        @Override public Insets getBorderInsets(Component c, Insets i) { i.set(1, 1, 1, 1); return i; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            p.setColor(BORDO);
            p.drawRoundRect(x, y, w - 1, h - 1, Scala.px(14), Scala.px(14));
            p.dispose();
        }
    }
}
