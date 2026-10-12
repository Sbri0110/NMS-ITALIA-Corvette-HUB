package it.nmsitalia.corvettehub.ui;

import it.nmsitalia.corvettehub.Main;
import javax.swing.*;
import java.awt.*;

/**
 * La testata: l'identita' dell'applicazione, presente in entrambe le schermate.
 *
 * Rispetto alla versione precedente:
 *   - un gradiente verticale molto tenue che stacca la testata dalla pagina;
 *   - il logo dentro un medaglione dorato, non appoggiato sul fondo;
 *   - la firma su due righe, con "Fleet workspace" come dettaglio;
 *   - il pulsante Discord come pill con bordo oro.
 */
public final class TestataPremium extends JPanel {

    private final JLabel marchio = new JLabel("CORVETTE HUB");
    private final JLabel firma = new JLabel("NMS ITALIA   /   FLEET WORKSPACE");
    private final JLabel versione = new JLabel();
    private final JButton discord = Comunita.pulsante();

    public TestataPremium() {
        super(new BorderLayout(Scala.px(16), 0));
        setOpaque(false);
        setBorder(Scala.bordo(7, 18, 7, 18));

        // --- marchio ---
        JPanel brand = new JPanel(new BorderLayout(Scala.px(11), 0));
        brand.setOpaque(false);
        brand.add(new Medaglione(), BorderLayout.WEST);

        JPanel testi = new JPanel();
        testi.setOpaque(false);
        testi.setLayout(new BoxLayout(testi, BoxLayout.Y_AXIS));
        marchio.setFont(Scala.font(Font.PLAIN, 16));
        marchio.setForeground(Theme.TESTO);
        marchio.setAlignmentX(Component.LEFT_ALIGNMENT);
        firma.setFont(Theme.etichetta());
        firma.setForeground(Theme.TESTO_DEBOLE);
        firma.setAlignmentX(Component.LEFT_ALIGNMENT);
        testi.add(Box.createVerticalGlue());
        testi.add(marchio);
        testi.add(Box.createVerticalStrut(Scala.px(3)));
        testi.add(firma);
        testi.add(Box.createVerticalGlue());
        brand.add(testi, BorderLayout.CENTER);

        // --- azioni ---
        JPanel azioni = new JPanel(new FlowLayout(FlowLayout.RIGHT, Scala.px(10), 0));
        azioni.setOpaque(false);
        versione.setText("v" + Main.VERSIONE);
        versione.setFont(Theme.etichetta());
        versione.setForeground(Theme.TESTO_DEBOLE);
        versione.setBorder(Scala.bordo(0, 0, 0, 2));
        azioni.add(versione);
        azioni.add(discord);

        add(brand, BorderLayout.WEST);
        add(azioni, BorderLayout.EAST);
    }

    private void adatta() {
        if (discord == null) return;
        int w = getParent() == null ? getWidth() : getParent().getWidth();
        boolean compatto = w > 0 && w < Scala.px(620);
        firma.setVisible(!compatto);
        versione.setVisible(!compatto);
        String testo = compatto ? "Discord  \u2197" : "Discord NMS ITALIA  \u2197";
        if (!testo.equals(discord.getText())) discord.setText(testo);
    }

    @Override public Dimension getPreferredSize() { adatta(); return super.getPreferredSize(); }
    @Override public void doLayout() { adatta(); super.doLayout(); }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D p = (Graphics2D) g.create();
        p.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        p.setPaint(new GradientPaint(0, 0, new Color(0x121C2E), 0, getHeight(), new Color(0x0A111C)));
        p.fillRect(0, 0, getWidth(), getHeight());
        p.setColor(Theme.BORDO);
        p.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
        p.dispose();
    }

    /** Il logo dentro un cerchio d'oro: il tocco che alza il livello. */
    private static final class Medaglione extends JPanel {
        private final Icon logo = Icone.logo(Scala.px(20));

        Medaglione() {
            setOpaque(false);
            setPreferredSize(Scala.dim(32, 32));
            setMinimumSize(Scala.dim(32, 32));
            setMaximumSize(Scala.dim(32, 32));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int d = Math.min(getWidth(), getHeight());
            int x = (getWidth() - d) / 2, y = (getHeight() - d) / 2;
            p.setColor(Theme.SUPERFICIE_ALTA);
            p.fillOval(x, y, d, d);
            p.setColor(new Color(Theme.ACCENTO.getRed(), Theme.ACCENTO.getGreen(), Theme.ACCENTO.getBlue(), 150));
            p.setStroke(new BasicStroke(Scala.px(1.2f)));
            p.drawOval(x, y, d - 1, d - 1);
            if (logo != null) {
                int lw = logo.getIconWidth();
                logo.paintIcon(this, p, x + (d - lw) / 2, y + (d - logo.getIconHeight()) / 2);
            }
            p.dispose();
        }
    }
}
