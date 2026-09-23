package it.nmsitalia.corvettehub.safety;

import it.nmsitalia.corvettehub.detect.SaveLocator;
import it.nmsitalia.corvettehub.domain.SaveSlotInfo;
import nomanssave.eY;
import nomanssave.fj;
import nomanssave.fs;
import nomanssave.gX;
import nomanssave.gZ;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Scrittura di un salvataggio Xbox (formato WGS) fatta bene.
 *
 * PERCHE' NON SI USA LA SCRITTURA DELLA LIBRERIA
 * ----------------------------------------------
 * La libreria di conversione legge e scrive, ma il descrittore del contenitore
 * lo riscrive in una forma diversa da quella del gioco:
 *
 *   - legge 280 byte e ne scrive 280, buttando via 80 byte di coda che
 *     contengono la difficolta' di gioco (verificato: "Personalizzate");
 *   - a offset 16 scrive la dimensione COMPRESSA, mentre gioco e pipeline
 *     verificata ci mettono la DECOMPRESSA.
 *
 * Questa classe invece parte dai file ORIGINALI e ne cambia il minimo
 * indispensabile, lasciando intatto tutto il resto.
 *
 * IL PROCEDIMENTO
 * ---------------
 *   1. controlla che il gioco sia chiuso
 *   2. fa un backup completo e ne verifica l'esito; se fallisce, si ferma
 *   3. decompressa il payload originale e ne apprende la forma esatta
 *      (lunghezza decompressa e byte di coda)
 *   4. applica la modifica al modello
 *   5. riserializza nella forma offuscata e ricomprime
 *   6. scrive il payload con file temporaneo e sostituzione atomica
 *   7. aggiorna il descrittore: SOLO il campo dimensione a offset 16,
 *      conservando gli 80 byte di coda
 *   8. aggiorna containers.index: SOLO il campo dimensione totale
 *   9. rilegge tutto e confronta; se non torna, ripristina il backup
 *
 * Tutto cio' che non deve cambiare resta byte per byte quello che era.
 */
public final class ScrittoreSalvataggio {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int OFFSET_DIMENSIONE_DESCRITTORE = 16;

    /**
     * I nomi dei file di salvataggio del formato a file singoli: save.hg,
     * save2.hg, save3.hg... Il pattern serve a non confondere i salvataggi con
     * il manifest mf_save.hg, con accountdata.hg o con i backup zip che la
     * libreria puo' elencare fra i file dello slot.
     */
    private static final java.util.regex.Pattern FILE_SALVATAGGIO =
            java.util.regex.Pattern.compile("save\\d*\\.hg");

    /** La modifica da applicare al modello, in memoria. */
    public interface Modifica {
        String descrizione();

        void applica(eY radice);
    }

    /** Esito di una scrittura. */
    public static final class Esito {
        public boolean riuscito;
        public String messaggio = "";
        public String dettaglio = "";
        public File backup;

        static Esito errore(String messaggio) {
            Esito e = new Esito();
            e.riuscito = false;
            e.messaggio = messaggio;
            return e;
        }
    }

    private ScrittoreSalvataggio() {
    }

    // ------------------------------------------------------------- scrittura

    public static Esito scrivi(SaveLocator.Rilevamento rilevamento, SaveSlotInfo slot,
                               Modifica modifica) {
        Esito esito = new Esito();
        StringBuilder log = new StringBuilder();

        if (rilevamento == null || slot == null) {
            return Esito.errore("Nessun salvataggio selezionato.");
        }
        if (giocoInEsecuzione()) {
            return Esito.errore("No Man's Sky e' in esecuzione. Chiudi il gioco prima di "
                    + "scrivere, altrimenti il gioco potrebbe sovrascrivere le modifiche.");
        }

        // ---- 1. i file dello slot
        //  Uno slot ne ha due: lo stato corrente e il punto di ripristino. La
        //  specifica (3.3) impone di aggiornarli ENTRAMBI, altrimenti il gioco
        //  puo' ricaricare la versione vecchia.
        File radiceStorage = rilevamento.cartella;
        File indice = new File(radiceStorage, "containers.index");
        if (!indice.isFile()) {
            // Non e' il formato a contenitori dell'app Xbox: e' il formato a
            // file singoli di Steam, GOG ed Epic. Il percorso e' diverso.
            return scriviFileSingolo(rilevamento, slot, modifica);
        }
        List<fs> fileSlot = slot.getFile();
        if (fileSlot.isEmpty()) {
            return Esito.errore("Questo slot non contiene file leggibili.");
        }

        List<Unita> unita = new ArrayList<Unita>();
        for (int i = 0; i < fileSlot.size(); i++) {
            Unita u = new Unita();
            u.file = fileSlot.get(i);
            try {
                u.modello = u.file.M();
            } catch (Throwable t) {
                return Esito.errore("Non riesco a leggere il file " + (i + 1)
                        + " dello slot: " + t);
            }
            if (u.modello == null) {
                return Esito.errore("Il file " + (i + 1) + " dello slot non si converte.");
            }
            byte[] atteso;
            try {
                atteso = fj.g(u.modello);
            } catch (Throwable t) {
                return Esito.errore("Non riesco a serializzare il file " + (i + 1) + ": " + t);
            }
            // Il contenitore si riconosce DAL CONTENUTO, non dal nome: i due file
            // di uno slot hanno lo stesso nome salvataggio e la stessa
            // descrizione, quindi cercare per nome rischia di far scrivere nel
            // file sbagliato (e' successo: e' stato modificato l'Auto invece del
            // Manual).
            File[] contenitore = trovaContenitore(radiceStorage, atteso);
            if (contenitore == null) {
                return Esito.errore("Non riesco a individuare il contenitore del file "
                        + (i + 1) + " dello slot.");
            }
            // La libreria puo' elencare lo stesso contenitore piu' volte (nel suo
            // elenco finiscono anche i propri backup). Lo si lavora una volta sola.
            boolean giaInLavorazione = false;
            for (int k = 0; k < unita.size(); k++) {
                if (unita.get(k).descrittore.equals(contenitore[0])) {
                    giaInLavorazione = true;
                    break;
                }
            }
            if (giaInLavorazione) {
                continue;
            }
            u.descrittore = contenitore[0];
            u.payload = contenitore[1];
            u.descrittoreOriginale = leggi(u.descrittore);
            if (u.descrittoreOriginale.length < 360) {
                return Esito.errore("Il descrittore del file " + (i + 1)
                        + " e' piu' corto del previsto ("
                        + u.descrittoreOriginale.length + " byte): non so interpretarlo.");
            }
            u.dimensionePayloadOriginale = u.payload.length();
            u.dimensioneDecompressaOriginale = leggiInt(u.descrittoreOriginale,
                    OFFSET_DIMENSIONE_DESCRITTORE) & 0xFFFFFFFFL;
            try {
                u.decompressoOriginale = decomprimi(u.payload);
            } catch (Throwable t) {
                return Esito.errore("Non riesco a decomprimere il file " + (i + 1) + ": " + t);
            }
            if (u.decompressoOriginale.length != u.dimensioneDecompressaOriginale) {
                return Esito.errore("Il file " + (i + 1) + " non e' nella forma attesa: il "
                        + "descrittore dichiara " + u.dimensioneDecompressaOriginale
                        + " byte decompressi, il contenuto ne misura "
                        + u.decompressoOriginale.length + ".");
            }
            u.coda = codaOriginale(u.decompressoOriginale);
            unita.add(u);

            log.append("file ").append(i + 1).append("  ").append(u.file.getDescription())
                    .append('\n');
            log.append("   payload originale : ").append(u.dimensionePayloadOriginale)
                    .append(" byte\n");
            log.append("   decompresso       : ").append(u.dimensioneDecompressaOriginale)
                    .append(" byte\n");
            log.append("   coda dopo il JSON : ").append(u.coda.length).append(" byte")
                    .append(u.coda.length == 1 ? " (valore " + (u.coda[0] & 0xFF) + ")" : "")
                    .append('\n');
        }

        // ---- 2. backup di tutto, prima di toccare qualsiasi cosa
        List<File> cartelle = new ArrayList<File>();
        for (int i = 0; i < unita.size(); i++) {
            File c = unita.get(i).descrittore.getParentFile();
            if (!cartelle.contains(c)) {
                cartelle.add(c);
            }
        }
        File backup = creaBackup(cartelle, indice);
        if (backup == null) {
            return Esito.errore("Il backup non e' riuscito. L'operazione si interrompe qui: "
                    + "non scrivo nulla senza una copia di sicurezza verificata.");
        }
        esito.backup = backup;
        log.append("backup                 : ").append(backup.getAbsolutePath()).append("\n");
        registra("backup creato: " + backup.getAbsolutePath());

        // ---- 3. modifica e scrittura, file per file
        String primaDescrizione = modifica.descrizione();
        registra("scrittura richiesta: " + primaDescrizione
                + "  (slot " + slot.getNumero() + ")");
        byte[] indiceOriginale = leggi(indice);
        byte[] indiceCorrente = indiceOriginale;
        for (int i = 0; i < unita.size(); i++) {
            Unita u = unita.get(i);
            try {
                modifica.applica(u.modello);
            } catch (Throwable t) {
                ripristina(backup, cartelle, indice);
                return Esito.errore("La modifica non e' riuscita: " + t);
            }
            byte[] jsonNuovo;
            try {
                jsonNuovo = fj.g(u.modello);
            } catch (Throwable t) {
                ripristina(backup, cartelle, indice);
                return Esito.errore("Non riesco a riserializzare il file " + (i + 1) + ": " + t);
            }
            u.decompressoNuovo = unisci(jsonNuovo, u.coda);
            try {
                u.payloadNuovo = comprimi(u.decompressoNuovo);
            } catch (Throwable t) {
                ripristina(backup, cartelle, indice);
                return Esito.errore("Non riesco a comprimere il file " + (i + 1) + ": " + t);
            }
            try {
                scriviAtomico(u.payload, u.payloadNuovo);
            } catch (Throwable t) {
                ripristina(backup, cartelle, indice);
                return Esito.errore("Scrittura del file " + (i + 1) + " non riuscita: " + t);
            }

            // descrittore: cambia SOLO il campo dimensione a offset 16, la coda
            // di 80 byte resta quella originale
            byte[] descrittoreNuovo = u.descrittoreOriginale.clone();
            scriviInt(descrittoreNuovo, OFFSET_DIMENSIONE_DESCRITTORE, u.decompressoNuovo.length);
            try {
                scriviAtomico(u.descrittore, descrittoreNuovo);
            } catch (Throwable t) {
                ripristina(backup, cartelle, indice);
                return Esito.errore("Descrittore del file " + (i + 1)
                        + " non aggiornato, backup ripristinato: " + t);
            }

            // indice: cambia SOLO la dimensione totale del contenitore
            long vecchioTotale = u.dimensionePayloadOriginale + u.descrittoreOriginale.length;
            int offTotale = cercaLong(indiceCorrente, vecchioTotale);
            if (offTotale >= 0) {
                byte[] idx = indiceCorrente.clone();
                scriviLong(idx, offTotale, u.payloadNuovo.length + u.descrittoreOriginale.length);
                try {
                    scriviAtomico(indice, idx);
                } catch (Throwable t) {
                    ripristina(backup, cartelle, indice);
                    return Esito.errore("Indice non aggiornato, backup ripristinato: " + t);
                }
                indiceCorrente = idx;
            }
            log.append("   scritto           : payload ").append(u.payloadNuovo.length)
                    .append(" byte, decompresso ").append(u.decompressoNuovo.length)
                    .append(" byte, descrittore ").append(u.descrittoreOriginale.length)
                    .append(" byte (coda conservata)")
                    .append(offTotale >= 0 ? ", indice a offset " + offTotale : "")
                    .append('\n');
        }

        // ---- 4. verifica in rilettura
        String verifica = verifica(unita, indice, indiceOriginale, radiceStorage);
        if (verifica != null) {
            ripristina(backup, cartelle, indice);
            registra("SCRITTURA FALLITA, backup ripristinato: " + verifica);
            return Esito.errore("Verifica fallita, backup ripristinato.\n\n" + verifica);
        }
        log.append("verifica               : superata\n");

        esito.riuscito = true;
        esito.messaggio = "Modifica scritta e verificata su " + unita.size()
                + (unita.size() == 1 ? " file." : " file dello slot.");
        registra("scrittura riuscita su " + unita.size() + " file: " + primaDescrizione);
        esito.dettaglio = "Modifica: " + primaDescrizione + "\n\n" + log.toString()
                + "\nBackup in: " + backup.getAbsolutePath()
                + "\n\nRiapri il gioco e controlla. Se qualcosa non va, ripristina il backup.";
        return esito;
    }

    // ------------------------------------------------ file singoli (Steam)

    /**
     * Scrittura sul formato a file singoli: Steam, GOG ed Epic.
     *
     * Qui non c'e' nessun contenitore e nessun containers.index: ogni slot e'
     * un file save*.hg nella cartella del profilo, con accanto il proprio
     * manifest mf_save*.hg. Il manifest contiene nome del salvataggio, ore di
     * gioco, dimensione e impronte: se restasse quello vecchio, il gioco
     * leggerebbe metadati che non descrivono piu' il file.
     *
     * Per questo la scrittura non e' artigianale come nel caso Xbox: si usa il
     * metodo b(eY) della libreria, che riscrive il file compresso E aggiorna il
     * manifest. Il descrittore a 360 byte che ci aveva costretto a riscrivere
     * la scrittura Xbox qui non esiste, quindi non c'e' motivo di non fidarsi
     * del codice della libreria.
     */
    private static Esito scriviFileSingolo(SaveLocator.Rilevamento rilevamento,
                                           SaveSlotInfo slot, Modifica modifica) {
        Esito esito = new Esito();
        StringBuilder log = new StringBuilder();

        File radiceStorage = rilevamento.cartella;

        // I file che sono davvero salvataggi. La libreria, nel suo elenco, puo'
        // infilare anche i propri backup zip: quelli non si toccano.
        List<fs> salvataggi = new ArrayList<fs>();
        List<fs> tutti = slot.getFile();
        for (int i = 0; i < tutti.size(); i++) {
            String nome = tutti.get(i).K();
            if (nome != null && FILE_SALVATAGGIO.matcher(nome).matches()) {
                salvataggi.add(tutti.get(i));
            }
        }
        if (salvataggi.isEmpty()) {
            return Esito.errore("In questo slot non trovo file di salvataggio leggibili.");
        }

        // La modifica si applica al file piu' recente: e' quello che il gioco
        // considera corrente.
        fs modelloDa = null;
        long massimo = Long.MIN_VALUE;
        for (int i = 0; i < salvataggi.size(); i++) {
            long t = salvataggi.get(i).lastModified();
            if (t > massimo) {
                massimo = t;
                modelloDa = salvataggi.get(i);
            }
        }
        eY modello;
        try {
            modello = modelloDa.M();
        } catch (Throwable t) {
            return Esito.errore("Non riesco a leggere " + modelloDa.K() + ": " + t);
        }
        if (modello == null) {
            return Esito.errore("Il file " + modelloDa.K() + " non si converte.");
        }

        // Backup: i file di salvataggio E i loro manifest. Senza il manifest un
        // ripristino rimetterebbe a posto un file che il gioco non sa piu'
        // descrivere, e lo slot risulterebbe vuoto o incoerente.
        List<File> daSalvare = new ArrayList<File>();
        for (int i = 0; i < salvataggi.size(); i++) {
            File f = new File(radiceStorage, salvataggi.get(i).K());
            if (f.isFile()) {
                daSalvare.add(f);
            }
            File mf = new File(radiceStorage, "mf_" + salvataggi.get(i).K());
            if (mf.isFile()) {
                daSalvare.add(mf);
            }
        }
        File backup = creaBackupFile(daSalvare);
        if (backup == null) {
            return Esito.errore("Il backup non e' riuscito. L'operazione si interrompe qui: "
                    + "non scrivo nulla senza una copia di sicurezza verificata.");
        }
        esito.backup = backup;
        log.append("backup                 : ").append(backup.getAbsolutePath()).append('\n');
        registra("backup creato: " + backup.getAbsolutePath());

        String descrizione = modifica.descrizione();
        registra("scrittura richiesta: " + descrizione
                + "  (slot " + slot.getNumero() + ")");

        try {
            modifica.applica(modello);
        } catch (Throwable t) {
            return Esito.errore("La modifica non e' riuscita: " + t);
        }

        // Si riscrivono TUTTI i file dello slot, non solo il piu' recente: se
        // ne restasse uno vecchio, il gioco potrebbe ricaricare quello.
        for (int i = 0; i < salvataggi.size(); i++) {
            fs f = salvataggi.get(i);
            try {
                f.b(modello);
            } catch (Throwable t) {
                ripristinaFile(backup, radiceStorage);
                return Esito.errore("Scrittura di " + f.K()
                        + " non riuscita, backup ripristinato: " + t);
            }
            log.append("scritto                : ").append(f.K())
                    .append("  (").append(new File(radiceStorage, f.K()).length())
                    .append(" byte)\n");
        }

        // La libreria, mentre scrive, lascia i propri backup zip nella cartella
        // che le abbiamo indicato. Se restassero, verrebbero elencati come file
        // dello slot e lo slot risulterebbe avere piu' file del vero.
        int rimossi = pulisciBackupLibreria();
        if (rimossi > 0) {
            log.append("backup di lavoro della libreria: ").append(rimossi)
                    .append(" file rimossi\n");
        }

        String verifica = verificaFileSingolo(radiceStorage, salvataggi, modello);
        if (verifica != null) {
            ripristinaFile(backup, radiceStorage);
            registra("SCRITTURA FALLITA, backup ripristinato: " + verifica);
            return Esito.errore("Verifica fallita, backup ripristinato.\n\n" + verifica);
        }
        log.append("verifica               : superata\n");

        esito.riuscito = true;
        esito.messaggio = "Modifica scritta e verificata su " + salvataggi.size()
                + (salvataggi.size() == 1 ? " file." : " file dello slot.");
        registra("scrittura riuscita su " + salvataggi.size() + " file: " + descrizione);
        esito.dettaglio = "Modifica: " + descrizione + "\n\n" + log.toString()
                + "\nBackup in: " + backup.getAbsolutePath()
                + "\n\nRiapri il gioco e controlla. Se qualcosa non va, ripristina il backup.";
        return esito;
    }

    /**
     * Rilegge i file appena scritti e li confronta con quello che si e'
     * voluto scrivere. Controlla anche che il manifest ci sia ancora: senza,
     * il gioco non riconoscerebbe lo slot.
     */
    private static String verificaFileSingolo(File radiceStorage, List<fs> salvataggi, eY atteso) {
        try {
            byte[] attesoBytes = fj.g(atteso);
            for (int i = 0; i < salvataggi.size(); i++) {
                String nome = salvataggi.get(i).K();
                eY riletto = salvataggi.get(i).M();
                if (riletto == null) {
                    return "Il file " + nome + " non si rilegge.";
                }
                byte[] rilettoBytes = fj.g(riletto);
                if (rilettoBytes.length != attesoBytes.length) {
                    return "Il file " + nome + " riscritto misura " + rilettoBytes.length
                            + " byte invece di " + attesoBytes.length + ".";
                }
                for (int k = 0; k < attesoBytes.length; k++) {
                    if (rilettoBytes[k] != attesoBytes[k]) {
                        return "Il file " + nome + " riscritto differisce al byte " + k + ".";
                    }
                }
                File mf = new File(radiceStorage, "mf_" + nome);
                if (!mf.isFile()) {
                    return "Dopo la scrittura manca il manifest mf_" + nome + ".";
                }
            }
            SaveLocator.Rilevamento rd = SaveLocator.apri(radiceStorage);
            if (rd == null) {
                return "Il salvataggio riscritto non e' piu' apribile.";
            }
            return null;
        } catch (Throwable t) {
            return "Errore durante la verifica: " + t;
        }
    }

    /**
     * Backup di file singoli, conservati con il loro nome nella cartella del
     * backup. Vale la stessa regola del formato a contenitori: se la copia non
     * si puo' verificare con SHA-256, il backup non e' valido e la scrittura
     * non parte.
     */
    private static File creaBackupFile(List<File> file) {
        if (file.isEmpty()) {
            return null;
        }
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
        File destinazione = new File(new File(cartellaProgramma(), "Backup"), f.format(new Date()));
        if (!destinazione.mkdirs()) {
            return null;
        }

        List<File> copie = new ArrayList<File>();
        for (int i = 0; i < file.size(); i++) {
            copie.add(new File(destinazione, file.get(i).getName()));
        }
        for (int i = 0; i < file.size(); i++) {
            try {
                copia(file.get(i), copie.get(i));
                if (!sha256(file.get(i)).equals(sha256(copie.get(i)))) {
                    return null;
                }
            } catch (Throwable t) {
                return null;
            }
        }

        try {
            StringBuilder b = new StringBuilder();
            b.append("Backup NMS ITALIA Corvette HUB\n");
            b.append("Data: ").append(new Date()).append("\n\n");
            b.append("Per ripristinare: usa il pulsante Ripristina backup nel programma,\n");
            b.append("oppure rimetti questi file nella cartella del profilo.\n\n");
            for (int i = 0; i < file.size(); i++) {
                b.append(file.get(i).getAbsolutePath()).append('\n');
                b.append("    copia: ").append(copie.get(i).getName())
                        .append("   ").append(file.get(i).length()).append(" byte\n");
                b.append("    impronta: ").append(sha256(file.get(i))).append('\n');
            }
            scriviAtomico(new File(destinazione, "backup.txt"), b.toString().getBytes(UTF8));
        } catch (Throwable ignored) {
            // la nota e' un extra, non blocca il backup
        }
        return destinazione;
    }

    /** Rimette a posto i file singoli da un backup. */
    private static void ripristinaFile(File backup, File radiceStorage) {
        File[] contenuto = backup.listFiles();
        if (contenuto == null) {
            return;
        }
        for (int i = 0; i < contenuto.length; i++) {
            if (!contenuto[i].isFile() || "backup.txt".equals(contenuto[i].getName())) {
                continue;
            }
            try {
                copia(contenuto[i], new File(radiceStorage, contenuto[i].getName()));
            } catch (Throwable ignored) {
                // si prova a rimettere gli altri
            }
        }
    }

    /**
     * Ripristino per il formato a file singoli (Steam, GOG, Epic).
     *
     * Si rimettono a posto i salvataggi E i loro manifest: rimettere il file
     * senza il manifest lascerebbe lo slot con metadati che non lo
     * descrivono. Anche qui, prima di toccare qualcosa si salva lo stato
     * attuale, cosi' un backup vecchio non e' una condanna.
     */
    private static Esito ripristinaFileSingolo(File backup, File radiceStorage) {
        List<File> salvati = new ArrayList<File>();
        File[] contenuto = backup.listFiles();
        if (contenuto != null) {
            for (int i = 0; i < contenuto.length; i++) {
                if (contenuto[i].isFile()
                        && FILE_SALVATAGGIO.matcher(contenuto[i].getName()).matches()) {
                    salvati.add(contenuto[i]);
                }
            }
        }
        if (salvati.isEmpty()) {
            return Esito.errore("Dentro questo backup non trovo file di salvataggio.\n\n"
                    + "Puo' succedere se il backup e' di un'altra partita o di un'altra "
                    + "piattaforma: un backup dell'app Xbox non si puo' rimettere in una "
                    + "cartella Steam, e viceversa.");
        }

        // copia di sicurezza dello stato attuale, prima di toccare
        List<File> attuali = new ArrayList<File>();
        for (int i = 0; i < salvati.size(); i++) {
            String nome = salvati.get(i).getName();
            File f = new File(radiceStorage, nome);
            if (f.isFile()) {
                attuali.add(f);
            }
            File mf = new File(radiceStorage, "mf_" + nome);
            if (mf.isFile()) {
                attuali.add(mf);
            }
        }
        File prima = creaBackupFile(attuali);
        if (prima == null) {
            return Esito.errore("Non riesco a fare una copia di sicurezza dello stato attuale. "
                    + "Il ripristino si interrompe qui: non ho toccato nulla.");
        }

        Esito esito = new Esito();
        esito.backup = prima;
        StringBuilder log = new StringBuilder();
        log.append("stato attuale salvato in : ").append(prima.getAbsolutePath()).append('\n');
        registra("ripristino richiesto dal backup " + backup.getName());

        int ripristinati = 0;
        try {
            for (int i = 0; i < salvati.size(); i++) {
                String nome = salvati.get(i).getName();
                copia(salvati.get(i), new File(radiceStorage, nome));
                ripristinati++;
                File mf = new File(backup, "mf_" + nome);
                if (mf.isFile()) {
                    copia(mf, new File(radiceStorage, "mf_" + nome));
                    ripristinati++;
                }
                log.append("rimesso a posto      : ").append(nome).append('\n');
            }
        } catch (Throwable t) {
            return Esito.errore("Il ripristino non e' riuscito: " + t
                    + "\n\nLo stato di prima del tentativo e' in:\n" + prima.getAbsolutePath());
        }

        SaveLocator.Rilevamento rd = SaveLocator.apri(radiceStorage);
        if (rd == null || rd.storage.bU() == null) {
            return Esito.errore("I file sono stati rimessi a posto ma il salvataggio non si "
                    + "riapre.\n\nLo stato di prima del tentativo e' in:\n"
                    + prima.getAbsolutePath());
        }

        esito.riuscito = true;
        esito.messaggio = "Ripristinati " + ripristinati + " file.";
        registra("ripristino riuscito: " + ripristinati + " file rimessi dal backup "
                + backup.getName());
        esito.dettaglio = "RIPRISTINO ESEGUITO\n\n" + log.toString()
                + "\nIl salvataggio si riapre correttamente.\n\n"
                + "Lo stato che c'era prima del ripristino e' stato salvato in:\n"
                + prima.getAbsolutePath();
        return esito;
    }

    /** Svuota la cartella dei backup di lavoro della libreria. */
    private static int pulisciBackupLibreria() {
        File dir = nomanssave.aH.cG;
        if (dir == null) {
            return 0;
        }
        File[] f = dir.listFiles();
        if (f == null) {
            return 0;
        }
        int n = 0;
        for (int i = 0; i < f.length; i++) {
            if (f[i].isFile()
                    && f[i].getName().toLowerCase(Locale.ROOT).endsWith(".zip")
                    && f[i].delete()) {
                n++;
            }
        }
        return n;
    }

    /** Un file dello slot con tutto cio' che serve a riscriverlo. */
    private static final class Unita {
        fs file;
        eY modello;
        File descrittore;
        File payload;
        byte[] descrittoreOriginale;
        byte[] decompressoOriginale;
        byte[] coda;
        byte[] decompressoNuovo;
        byte[] payloadNuovo;
        long dimensionePayloadOriginale;
        long dimensioneDecompressaOriginale;
    }

    // ------------------------------------------------------------ verifica

    private static String verifica(List<Unita> unita, File indice, byte[] indiceOriginale,
                                   File radiceStorage) {
        try {
            for (int i = 0; i < unita.size(); i++) {
                Unita u = unita.get(i);
                byte[] riletto = decomprimi(u.payload);
                if (riletto.length != u.decompressoNuovo.length) {
                    return "Il file " + (i + 1) + " riscritto misura " + riletto.length
                            + " byte invece di " + u.decompressoNuovo.length + ".";
                }
                for (int k = 0; k < riletto.length; k++) {
                    if (riletto[k] != u.decompressoNuovo[k]) {
                        return "Il file " + (i + 1) + " riscritto differisce al byte " + k + ".";
                    }
                }
                byte[] d = leggi(u.descrittore);
                if (d.length != u.descrittoreOriginale.length) {
                    return "Il descrittore del file " + (i + 1) + " e\' diventato di "
                            + d.length + " byte invece di " + u.descrittoreOriginale.length + ".";
                }
                long dichiarata = leggiInt(d, OFFSET_DIMENSIONE_DESCRITTORE) & 0xFFFFFFFFL;
                if (dichiarata != u.decompressoNuovo.length) {
                    return "Il descrittore del file " + (i + 1) + " dichiara " + dichiarata
                            + " byte invece di " + u.decompressoNuovo.length + ".";
                }
                for (int k = 280; k < u.descrittoreOriginale.length; k++) {
                    if (d[k] != u.descrittoreOriginale[k]) {
                        return "La coda del descrittore del file " + (i + 1)
                                + " e\' cambiata al byte " + k + ".";
                    }
                }
            }
            byte[] idx = leggi(indice);
            if (idx.length != indiceOriginale.length) {
                return "containers.index e\' diventato di " + idx.length + " byte invece di "
                        + indiceOriginale.length + ".";
            }
            SaveLocator.Rilevamento rd = SaveLocator.apri(radiceStorage);
            if (rd == null) {
                return "Il salvataggio riscritto non e\' piu\' apribile.";
            }
            if (rd.storage.bU() == null) {
                return "Il salvataggio riscritto non espone piu\' gli slot.";
            }
            return null;
        } catch (Throwable t) {
            return "Errore durante la verifica: " + t;
        }
    }

    // ------------------------------------------------------------- backup

    /**
     * Copia le cartelle dei contenitori e l'indice, e verifica che combacino.
     *
     * La copia conserva la struttura: dentro il backup c'e' una sottocartella
     * per ogni contenitore, con dentro i suoi file. Cosi' il ripristino sa
     * esattamente dove rimettere ogni cosa, senza doverlo indovinare.
     */
    private static File creaBackup(List<File> cartelle, File indice) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
        File destinazione = new File(new File(cartellaProgramma(), "Backup"), f.format(new Date()));
        if (!destinazione.mkdirs()) {
            return null;
        }

        List<File> originali = new ArrayList<File>();
        List<File> copie = new ArrayList<File>();

        for (int c = 0; c < cartelle.size(); c++) {
            File contenitore = cartelle.get(c);
            File sotto = new File(destinazione, contenitore.getName());
            if (!sotto.isDirectory() && !sotto.mkdirs()) {
                return null;
            }
            File[] contenuto = contenitore.listFiles();
            if (contenuto == null) {
                continue;
            }
            for (int i = 0; i < contenuto.length; i++) {
                if (contenuto[i].isFile()) {
                    originali.add(contenuto[i]);
                    copie.add(new File(sotto, contenuto[i].getName()));
                }
            }
        }
        originali.add(indice);
        copie.add(new File(destinazione, indice.getName()));

        for (int i = 0; i < originali.size(); i++) {
            try {
                copia(originali.get(i), copie.get(i));
                if (!sha256(originali.get(i)).equals(sha256(copie.get(i)))) {
                    return null;
                }
            } catch (Throwable t) {
                return null;
            }
        }

        try {
            StringBuilder b = new StringBuilder();
            b.append("Backup NMS ITALIA Corvette HUB\n");
            b.append("Data: ").append(new Date()).append("\n\n");
            b.append("Per ripristinare: usa il pulsante Ripristina backup nel programma,\n");
            b.append("oppure rimetti questi file nelle loro cartelle originali.\n\n");
            for (int i = 0; i < originali.size(); i++) {
                b.append(originali.get(i).getAbsolutePath()).append('\n');
                b.append("    copia: ").append(copie.get(i).getName())
                        .append("   ").append(originali.get(i).length()).append(" byte\n");
                b.append("    impronta: ").append(sha256(originali.get(i))).append('\n');
            }
            scriviAtomico(new File(destinazione, "backup.txt"), b.toString().getBytes(UTF8));
        } catch (Throwable ignored) {
            // la nota e' un extra, non blocca
        }
        return destinazione;
    }

    /** Rimette a posto i file originali dal backup, durante una scrittura fallita. */
    private static void ripristina(File backup, List<File> cartelle, File indice) {
        for (int c = 0; c < cartelle.size(); c++) {
            File contenitore = cartelle.get(c);
            File salvata = new File(backup, contenitore.getName());
            File[] contenuto = contenitore.listFiles();
            if (contenuto == null) {
                continue;
            }
            for (int i = 0; i < contenuto.length; i++) {
                File salvato = new File(salvata, contenuto[i].getName());
                if (!salvato.isFile()) {
                    salvato = new File(backup, contenuto[i].getName());
                }
                if (contenuto[i].isFile() && salvato.isFile()) {
                    try {
                        copia(salvato, contenuto[i]);
                    } catch (Throwable ignored) {
                        // si prova a rimettere gli altri
                    }
                }
            }
        }
        File indiceSalvato = new File(backup, indice.getName());
        if (indiceSalvato.isFile()) {
            try {
                copia(indiceSalvato, indice);
            } catch (Throwable ignored) {
                // nulla da fare
            }
        }
    }

    /**
     * Legge containers.index e restituisce la corrispondenza
     * nome dello slot -> cartella del contenitore.
     *
     * Serve perche' il gioco, quando salva, ricrea i contenitori con nomi
     * nuovi: un backup di ieri non si ritrova piu' per nome di cartella. Il
     * nome dello slot invece non cambia mai ("Slot3Manual" resta quello).
     *
     * Nel file, per ogni contenitore, il nome viene scritto PRIMA del percorso:
     * quindi l'i-esimo nome corrisponde all'i-esimo percorso.
     */
    /**
     * Rimette a posto i file di un backup.
     *
     * Va usato a gioco chiuso. Non tocca niente che non sia dentro il backup.
     *
     * LIMITE NOTO: il ripristino riconosce i contenitori dal NOME DELLA
     * CARTELLA. Il gioco, quando salva, tiene le cartelle ma puo' cambiare i
     * nomi dei file dentro. In quel caso i file vecchi vengono rimessi accanto
     * ai nuovi: lo stato torna comunque quello del backup, perche' anche
     * containers.index viene ripristinato e il gioco segue quello, ma nella
     * cartella restano dei file orfani che il gioco non usa piu'.
     */
    public static Esito ripristina(File backup, File radiceStorage) {
        if (backup == null || !backup.isDirectory()) {
            return Esito.errore("Questo backup non esiste piu'.");
        }
        if (radiceStorage == null || !radiceStorage.isDirectory()) {
            return Esito.errore("La cartella dei salvataggi non e' piu' raggiungibile.");
        }
        if (giocoInEsecuzione()) {
            return Esito.errore("No Man's Sky e' in esecuzione. Chiudi il gioco prima di "
                    + "ripristinare, altrimenti il gioco potrebbe sovrascrivere i file.");
        }

        Esito esito = new Esito();
        StringBuilder log = new StringBuilder();

        List<File> daCartelle = new ArrayList<File>();
        List<File> versoCartelle = new ArrayList<File>();
        File[] figli = backup.listFiles();
        if (figli != null) {
            for (int i = 0; i < figli.length; i++) {
                if (!figli[i].isDirectory()) {
                    continue;
                }
                File originale = new File(radiceStorage, figli[i].getName());
                if (originale.isDirectory()) {
                    daCartelle.add(figli[i]);
                    versoCartelle.add(originale);
                }
            }
        }
        if (versoCartelle.isEmpty()) {
            // Non e' un backup a contenitori: puo' essere un backup di file
            // singoli (Steam, GOG, Epic), dove i salvataggi stanno sciolti
            // nella cartella del profilo invece che dentro una sottocartella.
            return ripristinaFileSingolo(backup, radiceStorage);
        }

        File indice = new File(radiceStorage, "containers.index");

        File prima = creaBackup(versoCartelle, indice);
        if (prima == null) {
            return Esito.errore("Non riesco a fare una copia di sicurezza dello stato attuale. "
                    + "Il ripristino si interrompe qui: non ho toccato nulla.");
        }
        esito.backup = prima;
        log.append("stato attuale salvato in : ").append(prima.getAbsolutePath()).append('\n');
        registra("ripristino richiesto dal backup " + backup.getName());

        int ripristinati = 0;
        try {
            File idx = new File(backup, "containers.index");
            if (idx.isFile()) {
                copia(idx, indice);
                ripristinati++;
                log.append("containers.index rimesso a posto\n");
            }
            for (int i = 0; i < versoCartelle.size(); i++) {
                File[] contenuto = daCartelle.get(i).listFiles();
                if (contenuto == null) {
                    continue;
                }
                for (int k = 0; k < contenuto.length; k++) {
                    if (!contenuto[k].isFile()) {
                        continue;
                    }
                    copia(contenuto[k], new File(versoCartelle.get(i), contenuto[k].getName()));
                    ripristinati++;
                }
                log.append("contenitore ").append(versoCartelle.get(i).getName())
                        .append(": ").append(contenuto.length).append(" file rimessi\n");
            }
        } catch (Throwable t) {
            return Esito.errore("Il ripristino non e' riuscito: " + t
                    + "\n\nLo stato di prima del tentativo e' in:\n" + prima.getAbsolutePath());
        }

        SaveLocator.Rilevamento rd = SaveLocator.apri(radiceStorage);
        if (rd == null || rd.storage.bU() == null) {
            return Esito.errore("I file sono stati rimessi a posto ma il salvataggio non si "
                    + "riapre.\n\nLo stato di prima del tentativo e' in:\n"
                    + prima.getAbsolutePath());
        }

        esito.riuscito = true;
        esito.messaggio = "Ripristinati " + ripristinati + " file.";
        registra("ripristino riuscito: " + ripristinati + " file rimessi dal backup "
                + backup.getName());
        esito.dettaglio = "RIPRISTINO ESEGUITO\n\n" + log.toString()
                + "\nIl salvataggio si riapre correttamente.\n\n"
                + "Lo stato che c'era prima del ripristino e' stato salvato in:\n"
                + prima.getAbsolutePath();
        return esito;
    }
    // ------------------------------------------------------- ausiliari

    /** La cartella in cui vive il programma: li' stanno Backup/ e la configurazione. */
    public static File cartellaProgramma() {
        return it.nmsitalia.corvettehub.config.AppConfig.cartellaProgramma();
    }

    /**
     * Scrive una riga nel log delle operazioni, accanto all'eseguibile.
     *
     * Dice cosa e' stato fatto e quando: quale modifica, dove sta il backup,
     * com'e' finita. Non contiene dati personali oltre al nome del giocatore,
     * che e' gia' dentro il salvataggio.
     */
    public static void registra(String riga) {
        Writer w = null;
        try {
            File f = new File(cartellaProgramma(), "CorvetteHUB.log");
            w = new OutputStreamWriter(new FileOutputStream(f, true), UTF8);
            SimpleDateFormat d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            w.write("[" + d.format(new Date()) + "] " + riga + System.lineSeparator());
            w.flush();
        } catch (Throwable ignored) {
            // se il log non si puo' scrivere, l'operazione prosegue lo stesso
        } finally {
            if (w != null) {
                try {
                    w.close();
                } catch (Throwable ignored) {
                    // nulla
                }
            }
        }
    }

    /** Il file di log delle operazioni, se esiste. */
    public static File fileLog() {
        return new File(cartellaProgramma(), "CorvetteHUB.log");
    }

    /** Backup disponibili, dal piu' recente. */
    public static List<File> elencoBackup() {
        File dir = new File(cartellaProgramma(), "Backup");
        List<File> out = new ArrayList<File>();
        File[] f = dir.listFiles();
        if (f == null) {
            return out;
        }
        for (int i = 0; i < f.length; i++) {
            if (f[i].isDirectory() && !f[i].getName().startsWith("_")) {
                out.add(f[i]);
            }
        }
        java.util.Collections.sort(out, new java.util.Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return b.getName().compareTo(a.getName());
            }
        });
        return out;
    }

    // ------------------------------------------------------- individuazione

    /**
     * Trova il contenitore di un salvataggio confrontando il CONTENUTO.
     *
     * Non si puo' usare il nome del salvataggio: i due file di uno stesso slot
     * (Auto e Manual) hanno lo stesso nome e la stessa descrizione, quindi la
     * ricerca per nome puo' cadere sul file sbagliato. Si decomprime ogni
     * payload candidato e si confronta con quello che la libreria ha appena
     * letto: quello che combacia e' il contenitore giusto.
     *
     * @return {descrittore, payload}, oppure null se nessuno combacia.
     */
    static File[] trovaContenitore(File radiceStorage, byte[] atteso) {
        File[] contenitori = radiceStorage.listFiles();
        if (contenitori == null) {
            return null;
        }
        for (int i = 0; i < contenitori.length; i++) {
            if (!contenitori[i].isDirectory()) {
                continue;
            }
            File[] file = contenitori[i].listFiles();
            if (file == null) {
                continue;
            }
            File descrittore = null;
            File payload = null;
            for (int k = 0; k < file.length; k++) {
                if (!file[k].isFile() || file[k].getName().startsWith("container.")) {
                    continue;
                }
                if (file[k].length() < 4096) {
                    descrittore = file[k];
                } else {
                    payload = file[k];
                }
            }
            if (descrittore == null || payload == null) {
                continue;
            }
            try {
                byte[] decompresso = decomprimi(payload);
                if (iniziaCon(decompresso, atteso)) {
                    return new File[]{descrittore, payload};
                }
            } catch (Throwable t) {
                // non e' un payload leggibile: si passa al prossimo
            }
        }
        return null;
    }

    /** Vero se {@code pagliaio} comincia con tutti i byte di {@code ago}. */
    private static boolean iniziaCon(byte[] pagliaio, byte[] ago) {
        if (pagliaio.length < ago.length) {
            return false;
        }
        for (int i = 0; i < ago.length; i++) {
            if (pagliaio[i] != ago[i]) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------ compressione

    /** Decomprime un payload LZ4 del gioco. */
    static byte[] decomprimi(File payload) throws IOException {
        InputStream in = new FileInputStream(payload);
        try {
            byte[] testa = new byte[16];
            int letti = 0;
            while (letti < 16) {
                int n = in.read(testa, letti, 16 - letti);
                if (n < 0) {
                    throw new IOException("file troppo corto");
                }
                letti += n;
            }
            if ((testa[0] & 0xFF) != 0xE5 || (testa[1] & 0xFF) != 0xA1
                    || (testa[2] & 0xFF) != 0xED || (testa[3] & 0xFF) != 0xFE) {
                throw new IOException("non e' un payload LZ4 del gioco");
            }
            InputStream g = new gX(in, testa);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            int n;
            while ((n = g.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
                // nulla
            }
        }
    }

    /** Comprime nella forma usata dal gioco. */
    static byte[] comprimi(byte[] dati) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        gZ g = new gZ(out);
        g.write(dati);
        g.close();
        return out.toByteArray();
    }

    /** I byte che nel file originale seguono il JSON. */
    static byte[] codaOriginale(byte[] decompresso) {
        if (decompresso.length == 0) {
            return new byte[0];
        }
        int fine = decompresso.length;
        while (fine > 0 && (decompresso[fine - 1] == 0 || decompresso[fine - 1] == 0x0A
                || decompresso[fine - 1] == 0x0D)) {
            fine--;
        }
        int n = decompresso.length - fine;
        byte[] coda = new byte[n];
        System.arraycopy(decompresso, fine, coda, 0, n);
        return coda;
    }

    // ------------------------------------------------------------- file

    static byte[] leggi(File f) {
        InputStream in = null;
        try {
            in = new FileInputStream(f);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[65536];
            int n;
            while ((n = in.read(b)) > 0) {
                out.write(b, 0, n);
            }
            return out.toByteArray();
        } catch (Throwable t) {
            return new byte[0];
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {
                    // nulla
                }
            }
        }
    }

    static void scriviAtomico(File destinazione, byte[] dati) throws IOException {
        File temp = new File(destinazione.getParentFile(),
                destinazione.getName() + ".hub-tmp");
        OutputStream out = null;
        try {
            out = new FileOutputStream(temp);
            out.write(dati);
            out.flush();
        } finally {
            if (out != null) {
                out.close();
            }
        }
        if (destinazione.exists() && !destinazione.delete()) {
            temp.delete();
            throw new IOException("non riesco a sostituire " + destinazione.getName());
        }
        if (!temp.renameTo(destinazione)) {
            temp.delete();
            throw new IOException("non riesco a rinominare " + temp.getName());
        }
    }

    static void copia(File da, File a) throws IOException {
        InputStream in = new FileInputStream(da);
        OutputStream out = new FileOutputStream(a);
        try {
            byte[] b = new byte[65536];
            int n;
            while ((n = in.read(b)) > 0) {
                out.write(b, 0, n);
            }
        } finally {
            in.close();
            out.close();
        }
        a.setLastModified(da.lastModified());
    }

    static String sha256(File f) {
        InputStream in = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            in = new FileInputStream(f);
            byte[] b = new byte[65536];
            int n;
            while ((n = in.read(b)) > 0) {
                md.update(b, 0, n);
            }
            StringBuilder s = new StringBuilder();
            byte[] d = md.digest();
            for (int i = 0; i < d.length; i++) {
                s.append(String.format("%02x", d[i]));
            }
            return s.toString();
        } catch (Throwable t) {
            return "";
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {
                    // nulla
                }
            }
        }
    }

    // ------------------------------------------------------------- byte

    static int leggiInt(byte[] d, int off) {
        return (d[off] & 0xFF) | ((d[off + 1] & 0xFF) << 8)
                | ((d[off + 2] & 0xFF) << 16) | ((d[off + 3] & 0xFF) << 24);
    }

    static void scriviInt(byte[] d, int off, int v) {
        d[off] = (byte) (v & 0xFF);
        d[off + 1] = (byte) ((v >>> 8) & 0xFF);
        d[off + 2] = (byte) ((v >>> 16) & 0xFF);
        d[off + 3] = (byte) ((v >>> 24) & 0xFF);
    }

    static long leggiLong(byte[] d, int off) {
        long v = 0;
        for (int i = 0; i < 8; i++) {
            v |= ((long) (d[off + i] & 0xFF)) << (8 * i);
        }
        return v;
    }

    static void scriviLong(byte[] d, int off, long v) {
        for (int i = 0; i < 8; i++) {
            d[off + i] = (byte) ((v >>> (8 * i)) & 0xFF);
        }
    }

    /** Cerca un valore a 8 byte; restituisce l'offset o -1. */
    static int cercaLong(byte[] dati, long valore) {
        for (int i = 0; i + 8 <= dati.length; i++) {
            boolean ok = true;
            for (int k = 0; k < 8; k++) {
                if ((dati[i + k] & 0xFF) != ((valore >>> (8 * k)) & 0xFF)) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                return i;
            }
        }
        return -1;
    }

    static byte[] unisci(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    /** Rilevamento del gioco in esecuzione (requisito R8). */
    public static boolean giocoInEsecuzione() {
        Process p = null;
        try {
            p = new ProcessBuilder("tasklist", "/FI", "IMAGENAME eq NMS.exe", "/NH")
                    .redirectErrorStream(true).start();
            java.io.BufferedReader r =
                    new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
            String linea;
            while ((linea = r.readLine()) != null) {
                if (linea.toLowerCase(Locale.ROOT).contains("nms.exe")) {
                    return true;
                }
            }
        } catch (Throwable t) {
            return false;
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
        return false;
    }
}
