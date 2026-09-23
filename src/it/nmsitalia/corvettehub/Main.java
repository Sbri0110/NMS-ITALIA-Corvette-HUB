package it.nmsitalia.corvettehub;

import it.nmsitalia.corvettehub.config.AppConfig;
import it.nmsitalia.corvettehub.ui.MainFrame;
import it.nmsitalia.corvettehub.ui.Theme;

import javax.swing.SwingUtilities;

/**
 * NMS ITALIA Corvette HUB - punto di ingresso.
 *
 * Legge i salvataggi di No Man's Sky, mostra le Corvette, i loro moduli e il
 * deposito, ed esporta e importa progetti fra giocatori. Le scritture passano
 * sempre da backup verificato, controllo del gioco chiuso e rilettura.
 */
public final class Main {

    public static final String NOME = "NMS ITALIA Corvette HUB";
    public static final String VERSIONE = "1.0.0";

    private Main() {
    }

    public static void main(String[] args) {
        AppConfig config = AppConfig.carica();
        Theme.installa();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainFrame frame = new MainFrame(config);
                frame.setVisible(true);
            }
        });
    }
}
