package it.nmsitalia.corvettehub.ui;

import it.nmsitalia.corvettehub.domain.Corvette;
import it.nmsitalia.corvettehub.domain.LettoreCorvette;
import it.nmsitalia.corvettehub.domain.SaveSlotInfo;
import it.nmsitalia.corvettehub.domain.WrapperBuild;
import nomanssave.eV;
import nomanssave.eY;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Scheda Esporta: estrae una Corvette e la salva come progetto condivisibile.
 *
 * Sola lettura sul salvataggio: prende la lista Objects della Corvette scelta
 * e la scrive in un nuovo file dentro la libreria. Nessuna scrittura sul
 * salvataggio, quindi nessun backup necessario.
 *
 * La Corvette in uso non compare nell'elenco: esportarla non avrebbe senso,
 * perche' il gioco la ricarica in memoria.
 */
public final class SchedaEsporta extends JPanel {

    private final File cartellaLibreria;

    private final JLabel intestazione = new JLabel("Esporta una Corvette in un progetto");
    private final JComboBox<String> sceltaCorvette = new JComboBox<String>();
    private final JTextField nomeBuild = new JTextField(26);
    private final JTextField autore = new JTextField(20);
    private final Anteprima anteprima = new Anteprima();
    private final JTextArea esito = new JTextArea();
    private final JButton esporta = new JButton("Esporta progetto");

    private SaveSlotInfo slot;
    private LettoreCorvette.Esito corrente;
    private String autorePredefinito = "";

    public SchedaEsporta(File cartellaLibreria) {
        this.cartellaLibreria = cartellaLibreria;
        setBackground(Theme.SFONDO);
        setLayout(new BorderLayout());
        add(costruisciSinistra(), BorderLayout.WEST);
        add(costruisciCentro(), BorderLayout.CENTER);
        add(costruisciPiede(), BorderLayout.SOUTH);
        aggiorna(null, null, null);
    }

    private JPanel costruisciSinistra() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(Scala.bordo(16, 16, 16, 12));
        p.setPreferredSize(Scala.dim(330, 100));

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

        p.add(intestazione);
        p.add(Box.createVerticalStrut(14));
        p.add(etichetta("Corvette da esportare"));
        p.add(Box.createVerticalStrut(4));
        p.add(sceltaCorvette);
        p.add(Box.createVerticalStrut(16));
        p.add(etichetta("Nome della build"));
        p.add(Box.createVerticalStrut(4));
        nomeBuild.setAlignmentX(Component.LEFT_ALIGNMENT);
        nomeBuild.setMaximumSize(Scala.dimLarga(28));
        p.add(nomeBuild);
        p.add(Box.createVerticalStrut(14));
        p.add(etichetta("Autore"));
        p.add(Box.createVerticalStrut(4));
        autore.setAlignmentX(Component.LEFT_ALIGNMENT);
        autore.setMaximumSize(Scala.dimLarga(28));
        p.add(autore);
        p.add(Box.createVerticalGlue());

        JLabel nota = new JLabel("<html><body style='width:280px'>"
                + "L'export non tocca il salvataggio: legge la Corvette e scrive "
                + "un nuovo file nella cartella <b>Builds</b>. Non serve alcun "
                + "backup.</body></html>");
        nota.setForeground(Theme.TESTO_TENUE);
        nota.setFont(Scala.font(Font.PLAIN, 11));
        nota.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(nota);
        return p;
    }

    private Component costruisciCentro() {
        esito.setEditable(false);
        esito.setBackground(Theme.SUPERFICIE);
        esito.setForeground(Theme.TESTO);
        esito.setFont(Theme.monospazio());
        esito.setBorder(Scala.bordo(10, 12, 10, 12));

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(Scala.bordo(16, 0, 0, 16));

        JLabel t = new JLabel("Anteprima");
        t.setFont(Theme.sezione());
        t.setForeground(Theme.TESTO_TENUE);
        t.setBorder(Scala.bordo(0, 0, 6, 0));

        anteprima.setPreferredSize(Scala.dim(600, 240));

        JScrollPane scrollEsito = new JScrollPane(esito);
        scrollEsito.setBorder(BorderFactory.createLineBorder(Theme.BORDO));
        scrollEsito.setPreferredSize(Scala.dim(600, 180));

        p.add(t, BorderLayout.NORTH);
        p.add(anteprima, BorderLayout.CENTER);
        p.add(scrollEsito, BorderLayout.SOUTH);
        return p;
    }

    private JPanel costruisciPiede() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        p.setOpaque(false);
        p.setBorder(Scala.bordo(8, 16, 14, 16));

        JButton apri = new JButton("Apri cartella Builds");
        apri.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                apriCartella();
            }
        });

        esporta.setFont(Scala.font(Font.BOLD, 13));
        esporta.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                esportaOra();
            }
        });

        p.add(apri);
        p.add(esporta);
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

    public void aggiorna(SaveSlotInfo slot, LettoreCorvette.Esito esitoSlot, String autoreGiocatore) {
        this.slot = slot;
        this.corrente = esitoSlot;
        if (autoreGiocatore != null && !autoreGiocatore.isEmpty()) {
            this.autorePredefinito = autoreGiocatore;
            autore.setText(autoreGiocatore);
        }

        sceltaCorvette.removeAllItems();
        if (esitoSlot == null || esitoSlot.corvette.isEmpty()) {
            sceltaCorvette.addItem("(nessuna Corvette in questo slot)");
            esporta.setEnabled(false);
            anteprima.mostra(null);
            esito.setText("Questo slot non contiene Corvette da esportare.\n\n"
                    + "Le Corvette sono basi di tipo PlayerShipBase: se non ne\n"
                    + "compare nessuna, in questo salvataggio non ne hai ancora una.");
            return;
        }
        for (int i = 0; i < esitoSlot.corvette.size(); i++) {
            Corvette c = esitoSlot.corvette.get(i);
            sceltaCorvette.addItem((c.isAttiva() ? "\u26A0 " : "")
                    + c.getNome() + "   (" + c.getNumeroModuli() + " moduli)");
        }
        // il costruttore disattiva il pulsante: va riattivato quando i dati
        // arrivano davvero, altrimenti resta grigio per sempre
        esporta.setEnabled(true);
        sceltaCorvette.setSelectedIndex(0);
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
        anteprima.mostra(c);
        if (c == null) {
            return;
        }
        if (nomeBuild.getText().trim().isEmpty()) {
            nomeBuild.setText(c.getNome());
        }
        StringBuilder b = new StringBuilder();
        b.append("CORVETTE SELEZIONATA\n\n");
        b.append("Nome          : ").append(c.getNome()).append('\n');
        b.append("Moduli        : ").append(c.getNumeroModuli()).append('\n');
        b.append("Tipi distinti : ").append(c.getModuliDistinti()).append('\n');
        b.append("Nave collegata: indice ").append(c.getIndiceNave()).append('\n');
        if (c.isAttiva()) {
            b.append("\nQuesta e' la Corvette che stai usando in gioco.\n");
            b.append("Esportarla e' possibile, ma ricorda che il gioco la\n");
            b.append("ricarica in memoria: meglio esportarne un'altra.\n");
        }
        esito.setText(b.toString());
        esito.setCaretPosition(0);
    }

    // -------------------------------------------------------------- azioni

    private void esportaOra() {
        Corvette c = corvetteCorrente();
        if (c == null) {
            return;
        }
        String nome = nomeBuild.getText().trim();
        if (nome.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Dai un nome alla build.",
                    "Nome mancante", JOptionPane.WARNING_MESSAGE);
            return;
        }
        eY base = (eY) c.getNodo();
        if (base == null) {
            JOptionPane.showMessageDialog(this, "Non riesco a rileggere la Corvette.",
                    "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }
        eV oggetti;
        try {
            oggetti = base.d("Objects");
        } catch (Throwable t) {
            oggetti = null;
        }
        if (oggetti == null || oggetti.size() == 0) {
            JOptionPane.showMessageDialog(this, "Questa Corvette non ha moduli da esportare.",
                    "Niente da esportare", JOptionPane.WARNING_MESSAGE);
            return;
        }

        File destinazione = new File(cartellaLibreria, WrapperBuild.nomeFileSicuro(nome));
        if (destinazione.exists()) {
            int r = JOptionPane.showConfirmDialog(this,
                    "Esiste gia' un progetto chiamato:\n" + destinazione.getName()
                            + "\n\nSovrascriverlo?",
                    "File esistente", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r != JOptionPane.YES_OPTION) {
                return;
            }
        }
        try {
            int n = WrapperBuild.scrivi(destinazione, nome, autore.getText().trim(), oggetti);
            StringBuilder b = new StringBuilder();
            b.append("ESPORTAZIONE RIUSCITA\n\n");
            b.append("Progetto   : ").append(destinazione.getName()).append('\n');
            b.append("Percorso   : ").append(destinazione.getAbsolutePath()).append('\n');
            b.append("Moduli     : ").append(n).append('\n');
            b.append("Formato    : ").append(WrapperBuild.FORMATO)
                    .append(" v").append(WrapperBuild.VERSIONE).append('\n');
            b.append("Data UTC   : ").append(WrapperBuild.adessoUtc()).append("\n\n");
            b.append("Il salvataggio non e' stato toccato.\n");
            b.append("Trovi il progetto nella scheda Libreria, pronto da condividere.");
            esito.setText(b.toString());
            esito.setCaretPosition(0);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Errore durante l'esportazione:\n"
                    + e.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void apriCartella() {
        try {
            if (!cartellaLibreria.isDirectory() && !cartellaLibreria.mkdirs()) {
                JOptionPane.showMessageDialog(this,
                        "Non riesco a creare la cartella:\n" + cartellaLibreria.getAbsolutePath(),
                        "Errore", JOptionPane.ERROR_MESSAGE);
                return;
            }
            java.awt.Desktop.getDesktop().open(cartellaLibreria);
        } catch (Throwable t) {
            JOptionPane.showMessageDialog(this,
                    "Cartella dei progetti:\n" + cartellaLibreria.getAbsolutePath(),
                    "Cartella", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** Chiamato dalla libreria quando cambia il contenuto della cartella. */
    public void suggerisciNome(String nome) {
        if (nome != null && !nome.isEmpty()) {
            nomeBuild.setText(nome);
        }
    }
}
