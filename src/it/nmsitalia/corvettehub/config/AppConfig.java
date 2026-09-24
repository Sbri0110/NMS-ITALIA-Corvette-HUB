package it.nmsitalia.corvettehub.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Configurazione dell'applicazione, salvata in CorvetteHUB.conf accanto
 * all'eseguibile.
 *
 * Contiene solo la scelta della cartella dei salvataggi e il tipo di storage
 * rilevato, cosi' che l'utente non debba indicare il percorso ogni volta
 * (requisito R1). Nessun altro dato, nessun percorso di sistema memorizzato
 * di nascosto.
 */
public final class AppConfig {

    public static final String CHIAVE_CARTELLA = "GameSaveDir";
    public static final String CHIAVE_TIPO = "GameStorage";
    public static final String CHIAVE_LIMITE_BACKUP = "BackupRetention";

    /**
     * La scala dell'interfaccia: "auto" (dal DPI dello schermo) oppure un
     * numero, per esempio "1.5". Serve ai monitor 4K, dove l'ingrandimento di
     * Windows non basta al programma.
     */
    public static final String CHIAVE_SCALA = "UiScale";

    private final File file;
    private final Properties props = new Properties();

    private AppConfig(File file) {
        this.file = file;
    }

    /** Cartella in cui vive il programma (quella del JAR, non quella di avvio). */
    public static File cartellaProgramma() {
        try {
            File f = new File(AppConfig.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (f.isFile()) {
                f = f.getParentFile();
            }
            return f.getCanonicalFile();
        } catch (Exception e) {
            try {
                return new File(".").getCanonicalFile();
            } catch (IOException io) {
                return new File(".");
            }
        }
    }

    public static AppConfig carica() {
        AppConfig c = new AppConfig(new File(cartellaProgramma(), "CorvetteHUB.conf"));
        if (c.file.isFile()) {
            InputStream in = null;
            try {
                in = new FileInputStream(c.file);
                c.props.load(in);
            } catch (IOException e) {
                // configurazione illeggibile: si riparte dai valori predefiniti
            } finally {
                chiudi(in);
            }
        }
        return c;
    }

    public File getFile() {
        return file;
    }

    public String cartellaSalvataggi() {
        return props.getProperty(CHIAVE_CARTELLA);
    }

    public String tipoStorage() {
        return props.getProperty(CHIAVE_TIPO);
    }

    public int limiteBackup() {
        try {
            return Integer.parseInt(props.getProperty(CHIAVE_LIMITE_BACKUP, "20"));
        } catch (NumberFormatException e) {
            return 20;
        }
    }

    public void ricorda(String cartella, String tipo) {
        if (cartella != null) {
            props.setProperty(CHIAVE_CARTELLA, cartella);
        }
        if (tipo != null) {
            props.setProperty(CHIAVE_TIPO, tipo);
        }
        if (!props.containsKey(CHIAVE_LIMITE_BACKUP)) {
            props.setProperty(CHIAVE_LIMITE_BACKUP, "20");
        }
        salva();
    }

    /** La scala richiesta per l'interfaccia, oppure "auto". */
    public String scalaInterfaccia() {
        return props.getProperty(CHIAVE_SCALA, "auto");
    }

    public void dimentica() {
        props.remove(CHIAVE_CARTELLA);
        props.remove(CHIAVE_TIPO);
        salva();
    }

    private void salva() {
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            props.store(out, "NMS ITALIA Corvette HUB - configurazione locale");
        } catch (IOException e) {
            // non poter scrivere la configurazione non deve impedire l'uso
        } finally {
            chiudi(out);
        }
    }

    private static void chiudi(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
                // nulla da fare
            }
        }
    }
}
