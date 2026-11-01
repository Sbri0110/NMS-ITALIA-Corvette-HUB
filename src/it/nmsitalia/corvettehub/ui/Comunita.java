package it.nmsitalia.corvettehub.ui;

import java.awt.Desktop;
import java.net.URI;
import javax.swing.*;

/** Invito condiviso. Connessione solo quando l'utente preme il pulsante. */
public final class Comunita {
    public static final String DISCORD = "https://discord.gg/uvDTR3wRMg";
    /** Il nome del server Discord della community. */
    public static final String NOME_SERVER = "[AGIT] Alleanza Galattica Italiana";
    private Comunita() { }
    public static JButton pulsante() {
        JButton b = new JButton(NOME_SERVER + "  \u2197", new Segno("discord"));
        b.setToolTipText("Apri il server Discord " + NOME_SERVER + " nel browser");
        b.setForeground(Theme.TESTO); b.setBackground(new java.awt.Color(0x2A315C));
        b.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        b.addActionListener(e -> {
            try {
                if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    throw new IllegalStateException();
                }
                Desktop.getDesktop().browse(URI.create(DISCORD));
            } catch (Exception ex) {
                JTextField indirizzo = new JTextField(DISCORD, 32);
                indirizzo.setEditable(false); indirizzo.selectAll();
                JOptionPane.showMessageDialog(b, new Object[]{"Apri questo invito nel browser:", indirizzo},
                        NOME_SERVER, JOptionPane.INFORMATION_MESSAGE);
            }
        });
        return b;
    }
}
