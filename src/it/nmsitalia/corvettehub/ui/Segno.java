package it.nmsitalia.corvettehub.ui;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Le icone vettoriali della navigazione.
 *
 * Sono disegnate con Java2D invece di caricare immagini: restano nitide a
 * qualunque ingrandimento e non pesano sul pacchetto. Il tratto parte dal
 * bordo esterno e usa il colore del testo del componente, cosi' l'icona e'
 * coerente con l'etichetta accanto.
 */
public final class Segno implements Icon {

    private static final int BASE = 20;
    /** Lato della gabbia in cui ogni glifo viene centrato, in punti. */
    private static final double GABBIA = 20.0;
    private final String nome;

    public Segno(String nome) { this.nome = nome; }

    public int getIconWidth() { return Scala.px(BASE); }
    public int getIconHeight() { return Scala.px(BASE); }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        // I glifi hanno larghezze diverse (la matita e' piu' larga del
        // cestino, i libri piu' stretti) ma tutti partono dal bordo sinistro
        // della gabbia: cosi' le icone del menu cadono sulla stessa verticale
        // e i testi accanto restano allineati. Qui si centra solo in altezza.
        double[] ing = INGOMBRI(nome);
        double scartoY = GABBIA / 2.0 - (ing[1] + ing[3]) / 2.0;

        Graphics2D p = (Graphics2D) g.create();
        p.translate(x, y + scartoY * Scala.fattore());
        p.scale(Scala.fattore(), Scala.fattore());
        p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        p.setColor(c.isEnabled() ? c.getForeground() : Theme.TESTO_DEBOLE);
        p.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        disegna(p);
        p.dispose();
    }

    /**
     * L'ingombro {@code {minX, minY, maxX, maxY}} del segno, in punti.
     *
     * Sta in una tabella e non si misura disegnando: serve a centrare il
     * glifo prima di tracciarlo, e un doppio disegno lascerebbe il primo
     * fuori posto.
     */
    private static double[] INGOMBRI(String nome) {
        if ("Corvette".equals(nome)) return new double[] { 3, 2.4, 17, 16.4 };
        if ("Importa".equals(nome)) return new double[] { 3, 3, 17, 17 };
        if ("Esporta".equals(nome)) return new double[] { 3, 3, 17, 17 };
        if ("Rinomina".equals(nome)) return new double[] { 3, 6, 17, 18.5 };
        if ("Elimina".equals(nome)) return new double[] { 3, 2, 17, 17 };
        if ("Deposito".equals(nome)) return new double[] { 3, 3, 17, 17 };
        if ("Deposito Corvette".equals(nome)) return new double[] { 3, 4, 17, 17 };
        if ("Libreria".equals(nome)) return new double[] { 3, 4, 17, 17 };
        if ("discord".equals(nome)) return new double[] { 2, 4, 18, 18 };
        return new double[] { 3, 3, 17, 17 };
    }

    /**
     * Traccia il glifo nella griglia di 20 punti.
     *
     * Chi disegna non si cura della centratura: le coordinate sono gia'
     * riferite alla gabbia, ci pensa {@link #paintIcon}.
     */
    private void disegna(Graphics2D p) {
        if ("Corvette".equals(nome)) {
            // una freccia stilizzata: la rotta
            Path2D s = new Path2D.Double();
            s.moveTo(10, 2.4);
            s.lineTo(17, 16.4);
            s.lineTo(10, 13.2);
            s.lineTo(3, 16.4);
            s.closePath();
            p.draw(s);
            p.drawLine(10, 7, 10, 12);
            return;
        }
        if ("Importa".equals(nome)) {
            // freccia verso il basso dentro un vassoio
            p.drawLine(3, 12, 3, 17);
            p.drawLine(3, 17, 17, 17);
            p.drawLine(17, 17, 17, 12);
            p.drawLine(10, 3, 10, 12);
            p.drawLine(6, 8, 10, 12);
            p.drawLine(14, 8, 10, 12);
            return;
        }
        if ("Esporta".equals(nome)) {
            // freccia verso l'alto dentro un vassoio
            p.drawLine(3, 12, 3, 17);
            p.drawLine(3, 17, 17, 17);
            p.drawLine(17, 17, 17, 12);
            p.drawLine(10, 12, 10, 3);
            p.drawLine(6, 7, 10, 3);
            p.drawLine(14, 7, 10, 3);
            return;
        }
        if ("Rinomina".equals(nome)) {
            // matita
            Path2D pn = new Path2D.Double();
            pn.moveTo(4.5, 15.5);
            pn.lineTo(14, 6);
            pn.lineTo(17, 9);
            pn.lineTo(7.5, 18.5);
            pn.lineTo(3, 18.5);
            pn.closePath();
            p.draw(pn);
            return;
        }
        if ("Elimina".equals(nome)) {
            // cestino
            p.drawLine(3, 5, 17, 5);
            p.drawLine(8, 2, 12, 2);
            p.drawRoundRect(5, 5, 10, 12, 2, 2);
            p.drawLine(8, 8, 8, 14);
            p.drawLine(12, 8, 12, 14);
            return;
        }
        if ("Deposito".equals(nome)) {
            // scatole impilate
            p.drawRoundRect(3, 3, 14, 7, 2, 2);
            p.drawRoundRect(3, 11, 14, 6, 2, 2);
            p.drawLine(10, 3, 10, 10);
            return;
        }
        if ("Deposito Corvette".equals(nome)) {
            // uno scrigno con il coperchio: il deposito della nave
            p.drawRoundRect(3, 8, 14, 9, 2, 2);
            p.drawLine(3, 12, 17, 12);
            p.drawLine(5, 8, 7, 4);
            p.drawLine(7, 4, 13, 4);
            p.drawLine(13, 4, 15, 8);
            return;
        }
        if ("Libreria".equals(nome)) {
            // tre volumi in fila, lo schienale dell'ultimo inclinato
            p.drawRoundRect(3, 4, 4, 13, 1, 1);
            p.drawRoundRect(8, 4, 4, 13, 1, 1);
            p.drawLine(14, 5, 17, 16);
            return;
        }
        if ("discord".equals(nome)) {
            p.drawRoundRect(2, 4, 16, 12, 6, 6);
            p.fillOval(6, 8, 2, 3);
            p.fillOval(12, 8, 2, 3);
            p.drawLine(4, 16, 6, 18);
            p.drawLine(16, 16, 14, 18);
            return;
        }
        // segno generico: un foglio con tre righe
        p.drawRoundRect(3, 3, 14, 14, 3, 3);
        p.drawLine(3, 8, 17, 8);
        p.drawLine(6, 11, 14, 11);
        p.drawLine(6, 14, 11, 14);
    }

    /**
     * L'indicatore della scheda attiva: una barretta d'oro.
     *
     * Nella vecchia interfaccia la scheda scelta si distingueva solo per il
     * fondo. Qui si aggiunge una linea netta sul lato sinistro, come nelle
     * interfacce moderne.
     */
    public static final class BarraAttiva implements Icon {
        public int getIconWidth() { return Scala.px(3); }
        public int getIconHeight() { return Scala.px(24); }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            p.setColor(Theme.ACCENTO);
            p.fillRoundRect(x, y, Scala.px(3), getIconHeight(), Scala.px(3), Scala.px(3));
            p.dispose();
        }
    }

    /**
     * Il punto di stato: verde quando tutto e' a posto, ambra per un avviso.
     * E' un cerchio pieno con un alone tenue dello stesso colore.
     */
    public static final class Punto implements Icon {
        private final Color colore;
        public Punto(Color colore) { this.colore = colore; }
        public int getIconWidth() { return Scala.px(12); }
        public int getIconHeight() { return Scala.px(12); }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int d = Scala.px(8);
            int off = (getIconWidth() - d) / 2;
            p.setColor(new Color(colore.getRed(), colore.getGreen(), colore.getBlue(), 60));
            p.fillOval(x, y, getIconWidth(), getIconHeight());
            p.setColor(colore);
            p.fillOval(x + off + Scala.px(1), y + off + Scala.px(1), d - Scala.px(2), d - Scala.px(2));
            p.dispose();
        }
    }

    /**
     * Lo sfondo della scheda selezionata: superficie chiara con angoli
     * arrotondati e un accento d'oro a sinistra.
     */
    public static final class SfondoScheda extends AbstractBorder {
        private final boolean attiva;

        public SfondoScheda(boolean attiva) { this.attiva = attiva; }

        @Override public Insets getBorderInsets(Component c) { return new Insets(Scala.px(6), Scala.px(12), Scala.px(6), Scala.px(12)); }
        @Override public Insets getBorderInsets(Component c, Insets i) {
            i.set(Scala.px(6), Scala.px(12), Scala.px(6), Scala.px(12));
            return i;
        }

        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            if (!attiva) return;
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            p.setColor(Theme.SUPERFICIE_ALTA);
            p.fillRoundRect(x + Scala.px(1), y + Scala.px(1), w - Scala.px(2), h - Scala.px(2), Scala.px(10), Scala.px(10));
            p.setColor(Theme.ACCENTO);
            p.setStroke(new BasicStroke(Scala.px(3), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            p.drawLine(x + Scala.px(3), y + Scala.px(7), x + Scala.px(3), y + h - Scala.px(7));
            p.dispose();
        }
    }
}
