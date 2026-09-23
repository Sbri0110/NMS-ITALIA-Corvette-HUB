package nomanssave;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Segnaposto tecnico per la classe Application dell'editor completo.
 *
 * PERCHE' ESISTE QUESTO FILE
 * --------------------------
 * La classe nomanssave.eC, che carica il dizionario delle chiavi di
 * salvataggio (db/jsonmap.txt), risolve il percorso delle proprie risorse
 * partendo da Application.class:
 *
 *     Application.class.getProtectionDomain().getCodeSource().getLocation()
 *
 * Lo si vede dallo stack trace che si ottiene senza questa classe:
 *
 *     java.lang.NoClassDefFoundError: nomanssave/Application
 *         at nomanssave.eC.c(eC.java:69)
 *         at nomanssave.eC.<clinit>(eC.java:24)
 *         at nomanssave.ff.a(Unknown Source)
 *
 * Senza di essa la conversione di QUALUNQUE salvataggio fallisce, perche'
 * eC non riesce a inizializzarsi.
 *
 * COSA NON E' QUESTO FILE
 * -----------------------
 * Non e' una copia, nemmeno parziale, di Application. Non estende JFrame,
 * non crea finestre, non contiene nessuna delle schede dell'editor. L'unico
 * metodo presente (b) e' la formattazione di una data usata da alcune
 * toString() della libreria, cosi' che una chiamata accidentale non provochi
 * un errore a runtime.
 *
 * Le funzioni di modifica e di sblocco del deposito vivono in nomanssave.gz,
 * che NON e' inclusa nel parser di questo progetto: non essendoci la classe,
 * quelle funzioni non sono raggiungibili nemmeno per errore.
 */
public class Application {

    /** Formattazione di una data, usata dalle toString() della libreria. */
    public static String b(long millis) {
        if (millis <= 0L) {
            return "data sconosciuta";
        }
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(millis));
    }
}
