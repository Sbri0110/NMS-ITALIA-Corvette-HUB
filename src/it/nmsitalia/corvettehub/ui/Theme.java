package it.nmsitalia.corvettehub.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;

/**
 * Tema dell'applicazione.
 *
 * Regola della specifica (7.7): base scura, UN SOLO colore d'accento
 * (ambra/arancione No Man's Sky), piu' verde per le conferme e rosso per gli
 * errori. Nessun effetto gratuito, nessun gradiente.
 */
public final class Theme {

    // Base scura: superfici piatte, tre livelli.
    public static final Color SFONDO = new Color(0x1B1B1F);
    public static final Color SUPERFICIE = new Color(0x24242A);
    public static final Color SUPERFICIE_ALTA = new Color(0x2E2E36);
    public static final Color BORDO = new Color(0x3A3A44);

    // Testo
    public static final Color TESTO = new Color(0xE8E6E3);
    public static final Color TESTO_TENUE = new Color(0x9A97A0);

    // Accento unico: ambra NMS
    public static final Color ACCENTO = new Color(0xE8A33D);
    public static final Color ACCENTO_SCURO = new Color(0x8A6224);

    // Semantici
    public static final Color OK = new Color(0x5FBF7F);
    public static final Color ERRORE = new Color(0xE05A5A);
    public static final Color AVVISO = new Color(0xD8A93A);

    /** Sfondo attenuato per le barre di avviso. */
    public static final Color AVVISO_SCURO = new Color(0x3A, 0x30, 0x18);

    private Theme() {
    }

    public static void installa() {
        FlatDarkLaf.setup();

        // Il font di base passa dalla scala: su un monitor 4K diventa grande il
        // doppio, e con lui crescono anche le metriche che FlatLaf calcola dai
        // font (altezza delle righe, spaziature interne).
        Font base = Scala.font(Font.PLAIN, 13);
        UIManager.put("defaultFont", base);

        UIManager.put("Panel.background", SFONDO);
        UIManager.put("Component.background", SUPERFICIE);
        UIManager.put("Component.foreground", TESTO);
        UIManager.put("Label.foreground", TESTO);
        UIManager.put("Component.borderColor", BORDO);
        UIManager.put("Component.focusColor", ACCENTO);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.arc", Scala.px(8));
        UIManager.put("Button.arc", Scala.px(8));
        UIManager.put("TextComponent.arc", Scala.px(8));
        // Le metriche di FlatLaf sono in pixel e non seguono il font: qui
        // seguono la scala, altrimenti su 4K resterebbero fili sottili.
        UIManager.put("ScrollBar.width", Scala.px(14));
        UIManager.put("ScrollBar.thumbArc", Scala.px(999));
        UIManager.put("ScrollBar.trackArc", Scala.px(999));

        UIManager.put("List.background", SUPERFICIE);
        UIManager.put("List.foreground", TESTO);
        UIManager.put("List.selectionBackground", ACCENTO_SCURO);
        UIManager.put("List.selectionForeground", Color.WHITE);

        UIManager.put("Button.background", SUPERFICIE_ALTA);
        UIManager.put("Button.foreground", TESTO);
        UIManager.put("Button.hoverBackground", BORDO);

        UIManager.put("ScrollPane.background", SFONDO);
        UIManager.put("SplitPane.background", SFONDO);
        UIManager.put("SplitPaneDivider.style", "plain");
        UIManager.put("TabbedPane.background", SFONDO);
        UIManager.put("TabbedPane.selectedBackground", SUPERFICIE_ALTA);
        UIManager.put("TabbedPane.underlineColor", ACCENTO);
        UIManager.put("TabbedPane.focusColor", ACCENTO);

        FlatLaf.updateUI();
    }

    public static Font titolo() {
        return Scala.font(Font.BOLD, 15);
    }

    public static Font sezione() {
        return Scala.font(Font.BOLD, 12);
    }

    public static Font monospazio() {
        return Scala.mono(12);
    }
}
