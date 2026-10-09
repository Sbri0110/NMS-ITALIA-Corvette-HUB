package it.nmsitalia.corvettehub.domain;

import nomanssave.eV;
import nomanssave.eY;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * Il formato di scambio delle build di Corvette.
 *
 * In scrittura si produce sempre il wrapper ufficiale:
 *
 *   {
 *     "format": "NMS-CorvetteBuild",
 *     "version": 1,
 *     "name": "Nome della build",
 *     "author": "autore",
 *     "created_utc": "2026-09-23T00:00:00Z",
 *     "objects": [ ... ]
 *   }
 *
 * In lettura si accettano, per compatibilita' con i file che girano:
 *   - il wrapper ufficiale (campi format e version presenti);
 *   - il wrapper con la chiave Objects maiuscola;
 *   - una lista Objects[] grezza, senza wrapper.
 *
 * Verificato sui file reali trovati in corvette_project: i wrapper della
 * community usano "objects" minuscolo e non sempre hanno author e created_utc.
 */
public final class WrapperBuild {

    public static final String FORMATO = "NMS-CorvetteBuild";
    public static final int VERSIONE = 1;

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final File file;
    private final String nome;
    private final String autore;
    private final String dataUtc;
    private final int versione;
    private final String formatoDichiarato;
    private final eV oggetti;

    private WrapperBuild(File file, String nome, String autore, String dataUtc,
                         int versione, String formatoDichiarato, eV oggetti) {
        this.file = file;
        this.nome = nome;
        this.autore = autore;
        this.dataUtc = dataUtc;
        this.versione = versione;
        this.formatoDichiarato = formatoDichiarato;
        this.oggetti = oggetti;
    }

    public File getFile() {
        return file;
    }

    public String getNome() {
        if (nome == null || nome.trim().isEmpty()) {
            return "(senza nome)";
        }
        return nome;
    }

    public String getNomeGrezzo() {
        return nome;
    }

    public String getAutore() {
        return autore == null ? "" : autore;
    }

    public String getDataUtc() {
        return dataUtc == null ? "" : dataUtc;
    }

    public int getVersione() {
        return versione;
    }

    public String getFormatoDichiarato() {
        return formatoDichiarato;
    }

    public eV getOggetti() {
        return oggetti;
    }

    public int getNumeroModuli() {
        return oggetti == null ? 0 : oggetti.size();
    }

    /** Identificativi distinti richiesti dalla build. */
    public Set<String> getPartiRichieste() {
        Set<String> out = new LinkedHashSet<String>();
        if (oggetti == null) {
            return out;
        }
        for (int i = 0; i < oggetti.size(); i++) {
            eY o = oggetti.V(i);
            if (o == null) {
                continue;
            }
            String id = o.getValueAsString("ObjectID");
            if (id != null && !id.isEmpty()) {
                out.add(id);
            }
        }
        return out;
    }

    // ------------------------------------------------------------- lettura

    /** Legge un file di progetto. Lancia un'eccezione se non e' valido. */
    public static WrapperBuild leggi(File f) throws IOException {
        if (f == null || !f.isFile()) {
            throw new IOException("File non trovato.");
        }
        byte[] dati = leggiTutto(f);
        String testo = new String(dati, UTF8).trim();
        if (testo.startsWith("\ufeff")) testo = testo.substring(1).trim();
        if (testo.startsWith("[")) testo = "{\"objects\":" + testo + "}";

        eY radice;
        try {
            radice = eY.E(testo);
        } catch (Throwable t) {
            throw new IOException("Questo file non e' un progetto Corvette valido: "
                    + "non riesco a leggerne il contenuto JSON.");
        }
        if (radice == null) {
            throw new IOException("Questo file non e' un progetto Corvette valido: "
                    + "non riesco a leggerne il contenuto JSON.");
        }

        // caso 1: wrapper ufficiale o con Objects maiuscolo
        eV oggetti = null;
        try {
            oggetti = radice.d("objects");
        } catch (Throwable ignored) {
            // proviamo l'altra forma
        }
        if (oggetti == null) {
            try {
                oggetti = radice.d("Objects");
            } catch (Throwable ignored) {
                // proviamo la lista grezza
            }
        }
        String nome = primoNonVuoto(str(radice, "name"), str(radice, "Name"));
        String autore = primoNonVuoto(str(radice, "author"), str(radice, "Author"));
        String data = primoNonVuoto(str(radice, "created_utc"), str(radice, "CreatedUtc"));
        String formato = str(radice, "format");
        int versione = 0;
        try {
            versione = radice.J("version");
        } catch (Throwable ignored) {
            versione = 0;
        }

        if (oggetti == null || oggetti.size() == 0) {
            throw new IOException("Questo file non e' un progetto Corvette valido. "
                    + "Attesi i campi 'format' e 'objects'.");
        }

        if (formato != null && !FORMATO.equals(formato)) {
            throw new IOException("Formato progetto non supportato: " + formato);
        }
        if (versione < 0 || versione > VERSIONE) {
            throw new IOException("Versione progetto non supportata: " + versione);
        }
        if (radice.contains("version") && (!(radice.get("version") instanceof Number)
                || ((Number)radice.get("version")).doubleValue() != versione)) {
            throw new IOException("La versione del progetto deve essere un intero.");
        }
        for (int i = 0; i < oggetti.size(); i++) {
            eY o;
            try { o = oggetti.V(i); } catch (RuntimeException e) {
                throw new IOException("Modulo " + (i + 1) + " non valido.");
            }
            if (o == null || o.getValueAsString("ObjectID") == null
                    || o.getValueAsString("ObjectID").trim().isEmpty()) {
                throw new IOException("Modulo " + (i + 1) + " senza ObjectID.");
            }
            for (String campo : new String[]{"Position", "Up", "At"}) {
                try {
                    eV v = o.d(campo);
                    if (v == null || v.size() != 3) throw new IllegalArgumentException();
                    for (int k = 0; k < 3; k++) {
                        double n = v.aa(k);
                        if (Double.isNaN(n) || Double.isInfinite(n) || Math.abs(n) > 1000000) {
                            throw new IllegalArgumentException();
                        }
                    }
                } catch (RuntimeException e) {
                    throw new IOException("Modulo " + (i + 1) + ": vettore " + campo + " non valido.");
                }
            }
        }
        // caso 3: lista grezza
        if (formato == null) {
            formato = "(lista senza wrapper)";
        }
        return new WrapperBuild(f, nome, autore, data, versione, formato, oggetti);
    }

    // ------------------------------------------------------------ scrittura

    /**
     * Scrive un progetto nel wrapper ufficiale.
     *
     * @return il numero di moduli scritti.
     */
    public static int scrivi(File destinazione, String nome, String autore, eV oggetti)
            throws IOException {
        if (oggetti == null || oggetti.size() == 0) {
            throw new IOException("Non ci sono moduli da esportare.");
        }
        eY radice = new eY();
        radice.b("format", FORMATO);
        radice.b("version", Integer.valueOf(VERSIONE));
        radice.b("name", nome == null ? "" : nome);
        radice.b("author", autore == null ? "" : autore);
        radice.b("created_utc", adessoUtc());
        radice.b("objects", oggetti);

        File padre = destinazione.getParentFile();
        if (padre != null && !padre.isDirectory() && !padre.mkdirs()) {
            throw new IOException("Non riesco a creare la cartella " + padre.getAbsolutePath());
        }
        String json = radice.bz();

        it.nmsitalia.corvettehub.safety.FileSicuri.scrivi(destinazione, json.getBytes(UTF8));
        return oggetti.size();
    }

    public static String adessoUtc() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date());
    }

    /** Nome file proposto per una build, senza caratteri vietati da Windows. */
    public static String nomeFileSicuro(String nome) {
        String s = nome == null ? "build" : nome.trim();
        if (s.isEmpty()) {
            s = "build";
        }
        s = s.replaceAll("[\\\\/:*?\"<>|]", "-");
        s = s.replaceAll("[\\x00-\\x1F\\x7F]", "-");
        s = s.replaceAll("\\s+", " ");
        s = s.replaceAll("[. ]+$", "");
        if (s.isEmpty()) s = "build";
        if (s.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?")) s = "_" + s;
        if (s.length() > 80) {
            s = s.substring(0, 80).trim();
        }
        return s + ".json";
    }

    /** Elenco dei progetti nella libreria locale, dal piu' recente. */
    public static List<File> elencoLibreria(File cartella) {
        List<File> out = new ArrayList<File>();
        File[] f = cartella == null ? null : cartella.listFiles();
        if (f == null) {
            return out;
        }
        for (int i = 0; i < f.length; i++) {
            if (f[i].isFile() && f[i].getName().toLowerCase(Locale.ROOT).endsWith(".json")) {
                out.add(f[i]);
            }
        }
        java.util.Collections.sort(out, new java.util.Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return Long.compare(b.lastModified(), a.lastModified());
            }
        });
        return out;
    }

    // ------------------------------------------------------------- interni

    private static String str(eY n, String chiave) {
        try {
            return n.getValueAsString(chiave);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String primoNonVuoto(String a, String b) {
        if (a != null && !a.trim().isEmpty()) {
            return a;
        }
        return b;
    }

    private static byte[] leggiTutto(File f) throws IOException {
        if (f.length() > 32L * 1024 * 1024) throw new IOException("Progetto troppo grande (massimo 32 MB).");
        FileInputStream in = null;
        try {
            in = new FileInputStream(f);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) {
                if (out.size() + n > 32 * 1024 * 1024) throw new IOException("Progetto troppo grande.");
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
