package it.nmsitalia.corvettehub.ui;

import it.nmsitalia.corvettehub.Main;
import javax.swing.*;
import java.awt.*;

/** Identità e accesso community presenti in entrambe le schermate. */
public final class TestataPremium extends JPanel {
    private final JLabel firma = new JLabel("NMS ITALIA    /    FLEET WORKSPACE");
    private final JLabel versione = new JLabel("v" + Main.VERSIONE);
    private final JButton discord = Comunita.pulsante();
    public TestataPremium() {
        super(new BorderLayout(Scala.px(12),0));
        setBackground(Theme.SUPERFICIE); setBorder(Scala.bordo(10,16,10,16));
        JPanel brand = new JPanel(new BorderLayout(Scala.px(8),0)); brand.setOpaque(false);
        brand.add(new JLabel(Icone.logo(Scala.px(32))), BorderLayout.WEST);
        JPanel testi = new JPanel(); testi.setOpaque(false);
        testi.setLayout(new BoxLayout(testi,BoxLayout.Y_AXIS));
        JLabel nome = new JLabel("CORVETTE HUB"); nome.setFont(Scala.font(Font.BOLD,16));
        firma.setFont(Scala.font(Font.PLAIN,10)); firma.setForeground(Theme.TESTO_TENUE);
        testi.add(nome); testi.add(Box.createVerticalStrut(Scala.px(3))); testi.add(firma);
        brand.add(testi,BorderLayout.CENTER);
        JPanel azioni = new JPanel(new FlowLayout(FlowLayout.RIGHT,Scala.px(8),0)); azioni.setOpaque(false);
        versione.setForeground(Theme.TESTO_TENUE);
        versione.setFont(Scala.font(Font.PLAIN,10)); azioni.add(versione); azioni.add(discord);
        add(brand,BorderLayout.WEST); add(azioni,BorderLayout.EAST);
    }
    private void adatta() {
        if(discord==null) return;
        int w=getParent()==null?getWidth():getParent().getWidth();
        boolean compatto=w>0 && w<Scala.px(600);
        firma.setVisible(!compatto);versione.setVisible(!compatto);
        String testo=compatto?"Discord  ↗":"Discord NMS ITALIA  ↗";
        if(!testo.equals(discord.getText())) discord.setText(testo);
    }
    @Override public Dimension getPreferredSize() { adatta();return super.getPreferredSize(); }
    @Override public void doLayout() { adatta();super.doLayout(); }
    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g); g.setColor(Theme.BORDO);
        g.drawLine(0,getHeight()-1,getWidth(),getHeight()-1);
    }
}
