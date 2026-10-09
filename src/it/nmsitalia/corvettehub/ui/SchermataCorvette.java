package it.nmsitalia.corvettehub.ui;

import it.nmsitalia.corvettehub.Main;
import it.nmsitalia.corvettehub.detect.SaveLocator;
import it.nmsitalia.corvettehub.domain.LettoreCorvette;
import it.nmsitalia.corvettehub.domain.SaveSlotInfo;
import it.nmsitalia.corvettehub.safety.ScrittoreSalvataggio;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Seconda schermata: tutto quello che riguarda la Corvette.
 *
 * Si apre dopo la scelta del salvataggio e sa sempre su quale slot sta
 * lavorando: la barra in alto lo ripete, cosi' non si confonde un salvataggio
 * con un altro. Sette schede nella navigazione laterale.
 */
public final class SchermataCorvette extends JPanel {

    /** Chi vuole tornare alla scelta del salvataggio. */
    public interface Ascoltatore {
        void cambiaSalvataggio();
    }

    private final File cartellaLibreria;
    private final Ascoltatore ascoltatore;

    private final JLabel contesto = new JLabel();
    private final JLabel statoGioco = new JLabel();
    private final JLabel messaggio = new JLabel("Pronto.");
    private final JTabbedPane schede = new JTabbedPane();

    private final SchedaCorvette schedaCorvette;
    private final SchedaImporta schedaImporta;
    private final SchedaEsporta schedaEsporta;
    private final SchedaRinomina schedaRinomina;
    private final SchedaElimina schedaElimina;
    private final SchedaDeposito schedaDeposito;
    private final SchedaLibreria schedaLibreria;

    private SaveSlotInfo slot;
    private SaveLocator.Rilevamento rilevamentoCorrente;
    private JButton salvaModifiche;
    private JButton annullaModifiche;
    private SchedaModificabile schedaAttiva;
    private int generazione;
    private boolean occupata;
    private boolean ripristinoScheda;
    private final java.util.Map<Component, Boolean> abilitazioni = new java.util.IdentityHashMap<Component, Boolean>();

    public SchermataCorvette(File cartellaLibreria, Ascoltatore ascoltatore) {
        this.cartellaLibreria = cartellaLibreria;
        this.ascoltatore = ascoltatore;

        setBackground(Theme.SFONDO);
        setLayout(new BorderLayout());

        schedaCorvette = new SchedaCorvette();
        schedaImporta = new SchedaImporta();
        schedaEsporta = new SchedaEsporta(cartellaLibreria);
        schedaRinomina = new SchedaRinomina();
        schedaElimina = new SchedaElimina(cartellaLibreria, new SchedaElimina.Ascoltatore() {
            @Override
            public void eliminazioneEseguita() {
                // la Corvette non esiste piu': tutto quello che e' a schermo va riletto
                if (rilevamentoCorrente != null && slot != null) {
                    ricarica();
                }
            }
        });
        schedaDeposito = new SchedaDeposito(cartellaLibreria);
        schedaLibreria = new SchedaLibreria(cartellaLibreria, new SchedaLibreria.Ascoltatore() {
            @Override
            public void libreriaCambiata() {
                schedaDeposito.ricaricaLibreria();
            }
            @Override public void importaProgetto(File file) {
                schede.setSelectedComponent(schedaImporta);
                schedaImporta.caricaFile(file);
            }
        });

        add(costruisciTestata(), BorderLayout.NORTH);
        add(costruisciSchede(), BorderLayout.CENTER);
        add(costruisciBarraStato(), BorderLayout.SOUTH);

        collegaPulsanti();
        Theme.rifinisci(this);
        avviaControlloGioco();
    }

    private JPanel costruisciTestata() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.SUPERFICIE);
        p.setBorder(Scala.bordo(12, 16, 12, 16));

        JPanel sinistra = new JPanel();
        sinistra.setOpaque(false);
        sinistra.setLayout(new BoxLayout(sinistra, BoxLayout.X_AXIS));

        JLabel logo = new JLabel();
        try {
            logo.setIcon(Icone.logo(Scala.px(44)));
        } catch (Throwable ignored) {
            // senza logo la schermata funziona lo stesso
        }
        logo.setBorder(Scala.bordo(0, 0, 0, 12));

        JPanel testi = new JPanel();
        testi.setOpaque(false);
        testi.setLayout(new BoxLayout(testi, BoxLayout.Y_AXIS));

        JLabel titolo = new JLabel("Il tuo hangar");
        titolo.setFont(Theme.titolo());
        titolo.setForeground(Theme.TESTO);
        titolo.setAlignmentX(Component.LEFT_ALIGNMENT);

        contesto.setFont(Scala.font(Font.PLAIN, 12));
        contesto.setForeground(Theme.TESTO_TENUE);
        contesto.setAlignmentX(Component.LEFT_ALIGNMENT);

        testi.add(Box.createVerticalGlue());
        testi.add(titolo);
        testi.add(Box.createVerticalStrut(4));
        testi.add(contesto);
        testi.add(Box.createVerticalGlue());


        sinistra.add(testi);

        // I pulsanti per salvare la disposizione dell'inventario stanno qui, in
        // alto: nella scheda finirebbero sotto una griglia alta piu' di 700
        // pixel e si vedrebbero solo scorrendo.
        salvaModifiche = new JButton("Salva le modifiche");
        salvaModifiche.setFont(Scala.font(Font.BOLD, 12));
        salvaModifiche.setEnabled(false);
        salvaModifiche.setToolTipText("Scrive sul salvataggio la nuova "
                + "disposizione dell'inventario");
        salvaModifiche.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (schedaAttiva != null) {
                    schedaAttiva.salvaModifiche();
                }
            }
        });

        annullaModifiche = new JButton("Annulla");
        annullaModifiche.setEnabled(false);
        annullaModifiche.setToolTipText("Butta via gli spostamenti non salvati");
        annullaModifiche.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (schedaAttiva != null) {
                    schedaAttiva.annullaModifiche();
                }
            }
        });

        JButton ripristina = new JButton("Ripristina backup...");
        ripristina.setToolTipText("Rimette a posto i file di un salvataggio "
                + "da una copia di sicurezza");
        ripristina.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ripristinaBackup();
            }
        });

        JButton cambia = new JButton("Cambia salvataggio");
        cambia.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (puoUscire()) ascoltatore.cambiaSalvataggio();
            }
        });

        JPanel destra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        destra.setOpaque(false);
        destra.add(salvaModifiche);
        destra.add(annullaModifiche);
        destra.add(ripristina);
        destra.add(cambia);

        p.setBackground(Theme.SFONDO);
        p.setBorder(Scala.bordo(22, 24, 14, 24));
        p.add(sinistra, BorderLayout.NORTH);
        destra.setBorder(Scala.bordo(14, 0, 0, 0));
        ((FlowLayout) destra.getLayout()).setAlignment(FlowLayout.LEFT);
        p.add(destra, BorderLayout.CENTER);
        return p;
    }

    /**
     * Collega i pulsanti Salva e Annulla alla scheda che si sta guardando.
     *
     * Si rifa' a ogni cambio di scheda: cosi' i due pulsanti funzionano con
     * l'inventario, con le tecnologie e con il deposito, senza sapere quale sia
     * aperto.
     */
    private void collegaPulsanti() {
        if (schedaAttiva != null) schedaAttiva.setAscoltatoreModifiche(null);
        java.awt.Component sel = schede.getSelectedComponent();
        schedaAttiva = sel instanceof SchedaModificabile ? (SchedaModificabile) sel : null;
        if (schedaAttiva != null) {
            schedaAttiva.setAscoltatoreModifiche(new SchedaModificabile.AscoltatoreModifiche() {
                @Override
                public void modificheCambiate(boolean presenti) {
                    if (schede.getSelectedComponent() == schedaAttiva) {
                        boolean ha = schedaAttiva != null && schedaAttiva.haModifiche();
                        salvaModifiche.setEnabled(ha && !occupata);
                        annullaModifiche.setEnabled(ha && !occupata);
                    }
                }
            });
        }
        boolean ha = schedaAttiva != null && schedaAttiva.haModifiche();
        salvaModifiche.setEnabled(ha && !occupata);
        annullaModifiche.setEnabled(ha && !occupata);
    }

    private JTabbedPane costruisciSchede() {
        schede.setTabPlacement(JTabbedPane.LEFT);
        schede.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        schede.putClientProperty("JTabbedPane.tabWidthMode", "equal");
        schede.putClientProperty("JTabbedPane.minimumTabWidth", Scala.px(166));
        schede.setBackground(Theme.SFONDO);
        schede.setForeground(Theme.TESTO);
        schede.setFont(Scala.font(Font.PLAIN, 13));
        schede.addTab("Corvette", new Segno("Corvette"), schedaCorvette);
        schede.addTab("Importa", new Segno("Importa"), schedaImporta);
        schede.addTab("Esporta", new Segno("Esporta"), schedaEsporta);
        schede.addTab("Rinomina", new Segno("Rinomina"), schedaRinomina);
        schede.addTab("Elimina", new Segno("Elimina"), schedaElimina);
        schede.addTab("Deposito", new Segno("Deposito"), schedaDeposito);
        schede.addTab("Libreria", new Segno("Libreria"), schedaLibreria);
        // cambiando scheda i pulsanti in alto si ricollegano a quella nuova
        schede.addChangeListener(new javax.swing.event.ChangeListener() {
            @Override
            public void stateChanged(javax.swing.event.ChangeEvent e) {
                if (ripristinoScheda) return;
                if (schedaAttiva != null && schedaAttiva != schede.getSelectedComponent()
                        && schedaAttiva.haModifiche()) {
                    int scelta = JOptionPane.showConfirmDialog(SchermataCorvette.this,
                            "Ci sono spostamenti non salvati. Vuoi abbandonarli e cambiare scheda?",
                            "Modifiche non salvate", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (scelta != JOptionPane.YES_OPTION) {
                        ripristinoScheda = true;
                        try { schede.setSelectedComponent((Component) schedaAttiva); }
                        finally { ripristinoScheda = false; }
                        return;
                    }
                    schedaAttiva.annullaModifiche();
                }
                collegaPulsanti();
            }
        });
        return schede;
    }

    private JPanel pannelloProssimaFase(String titolo, String testo) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.SFONDO);

        JLabel t = new JLabel(titolo);
        t.setFont(Scala.font(Font.BOLD, 16));
        t.setForeground(Theme.TESTO);

        JLabel b = new JLabel("<html><body style='width:560px'><pre style='font-family:"
                + "Segoe UI; font-size:12px'>" + testo + "</pre></body></html>");
        b.setForeground(Theme.TESTO_TENUE);

        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.setBorder(Scala.bordo(40, 40, 40, 40));
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        centro.add(t);
        centro.add(Box.createVerticalStrut(16));
        centro.add(b);

        p.add(centro, BorderLayout.NORTH);
        return p;
    }

    private JPanel costruisciBarraStato() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.SUPERFICIE);
        p.setBorder(Scala.bordo(6, 16, 6, 16));

        messaggio.setForeground(Theme.TESTO_TENUE);
        messaggio.setFont(Scala.font(Font.PLAIN, 12));
        statoGioco.setFont(Scala.font(Font.PLAIN, 12));

        JPanel destra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        destra.setOpaque(false);
        destra.add(statoGioco);

        p.add(messaggio, BorderLayout.WEST);
        p.add(destra, BorderLayout.EAST);
        return p;
    }

    // ---------------------------------------------------------------- dati

    /** Carica lo slot scelto e riempie le schede. */
    public void aggiorna(final SaveLocator.Rilevamento r, final SaveSlotInfo slot) {
        final int richiesta = ++generazione;
        this.slot = slot;
        this.rilevamentoCorrente = r;
        setOccupata(true);
        contesto.setText("Slot " + slot.getNumero() + "   ·   " + slot.getModalita()
                + "   ·   " + r.tipo
                + "   ·   " + slot.getDataFormattata());
        messaggio.setText("Converto il salvataggio dello slot " + slot.getNumero() + "...");

        new SwingWorker<LettoreCorvette.Esito, Void>() {
            @Override
            protected LettoreCorvette.Esito doInBackground() {
                return LettoreCorvette.leggi(slot.getModello());
            }

            @Override
            protected void done() {
                try {
                    if (richiesta != generazione) return;
                    LettoreCorvette.Esito esito = get();
                    setOccupata(false);
                    if (!esito.ok()) {
                        messaggio.setText(esito.errore);
                        JOptionPane.showMessageDialog(SchermataCorvette.this, esito.errore,
                                "Salvataggio non leggibile", JOptionPane.ERROR_MESSAGE);
                        ascoltatore.cambiaSalvataggio();
                        return;
                    }
                    String autore = slot.getNomeSalvataggio();
                    schedaCorvette.aggiorna(r, slot, esito);
                    schedaImporta.aggiorna(r, slot, esito);
                    schedaEsporta.aggiorna(slot, esito, autore);
                    schedaRinomina.aggiorna(r, slot, esito);
                    schedaElimina.aggiorna(r, slot, esito);
                    schedaDeposito.aggiorna(r, slot,
                            "Slot " + slot.getNumero() + " · " + slot.getModalita());
                    schedaLibreria.ricarica();
                    Theme.rifinisci(schede);
                    collegaPulsanti();
                    messaggio.setText("Slot " + slot.getNumero() + ": "
                            + esito.corvette.size() + " Corvette. "
                            + (esito.naveAttiva >= 0 ? "Nave in uso protetta." : ""));
                } catch (Exception e) {
                    if (richiesta == generazione) {
                        setOccupata(false);
                        messaggio.setText("Errore nella conversione: " + e.getMessage());
                        ascoltatore.cambiaSalvataggio();
                    }
                }
            }
        }.execute();
    }

    // -------------------------------------------------------- ripristino

    /**
     * Ripristina un backup scelto dall'utente.
     *
     * Prima di rimettere i file vecchi il programma salva lo stato attuale:
     * se il backup fosse vecchio, si deve poter tornare indietro.
     */
    private void ripristinaBackup() {
        if (rilevamentoCorrente == null) {
            JOptionPane.showMessageDialog(this,
                    "Prima scegli un salvataggio.", "Nessun salvataggio",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<File> backups = ScrittoreSalvataggio.elencoBackup();
        if (backups.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Non ci sono ancora backup.\n\nIl programma ne crea uno automaticamente "
                            + "prima di ogni scrittura, nella cartella Backup accanto "
                            + "all'eseguibile.",
                    "Nessun backup", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (ScrittoreSalvataggio.giocoInEsecuzione()) {
            JOptionPane.showMessageDialog(this,
                    "No Man's Sky e' in esecuzione.\n\nChiudi il gioco prima di ripristinare, "
                            + "altrimenti il gioco potrebbe sovrascrivere i file.",
                    "Gioco aperto", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String[] voci = new String[backups.size()];
        for (int i = 0; i < backups.size(); i++) {
            File b = backups.get(i);
            int file = 0;
            File[] figli = b.listFiles();
            if (figli != null) {
                for (int k = 0; k < figli.length; k++) {
                    if (figli[k].isDirectory()) {
                        File[] dentro = figli[k].listFiles();
                        if (dentro != null) {
                            file += dentro.length;
                        }
                    } else {
                        file++;
                    }
                }
            }
            voci[i] = b.getName() + "   ·   " + f.format(new Date(b.lastModified()))
                    + "   ·   " + file + " file";
        }

        Object scelta = JOptionPane.showInputDialog(this,
                "Quale backup vuoi rimettere a posto?\n\n"
                        + "I file attuali verranno prima salvati a parte, cosi' puoi\n"
                        + "tornare indietro anche dopo il ripristino.",
                "Ripristina backup", JOptionPane.QUESTION_MESSAGE, null, voci, voci[0]);
        if (scelta == null) {
            return;
        }
        int indice = 0;
        for (int i = 0; i < voci.length; i++) {
            if (voci[i].equals(scelta)) {
                indice = i;
                break;
            }
        }
        final File scelto = backups.get(indice);

        int r = JOptionPane.showConfirmDialog(this,
                "Sto per rimettere a posto i file del salvataggio da:\n\n"
                        + scelto.getName() + "\n\n"
                        + "Le Corvette, i progressi e tutto il resto torneranno a com'erano\n"
                        + "in quel momento. Procedo?",
                "Conferma ripristino", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }

        messaggio.setText("Ripristino in corso...");
        final SaveLocator.Rilevamento rl = rilevamentoCorrente;

        Theme.lavora(this, true);
        new SwingWorker<ScrittoreSalvataggio.Esito, Void>() {
            @Override
            protected ScrittoreSalvataggio.Esito doInBackground() {
                return ScrittoreSalvataggio.ripristina(scelto, rl.cartella);
            }

            @Override
            protected void done() {
                try {
                    Theme.lavora(SchermataCorvette.this, false);
                    ScrittoreSalvataggio.Esito e = get();
                    messaggio.setText(e.messaggio);
                    JOptionPane.showMessageDialog(SchermataCorvette.this,
                            (e.dettaglio == null || e.dettaglio.isEmpty() ? e.messaggio : e.dettaglio),
                            e.riuscito ? "Ripristino eseguito" : "Ripristino non riuscito",
                            e.riuscito ? JOptionPane.INFORMATION_MESSAGE
                                    : JOptionPane.ERROR_MESSAGE);
                    // i dati a schermo non valgono piu': si ricarica tutto
                    if (e.riuscito && slot != null) {
                        ricarica();
                    }
                } catch (Exception ex) {
                    Theme.lavora(SchermataCorvette.this, false);
                    messaggio.setText("Errore: " + ex.getMessage());
                }
            }
        }.execute();
    }

    // -------------------------------------------------------- gioco aperto

    private void avviaControlloGioco() {
        Timer t = new Timer("controllo-gioco", true);
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                aggiornaStatoGioco();
            }
        }, 0, 10000);
    }

    private void aggiornaStatoGioco() {
        final String problema = ScrittoreSalvataggio.verificaGioco();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (problema != null) {
                    statoGioco.setText(problema.contains("in esecuzione") ? "●  Gioco in esecuzione" : "●  Controllo gioco non disponibile");
                    statoGioco.setToolTipText(problema);
                    statoGioco.setForeground(Theme.AVVISO);
                } else {
                    statoGioco.setText("●  Gioco chiuso · pronto a lavorare");
                    statoGioco.setToolTipText(null);
                    statoGioco.setForeground(Theme.OK);
                }
            }
        });
    }

    /** Rilevamento del gioco in esecuzione (requisito R8). */
    private static boolean giocoInEsecuzione() {
        return ScrittoreSalvataggio.giocoInEsecuzione();
    }

    public void ricarica() {
        if (rilevamentoCorrente == null || slot == null) return;
        final SaveLocator.Rilevamento precedente = rilevamentoCorrente;
        final int indice = slot.getIndice();
        final int richiesta = ++generazione;
        setOccupata(true);
        new SwingWorker<SaveLocator.Rilevamento, Void>() {
            protected SaveLocator.Rilevamento doInBackground() {
                return SaveLocator.apri(precedente.cartella, precedente.tipo);
            }
            protected void done() {
                if (richiesta != generazione) return;
                try {
                    SaveLocator.Rilevamento r = get();
                    if (r != null) for (nomanssave.ft s : r.storage.bU()) {
                        if (s != null && s.getIndex() == indice) {
                            aggiorna(r, new SaveSlotInfo(s));
                            return;
                        }
                    }
                    throw new IllegalStateException("Lo slot non e' piu' disponibile.");
                } catch (Exception e) {
                    setOccupata(false);
                    JOptionPane.showMessageDialog(SchermataCorvette.this,
                            "Non riesco a rileggere il salvataggio: " + e.getMessage(),
                            "Ricaricamento non riuscito", JOptionPane.ERROR_MESSAGE);
                    ascoltatore.cambiaSalvataggio();
                }
            }
        }.execute();
    }

    public boolean isOccupata() { return occupata; }

    public boolean puoUscire() {
        if (occupata) return false;
        if (schedaCorvette.haModifiche() || schedaDeposito.haModifiche()) {
            int r = JOptionPane.showConfirmDialog(this,
                    "Ci sono spostamenti non salvati. Vuoi abbandonarli?",
                    "Modifiche non salvate", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r != JOptionPane.YES_OPTION) return false;
            schedaCorvette.annullaModifiche(); schedaDeposito.annullaModifiche();
        }
        return true;
    }

    public void setOccupata(boolean valore) {
        if (occupata == valore) return;
        occupata = valore;
        if (valore) {
            abilitazioni.clear(); disabilita(this);
            messaggio.setText("Operazione in corso · backup e verifica dei file…");
        } else {
            for (java.util.Map.Entry<Component, Boolean> e : abilitazioni.entrySet()) e.getKey().setEnabled(e.getValue());
            abilitazioni.clear(); collegaPulsanti(); messaggio.setText("Pronto.");
        }
    }
    private void disabilita(Component c) {
        abilitazioni.put(c, c.isEnabled()); c.setEnabled(false);
        if (c instanceof java.awt.Container) for (Component figlio : ((java.awt.Container)c).getComponents()) disabilita(figlio);
    }

    public SaveSlotInfo getSlot() {
        return slot;
    }
}
