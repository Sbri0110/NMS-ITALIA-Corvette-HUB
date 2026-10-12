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
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
    private final JLabel statoGioco = new JLabel() {
        @Override public java.awt.Dimension getPreferredSize() {
            java.awt.Dimension d=super.getPreferredSize();
            if(getParent()!=null && getParent().getWidth()>0) {
                java.awt.Insets in=getParent().getInsets();
                int disponibile=getParent().getWidth()-in.left-in.right;
                d.width=Math.min(d.width,Math.max(0,disponibile/2));
            }
            return d;
        }
    };
    private final JLabel messaggio = new JLabel("Pronto.") {
        @Override public String getToolTipText() { return getText(); }
    };
    private final JTabbedPane schede = new JTabbedPane();

    private final SchedaCorvette schedaCorvette;
    private final SchedaImporta schedaImporta;
    private final SchedaEsporta schedaEsporta;
    private final SchedaRinomina schedaRinomina;
    private final SchedaElimina schedaElimina;
    private final SchedaDeposito schedaDeposito;
    private final SchedaDepositoCorvette schedaDepositoCorvette;
    private final SchedaLibreria schedaLibreria;

    private SaveSlotInfo slot;
    private SaveLocator.Rilevamento rilevamentoCorrente;
    private JButton salvaModifiche;
    private JButton annullaModifiche;
    private JButton ripristina, cambia, azioniCompatte;
    private final JLabel titoloHangar = new JLabel("Il tuo hangar");
    private JPanel testata, pannelloContesto, barraComandi;
    private String contestoCompleto = "";
    private int modoComandi = -1;
    private boolean modoBasso;
    private JLabel marchioHangar;
    private Attesa.Velo velo;
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
        schedaDepositoCorvette = new SchedaDepositoCorvette();
        // la scheda dei depositi segue la Corvette scelta nella scheda Corvette
        schedaCorvette.setAscoltatoreSelezione(new SchedaCorvette.AscoltatoreSelezione() {
            @Override
            public void selezioneCambiata(int indice, LettoreCorvette.Esito esito) {
                schedaDepositoCorvette.cambiaCorvette(indice, esito);
            }
        });
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

        testata=costruisciTestata();
        JPanel corpo = new JPanel(new BorderLayout());
        corpo.setOpaque(false);
        corpo.add(testata, BorderLayout.NORTH);
        corpo.add(costruisciSchede(), BorderLayout.CENTER);
        corpo.add(costruisciBarraStato(), BorderLayout.SOUTH);

        // Il contenuto e il velo di attesa stanno sullo stesso strato: il velo
        // e' trasparente agli eventi quando e' spento, e copre tutto quando
        // un'operazione lunga e' in corso.
        velo = new Attesa.Velo();
        JPanel strati = new JPanel();
        strati.setOpaque(false);
        strati.setLayout(new OverlayLayout(strati));
        strati.add(velo);
        strati.add(corpo);
        add(strati, BorderLayout.CENTER);

        collegaPulsanti();
        Theme.rifinisci(this);
        avviaControlloGioco();
    }

    private JPanel costruisciTestata() {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(0x121C2E), 0, getHeight(), new Color(0x0A111C)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.BORDO);
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(Scala.bordo(10, 18, 10, 18));

        JPanel sinistra = new JPanel();
        pannelloContesto = sinistra;
        sinistra.setOpaque(false);
        sinistra.setMinimumSize(Scala.dim(0,0));
        sinistra.setLayout(new BoxLayout(sinistra, BoxLayout.X_AXIS));

        JLabel logo = new JLabel();
        marchioHangar = logo;
        try {
            logo.setIcon(Icone.logo(Scala.px(30)));
        } catch (Throwable ignored) {
            // senza logo la schermata funziona lo stesso
        }
        logo.setBorder(Scala.bordo(0, 0, 0, 14));

        JPanel testi = new JPanel();
        testi.setOpaque(false);
        testi.setMinimumSize(Scala.dim(0,0));
        testi.setLayout(new BoxLayout(testi, BoxLayout.Y_AXIS));

        JLabel titolo = titoloHangar;
        titolo.setFont(Theme.titolo());
        titolo.setForeground(Theme.TESTO);
        titolo.setAlignmentX(Component.LEFT_ALIGNMENT);

        contesto.setFont(Theme.corpo());
        contesto.setForeground(Theme.TESTO_TENUE);
        contesto.setAlignmentX(Component.LEFT_ALIGNMENT);
        contesto.setMinimumSize(Scala.dim(0,0));

        testi.add(Box.createVerticalGlue());
        testi.add(titolo);
        testi.add(Box.createVerticalStrut(3));
        testi.add(contesto);
        testi.add(Box.createVerticalGlue());


        sinistra.add(testi);

        // I pulsanti per salvare la disposizione dell'inventario stanno qui, in
        // alto: nella scheda finirebbero sotto una griglia alta piu' di 700
        // pixel e si vedrebbero solo scorrendo.
        salvaModifiche = new JButton("Salva le modifiche");
        Theme.primaria(salvaModifiche);
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
        Theme.secondaria(annullaModifiche);
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

        ripristina = new JButton("Ripristina backup\u2026");
        Theme.secondaria(ripristina);
        ripristina.setToolTipText("Rimette a posto i file di un salvataggio "
                + "da una copia di sicurezza");
        ripristina.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ripristinaBackup();
            }
        });

        cambia = new JButton("Cambia salvataggio");
        Theme.secondaria(cambia);
        cambia.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (puoUscire()) ascoltatore.cambiaSalvataggio();
            }
        });

        JPanel destra = new JPanel(new FlowLayout(FlowLayout.RIGHT,Scala.px(6),0)) {
            @Override public void doLayout() {
                super.doLayout();
                if(modoComandi!=2) {
                    java.awt.Insets in=getInsets();
                    int disponibile=getHeight()-in.top-in.bottom;
                    for(Component c:getComponents()) if(c.isVisible()) {
                        c.setLocation(c.getX(),in.top+Math.max(0,(disponibile-c.getHeight())/2));
                    }
                }
            }
        };
        barraComandi=destra;
        destra.setOpaque(false);
        destra.add(salvaModifiche);
        destra.add(annullaModifiche);
        destra.add(ripristina);
        destra.add(cambia);
        azioniCompatte=new JButton("Azioni\u2026");
        Theme.secondaria(azioniCompatte);
        final javax.swing.JPopupMenu menu=new javax.swing.JPopupMenu();
        javax.swing.JMenuItem ripristino=new javax.swing.JMenuItem("Ripristina backup\u2026");
        ripristino.addActionListener(e -> ripristina.doClick());menu.add(ripristino);
        javax.swing.JMenuItem cambio=new javax.swing.JMenuItem("Cambia salvataggio");
        cambio.addActionListener(e -> cambia.doClick());menu.add(cambio);
        azioniCompatte.addActionListener(e -> menu.show(azioniCompatte,0,azioniCompatte.getHeight()));
        azioniCompatte.setVisible(false);destra.add(azioniCompatte);

        p.add(sinistra, BorderLayout.CENTER);
        p.add(destra, BorderLayout.EAST);
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
                        adattaComandi();
                    }
                }
            });
        }
        boolean ha = schedaAttiva != null && schedaAttiva.haModifiche();
        salvaModifiche.setEnabled(ha && !occupata);
        annullaModifiche.setEnabled(ha && !occupata);
        adattaComandi();
    }

    private JTabbedPane costruisciSchede() {
        schede.setTabPlacement(JTabbedPane.LEFT);
        schede.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        schede.putClientProperty("JTabbedPane.tabWidthMode", "equal");
        schede.putClientProperty("JTabbedPane.minimumTabWidth", Scala.px(150));
        // Il contenuto di ogni scheda si allinea a sinistra: centrato, le
        // voci con etichetta lunga ("Deposito Corvette") scivolavano a
        // sinistra e le icone non cadevano piu' sulla stessa verticale.
        schede.putClientProperty("JTabbedPane.tabAlignment", "leading");
        // Scheda moderna: nessun separatore, indicatore d'oro a sinistra,
        // etichetta con una riga di respiro in piu'.
        schede.putClientProperty("JTabbedPane.showTabSeparators", Boolean.FALSE);
        schede.putClientProperty("JTabbedPane.tabType", "underlined");
        schede.putClientProperty("JTabbedPane.underlineColor", Theme.ACCENTO);
        schede.putClientProperty("JTabbedPane.hoverColor", Theme.SUPERFICIE);
        schede.putClientProperty("JTabbedPane.selectedBackground", Theme.SUPERFICIE_ALTA);
        schede.putClientProperty("JTabbedPane.tabHeight", Scala.px(38));
        schede.putClientProperty("JTabbedPane.tabSelectionHeight", 0);
        schede.setBackground(Theme.ABISSO);
        schede.setForeground(Theme.TESTO_TENUE);
        schede.setFont(Theme.sezione());
        schede.setBorder(Scala.bordo(8, 0, 8, 8));
        aggiungiScheda("Corvette", schedaCorvette);
        aggiungiScheda("Importa", schedaImporta);
        aggiungiScheda("Esporta", schedaEsporta);
        aggiungiScheda("Rinomina", schedaRinomina);
        aggiungiScheda("Elimina", schedaElimina);
        aggiungiScheda("Deposito", schedaDeposito);
        aggiungiScheda("Deposito Corvette", schedaDepositoCorvette);
        aggiungiScheda("Libreria", schedaLibreria);
        // cambiando scheda i pulsanti in alto si ricollegano a quella nuova
        schede.addChangeListener(new javax.swing.event.ChangeListener() {
            @Override
            public void stateChanged(javax.swing.event.ChangeEvent e) {
                aggiornaSchedaAttiva();
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
        aggiornaSchedaAttiva();
        return schede;
    }

    /**
     * Aggiunge una scheda con il suo segno vettoriale.
     *
     * FlatLaf colora automaticamente l'icona con il colore del testo: quando
     * la scheda e' scelta, l'icona passa al bianco insieme all'etichetta e
     * l'indicatore d'oro resta l'unico elemento acceso.
     */
    private void aggiungiScheda(String nome, Component corpo) {
        schede.addTab(nome, new Segno(nome), corpo);
    }

    /** Riallinea le schede disegnate a mano quando cambia quella scelta. */
    private void aggiornaSchedaAttiva() {
        // FlatLaf gestisce da se' selezione e hover delle schede "underlined";
        // qui resta il bordo arrotondato sui lati per non avere spigoli vivi.
        schede.setBorder(Scala.bordo(8, 0, 8, 8));
    }


    private JPanel pannelloProssimaFase(String titolo, String testo) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.SFONDO);

        JLabel t = new JLabel(titolo);
        t.setFont(Scala.font(Font.PLAIN, 19));
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
        JPanel p = new JPanel(new BorderLayout(Scala.px(10),0));
        p.setBackground(Theme.SUPERFICIE);
        p.setBorder(Scala.bordo(5, 18, 5, 18));

        messaggio.setForeground(Theme.TESTO_TENUE);
        messaggio.setFont(Theme.corpo());
        messaggio.setToolTipText("Pronto.");
        statoGioco.setFont(Theme.corpo());
        statoGioco.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        statoGioco.setIconTextGap(Scala.px(6));
        p.add(messaggio, BorderLayout.CENTER);
        p.add(statoGioco, BorderLayout.EAST);

        // una linea di separazione sottile fra il contenuto e la barra
        JPanel bordo = new JPanel(new BorderLayout());
        bordo.setBackground(Theme.SUPERFICIE);
        JPanel linea = new JPanel();
        linea.setBackground(Theme.BORDO);
        linea.setPreferredSize(new java.awt.Dimension(1, Scala.px(1)));
        bordo.add(linea, BorderLayout.NORTH);
        bordo.add(p, BorderLayout.CENTER);
        return bordo;
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
        contestoCompleto=contesto.getText();adattaComandi();
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
                    schedaDepositoCorvette.aggiorna(r, slot, esito,
                            schedaCorvette.getIndiceSelezionato());
                    schedaLibreria.ricarica();
                    Theme.rifinisci(schede);
                    collegaPulsanti();
                    messaggio.setText("Slot " + slot.getNumero() + ": "
                            + esito.corvette.size() + " Corvette. "
                            + "Scritture disponibili a gioco chiuso.");
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
                    boolean inCorso = problema.contains("in esecuzione");
                    statoGioco.setIcon(new Segno.Punto(inCorso ? Theme.AVVISO : Theme.ERRORE));
                    statoGioco.setText(inCorso ? "Gioco in esecuzione" : "Controllo gioco non disponibile");
                    statoGioco.setToolTipText(problema);
                    statoGioco.setForeground(Theme.AVVISO);
                } else {
                    statoGioco.setIcon(new Segno.Punto(Theme.OK));
                    statoGioco.setText("Gioco chiuso · pronto a lavorare");
                    statoGioco.setToolTipText("Gioco chiuso · pronto a lavorare");
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
        if (schedaCorvette.haModifiche() || schedaDeposito.haModifiche()
                || schedaDepositoCorvette.haModifiche()) {
            int r = JOptionPane.showConfirmDialog(this,
                    "Ci sono spostamenti non salvati. Vuoi abbandonarli?",
                    "Modifiche non salvate", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r != JOptionPane.YES_OPTION) return false;
            schedaCorvette.annullaModifiche(); schedaDeposito.annullaModifiche();
            schedaDepositoCorvette.annullaModifiche();
        }
        return true;
    }

    public void setOccupata(boolean valore) {
        if (occupata == valore) return;
        occupata = valore;
        if (valore) {
            abilitazioni.clear(); disabilita(this);
            messaggio.setText("Operazione in corso \u00b7 backup e verifica dei file\u2026");
            if (velo != null) {
                velo.titolo("Operazione in corso");
                velo.fase("Backup e verifica dei file\u2026");
                velo.mostra(true);
            }
        } else {
            for (java.util.Map.Entry<Component, Boolean> e : abilitazioni.entrySet()) e.getKey().setEnabled(e.getValue());
            abilitazioni.clear(); collegaPulsanti(); messaggio.setText("Pronto.");
            if (velo != null) velo.mostra(false);
        }
    }

    /**
     * Aggiorna la riga di stato del velo.
     *
     * Serve a raccontare le fasi: conversione, backup, riscrittura, rilettura.
     * Se il velo non e' acceso non fa nulla.
     */
    public void fase(String testo) {
        if (velo != null && velo.isVisible()) velo.fase(testo);
    }

    /** Accende il velo con un titolo suo, per i lavori che non sono scritture. */
    public void lavoraCon(String titolo, String fase) {
        if (velo == null) return;
        velo.titolo(titolo);
        velo.fase(fase);
        velo.mostra(true);
    }

    /** Spegne il velo acceso con {@link #lavoraCon}. */
    public void veloOff() {
        if (velo != null) velo.mostra(false);
    }
    private void disabilita(Component c) {
        abilitazioni.put(c, c.isEnabled()); c.setEnabled(false);
        if (c instanceof java.awt.Container) for (Component figlio : ((java.awt.Container)c).getComponents()) disabilita(figlio);
    }

    public SaveSlotInfo getSlot() {
        return slot;
    }

    public void aggiornaLibreria() {
        schedaLibreria.ricarica();
        schedaDeposito.ricaricaLibreria();
    }

    @Override public void doLayout() {
        int placement=getWidth()<Scala.px(900)?JTabbedPane.TOP:JTabbedPane.LEFT;
        if(schede.getTabPlacement()!=placement) schede.setTabPlacement(placement);
        adattaComandi();
        super.doLayout();
    }
    private void adattaComandi() {
        if(azioniCompatte==null || testata==null) return;
        boolean compatto=getWidth()>0 && getWidth()<Scala.px(760);
        // Finestra bassa: la testata si stringe. Le schede hanno bisogno di
        // almeno Scala.px(120) di altezza utile, e fra testata e barra di stato
        // lo spazio e' poco. La riga di contesto resta nel tooltip.
        boolean basso=getHeight()>0 && getHeight()<Scala.px(720);
        boolean modifiche=schedaAttiva!=null && schedaAttiva.haModifiche();
        salvaModifiche.setVisible(!compatto || modifiche);
        annullaModifiche.setVisible(!compatto || modifiche);
        ripristina.setVisible(!compatto);cambia.setVisible(!compatto);
        azioniCompatte.setVisible(compatto);titoloHangar.setVisible(!compatto);
        contesto.setVisible(!basso);
        if(basso!=modoBasso) {
            modoBasso=basso;
            testata.setBorder(basso?Scala.bordo(6,18,6,18):Scala.bordo(10,18,10,18));
            if(marchioHangar!=null && marchioHangar.getIcon()!=null) {
                try { marchioHangar.setIcon(Icone.logo(Scala.px(basso?24:30))); }
                catch (Throwable ignored) { }
            }
        }
        int modo=compatto?(modifiche?2:1):0;
        if(modoComandi!=modo) {
            modoComandi=modo;
            salvaModifiche.setText(compatto?"Salva":"Salva le modifiche");
            testata.setBorder((basso||compatto)?Scala.bordo(6,14,6,14):Scala.bordo(10,18,10,18));
            barraComandi.setBorder(Scala.bordo(modo==2?4:0,0,0,0));
            barraComandi.setLayout(modo==2
                    ?new LayoutFluido(Scala.px(6),Scala.px(4))
                    :new FlowLayout(FlowLayout.RIGHT,Scala.px(6),0));
            testata.removeAll();
            testata.setLayout(new BorderLayout(Scala.px(12),0));
            testata.add(pannelloContesto,modo==2?BorderLayout.NORTH:BorderLayout.CENTER);
            testata.add(barraComandi,modo==2?BorderLayout.CENTER:BorderLayout.EAST);
        }
        if(slot!=null) {
            String testo=compatto?"Slot "+slot.getNumero()+" \u00b7 "+slot.getModalita()+" \u00b7 "+rilevamentoCorrente.tipo:contestoCompleto;
            if(!testo.equals(contesto.getText())) contesto.setText(testo);
            contesto.setToolTipText(contestoCompleto);
        }
    }
}
