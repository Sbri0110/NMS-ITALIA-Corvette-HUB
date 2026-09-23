package it.nmsitalia.corvettehub.detect;

import nomanssave.aH;
import nomanssave.fq;
import nomanssave.fR;
import nomanssave.ft;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Rilevamento automatico della cartella dei salvataggi (requisito R1).
 *
 * Strategia imposta dalla specifica (3.4):
 *  1. cerca i candidati noti invece di chiedere all'utente;
 *  2. VALIDA ogni candidato provando davvero a convertire un salvataggio,
 *     non limitandosi a controllare che la cartella esista;
 *  3. se nessun candidato regge, si passa alla scelta manuale.
 *
 * Il percorso Steam/GOG/Epic e il percorso Xbox app su PC sono entrambi
 * coperti. Il riconoscimento del tipo lo fa la libreria di conversione
 * guardando il contenuto: containers.index -> Xbox, save*.hg -> Steam.
 */
public final class SaveLocator {

    /** Ascoltatore vuoto: la libreria lo richiede, a noi non serve. */
    private static final fR NESSUN_ASCOLTATORE = new fR() {
        @Override
        public void a(fq storage) {
            // nessuna azione: non stiamo scrivendo nulla
        }

        @Override
        public void a(fq storage, int indice, String messaggio) {
            // nessuna azione
        }
    };

    private SaveLocator() {
    }

    /** Esito di un rilevamento riuscito. */
    public static final class Rilevamento {
        public final File cartella;
        public final String tipo;
        public final fq storage;

        Rilevamento(File cartella, String tipo, fq storage) {
            this.cartella = cartella;
            this.tipo = tipo;
            this.storage = storage;
        }

        public String etichetta() {
            return tipo + " · " + cartella.getAbsolutePath();
        }
    }

    /**
     * Prepara i campi statici della libreria di conversione che altrimenti
     * verrebbero impostati dall'editor completo.
     *
     * aH.cG e' la cartella in cui la libreria cerca i propri backup, e li
     * elenca come se fossero file dello slot: se contiene degli zip, lo slot
     * risulta avere piu' file di quanti ne abbia davvero. Il nostro tool ha un
     * suo schema di backup (copie complete in Backup/AAAA-MM-GG_HH-MM-SS/) e non
     * usa quello della libreria, quindi le diamo una cartella dedicata che
     * resta vuota.
     */
    public static void preparaAmbiente(File cartellaProgramma) {
        File backup = new File(cartellaProgramma, "Backup");
        if (!backup.isDirectory() && !backup.mkdirs()) {
            System.err.println("[CorvetteHUB] Impossibile creare " + backup.getAbsolutePath());
        }
        File indice = new File(backup, "_indice-libreria");
        if (!indice.isDirectory() && !indice.mkdirs()) {
            System.err.println("[CorvetteHUB] Impossibile creare " + indice.getAbsolutePath());
        }
        aH.cD = cartellaProgramma;
        aH.cG = indice;
    }

    /**
     * Prova ad aprire una cartella come archivio di salvataggi.
     *
     * @return il rilevamento, oppure null se la cartella non contiene un
     *         salvataggio valido e leggibile.
     */
    public static Rilevamento apri(File cartella) {
        if (cartella == null || !cartella.exists() || !cartella.isDirectory()) {
            return null;
        }
        fq storage = fq.a(cartella, NESSUN_ASCOLTATORE);
        if (storage == null) {
            return null;
        }
        String tipo = fq.c(storage);
        if (tipo == null) {
            return null;
        }
        return new Rilevamento(cartella, tipo, storage);
    }

    /**
     * Apre una cartella forzando il tipo indicato (usato quando l'utente
     * sceglie la cartella a mano e il tipo e' gia' noto).
     */
    public static Rilevamento apri(File cartella, String tipo) {
        if (tipo == null) {
            return apri(cartella);
        }
        fq storage = fq.a(tipo, cartella, NESSUN_ASCOLTATORE);
        if (storage == null) {
            return apri(cartella);
        }
        return new Rilevamento(cartella, tipo, storage);
    }

    /** Conta gli slot non vuoti, per capire se un candidato e' utile. */
    public static int contaSlotPieni(fq storage) {
        if (storage == null) {
            return 0;
        }
        int n = 0;
        try {
            ft[] slots = storage.bU();
            for (int i = 0; i < slots.length; i++) {
                if (slots[i] != null && !slots[i].isEmpty()) {
                    n++;
                }
            }
        } catch (Throwable t) {
            return 0;
        }
        return n;
    }

    /** Cartelle candidate, in ordine di probabilita'. */
    public static List<File> candidati() {
        List<File> out = new ArrayList<File>();

        String roaming = System.getenv("APPDATA");
        if (roaming != null) {
            File nms = new File(new File(roaming, "HelloGames"), "NMS");
            File[] profili = nms.listFiles();
            if (profili != null) {
                for (int i = 0; i < profili.length; i++) {
                    if (profili[i].isDirectory()) {
                        out.add(profili[i]);
                    }
                }
            }
            // a volte i file stanno direttamente in NMS\
            if (nms.isDirectory()) {
                out.add(nms);
            }
        }

        String locale = System.getenv("LOCALAPPDATA");
        if (locale != null) {
            File packages = new File(locale, "Packages");
            File[] pacchetti = packages.listFiles();
            if (pacchetti != null) {
                for (int i = 0; i < pacchetti.length; i++) {
                    String nome = pacchetti[i].getName();
                    if (!pacchetti[i].isDirectory()
                            || !nome.startsWith("HelloGames.NoMansSky_")) {
                        continue;
                    }
                    File wgs = new File(new File(pacchetti[i], "SystemAppData"), "wgs");
                    File[] contenitori = wgs.listFiles();
                    if (contenitori != null) {
                        for (int j = 0; j < contenitori.length; j++) {
                            if (contenitori[j].isDirectory()) {
                                out.add(contenitori[j]);
                            }
                        }
                    }
                }
            }
        }
        return out;
    }

    /**
     * Cerca il primo candidato valido.
     *
     * @return il rilevamento, oppure null se nessun candidato contiene un
     *         salvataggio leggibile.
     */
    public static Rilevamento rileva() {
        List<File> candidati = candidati();
        for (int i = 0; i < candidati.size(); i++) {
            Rilevamento r = apri(candidati.get(i));
            if (r != null && contaSlotPieni(r.storage) > 0) {
                return r;
            }
        }
        return null;
    }
}
