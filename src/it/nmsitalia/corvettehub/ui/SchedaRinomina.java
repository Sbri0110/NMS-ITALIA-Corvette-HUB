package it.nmsitalia.corvettehub.ui;

import it.nmsitalia.corvettehub.detect.SaveLocator;
import it.nmsitalia.corvettehub.domain.Corvette;
import it.nmsitalia.corvettehub.domain.LettoreCorvette;
import it.nmsitalia.corvettehub.domain.SaveSlotInfo;
import it.nmsitalia.corvettehub.safety.ScrittoreSalvataggio;
import nomanssave.eV;
import nomanssave.eY;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Scheda Rinomina: la prima operazione che SCRIVE sul salvataggio.
 *
 * E' la scrittura piu' semplice del tool ed e' per questo che viene per prima:
 * serve da collaudo reale del ciclo backup -> scrittura -> verifica, che poi
 * l'import riusera'.
 *
 * Regole applicate:
 *   - la Corvette in uso non compare nell'elenco (il gioco la ricarica in
 *     memoria e sovrascriverebbe la modifica);
 *   - il gioco deve essere chiuso;
 *   - backup verificato prima di scrivere, altrimenti ci si ferma;
 *   - si scrivono ENTRAMBI i file dello slot;
 *   - rilettura di conferma, e ripristino automatico se qualcosa non torna.
 */
public final class SchedaRinomina extends JPanel {

    private static final int LUNGHEZZA_MASSIMA = 64;

    private final JLabel intestazione = new JLabel("Rinomina una Corvette");
    private final JComboBox<String> sceltaCorvette = new JComboBox<String>();
    private final JLabel nomeAttuale = new JLabel("—");
    private final JTextField nomeNuovo = new JTextField(28);
    private final JLabel anteprima = new JLabel(" ");
    private final JTextArea esito = new JTextArea();
    private final JButton rinomina = new JButton("Rinomina");

    private SaveLocator.Rilevamento rilevamento;
    private SaveSlotInfo slot;
    private LettoreCorvette.Esito corrente;

    public SchedaRinomina() {
        setBackground(Theme.SFONDO);
        setLayout(new BorderLayout());
        add(costruisciSinistra(), BorderLayout.WEST);
        add(costruisciDestra(), BorderLayout.CENTER);
        add(costruisciPiede(), BorderLayout.SOUTH);
        aggiorna(null, null, null);
    }

    private JPanel costruisciSinistra() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(Scala.bordo(16, 16, 16, 12));
        p.setPreferredSize(Scala.dim(400, 100));

        intestazione.setFont(Scala.font(Font.BOLD, 14));
        intestazione.setForeground(Theme.TESTO);
        intestazione.setAlignmentX(Component.LEFT_ALIGNMENT);

        sceltaCorvette.setAlignmentX(Component.LEFT_ALIGNMENT);
        sceltaCorvette.setMaximumSize(Scala.dimLarga(28));
        sceltaCorvette.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                corvetteScelta();
            }
        });

        nomeAttuale.setFont(Scala.font(Font.BOLD, 13));
        nomeAttuale.setForeground(Theme.ACCENTO);
        nomeAttuale.setAlignmentX(Component.LEFT_ALIGNMENT);

        nomeNuovo.setAlignmentX(Component.LEFT_ALIGNMENT);
        nomeNuovo.setMaximumSize(Scala.dimLarga(28));
        nomeNuovo.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                aggiornaAnteprima();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                aggiornaAnteprima();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                aggiornaAnteprima();
            }
        });

        anteprima.setFont(Scala.font(Font.PLAIN, 12));
        anteprima.setForeground(Theme.TESTO_TENUE);
        anteprima.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(intestazione);
        p.add(Box.createVerticalStrut(14));
        p.add(etichetta("Corvette"));
        p.add(Box.createVerticalStrut(4));
        p.add(sceltaCorvette);
        p.add(Box.createVerticalStrut(16));
        p.add(etichetta("Nome attuale"));
        p.add(Box.createVerticalStrut(4));
        p.add(nomeAttuale);
        p.add(Box.createVerticalStrut(16));
        p.add(etichetta("Nuovo nome"));
        p.add(Box.createVerticalStrut(4));
        p.add(nomeNuovo);
        p.add(Box.createVerticalStrut(10));
        p.add(anteprima);
        p.add(Box.createVerticalGlue());

        JLabel nota = new JLabel("<html><body style='width:350px'>"
                + "Prima di scrivere il tool fa una copia di sicurezza verificata di "
                + "<b>entrambi</b> i file dello slot, poi rilegge per confermare. "
                + "Se qualcosa non torna, rimette a posto da solo.</body></html>");
        nota.setForeground(Theme.TESTO_TENUE);
        nota.setFont(Scala.font(Font.PLAIN, 11));
        nota.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(nota);
        return p;
    }

    private Component costruisciDestra() {
        esito.setEditable(false);
        esito.setBackground(Theme.SUPERFICIE);
        esito.setForeground(Theme.TESTO);
        esito.setFont(Theme.monospazio());
        esito.setBorder(Scala.bordo(10, 12, 10, 12));

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(Scala.bordo(16, 0, 0, 16));

        JLabel t = new JLabel("Esito");
        t.setFont(Theme.sezione());
        t.setForeground(Theme.TESTO_TENUE);
        t.setBorder(Scala.bordo(0, 0, 6, 0));

        p.add(t, BorderLayout.NORTH);
        p.add(new JScrollPane(esito), BorderLayout.CENTER);
        return p;
    }

    private JPanel costruisciPiede() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        p.setOpaque(false);
        p.setBorder(Scala.bordo(8, 16, 14, 16));
        rinomina.setFont(Scala.font(Font.BOLD, 13));
        rinomina.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rinominaOra();
            }
        });
        p.add(rinomina);
        return p;
    }

    private JLabel etichetta(String testo) {
        JLabel l = new JLabel(testo);
        l.setForeground(Theme.TESTO_TENUE);
        l.setFont(Scala.font(Font.PLAIN, 12));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    // ---------------------------------------------------------------- dati

    public void aggiorna(SaveLocator.Rilevamento rilevamento, SaveSlotInfo slot,
                         LettoreCorvette.Esito esitoSlot) {
        this.rilevamento = rilevamento;
        this.slot = slot;
        this.corrente = esitoSlot;

        sceltaCorvette.removeAllItems();
        if (esitoSlot == null || esitoSlot.corvette.isEmpty()) {
            sceltaCorvette.addItem("(nessuna Corvette in questo slot)");
            rinomina.setEnabled(false);
            nomeAttuale.setText("—");
            esito.setText("Questo slot non contiene Corvette da rinominare.\n\n"
                    + "Le Corvette sono basi di tipo PlayerShipBase: se non ne compare\n"
                    + "nessuna, in questo salvataggio non ne hai ancora una.");
            return;
        }
        for (int i = 0; i < esitoSlot.corvette.size(); i++) {
            Corvette c = esitoSlot.corvette.get(i);
            sceltaCorvette.addItem((c.isAttiva() ? "\u26A0 " : "")
                    + c.getNome() + "   (" + c.getNumeroModuli() + " moduli)");
        }
        sceltaCorvette.setSelectedIndex(0);
        rinomina.setEnabled(true);
    }

    private Corvette corvetteCorrente() {
        int i = sceltaCorvette.getSelectedIndex();
        if (corrente == null || i < 0 || i >= corrente.corvette.size()) {
            return null;
        }
        return corrente.corvette.get(i);
    }

    private void corvetteScelta() {
        Corvette c = corvetteCorrente();
        if (c == null) {
            return;
        }
        nomeAttuale.setText(c.getNome());
        nomeNuovo.setText(c.getNome());
        StringBuilder b = new StringBuilder();
        b.append("CORVETTE\n\n");
        b.append("Nome          : ").append(c.getNome()).append('\n');
        b.append("Moduli        : ").append(c.getNumeroModuli()).append('\n');
        b.append("Nave collegata: indice ").append(c.getIndiceNave()).append('\n');
        if (c.isAttiva()) {
            b.append("\nATTENZIONE: questa e' la Corvette che stai usando in gioco.\n");
            b.append("Il gioco la ricarica in memoria e potrebbe sovrascrivere la\n");
            b.append("modifica. Scegline un'altra.\n");
            rinomina.setEnabled(false);
        } else {
            rinomina.setEnabled(true);
        }
        esito.setText(b.toString());
        esito.setCaretPosition(0);
        aggiornaAnteprima();
    }

    private void aggiornaAnteprima() {
        Corvette c = corvetteCorrente();
        if (c == null) {
            anteprima.setText(" ");
            return;
        }
        String nuovo = nomeNuovo.getText();
        String problema = valida(nuovo);
        if (problema != null) {
            anteprima.setText("⚠ " + problema);
            anteprima.setForeground(Theme.AVVISO);
            return;
        }
        if (nuovo.equals(c.getNome())) {
            anteprima.setText("Il nome non e' cambiato.");
            anteprima.setForeground(Theme.TESTO_TENUE);
        } else {
            anteprima.setText("\"" + c.getNome() + "\"  \u2192  \"" + nuovo + "\"");
            anteprima.setForeground(Theme.OK);
        }
    }

    /** Controlli sul nome. Restituisce il problema, o null se va bene. */
    private static String valida(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            return "Il nome non puo' essere vuoto.";
        }
        if (nome.length() > LUNGHEZZA_MASSIMA) {
            return "Il nome supera i " + LUNGHEZZA_MASSIMA + " caratteri.";
        }
        for (int i = 0; i < nome.length(); i++) {
            char ch = nome.charAt(i);
            if (ch < 0x20 || ch == 0x7F) {
                return "Il nome contiene un carattere di controllo.";
            }
        }
        boolean soloSpazi = true;
        for (int i = 0; i < nome.length(); i++) {
            if (nome.charAt(i) != ' ') {
                soloSpazi = false;
                break;
            }
        }
        if (soloSpazi) {
            return "Il nome e' fatto solo di spazi.";
        }
        return null;
    }

    // -------------------------------------------------------------- azione

    private void rinominaOra() {
        Corvette c = corvetteCorrente();
        if (c == null || rilevamento == null || slot == null) {
            return;
        }
        final String nuovo = nomeNuovo.getText();
        String problema = valida(nuovo);
        if (problema != null) {
            JOptionPane.showMessageDialog(this, problema, "Nome non valido",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (nuovo.equals(c.getNome())) {
            JOptionPane.showMessageDialog(this, "Il nome non e' cambiato.",
                    "Nessuna modifica", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (ScrittoreSalvataggio.giocoInEsecuzione()) {
            JOptionPane.showMessageDialog(this,
                    "No Man's Sky e' in esecuzione.\n\nChiudi il gioco prima di "
                            + "rinominare, altrimenti il gioco potrebbe sovrascrivere "
                            + "la modifica.",
                    "Gioco aperto", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int r = JOptionPane.showConfirmDialog(this,
                "Sto per rinominare una Corvette nel tuo salvataggio.\n\n"
                        + "Nome attuale:  " + c.getNome() + "\n"
                        + "Nuovo nome:    " + nuovo + "\n\n"
                        + "Verranno scritti ENTRAMBI i file dello slot, dopo una copia di "
                        + "sicurezza verificata.\n\nProcedo?",
                "Conferma rinomina", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }

        final int indiceBase = c.getIndiceBase();
        final int indiceNave = c.getIndiceNave();
        rinomina.setEnabled(false);
        esito.setText("Scrivo...\n\nQuesta operazione richiede qualche secondo: "
                + "converte il salvataggio, fa il backup, riscrive e rilegge.");
        esito.setCaretPosition(0);

        final SaveLocator.Rilevamento rl = rilevamento;
        final SaveSlotInfo sl = slot;

        new javax.swing.SwingWorker<ScrittoreSalvataggio.Esito, Void>() {
            @Override
            protected ScrittoreSalvataggio.Esito doInBackground() {
                return ScrittoreSalvataggio.scrivi(rl, sl,
                        new ScrittoreSalvataggio.Modifica() {
                            @Override
                            public String descrizione() {
                                return "rinomina della Corvette in \"" + nuovo + "\"";
                            }

                            @Override
                            public void applica(eY radice) {
                                eY stato = radice.H("PlayerStateData");
                                eV basi = stato.d("PersistentPlayerBases");
                                eY base = basi.V(indiceBase);
                                base.b("Name", nuovo);
                                if (indiceNave >= 0) {
                                    eV navi = stato.d("ShipOwnership");
                                    if (indiceNave < navi.size()) {
                                        navi.V(indiceNave).b("Name", nuovo);
                                    }
                                }
                            }
                        });
            }

            @Override
            protected void done() {
                rinomina.setEnabled(true);
                try {
                    ScrittoreSalvataggio.Esito e = get();
                    StringBuilder b = new StringBuilder();
                    if (e.riuscito) {
                        b.append("RINOMINA RIUSCITA\n\n");
                    } else {
                        b.append("RINOMINA NON RIUSCITA\n\n");
                    }
                    b.append(e.messaggio).append("\n\n");
                    if (e.dettaglio != null) {
                        b.append(e.dettaglio);
                    }
                    esito.setText(b.toString());
                    esito.setCaretPosition(0);
                    if (!e.riuscito) {
                        JOptionPane.showMessageDialog(SchedaRinomina.this,
                                e.messaggio, "Non riuscito", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    esito.setText("Errore inatteso:\n" + ex);
                }
            }
        }.execute();
    }

    /** Chiamato dalla schermata quando cambia lo slot. */
    public void setContesto(SaveLocator.Rilevamento r, SaveSlotInfo slot,
                            LettoreCorvette.Esito esito) {
        aggiorna(r, slot, esito);
    }

    public File getBackupPiuRecente() {
        java.util.List<File> b = ScrittoreSalvataggio.elencoBackup();
        return b.isEmpty() ? null : b.get(0);
    }
}
