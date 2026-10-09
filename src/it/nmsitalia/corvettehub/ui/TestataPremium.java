package it.nmsitalia.corvettehub.ui;

import it.nmsitalia.corvettehub.Main;
import javax.swing.*;
import java.awt.*;

/** Identità e accesso community presenti in entrambe le schermate. */
public final class TestataPremium extends JPanel {
    public TestataPremium() {
        super(new BorderLayout(Scala.px(20),0));
        setBackground(Theme.SUPERFICIE); setBorder(Scala.bordo(16,28,16,28));
        JPanel brand = new JPanel(new BorderLayout(Scala.px(12),0)); brand.setOpaque(false);
        brand.add(new JLabel(Icone.logo(Scala.px(46))), BorderLayout.WEST);
        JPanel testi = new JPanel(); testi.setOpaque(false);
        testi.setLayout(new BoxLayout(testi,BoxLayout.Y_AXIS));
        JLabel nome = new JLabel("CORVETTE HUB"); nome.setFont(Scala.font(Font.BOLD,19));
        JLabel firma = new JLabel("NMS ITALIA    /    FLEET WORKSPACE");
        firma.setFont(Scala.font(Font.PLAIN,10)); firma.setForeground(Theme.TESTO_TENUE);
        testi.add(nome); testi.add(Box.createVerticalStrut(Scala.px(3))); testi.add(firma);
        brand.add(testi,BorderLayout.CENTER);
        JPanel azioni = new JPanel(new FlowLayout(FlowLayout.RIGHT,Scala.px(14),0)); azioni.setOpaque(false);
        JLabel versione = new JLabel("v" + Main.VERSIONE); versione.setForeground(Theme.TESTO_TENUE);
        versione.setFont(Scala.font(Font.PLAIN,11)); azioni.add(versione); azioni.add(Comunita.pulsante());
        add(brand,BorderLayout.WEST); add(azioni,BorderLayout.EAST);
    }
    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g); g.setColor(Theme.BORDO);
        g.drawLine(0,getHeight()-1,getWidth(),getHeight()-1);
    }
}
