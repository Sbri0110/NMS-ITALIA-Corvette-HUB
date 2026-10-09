package it.nmsitalia.corvettehub.ui;

import it.nmsitalia.corvettehub.domain.CatalogoParti;
import it.nmsitalia.corvettehub.domain.Corvette;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

/**
 * Anteprima disegnata di una Corvette.
 *
 * Non e' un'icona segnaposto: e' una vista schematica dei moduli disposti
 * nelle loro posizioni reali, letta dal campo Position di ogni oggetto. Due
 * proiezioni, dall'alto (X e Z) e di lato (X e Y), colorate per categoria.
 *
 * Serve a riconoscere la forma della nave a colpo d'occhio, che e' molto piu'
 * utile di un rettangolo grigio (specifica 7.7).
 */
public final class Anteprima extends JPanel {

    private Corvette corvette;

    public Anteprima() {
        setBackground(Theme.SUPERFICIE);
        setOpaque(true);
    }

    public void mostra(Corvette c) {
        this.corvette = c;
        repaint();
    }

    public Corvette getCorvette() {
        return corvette;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (corvette == null || corvette.getPosizioniModuli().isEmpty()) {
            g.setColor(Theme.TESTO_TENUE);
            g.setFont(Scala.font(Font.PLAIN, 12));
            String msg = corvette == null
                    ? "Scegli una Corvette per vederne l'anteprima."
                    : "Questa Corvette non ha moduli da disegnare.";
            g.drawString(msg, 16, h / 2);
            return;
        }

        List<String> parti = corvette.getParti();
        List<double[]> pos = corvette.getPosizioniModuli();

        // riquadro di ingombro, su entrambe le proiezioni
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (int i = 0; i < pos.size(); i++) {
            double[] p = pos.get(i);
            if (p[0] < minX) minX = p[0];
            if (p[0] > maxX) maxX = p[0];
            if (p[1] < minY) minY = p[1];
            if (p[1] > maxY) maxY = p[1];
            if (p[2] < minZ) minZ = p[2];
            if (p[2] > maxZ) maxZ = p[2];
        }

        int margine = Scala.px(24);
        int metaL = (w - margine * 3) / 2;
        int altezza = h - margine * 2 - Scala.px(14);
        if (metaL < Scala.px(40) || altezza < Scala.px(30)) return;

        disegnaVista(g, "Dall'alto", margine, margine + 14, metaL, altezza,
                parti, pos, minX, maxX, minZ, maxZ, 0, 2);
        disegnaVista(g, "Di lato", margine * 2 + metaL, margine + 14, metaL, altezza,
                parti, pos, minX, maxX, minY, maxY, 0, 1);

        // riga di ingombro
        g.setColor(Theme.TESTO_TENUE);
        g.setFont(Scala.font(Font.PLAIN, 11));
        g.drawString(String.format("Ingombro %.0f x %.0f x %.0f m   ·   %d moduli",
                maxX - minX, maxY - minY, maxZ - minZ, pos.size()),
                margine, h - 8);
    }

    private void disegnaVista(Graphics2D g, String titolo,
                              int x0, int y0, int larg, int alt,
                              List<String> parti, List<double[]> pos,
                              double minX, double maxX, double minV, double maxV,
                              int iOriz, int iVert) {

        // sfondo della vista
        g.setColor(Theme.SFONDO);
        g.fillRoundRect(x0, y0, larg, alt, 10, 10);
        g.setColor(Theme.BORDO);
        g.setStroke(new BasicStroke(0.5f));
        g.drawRoundRect(x0, y0, larg, alt, 10, 10);

        g.setColor(Theme.TESTO_TENUE);
        g.setFont(Scala.font(Font.BOLD, 11));
        g.drawString(titolo, x0 + 10, y0 - 4);

        double spanO = Math.max(6.0, maxX - minX);
        double spanV = Math.max(6.0, maxV - minV);
        double scala = Math.min((larg - 24) / spanO, (alt - 24) / spanV);
        double centroO = (minX + maxX) / 2;
        double centroV = (minV + maxV) / 2;
        double origineX = x0 + larg / 2.0;
        double origineY = y0 + alt / 2.0;

        // griglia ogni 6 metri, tenue
        g.setColor(Theme.BORDO);
        g.setStroke(new BasicStroke(0.5f));
        double passoO = Math.max(6, spanO / 100);
        double passoV = Math.max(6, spanV / 100);
        for (double v = Math.ceil(minX / passoO) * passoO; v <= maxX; v += passoO) {
            int px = (int)(origineX + (v - centroO) * scala);
            g.drawLine(px, y0 + 12, px, y0 + alt - 12);
        }
        for (double v = Math.ceil(minV / passoV) * passoV; v <= maxV; v += passoV) {
            int py = (int)(origineY - (v - centroV) * scala);
            g.drawLine(x0 + 12, py, x0 + larg - 12, py);
        }

        // i moduli, dal piu' basso al piu' alto per una sovrapposizione sensata
        int lato = Math.max(Scala.px(4), Math.min(Scala.px(18), (int) (3.0 * scala)));
        for (int i = 0; i < pos.size(); i++) {
            double[] p = pos.get(i);
            int px = (int)(origineX + (p[iOriz] - centroO) * scala);
            int py = (int)(origineY - (p[iVert] - centroV) * scala);
            Color c = Icone.coloreCategoria(CatalogoParti.categoria(parti.get(i)));
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 170));
            g.fillRect(px - lato / 2, py - lato / 2, lato, lato);
            g.setColor(c);
            g.setStroke(new BasicStroke(0.5f));
            g.drawRect(px - lato / 2, py - lato / 2, lato, lato);
        }
    }
}
