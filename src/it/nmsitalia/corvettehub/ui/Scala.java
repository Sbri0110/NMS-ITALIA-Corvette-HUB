package it.nmsitalia.corvettehub.ui;

import it.nmsitalia.corvettehub.config.AppConfig;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;

/**
 * La scala dell'interfaccia, per i monitor ad alta densita' (4K e simili).
 *
 * IL PROBLEMA
 * -----------
 * Tutte le misure del programma sono in pixel: font a 12 punti, pulsanti alti
 * 28, celle larghe 60. Su un monitor 4K con l'ingrandimento di Windows al 150%
 * o al 200%, la macchina virtuale Java 8 e' "consapevole del DPI": il sistema
 * NON ingrandisce la finestra, quindi Swing disegna ai pixel fisici e
 * l'interfaccia riesce minuscola e illeggibile.
 *
 * LA SOLUZIONE
 * ------------
 * Un fattore di scala unico, applicato a ogni font, a ogni misura fissa e a
 * ogni margine. Il fattore si ricava dal DPI dello schermo (vedi
 * {@link #daDpi(int)}) e si puo' forzare a mano in CorvetteHUB.conf con la
 * chiave UiScale, oppure dal selettore nella prima schermata.
 *
 * PERCHE' dpi/96 FUNZIONA IN ENTRAMBI I CASI
 * ------------------------------------------
 * Se la macchina virtuale e' consapevole del DPI, il sistema non ingrandisce
 * nulla e getScreenResolution() restituisce il DPI vero (192 al 200%): il
 * fattore diventa 2 e ci pensiamo noi. Se invece la macchina virtuale NON e'
 * consapevole, e' Windows a ingrandire l'immagine (che risulta sfocata) e
 * getScreenResolution() resta 96: il fattore e' 1 e non si somma nulla. In
 * nessuno dei due casi l'ingrandimento viene applicato due volte.
 */
public final class Scala {

    /** Valore che significa "decidi tu dal DPI". */
    public static final String AUTOMATICA = "auto";

    /**
     * I fattori fra cui si sceglie quando il DPI non e' un multiplo esatto.
     * Serve a evitare ingrandimenti strani come 156%: Windows stesso usa
     * questi valori, e cosi' l'interfaccia resta coerente col sistema.
     */
    private static final double[] FATTORI = {1.0, 1.25, 1.5, 1.75, 2.0, 2.25, 2.5, 3.0};

    private static final double MINIMO = 0.75;
    private static final double MASSIMO = 4.0;

    private static double fattore = 1.0;
    private static int dpi = 96;
    private static boolean daConfigurazione;

    private Scala() {
    }

    /**
     * Fissa la scala. Va chiamata PRIMA di creare qualunque componente e prima
     * di installare il tema: i font vengono costruiti una volta sola.
     */
    public static void inizializza(AppConfig config) {
        dpi = rilevaDpi();

        String impostato = config == null ? null : config.scalaInterfaccia();
        if (impostato != null && !AUTOMATICA.equalsIgnoreCase(impostato.trim())) {
            try {
                double f = Double.parseDouble(impostato.trim().replace(',', '.'));
                if (f >= MINIMO && f <= MASSIMO) {
                    fattore = f;
                    daConfigurazione = true;
                    return;
                }
            } catch (NumberFormatException e) {
                // valore illeggibile: si ricade sul rilevamento automatico
            }
        }
        fattore = daDpi(dpi);
        daConfigurazione = false;
    }

    /** Il DPI dichiarato dallo schermo principale. */
    public static int rilevaDpi() {
        try {
            int r = Toolkit.getDefaultToolkit().getScreenResolution();
            return r > 0 ? r : 96;
        } catch (Throwable t) {
            return 96;
        }
    }

    /** Il fattore noto piu' vicino a dpi/96. */
    public static double daDpi(int dpi) {
        double grezzo = dpi / 96.0;
        double migliore = FATTORI[0];
        double distanza = Math.abs(grezzo - migliore);
        for (int i = 1; i < FATTORI.length; i++) {
            double d = Math.abs(grezzo - FATTORI[i]);
            if (d < distanza) {
                distanza = d;
                migliore = FATTORI[i];
            }
        }
        return migliore;
    }

    public static double fattore() {
        return fattore;
    }

    public static int dpi() {
        return dpi;
    }

    /** Vero se la scala e' stata ricavata dal DPI e non imposta a mano. */
    public static boolean automatica() {
        return !daConfigurazione;
    }

    // ------------------------------------------------------------- misure

    /** Un numero di pixel, ingrandito. */
    public static int px(double valore) {
        return (int) Math.round(valore * fattore);
    }

    public static int px(int valore) {
        return px((double) valore);
    }

    // --------------------------------------------------------------- font

    /** Un font dell'interfaccia. */
    public static Font font(int stile, int dimensione) {
        return new Font("Segoe UI", stile, px(dimensione));
    }

    /** Il font a spaziatura fissa, per i riquadri di esito e i log. */
    public static Font mono(int dimensione) {
        return new Font("Consolas", Font.PLAIN, px(dimensione));
    }

    // ---------------------------------------------------------- dimensioni

    public static Dimension dim(int larghezza, int altezza) {
        return new Dimension(px(larghezza), px(altezza));
    }

    /**
     * Una misura alta quanto indicato e larga quanto serve. Serve per le combo
     * e i campi dentro un BoxLayout verticale, che altrimenti restano stretti.
     */
    public static Dimension dimLarga(int altezza) {
        return new Dimension(Integer.MAX_VALUE, px(altezza));
    }

    // -------------------------------------------------------------- bordi

    public static Border bordo(int alto, int sinistra, int basso, int destra) {
        return BorderFactory.createEmptyBorder(px(alto), px(sinistra), px(basso), px(destra));
    }

    public static Border bordo(int verticale, int orizzontale) {
        return bordo(verticale, orizzontale, verticale, orizzontale);
    }

    public static Border bordo(int tutti) {
        return bordo(tutti, tutti, tutti, tutti);
    }
}
