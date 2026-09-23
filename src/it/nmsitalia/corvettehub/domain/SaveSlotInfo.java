package it.nmsitalia.corvettehub.domain;

import nomanssave.eY;
import nomanssave.fn;
import nomanssave.fs;
import nomanssave.ft;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Uno slot di salvataggio, come lo vede l'utente (requisito R2).
 *
 * Uno slot occupa due file: lo stato corrente e il punto di ripristino. Qui
 * li teniamo entrambi, perche' in fase di scrittura andranno aggiornati
 * insieme (specifica 3.3).
 */
public final class SaveSlotInfo {

    private final int indice;
    private final boolean vuoto;
    private final List<fs> file = new ArrayList<fs>();
    private fn modalita;

    private boolean modelloLetto;
    private eY modello;
    private String erroreLettura;

    public SaveSlotInfo(ft slot) {
        this.indice = slot.getIndex();
        this.vuoto = slot.isEmpty();
        this.modalita = slot.L();
        try {
            fs[] f = slot.bX();
            if (f != null) {
                for (int i = 0; i < f.length; i++) {
                    if (f[i] != null) {
                        file.add(f[i]);
                    }
                }
            }
        } catch (Throwable t) {
            erroreLettura = "Impossibile leggere i file dello slot: " + t;
        }
    }

    public int getIndice() {
        return indice;
    }

    /** Vero se lo slot e' libero: si mostra comunque, come nel gioco. */
    public boolean isVuoto() {
        return vuoto;
    }

    /** Numero mostrato all'utente (1-based, come nel gioco). */
    public int getNumero() {
        return indice + 1;
    }

    public List<fs> getFile() {
        return file;
    }

    public String getModalita() {
        if (modalita == null) {
            return "sconosciuta";
        }
        return nomeModalita(modalita);
    }

    public String getErroreLettura() {
        return erroreLettura;
    }

    /** Data dell'ultima modifica, presa dal file piu' recente dello slot. */
    public long getUltimaModifica() {
        long max = 0;
        for (int i = 0; i < file.size(); i++) {
            long t = file.get(i).lastModified();
            if (t > max) {
                max = t;
            }
        }
        return max;
    }

    public String getDataFormattata() {
        long t = getUltimaModifica();
        if (t <= 0) {
            return "data sconosciuta";
        }
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(t));
    }

    /**
     * Il modello convertito del file piu' recente dello slot.
     * La conversione avviene una sola volta, alla prima richiesta: e' la parte
     * costosa e non serve finche' l'utente non sceglie lo slot.
     */
    public eY getModello() {
        if (modelloLetto) {
            return modello;
        }
        modelloLetto = true;
        fs piuRecente = null;
        long max = Long.MIN_VALUE;
        for (int i = 0; i < file.size(); i++) {
            long t = file.get(i).lastModified();
            if (t > max) {
                max = t;
                piuRecente = file.get(i);
            }
        }
        if (piuRecente == null) {
            erroreLettura = "Lo slot non contiene file leggibili.";
            return null;
        }
        try {
            modello = piuRecente.M();
        } catch (Throwable t) {
            erroreLettura = "Conversione fallita: " + t;
            modello = null;
        }
        return modello;
    }

    /** Nome del salvataggio (CommonStateData.SaveName), se leggibile. */
    public String getNomeSalvataggio() {
        eY m = getModello();
        if (m == null) {
            return null;
        }
        try {
            return m.getValueAsString("CommonStateData.SaveName");
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Ore di gioco complessive, se leggibili.
     *
     * Verificato sul salvataggio reale del 2026-09-23: il campo sta in
     * CommonStateData.TotalPlayTime (NON in PlayerStateData, come pure
     * suggerirebbe il codice di Atlante) ed e' un intero in SECONDI.
     * Valore misurato: 669511 secondi = 185 ore e 58 minuti, che coincide
     * con il "185:58" stampato dalla libreria di conversione.
     */
    public double getOreGiocate() {
        eY m = getModello();
        if (m == null) {
            return -1;
        }
        try {
            long secondi = m.K("CommonStateData.TotalPlayTime");
            if (secondi <= 0L) {
                return -1;
            }
            return secondi / 3600.0;
        } catch (Throwable t) {
            return -1;
        }
    }

    public String getOreFormattate() {
        double ore = getOreGiocate();
        if (ore < 0) {
            return "ore non disponibili";
        }
        long h = (long) ore;
        long min = Math.round((ore - h) * 60);
        return h + " ore e " + min + " minuti";
    }

    /** Etichetta compatta per l'elenco, senza convertire il file. */
    public String etichettaBreve() {
        return "Slot " + getNumero() + " · " + getModalita() + " · " + getDataFormattata();
    }

    @Override
    public String toString() {
        return etichettaBreve();
    }

    /** Traduzione delle modalita' di gioco. */
    public static String nomeModalita(fn m) {
        String grezzo = m.name();
        // Le costanti dell'enum nel salvataggio sono offuscate: il nome vero
        // arriva da PlayerStateData.DifficultyState.Preset.DifficultyPresetType,
        // che qui non abbiamo. Mostriamo il valore grezzo se non riconosciuto.
        if ("lm".equals(grezzo)) return "Normale";
        if ("ln".equals(grezzo)) return "Creativa";
        if ("lo".equals(grezzo)) return "Sopravvivenza";
        if ("lp".equals(grezzo)) return "Permadeath";
        if ("lq".equals(grezzo)) return "Rilassata";
        if ("lr".equals(grezzo)) return "Personalizzata";
        return grezzo;
    }
}
