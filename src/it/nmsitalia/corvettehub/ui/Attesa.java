package it.nmsitalia.corvettehub.ui;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * L'indicatore di lavoro: dice che il programma sta facendo qualcosa.
 *
 * Prima non c'era nulla: quando si importava una Corvette l'interfaccia si
 * spegneva e basta, e non si capiva se stesse lavorando o se si fosse
 * bloccata. Qui ci sono due pezzi che lavorano insieme:
 *
 *   - {@link Giro}: un arco che gira, disegnato con Java2D. Nessuna
 *     immagine, nessuna dipendenza: solo un Timer da 16 ms che sposta
 *     l'angolo di partenza.
 *   - {@link Velo}: un pannello che copre la schermata con una patina scura
 *     e mette al centro una carta con il giro, il titolo del lavoro e la
 *     riga di stato che cambia mentre le fasi si susseguono.
 *
 * Il velo non blocca di suo: e' l'ascoltatore a decidere se e quando
 * mostrarlo. Serve a rendere leggibile un'attesa lunga, non a impedire
 * l'uso dell'interfaccia.
 */
public final class Attesa {

    private Attesa() { }

    // ------------------------------------------------------------------ giro

    /**
     * Un arco che gira.
     *
     * Il disegno e' volutamente semplice: un anello tenue di fondo e un arco
     * d'oro che ruota. La rotazione e' costante, senza scatti, e si ferma
     * quando il componente non e' piu' visibile: un'animazione che continua
     * a girare a vuoto consuma CPU per niente.
     */
    public static final class Giro extends JComponent {

        private final Timer orologio;
        private float angolo;
        private int diametro;
        private boolean attivo = true;

        public Giro(int diametro) {
            this.diametro = Scala.px(diametro);
            setOpaque(false);
            setPreferredSize(new Dimension(this.diametro, this.diametro));
            setMinimumSize(new Dimension(this.diametro, this.diametro));
            setMaximumSize(new Dimension(this.diametro, this.diametro));

            orologio = new Timer(16, new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    // un giro completo in circa 0,9 secondi
                    angolo = (angolo + 11f) % 360f;
                    repaint();
                }
            });
        }

        /** Avvia o ferma la rotazione. */
        public void gira(boolean acceso) {
            attivo = acceso;
            if (acceso) {
                if (!orologio.isRunning()) orologio.start();
            } else {
                orologio.stop();
            }
        }

        public void cambiaDiametro(int diametro) {
            this.diametro = Scala.px(diametro);
            setPreferredSize(new Dimension(this.diametro, this.diametro));
            setMinimumSize(new Dimension(this.diametro, this.diametro));
            setMaximumSize(new Dimension(this.diametro, this.diametro));
            revalidate();
            repaint();
        }

        @Override public void addNotify() {
            super.addNotify();
            if (attivo && !orologio.isRunning()) orologio.start();
        }

        @Override public void removeNotify() {
            orologio.stop();
            super.removeNotify();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int d = Math.min(getWidth(), getHeight()) - Scala.px(2);
            int x = (getWidth() - d) / 2;
            int y = (getHeight() - d) / 2;
            float spessore = Math.max(2f, Scala.px(2.2f));

            // l'anello di fondo: dice che il cerchio e' li'
            p.setStroke(new BasicStroke(spessore, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            p.setColor(new Color(Theme.BORDO.getRed(), Theme.BORDO.getGreen(), Theme.BORDO.getBlue(), 130));
            p.drawArc(x, y, d, d, 0, 360);

            // l'arco d'oro che gira
            p.setColor(Theme.ACCENTO);
            p.drawArc(x, y, d, d, Math.round(angolo), -110);

            // un secondo arco corto, piu' tenue: da' il senso di velocita'
            p.setColor(new Color(Theme.ACCENTO_CHIARO.getRed(), Theme.ACCENTO_CHIARO.getGreen(),
                    Theme.ACCENTO_CHIARO.getBlue(), 150));
            p.drawArc(x, y, d, d, Math.round(angolo + 190), -34);

            p.dispose();
        }
    }

    /**
     * Una barra indeterminata: un segmento d'oro che scorre da sinistra a
     * destra. Non dice quanto manca (non lo sappiamo), dice che qualcosa si
     * sta muovendo.
     */
    public static final class Barra extends JComponent {

        private final Timer orologio;
        private float posizione;
        private boolean attivo = true;

        public Barra() {
            setOpaque(false);
            setPreferredSize(new Dimension(10, Scala.px(3)));
            setMinimumSize(new Dimension(10, Scala.px(3)));
            orologio = new Timer(16, new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    posizione += 0.014f;
                    if (posizione > 1.35f) posizione = -0.35f;
                    repaint();
                }
            });
        }

        public void scorri(boolean acceso) {
            attivo = acceso;
            if (acceso) {
                if (!orologio.isRunning()) orologio.start();
            } else {
                orologio.stop();
            }
        }

        @Override public void addNotify() {
            super.addNotify();
            if (attivo && !orologio.isRunning()) orologio.start();
        }

        @Override public void removeNotify() {
            orologio.stop();
            super.removeNotify();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = Math.max(2, getHeight());
            int r = h;
            p.setColor(Theme.BORDO);
            p.fillRoundRect(0, 0, getWidth(), h, r, r);
            int larghezza = Math.max(Scala.px(70), getWidth() / 3);
            int x = Math.round(posizione * (getWidth() + larghezza)) - larghezza;
            p.setColor(Theme.ACCENTO);
            p.fillRoundRect(x, 0, larghezza, h, r, r);
            p.dispose();
        }
    }

    // ------------------------------------------------------------------ velo

    /**
     * Il velo: copre la schermata durante un lavoro lungo.
     *
     * Sta sopra tutto con un fondo semi-trasparente, cosi' il contesto resta
     * visibile ma spento. Al centro una carta con il giro, il titolo e la
     * fase corrente. Il metodo {@link #fase(String)} cambia la riga di stato
     * mentre il lavoro avanza.
     */
    public static final class Velo extends JPanel {

        private final Giro giro = new Giro(42);
        private final Barra barra = new Barra();
        private final JLabel titolo = new JLabel("Operazione in corso");
        private final JLabel fase = new JLabel(" ");
        private final Theme.Carta carta = new Theme.Carta();

        public Velo() {
            setOpaque(false);
            setLayout(new BorderLayout());
            setVisible(false);

            titolo.setFont(Theme.titoloCarta());
            titolo.setForeground(Theme.TESTO);
            titolo.setHorizontalAlignment(SwingConstants.CENTER);

            fase.setFont(Theme.corpo());
            fase.setForeground(Theme.TESTO_TENUE);
            fase.setHorizontalAlignment(SwingConstants.CENTER);

            // la barra indeterminata: un segmento d'oro che va e viene
            barra.setPreferredSize(new Dimension(10, Scala.px(3)));

            carta.riempi(Theme.SUPERFICIE_ALTA).raggio(Scala.px(14));
            carta.setLayout(new BorderLayout(0, Scala.px(2)));
            carta.setBorder(Scala.bordo(24, 34, 22, 34));

            JPanel centro = new JPanel(new BorderLayout(0, Scala.px(10)));
            centro.setOpaque(false);
            centro.add(giro, BorderLayout.NORTH);

            JPanel testi = new JPanel(new BorderLayout(0, Scala.px(6)));
            testi.setOpaque(false);
            testi.add(titolo, BorderLayout.NORTH);
            testi.add(fase, BorderLayout.CENTER);

            centro.add(testi, BorderLayout.CENTER);
            carta.add(centro, BorderLayout.CENTER);
            carta.add(barra, BorderLayout.SOUTH);

            JPanel mezzo = new JPanel(new BorderLayout());
            mezzo.setOpaque(false);
            mezzo.setBorder(Scala.bordo(0, 40, 0, 40));
            mezzo.add(carta, BorderLayout.NORTH);

            add(mezzo, BorderLayout.NORTH);
        }

        /** Accende o spegne il velo. */
        public void mostra(boolean acceso) {
            setVisible(acceso);
            giro.gira(acceso);
            barra.scorri(acceso);
            if (acceso) {
                // Il velo deve stare sopra il contenuto, non sopra se stesso:
                // il contenitore usa OverlayLayout, dove l'ultimo figlio
                // aggiunto e' quello disegnato per primo. Qui lo riportiamo
                // in fondo all'ordine dei figli del padre.
                java.awt.Container padre = getParent();
                if (padre != null && padre.getComponentCount() > 1) {
                    padre.setComponentZOrder(this, padre.getComponentCount() - 1);
                }
                revalidate();
                repaint();
            }
        }

        public void titolo(String t) {
            titolo.setText(t);
        }

        public void fase(String t) {
            fase.setText(t == null || t.isEmpty() ? " " : t);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // patina scura: il contesto resta leggibile ma spento
            p.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.62f));
            p.setColor(Theme.ABISSO);
            p.fillRect(0, 0, getWidth(), getHeight());
            p.dispose();
            super.paintComponent(g);
        }
    }
}
