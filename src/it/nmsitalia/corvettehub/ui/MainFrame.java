package it.nmsitalia.corvettehub.ui;

import it.nmsitalia.corvettehub.Main;
import it.nmsitalia.corvettehub.config.AppConfig;
import it.nmsitalia.corvettehub.detect.SaveLocator;
import it.nmsitalia.corvettehub.domain.SaveSlotInfo;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;

/**
 * La finestra dell'applicazione.
 *
 * Due schermate, non una sola:
 *
 *   1. "salvataggi"  la scelta del salvataggio, senza nulla sulle Corvette;
 *   2. "corvette"    l'hub della Corvette, con le cinque schede.
 *
 * La separazione serve a non mescolare due momenti diversi: prima si decide
 * su quale partita si lavora, poi si lavora.
 */
public final class MainFrame extends JFrame {

    private static final String CARTE_SALVATAGGI = "salvataggi";
    private static final String CARTE_CORVETTE = "corvette";

    private final CardLayout carte = new CardLayout();
    private final JPanel contenitore = new JPanel(carte);

    private final SchermataSalvataggi schermataSalvataggi;
    private final SchermataCorvette schermataCorvette;

    private SaveLocator.Rilevamento ultimoRilevamento;
    private SaveSlotInfo ultimoSlot;

    public MainFrame(AppConfig config) {
        super(Main.NOME + " " + Main.VERSIONE);

        File programma = AppConfig.cartellaProgramma();
        File libreria = new File(programma, "Builds");

        SaveLocator.preparaAmbiente(programma);

        schermataSalvataggi = new SchermataSalvataggi(config,
                new SchermataSalvataggi.Ascoltatore() {
                    @Override
                    public void salvataggioScelto(SaveLocator.Rilevamento rilevamento,
                                                  SaveSlotInfo slot) {
                        ultimoRilevamento = rilevamento;
                        ultimoSlot = slot;
                        mostraCorvette();
                    }
                });

        schermataCorvette = new SchermataCorvette(libreria,
                new SchermataCorvette.Ascoltatore() {
                    @Override
                    public void cambiaSalvataggio() {
                        mostraSalvataggi();
                    }
                });

        contenitore.add(schermataSalvataggi, CARTE_SALVATAGGI);
        contenitore.add(schermataCorvette, CARTE_CORVETTE);
        contenitore.setBackground(Theme.SFONDO);

        setContentPane(contenitore);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        applicaIcona();

        mostraSalvataggi();

        // La finestra si ridimensiona trascinando i bordi, come qualunque altro
        // programma: niente dimensione preferita e niente pack(), che la
        // bloccherebbe sulla misura del contenuto.
        //
        // Misura di partenza e minimo stanno dentro lo schermo: su un 4K con la
        // scala al 200% una finestra da 1280 punti diventerebbe 2560 pixel e
        // uscirebbe dallo schermo.
        Dimension schermo = Toolkit.getDefaultToolkit().getScreenSize();
        setMinimumSize(new Dimension(
                Math.min(Scala.px(880), schermo.width - Scala.px(80)),
                Math.min(Scala.px(560), schermo.height - Scala.px(120))));
        setSize(
                Math.min(Scala.px(1280), schermo.width - Scala.px(60)),
                Math.min(Scala.px(820), schermo.height - Scala.px(120)));
        setLocationRelativeTo(null);
    }

    /** L'icona della finestra e della barra delle applicazioni. */
    private void applicaIcona() {
        try {
            java.util.List<java.awt.Image> immagini = new java.util.ArrayList<java.awt.Image>();
            String[] misure = {"256", "128", "64", "48", "32", "16"};
            for (int i = 0; i < misure.length; i++) {
                java.net.URL u = MainFrame.class.getResource("/res/icona-" + misure[i] + ".png");
                if (u != null) {
                    immagini.add(new javax.swing.ImageIcon(u).getImage());
                }
            }
            if (!immagini.isEmpty()) {
                setIconImages(immagini);
            }
        } catch (Throwable t) {
            // senza icona il programma funziona lo stesso
        }
    }

    private void mostraSalvataggi() {
        carte.show(contenitore, CARTE_SALVATAGGI);
        setTitle(Main.NOME + " " + Main.VERSIONE + "  ·  scelta del salvataggio");
    }

    private void mostraCorvette() {
        carte.show(contenitore, CARTE_CORVETTE);
        if (ultimoRilevamento != null && ultimoSlot != null) {
            setTitle(Main.NOME + "  ·  Slot " + ultimoSlot.getNumero()
                    + " · " + ultimoSlot.getModalita());
            schermataCorvette.aggiorna(ultimoRilevamento, ultimoSlot);
        }
    }
}
