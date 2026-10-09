package it.nmsitalia.corvettehub.ui;

import javax.swing.Icon;
import java.awt.*;
import java.awt.geom.Path2D;

/** Icone vettoriali scalabili, indipendenti dai font di sistema. */
public final class Segno implements Icon {
    private final String nome;
    public Segno(String nome) { this.nome = nome; }
    public int getIconWidth() { return Scala.px(20); }
    public int getIconHeight() { return Scala.px(20); }
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D p = (Graphics2D)g.create();
        p.translate(x,y); p.scale(Scala.fattore(),Scala.fattore());
        p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        p.setColor(c.isEnabled() ? c.getForeground() : Theme.TESTO_TENUE);
        p.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        if ("Corvette".equals(nome)) {
            Path2D s = new Path2D.Double(); s.moveTo(10,2); s.lineTo(17,16);
            s.lineTo(10,13); s.lineTo(3,16); s.closePath(); p.draw(s); p.drawLine(10,7,10,12);
        } else if ("Importa".equals(nome) || "Esporta".equals(nome)) {
            p.drawLine(3,13,3,17); p.drawLine(3,17,17,17); p.drawLine(17,17,17,13);
            boolean in = "Importa".equals(nome); int a = in ? 12 : 3;
            p.drawLine(10,3,10,12); p.drawLine(6,in ? 8 : 7,10,a); p.drawLine(14,in ? 8 : 7,10,a);
        } else if ("Rinomina".equals(nome)) {
            p.drawLine(4,15,14,5); p.drawLine(14,5,17,8); p.drawLine(17,8,7,18);
            p.drawLine(7,18,3,18); p.drawLine(3,18,4,15);
        } else if ("Elimina".equals(nome)) {
            p.drawLine(3,5,17,5); p.drawRoundRect(5,5,10,12,2,2);
            p.drawLine(8,2,12,2); p.drawLine(8,8,8,14); p.drawLine(12,8,12,14);
        } else if ("discord".equals(nome)) {
            p.drawRoundRect(2,4,16,12,6,6); p.fillOval(6,8,2,3); p.fillOval(12,8,2,3);
            p.drawLine(4,16,6,18); p.drawLine(16,16,14,18);
        } else {
            p.drawRoundRect(3,3,14,14,3,3); p.drawLine(3,8,17,8);
            if ("Deposito".equals(nome)) { p.drawLine(8,8,8,17); p.drawLine(12,8,12,17); }
            else { p.drawLine(6,11,14,11); p.drawLine(6,14,11,14); }
        }
        p.dispose();
    }
}
