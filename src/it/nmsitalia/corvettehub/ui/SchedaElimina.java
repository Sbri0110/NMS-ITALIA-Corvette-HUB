package it.nmsitalia.corvettehub.ui;

import it.nmsitalia.corvettehub.detect.SaveLocator;
import it.nmsitalia.corvettehub.domain.Corvette;
import it.nmsitalia.corvettehub.domain.EliminazioneCorvette;
import it.nmsitalia.corvettehub.domain.LettoreCorvette;
import it.nmsitalia.corvettehub.domain.SaveSlotInfo;
import it.nmsitalia.corvettehub.domain.WrapperBuild;
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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Scheda Elimina: toglie una Corvette dal salvataggio, restituendo i moduli.
 *
 * E' l'operazione piu' distruttiva del tool, e per questo la piu' protetta:
 *
 *   1. la Corvette in uso non si tocca (il gioco la tiene in memoria);
 *   2. il gioco deve essere chiuso;
 *   3. i moduli tornano nel deposito della Stazione, e se non c'e' posto NON
 *      si procede: il deposito ha una capienza fissa e non va allargata;
 *   4. prima di scrivere, la Corvette viene esportata in Builds/: anche se
 *      tutto il resto andasse storto, il progetto e' al sicuro;
 *   5. per confermare bisogna scrivere il nome della Corvette: un "si" di
 *      riflesso non basta per un'operazione che non si annulla;
 *   6. backup verificato, entrambi i file dello slot, rilettura di controllo.
 */
public final class SchedaElimina extends JPanel {

    /** Chi vuole sapere che il salvataggio e' cambiato. */
    public interface Ascoltatore {
        void eliminazioneEseguita();
    }

    private final File cartellaLibreria;
    private final Ascoltatore ascoltatore;

    private final JComboBox<String> sceltaCorvette = new JComboBox<String>();
    private final JTextArea riepilogo = new JTextArea();
    private final JTextArea esito = new JTextArea();
    private final JButton elimina = new JButton("Elimina la Corvette...");
    private final JLabel avviso = new JLabel(" ");

    private SaveLocator.Rilevamento rilevamento;
    private SaveSlotInfo slot;
    private LettoreCorvette.Esito corrente;
    private EliminazioneCorvette.Piano piano;

    public SchedaElimina(File cartellaLibreria, Ascoltatore ascoltatore) {
        this.cartellaLibreria = cartellaLibreria;
        this.ascoltatore = ascoltatore;

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
        p.setPreferredSize(Scala.dim(460, 100));

        JLabel intestazione = new JLabel("Elimina una Corvette");
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

        riepilogo.setEditable(false);
        riepilogo.setBackground(Theme.SUPERFICIE);
        riepilogo.setForeground(Theme.TESTO);
        riepilogo.setFont(Theme.monospazio());
        riepilogo.setBorder(Scala.bordo(10, 12, 10, 12));
        riepilogo.setAlignmentX(Component.LEFT_ALIGNMENT);

        avviso.setFont(Scala.font(Font.PLAIN, 11));
        avviso.setForeground(Theme.TESTO_TENUE);
        avviso.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(intestazione);
        p.add(Box.createVerticalStrut(14));
        p.add(etichetta("Corvette"));
        p.add(Box.createVerticalStrut(4));
        p.add(sceltaCorvette);
        p.add(Box.createVerticalStrut(14));
        p.add(etichetta("Cosa succede"));
        p.add(Box.createVerticalStrut(6));
        p.add(riepilogo);
        p.add(Box.createVerticalStrut(10));
        p.add(avviso);
        p.add(Box.createVerticalGlue());

        JLabel nota = new JLabel("<html><body style='width:410px'>"
                + "I moduli della Corvette tornano nel <b>deposito della Stazione "
                + "Spaziale</b>. Le decorazioni che non sono moduli da Corvette "
                + "(luci, corridoi, porte) spariscono con la nave, come quando si "
                + "elimina una base nel gioco.<br><br>"
                + "Prima di scrivere, la Corvette viene <b>esportata in Builds/</b>: "
                + "il progetto resta recuperabile anche dopo l'eliminazione.</body></html>");
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

        elimina.setFont(Scala.font(Font.BOLD, 13));
        elimina.setForeground(Color.WHITE);
        elimina.putClientProperty("JButton.backgroundColor", Theme.ERRORE);
        elimina.putClientProperty("JButton.hoverBackground", Theme.ERRORE.brighter());
        elimina.putClientProperty("JButton.focusedBackground", Theme.ERRORE);
        elimina.setToolTipText("Rimuove la Corvette dal salvataggio e restituisce "
                + "i moduli al deposito della Stazione");
        elimina.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                eliminaOra();
            }
        });

        p.add(elimina);
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
            elimina.setEnabled(false);
            riepilogo.setText("");
            avviso.setText(" ");
            esito.setText("Questo slot non contiene Corvette da eliminare.\n\n"
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
        corvetteScelta();
    }

    private Corvette corvetteCorrente() {
        int i = sceltaCorvette.getSelectedIndex();
        if (corrente == null || i < 0 || i >= corrente.corvette.size()) {
            return null;
        }
        return corrente.corvette.get(i);
    }

    /** Ricalcola il piano e aggiorna quello che si vede. */
    private void corvetteScelta() {
        Corvette c = corvetteCorrente();
        if (c == null || slot == null) {
            piano = null;
            elimina.setEnabled(false);
            riepilogo.setText("");
            avviso.setText(" ");
            return;
        }

        piano = EliminazioneCorvette.pianifica(slot.getModello(), c);
        riepilogo.setText(piano.riepilogo());
        riepilogo.setCaretPosition(0);

        if (!piano.possibile) {
            elimina.setEnabled(false);
            avviso.setText("Non si puo' procedere: vedi il messaggio a destra.");
            avviso.setForeground(Theme.AVVISO);
            esito.setText("NON SI PUO' ELIMINARE QUESTA CORVETTE\n\n" + piano.motivo + "\n");
            esito.setCaretPosition(0);
            return;
        }

        elimina.setEnabled(true);
        StringBuilder b = new StringBuilder();
        b.append("PRONTO PER L'ELIMINAZIONE\n\n");
        b.append("La Corvette \"").append(c.getNome()).append("\" verra' rimossa dal\n");
        b.append("salvataggio, dopo che i moduli saranno passati al deposito.\n\n");
        if (piano.tipiDecorazioni > 0) {
            b.append("Attenzione: ").append(piano.decorazioni).append(" pezzi di decorazione\n");
            b.append("(").append(piano.tipiDecorazioni).append(" tipi, luci e simili) NON sono moduli\n");
            b.append("da Corvette e spariranno con la nave.\n\n");
        }
        b.append("Premi il pulsante rosso per procedere. Ti verra' chiesto di\n");
        b.append("scrivere il nome della Corvette per confermare.\n");
        esito.setText(b.toString());
        esito.setCaretPosition(0);

        avviso.setText("L'operazione non si annulla: il backup resta in Backup/.");
        avviso.setForeground(Theme.TESTO_TENUE);
    }

    // -------------------------------------------------------------- azione

    private void eliminaOra() {
        final Corvette c = corvetteCorrente();
        if (c == null || rilevamento == null || slot == null || piano == null) {
            return;
        }
        if (!piano.possibile) {
            JOptionPane.showMessageDialog(this, piano.motivo, "Non si puo' procedere",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (ScrittoreSalvataggio.giocoInEsecuzione()) {
            JOptionPane.showMessageDialog(this,
                    "No Man's Sky e' in esecuzione.\n\nChiudi il gioco prima di eliminare "
                            + "la Corvette, altrimenti il gioco potrebbe sovrascrivere "
                            + "la modifica.",
                    "Gioco aperto", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // --- la conferma: va scritto il nome, un "si" distratto non basta
        String richiesto = c.getNome();
        String scritto = JOptionPane.showInputDialog(this,
                "Sto per ELIMINARE questa Corvette dal salvataggio:\n\n"
                        + "    " + richiesto + "\n\n"
                        + "I moduli torneranno nel deposito della Stazione.\n"
                        + "La Corvette, e le decorazioni che non sono moduli, spariranno.\n\n"
                        + "Per confermare scrivi il nome della Corvette:",
                "Conferma eliminazione", JOptionPane.WARNING_MESSAGE);
        if (scritto == null) {
            return;
        }
        if (!scritto.trim().equals(richiesto)) {
            JOptionPane.showMessageDialog(this,
                    "Il nome non corrisponde: non ho eliminato nulla.",
                    "Annullato", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        final SaveLocator.Rilevamento rl = rilevamento;
        final SaveSlotInfo sl = slot;
        final int indiceBase = c.getIndiceBase();
        final int indiceNave = c.getIndiceNave();

        elimina.setEnabled(false);
        esito.setText("Esporto la Corvette in Builds/, poi scrivo...\n\n"
                + "Questa operazione richiede qualche secondo: converte il salvataggio,\n"
                + "fa il backup, riscrive e rilegge.");
        esito.setCaretPosition(0);

        new javax.swing.SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() {
                Object[] out = new Object[3];

                // ---- 1. rete di sicurezza: la Corvette va in Builds/
                File esportato = null;
                String erroreExport = null;
                try {
                    eY base = (eY) c.getNodo();
                    eV oggetti = base == null ? null : base.d("Objects");
                    if (oggetti == null || oggetti.size() == 0) {
                        erroreExport = "La Corvette non ha moduli da esportare.";
                    } else {
                        String quando = new SimpleDateFormat("yyyy-MM-dd HH-mm")
                                .format(new Date());
                        String nome = c.getNome() + " - eliminata " + quando;
                        File destinazione = new File(cartellaLibreria,
                                WrapperBuild.nomeFileSicuro(nome));
                        WrapperBuild.scrivi(destinazione, c.getNome(), "", oggetti);
                        esportato = destinazione;
                    }
                } catch (Throwable t) {
                    erroreExport = String.valueOf(t);
                }
                out[0] = esportato;
                out[1] = erroreExport;

                // se la copia di sicurezza non riesce, non si procede
                if (esportato == null) {
                    out[2] = null;
                    return out;
                }

                // ---- 2. la scrittura vera, col ciclo backup -> verifica
                out[2] = ScrittoreSalvataggio.scrivi(rl, sl,
                        new ScrittoreSalvataggio.Modifica() {
                            @Override
                            public String descrizione() {
                                return "eliminazione della Corvette \""
                                        + c.getNome() + "\" con recupero dei moduli";
                            }

                            @Override
                            public void applica(eY radice) {
                                // il modello arriva fresco dal file: si ritrova la
                                // Corvette dalla posizione, non dall'istanza
                                eY stato = radice.H("PlayerStateData");
                                eV basi = stato.d("PersistentPlayerBases");
                                if (basi == null || indiceBase >= basi.size()) {
                                    throw new IllegalStateException(
                                            "La Corvette non e' piu' al suo posto: "
                                                    + "rileggi il salvataggio.");
                                }
                                eY base = basi.V(indiceBase);
                                if (base == null || !"PlayerShipBase".equals(
                                        base.getValueAsString("BaseType.PersistentBaseTypes"))) {
                                    throw new IllegalStateException(
                                            "La base non e' piu' una Corvette: "
                                                    + "rileggi il salvataggio.");
                                }
                                EliminazioneCorvette.applica(radice,
                                        new Corvette(indiceBase, indiceNave,
                                                c.getNome(), c.getParti(),
                                                c.getPosizioniModuli(), c.isAttiva(),
                                                c.getPosizione()));
                            }
                        });
                return out;
            }

            @Override
            protected void done() {
                elimina.setEnabled(true);
                try {
                    Object[] r = get();
                    File esportato = (File) r[0];
                    String erroreExport = (String) r[1];
                    ScrittoreSalvataggio.Esito e =
                            r[2] == null ? null : (ScrittoreSalvataggio.Esito) r[2];

                    StringBuilder b = new StringBuilder();
                    if (esportato != null) {
                        b.append("COPIA DI SICUREZZA\n  ");
                        b.append(esportato.getAbsolutePath()).append("\n\n");
                    } else {
                        b.append("COPIA DI SICUREZZA NON RIUSCITA\n  ");
                        b.append(erroreExport).append("\n\n");
                        b.append("L'operazione si e' fermata qui: non ho toccato il "
                                + "salvataggio.\n");
                        esito.setText(b.toString());
                        esito.setCaretPosition(0);
                        JOptionPane.showMessageDialog(SchedaElimina.this,
                                "Non sono riuscito a salvare la copia di sicurezza della\n"
                                        + "Corvette in Builds/. Senza quella non procedo.\n\n"
                                        + erroreExport,
                                "Non riuscito", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    if (e == null) {
                        b.append("Salvataggio non modificato.\n");
                    } else if (e.riuscito) {
                        b.append("ELIMINAZIONE RIUSCITA\n\n");
                        b.append(e.messaggio).append("\n\n");
                        if (e.dettaglio != null) {
                            b.append(e.dettaglio);
                        }
                    } else {
                        b.append("ELIMINAZIONE NON RIUSCITA\n\n");
                        b.append(e.messaggio).append("\n\n");
                        if (e.dettaglio != null) {
                            b.append(e.dettaglio);
                        }
                    }
                    esito.setText(b.toString());
                    esito.setCaretPosition(0);

                    if (e == null) {
                        return;
                    }
                    if (e.riuscito) {
                        JOptionPane.showMessageDialog(SchedaElimina.this,
                                "La Corvette e' stata eliminata e i moduli sono nel\n"
                                        + "deposito della Stazione.\n\n"
                                        + "Il progetto e' stato salvato in Builds/.\n"
                                        + "Apri il gioco e controlla.",
                                "Eliminata", JOptionPane.INFORMATION_MESSAGE);
                        if (ascoltatore != null) {
                            ascoltatore.eliminazioneEseguita();
                        }
                    } else {
                        JOptionPane.showMessageDialog(SchedaElimina.this,
                                e.messaggio, "Non riuscito", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    esito.setText("Errore inatteso:\n" + ex);
                }
            }
        }.execute();
    }
}
