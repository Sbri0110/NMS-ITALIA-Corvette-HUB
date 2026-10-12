package it.nmsitalia.corvettehub.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Il pannello introduttivo della schermata salvataggi.
 *
 * Due cose insieme: racconta cosa fa il programma e indica in che punto del
 * percorso si trova l'utente. Il disegno vettoriale al centro (una rotta con
 * un pianeta) e' lo stesso linguaggio delle icone della navigazione.
 *
 * Rispetto alla versione precedente: carta con ombra, testi su una scala piu'
 * chiara, i tre passi numerati con il segno della tappa, e un riquadro di
 * sicurezza in fondo.
 */
public final class Manifesto extends JPanel {

    public Manifesto() {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(Scala.bordo(22, 20, 22, 20));

        add(riga(Theme.maiuscoletto("Il tuo centro di comando")));
        add(Box.createVerticalStrut(Scala.px(14)));
        add(riga(titolo("Pronto a partire.")));
        add(Box.createVerticalStrut(Scala.px(8)));
        add(riga(corpo("Progetti, inventari e tecnologie.", Theme.TESTO_TENUE)));
        add(Box.createVerticalStrut(Scala.px(2)));
        add(riga(corpo("Ogni Corvette, al suo posto.", Theme.TESTO_TENUE)));

        add(Box.createVerticalStrut(Scala.px(22)));

        add(passo("1", "Scegli il salvataggio", true));
        add(Box.createVerticalStrut(Scala.px(12)));
        add(passo("2", "Esplora il tuo hangar", false));
        add(Box.createVerticalStrut(Scala.px(12)));
        add(passo("3", "Scambia i tuoi progetti", false));

        add(Box.createVerticalStrut(Scala.px(22)));
        add(sicurezza());

        // La carta resta compatta: con un'altezza libera il BoxLayout la
        // stirava a tutta finestra e sembrava vuota.
        add(Box.createVerticalGlue());
    }

    @Override public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    // ------------------------------------------------------------ elementi

    private JPanel riga(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        p.add(c, BorderLayout.WEST);
        return p;
    }

    private JLabel titolo(String t) {
        JLabel l = new JLabel(t);
        l.setFont(Scala.font(Font.PLAIN, 20));
        l.setForeground(Theme.TESTO);
        return l;
    }

    private JLabel corpo(String t, Color c) {
        JLabel l = new JLabel(t);
        l.setFont(Theme.corpo());
        l.setForeground(c);
        return l;
    }

    /** Un passo del percorso: il numero in un cerchio, accanto al testo. */
    private JPanel passo(String numero, String testo, boolean attivo) {
        JPanel p = new JPanel(new BorderLayout(Scala.px(11), 0));
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);

        Theme.SegnoNumero segno = new Theme.SegnoNumero(numero);
        if (attivo) segno.attivo(); else segno.spento();
        segno.setPreferredSize(Scala.dim(26, 26));
        segno.setMinimumSize(Scala.dim(26, 26));
        segno.setMaximumSize(Scala.dim(26, 26));

        JLabel t = new JLabel(testo);
        t.setFont(Scala.font(attivo ? Font.BOLD : Font.PLAIN, 12));
        t.setForeground(attivo ? Theme.TESTO : Theme.TESTO_TENUE);

        p.add(segno, BorderLayout.WEST);
        p.add(t, BorderLayout.CENTER);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Scala.px(26)));
        return p;
    }

    /** Il riquadro in fondo: le due promesse di sicurezza. */
    private JPanel sicurezza() {
        Theme.Carta c = new Theme.Carta().riempi(Theme.SUPERFICIE).senzaOmbra().raggio(Scala.px(10));
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBorder(Scala.bordo(12, 12, 12, 12));
        c.setAlignmentX(LEFT_ALIGNMENT);

        JLabel a = new JLabel("Scritture verificate");
        a.setFont(Scala.font(Font.BOLD, 11));
        a.setForeground(Theme.OK);
        a.setAlignmentX(LEFT_ALIGNMENT);

        JLabel b = new JLabel("<html>Backup prima di ogni modifica.<br>"
                + "La tua partita resta al centro.</html>");
        b.setFont(Scala.font(Font.PLAIN, 11));
        b.setForeground(Theme.TESTO_TENUE);
        b.setAlignmentX(LEFT_ALIGNMENT);

        c.add(a);
        c.add(Box.createVerticalStrut(Scala.px(5)));
        c.add(b);
        JPanel riga = new JPanel(new BorderLayout());
        riga.setOpaque(false);
        riga.setAlignmentX(LEFT_ALIGNMENT);
        riga.add(c);
        return riga;
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D p = (Graphics2D) g.create();
        p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth() - 1, h = getHeight() - 1;
        int r = Scala.px(16);

        Theme.ombra(p, Scala.px(3), Scala.px(3), w - Scala.px(6), h - Scala.px(6), r, 0.5f);
        p.setPaint(new GradientPaint(0, 0, Theme.SUPERFICIE_ALTA, 0, h, Theme.SUPERFICIE));
        p.fillRoundRect(Scala.px(3), Scala.px(3), w - Scala.px(6), h - Scala.px(6), r, r);
        p.setColor(Theme.BORDO);
        p.drawRoundRect(Scala.px(3), Scala.px(3), w - Scala.px(6) - 1, h - Scala.px(6) - 1, r, r);

        // il disegno vettoriale: un pianeta con la rotta
        int cx = getWidth() / 2, cy = getHeight() / 2 - Scala.px(6);
        if (getHeight() > Scala.px(430)) {
            p.setColor(new Color(0x24344C));
            p.drawOval(cx - Scala.px(46), cy - Scala.px(28), Scala.px(92), Scala.px(56));
            p.setColor(new Color(0x2F4258));
            p.drawOval(cx - Scala.px(34), cy - Scala.px(21), Scala.px(68), Scala.px(42));
            p.setColor(Theme.ACCENTO);
            p.setStroke(new BasicStroke(Scala.px(1.6f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D rotta = new Path2D.Double();
            rotta.moveTo(cx - Scala.px(30), cy + Scala.px(18));
            rotta.curveTo(cx - Scala.px(10), cy - Scala.px(30),
                    cx + Scala.px(10), cy + Scala.px(30),
                    cx + Scala.px(32), cy - Scala.px(16));
            p.draw(rotta);
            p.fillOval(cx + Scala.px(28), cy - Scala.px(20), Scala.px(6), Scala.px(6));
            p.setColor(Theme.TESTO_TENUE);
            p.fillOval(cx - Scala.px(33), cy + Scala.px(15), Scala.px(5), Scala.px(5));
        }
        p.dispose();
    }
}
