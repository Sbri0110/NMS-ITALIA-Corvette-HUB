package it.nmsitalia.corvettehub.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;

/** Sistema visivo condiviso: blu notte, accento champagne, controlli coerenti. */
public final class Theme {
    public static final Color SFONDO = new Color(0x0B1018);
    public static final Color SUPERFICIE = new Color(0x121B28);
    public static final Color SUPERFICIE_ALTA = new Color(0x1B2839);
    public static final Color BORDO = new Color(0x2A394D);
    public static final Color TESTO = new Color(0xEEF2F8);
    public static final Color TESTO_TENUE = new Color(0xA6B4C8);
    public static final Color ACCENTO = new Color(0xE8BD78);
    public static final Color ACCENTO_SCURO = new Color(0x3F3528);
    public static final Color OK = new Color(0x79D7AB);
    public static final Color ERRORE = new Color(0xF08089);
    public static final Color AVVISO = new Color(0xE8BD78);
    public static final Color AVVISO_SCURO = new Color(0x30291F);
    private Theme() { }

    public static void installa() {
        // Scala gestisce già i pixel: evita una seconda scala FlatLaf.
        System.setProperty("flatlaf.uiScale", "1x");
        FlatDarkLaf.setup();
        UIManager.put("defaultFont", Scala.font(Font.PLAIN, 13));
        UIManager.put("Panel.background", SFONDO);
        UIManager.put("Component.background", SUPERFICIE);
        UIManager.put("Component.foreground", TESTO);
        UIManager.put("Label.foreground", TESTO);
        UIManager.put("Component.borderColor", BORDO);
        UIManager.put("Component.focusColor", ACCENTO);
        UIManager.put("Component.focusWidth", Scala.px(1));
        UIManager.put("Component.arc", Scala.px(12));
        UIManager.put("Button.arc", Scala.px(12));
        UIManager.put("TextComponent.arc", Scala.px(12));
        UIManager.put("Button.margin", new Insets(Scala.px(10), Scala.px(18), Scala.px(10), Scala.px(18)));
        UIManager.put("Button.background", SUPERFICIE_ALTA);
        UIManager.put("Button.foreground", TESTO);
        UIManager.put("Button.hoverBackground", BORDO);
        UIManager.put("Button.disabledBackground", SUPERFICIE);
        UIManager.put("Button.disabledText", TESTO_TENUE);
        UIManager.put("Button.default.background", ACCENTO);
        UIManager.put("Button.default.foreground", SFONDO);
        UIManager.put("TextField.background", SUPERFICIE);
        UIManager.put("TextField.foreground", TESTO);
        UIManager.put("ComboBox.background", SUPERFICIE);
        UIManager.put("ComboBox.foreground", TESTO);
        UIManager.put("TextComponent.selectionBackground", ACCENTO_SCURO);
        UIManager.put("TextArea.background", SUPERFICIE);
        UIManager.put("TextArea.foreground", TESTO);
        UIManager.put("ScrollBar.width", Scala.px(10));
        UIManager.put("ScrollBar.thumbArc", Scala.px(999));
        UIManager.put("ScrollBar.track", SFONDO);
        UIManager.put("ScrollBar.thumb", BORDO);
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("List.background", SUPERFICIE);
        UIManager.put("List.foreground", TESTO);
        UIManager.put("List.selectionBackground", ACCENTO_SCURO);
        UIManager.put("List.selectionForeground", TESTO);
        UIManager.put("ScrollPane.background", SFONDO);
        UIManager.put("SplitPane.background", SFONDO);
        UIManager.put("SplitPane.dividerSize", Scala.px(8));
        UIManager.put("SplitPaneDivider.style", "plain");
        UIManager.put("TabbedPane.background", SFONDO);
        UIManager.put("TabbedPane.foreground", TESTO_TENUE);
        UIManager.put("TabbedPane.selectedBackground", SUPERFICIE_ALTA);
        UIManager.put("TabbedPane.underlineColor", ACCENTO);
        UIManager.put("TabbedPane.focusColor", ACCENTO);
        UIManager.put("TabbedPane.tabHeight", Scala.px(48));
        UIManager.put("TabbedPane.tabSelectionHeight", Scala.px(3));
        UIManager.put("TabbedPane.contentAreaColor", BORDO);
        UIManager.put("TabbedPane.contentSeparatorHeight", 0);
        UIManager.put("TabbedPane.tabInsets", new Insets(Scala.px(8), Scala.px(20), Scala.px(8), Scala.px(20)));
        UIManager.put("ToolTip.background", SUPERFICIE_ALTA);
        UIManager.put("ToolTip.foreground", TESTO);
    }
    public static Font titolo() { return Scala.font(Font.BOLD, 18); }
    public static Font sezione() { return Scala.font(Font.BOLD, 13); }
    public static Font monospazio() { return Scala.mono(12); }
    public static void primaria(JButton b) {
        b.setBackground(ACCENTO); b.setForeground(SFONDO);
        b.setFont(Scala.font(Font.BOLD, 13));
        b.putClientProperty("JButton.buttonType", "roundRect");
    }
    public static void rifinisci(Component c) {
        if (c instanceof JScrollPane) {
            JScrollPane s = (JScrollPane) c; s.setBorder(new BordoArrotondato());
            s.getViewport().setBackground(SUPERFICIE);
            s.getVerticalScrollBar().setUnitIncrement(Scala.px(24));
            s.getHorizontalScrollBar().setUnitIncrement(Scala.px(24));
        }
        if (c instanceof JSplitPane) {
            JSplitPane s = (JSplitPane) c;
            if (s.getLeftComponent() != null) s.getLeftComponent().setMinimumSize(Scala.dim(160, 100));
            if (s.getRightComponent() != null) s.getRightComponent().setMinimumSize(Scala.dim(240, 100));
        }
        if (c instanceof JTextArea) {
            ((JTextArea) c).setLineWrap(true); ((JTextArea) c).setWrapStyleWord(true);
        }
        if (c instanceof JButton) {
            JButton b = (JButton) c; b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            String t = b.getText();
            if (t != null && (t.startsWith("Continua") || t.equals("Importa")
                    || t.startsWith("Esporta progetto") || t.equals("Rinomina")
                    || t.startsWith("Salva le modifiche"))) primaria(b);
            if (t != null && t.startsWith("Elimina")) b.setForeground(ERRORE);
        }
        if (c instanceof Container) for (Component child : ((Container)c).getComponents()) rifinisci(child);
    }
    public static void dopoScrittura(Component c) {
        while (c != null && !(c instanceof SchermataCorvette)) c = c.getParent();
        if (c instanceof SchermataCorvette) ((SchermataCorvette)c).ricarica();
    }
    public static void lavora(Component c, boolean occupata) {
        while (c != null && !(c instanceof SchermataCorvette)) c = c.getParent();
        if (c instanceof SchermataCorvette) ((SchermataCorvette)c).setOccupata(occupata);
    }
    public static final class BordoArrotondato extends AbstractBorder {
        @Override public Insets getBorderInsets(Component c) { return new Insets(1,1,1,1); }
        @Override public Insets getBorderInsets(Component c, Insets i) { i.set(1,1,1,1); return i; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D p = (Graphics2D)g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            p.setColor(BORDO); p.drawRoundRect(x,y,w-1,h-1,Scala.px(14),Scala.px(14)); p.dispose();
        }
    }
    public static final class Carta extends JPanel {
        public Carta() { setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D p=(Graphics2D)g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            p.setColor(SUPERFICIE);p.fillRoundRect(0,0,getWidth()-1,getHeight()-1,Scala.px(16),Scala.px(16));
            p.setColor(BORDO);p.drawRoundRect(0,0,getWidth()-1,getHeight()-1,Scala.px(16),Scala.px(16));p.dispose();
        }
    }
}
