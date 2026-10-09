package it.nmsitalia.corvettehub.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/** Pannello introduttivo, disegno vettoriale e percorso di lavoro. */
public final class Manifesto extends JPanel {
    public Manifesto() {
        setOpaque(false); setPreferredSize(Scala.dim(300,420));
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS)); setBorder(Scala.bordo(26));
        add(testo("IL TUO CENTRO DI COMANDO", 10, Theme.ACCENTO, true));
        add(Box.createVerticalStrut(Scala.px(16)));
        add(testo("Pronto a partire.", 23, Theme.TESTO, true));
        add(Box.createVerticalStrut(Scala.px(12)));
        add(testo("<html>Progetti, inventari e tecnologie.<br>Ogni Corvette, al suo posto.</html>",13,Theme.TESTO_TENUE,false));
        add(Box.createVerticalGlue());
        add(testo("01   Scegli il salvataggio",13,Theme.TESTO,true));
        add(Box.createVerticalStrut(Scala.px(14)));
        add(testo("02   Esplora il tuo hangar",13,Theme.TESTO_TENUE,false));
        add(Box.createVerticalStrut(Scala.px(14)));
        add(testo("03   Scambia i tuoi progetti",13,Theme.TESTO_TENUE,false));
        add(Box.createVerticalStrut(Scala.px(30)));
        add(testo("●  Backup prima di ogni modifica",11,Theme.OK,false));
        add(Box.createVerticalStrut(Scala.px(8)));
        add(testo("<html>Le scritture sono verificate.<br>La tua partita resta al centro.</html>",11,Theme.TESTO_TENUE,false));
    }
    private JLabel testo(String t,int n,Color c,boolean bold) {
        JLabel l=new JLabel(t); l.setFont(Scala.font(bold?Font.BOLD:Font.PLAIN,n));
        l.setForeground(c); l.setAlignmentX(LEFT_ALIGNMENT); return l;
    }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D p=(Graphics2D)g.create();
        p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        p.setPaint(new GradientPaint(0,0,Theme.SUPERFICIE_ALTA,getWidth(),getHeight(),Theme.SUPERFICIE));
        p.fillRoundRect(0,0,getWidth()-1,getHeight()-1,Scala.px(20),Scala.px(20));
        p.setColor(Theme.BORDO); p.drawRoundRect(0,0,getWidth()-1,getHeight()-1,Scala.px(20),Scala.px(20));
        int mid=getHeight()/2-Scala.px(20),cx=getWidth()/2;
        if (getHeight()>Scala.px(450)) {
            p.setColor(new Color(0x304053));
            p.drawOval(cx-Scala.px(83),mid-Scala.px(54),Scala.px(166),Scala.px(108));
            p.drawOval(cx-Scala.px(105),mid-Scala.px(72),Scala.px(210),Scala.px(144));
            p.setColor(Theme.ACCENTO); p.setStroke(new BasicStroke(Scala.px(2)));
            Path2D s=new Path2D.Double(); s.moveTo(cx,mid-Scala.px(38));
            s.lineTo(cx+Scala.px(28),mid+Scala.px(25)); s.lineTo(cx,mid+Scala.px(10));
            s.lineTo(cx-Scala.px(28),mid+Scala.px(25)); s.closePath(); p.draw(s);
            p.fillOval(cx+Scala.px(72),mid-Scala.px(30),Scala.px(5),Scala.px(5));
        }
        p.dispose();
    }
}
