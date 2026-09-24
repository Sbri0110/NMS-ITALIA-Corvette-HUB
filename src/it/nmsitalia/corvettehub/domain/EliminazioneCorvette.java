package it.nmsitalia.corvettehub.domain;

import nomanssave.eV;
import nomanssave.eY;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Eliminazione di una Corvette dal salvataggio, con recupero dei moduli.
 *
 * COSA VUOL DIRE "ELIMINARE UNA CORVETTE"
 * ---------------------------------------
 * Una Corvette non e' un oggetto isolato: e' un intreccio di riferimenti.
 *
 *   1. una base in PlayerStateData.PersistentPlayerBases[] con
 *      BaseType.PersistentBaseTypes == "PlayerShipBase", che porta i moduli
 *      costruiti dentro Objects[];
 *   2. la nave corrispondente in PlayerStateData.ShipOwnership[], indicata dal
 *      campo UserData della base;
 *   3. un valore parallelo in ShipUsesLegacyColours[], della stessa lunghezza
 *      di ShipOwnership;
 *   4. l'indice della nave in uso, PlayerStateData.PrimaryShip.
 *
 * Togliere la base e basta lascerebbe una nave orfana; togliere anche la nave
 * sposta tutti gli indici successivi, che vanno rimappati. Questa classe fa
 * tutte e quattro le cose, e non tocca nient'altro.
 *
 * PRIMA SI SMONTA, POI SI CANCELLA
 * --------------------------------
 * I moduli della Corvette sono moduli da costruzione: nel gioco si staccano e
 * tornano nel deposito della Stazione. Il tool fa lo stesso — i moduli
 * identificati da ^B_ finiscono nel deposito della Stazione (impilati per
 * tipo, fino a 500 per voce), e solo dopo la Corvette viene rimossa.
 *
 * SE NEL DEPOSITO NON C'E' POSTO, NON SI PROCEDE
 * ----------------------------------------------
 * Il deposito della Stazione ha una capienza fissa (160 celle: 10 x 16) che
 * NON va allargata: sarebbe barare. Se i moduli non ci stanno, il tool si
 * ferma e dice quante celle liberare. Vedi {@link Piano#possibile}.
 *
 * LE DECORAZIONI
 * --------------
 * Sulla Corvette possono esserci anche parti che non sono moduli da Corvette
 * (luci, corridoi, porte). Il deposito della Stazione accetta solo ^B_:
 * le altre spariscono con la base, esattamente come quando si elimina una
 * base nel gioco. Il piano le conta e le dichiara.
 *
 * Questa classe non scrive su disco: modifica il modello in memoria. La
 * scrittura la fa ScrittoreSalvataggio, con backup verificato e rilettura.
 */
public final class EliminazioneCorvette {

    private static final String TIPO_BASE_NAVE = "PlayerShipBase";

    /** Quanti pezzi stanno in una voce del deposito. */
    public static final int MASSIMO_PER_VOCE = 500;

    /** Il prefisso dei moduli che appartengono alle Corvette. */
    public static final String PREFISSO_CORVETTE = "^B_";

    private EliminazioneCorvette() {
    }

    // ------------------------------------------------------------------ piano

    /** Cosa succederebbe, calcolato senza modificare nulla. */
    public static final class Piano {

        /** Vero se l'operazione si puo' fare. */
        public boolean possibile;

        /** Perche' non si puo', in una riga. */
        public String motivo;

        /** Tutti i pezzi costruiti sulla Corvette. */
        public int pezziTotali;

        /** Quanti sono moduli da Corvette (^B_). */
        public int moduliCorvette;

        /** Quanti sono decorazioni di base (il deposito non li accetta). */
        public int decorazioni;

        /** Tipi distinti, nelle due categorie. */
        public int tipiCorvette;
        public int tipiDecorazioni;

        /** Tipi di modulo che il deposito ha gia'. */
        public int tipiGiaPresenti;

        /** Tipi di modulo che vanno inseriti in una cella nuova. */
        public int tipiNuovi;

        /** Celle che servono e celle disponibili. */
        public int celleNecessarie;
        public int celleLibere;

        /** Il deposito, per il riepilogo. */
        public int larghezzaDeposito;
        public int altezzaDeposito;

        /** Vero se la Corvette e' quella che il giocatore sta usando. */
        public boolean inUso;

        /** Nome della Corvette, come lo mostra il gioco. */
        public String nome;

        /** Celle che mancano, se non ce ne sono abbastanza. */
        public int celleMancanti() {
            int m = celleNecessarie - celleLibere;
            return m > 0 ? m : 0;
        }

        /** Un riepilogo leggibile, per l'interfaccia. */
        public String riepilogo() {
            StringBuilder b = new StringBuilder();
            b.append("Corvette          : ").append(nome).append('\n');
            b.append("Pezzi costruiti   : ").append(pezziTotali).append('\n');
            b.append("  moduli Corvette : ").append(moduliCorvette)
                    .append("  (").append(tipiCorvette).append(" tipi)\n");
            b.append("  decorazioni     : ").append(decorazioni)
                    .append("  (").append(tipiDecorazioni).append(" tipi)\n");
            b.append('\n');
            b.append("Deposito Stazione : ").append(larghezzaDeposito)
                    .append(" x ").append(altezzaDeposito)
                    .append("  (").append(larghezzaDeposito * altezzaDeposito)
                    .append(" celle)\n");
            b.append("  gia' nel deposito: ").append(tipiGiaPresenti).append(" tipi\n");
            b.append("  tipi nuovi      : ").append(tipiNuovi).append('\n');
            b.append("  celle necessarie: ").append(celleNecessarie).append('\n');
            b.append("  celle libere    : ").append(celleLibere).append('\n');
            if (celleMancanti() > 0) {
                b.append("  MANCANO         : ").append(celleMancanti()).append(" celle\n");
            }
            return b.toString();
        }
    }

    // -------------------------------------------------------------- calcolo

    /**
     * Calcola il piano. Non modifica il modello.
     *
     * @param radice il modello del salvataggio
     * @param c      la Corvette da eliminare
     */
    public static Piano pianifica(eY radice, Corvette c) {
        Piano p = new Piano();
        p.nome = c == null ? "?" : c.getNome();

        if (radice == null || c == null) {
            p.motivo = "Nessuna Corvette da eliminare.";
            return p;
        }

        eY stato = stato(radice);
        if (stato == null) {
            p.motivo = "Il salvataggio non contiene PlayerStateData.";
            return p;
        }

        // --- la Corvette deve essere dove ci aspettiamo
        eV basi = array(stato, "PersistentPlayerBases");
        if (basi == null || c.getIndiceBase() < 0 || c.getIndiceBase() >= basi.size()) {
            p.motivo = "La Corvette non e' piu' dove l'avevamo trovata: rileggi il salvataggio.";
            return p;
        }
        eY base = basi.V(c.getIndiceBase());
        if (base == null || !TIPO_BASE_NAVE.equals(
                base.getValueAsString("BaseType.PersistentBaseTypes"))) {
            p.motivo = "La base indicata non e' una Corvette: rileggi il salvataggio.";
            return p;
        }

        // --- e' quella in uso?
        int primaria = intero(stato, "PrimaryShip", -1);
        p.inUso = primaria >= 0 && primaria == c.getIndiceNave();
        if (p.inUso) {
            p.motivo = "Questa e' la Corvette che stai usando in gioco.\n\n"
                    + "Il gioco la tiene in memoria e sovrascriverebbe la modifica.\n"
                    + "Cambia nave nel gioco, salva, e riprova.";
            return p;
        }

        // --- conta i pezzi per tipo
        Map<String, Integer> perTipo = contaModuli(base);
        for (Map.Entry<String, Integer> e : perTipo.entrySet()) {
            int n = e.getValue().intValue();
            p.pezziTotali += n;
            if (e.getKey().startsWith(PREFISSO_CORVETTE)) {
                p.moduliCorvette += n;
                p.tipiCorvette++;
            } else {
                p.decorazioni += n;
                p.tipiDecorazioni++;
            }
        }

        // --- il deposito
        eY deposito = oggetto(stato, "CorvetteStorageInventory");
        if (p.tipiCorvette > 0 && deposito == null) {
            p.motivo = "Questo salvataggio non ha il deposito della Corvette: i moduli\n"
                    + "non avrebbero dove andare.";
            return p;
        }

        if (deposito != null) {
            p.larghezzaDeposito = intero(deposito, "Width", 10);
            p.altezzaDeposito = intero(deposito, "Height", 16);

            Map<String, List<Integer>> vociPerTipo = new LinkedHashMap<String, List<Integer>>();
            Set<String> occupate = new LinkedHashSet<String>();
            eV voci = array(deposito, "Slots");
            if (voci != null) {
                for (int i = 0; i < voci.size(); i++) {
                    eY v = voci.V(i);
                    if (v == null) {
                        continue;
                    }
                    String id = v.getValueAsString("Id");
                    if (id != null) {
                        List<Integer> l = vociPerTipo.get(id);
                        if (l == null) {
                            l = new ArrayList<Integer>();
                            vociPerTipo.put(id, l);
                        }
                        l.add(Integer.valueOf(i));
                    }
                    occupate.add(cellaDi(v));
                }
            }
            p.celleLibere = contaCelleLibere(deposito, occupate);

            // --- quanto spazio serve
            for (Map.Entry<String, Integer> e : perTipo.entrySet()) {
                String id = e.getKey();
                if (!id.startsWith(PREFISSO_CORVETTE)) {
                    continue;
                }
                int rimanenti = e.getValue().intValue();
                List<Integer> esistenti = vociPerTipo.get(id);
                if (esistenti != null) {
                    p.tipiGiaPresenti++;
                    for (int k = 0; k < esistenti.size() && rimanenti > 0; k++) {
                        eY v = voci.V(esistenti.get(k).intValue());
                        int massimo = Math.max(intero(v, "MaxAmount", MASSIMO_PER_VOCE), 1);
                        int spazio = massimo - intero(v, "Amount", 0);
                        if (spazio > 0) {
                            rimanenti -= Math.min(spazio, rimanenti);
                        }
                    }
                } else {
                    p.tipiNuovi++;
                }
                // quello che avanza va in celle nuove, a blocchi di 500
                while (rimanenti > 0) {
                    p.celleNecessarie++;
                    rimanenti -= MASSIMO_PER_VOCE;
                }
            }
        }

        if (p.celleNecessarie > p.celleLibere) {
            p.motivo = "Nel deposito della Stazione non c'e' posto per tutti i moduli.\n\n"
                    + "Servono " + p.celleNecessarie + " celle libere, ce ne sono "
                    + p.celleLibere + ".\n"
                    + "Liberane almeno " + p.celleMancanti()
                    + " in gioco (sposta o vendi dei moduli), poi riprova.";
            return p;
        }

        p.possibile = true;
        return p;
    }

    // ----------------------------------------------------------- esecuzione

    /**
     * Applica l'eliminazione a un modello. Ricalcola il piano sul modello che
     * riceve e, se qualcosa non torna, lancia un'eccezione: lo scrittore del
     * salvataggio ripristina il backup da solo.
     *
     * Va chiamata su un modello FRESCO, letto dal file: e' quella che riceve
     * la modifica di ScrittoreSalvataggio.
     */
    public static Piano applica(eY radice, Corvette c) {
        Piano p = pianifica(radice, c);
        if (!p.possibile) {
            throw new IllegalStateException(p.motivo);
        }

        eY stato = radice.H("PlayerStateData");
        eV basi = stato.d("PersistentPlayerBases");
        eV navi = stato.d("ShipOwnership");
        if (navi == null || c.getIndiceNave() < 0 || c.getIndiceNave() >= navi.size()) {
            throw new IllegalStateException("La nave della Corvette non e' piu' al suo posto.");
        }
        int numeroNaviPrima = navi.size();

        // ---- 1. smontare: i moduli tornano nel deposito della Stazione
        riversaNelDeposito(stato, basi.V(c.getIndiceBase()));

        // ---- 2. togliere la base
        basi.remove(c.getIndiceBase());

        // ---- 3. togliere la nave
        navi.remove(c.getIndiceNave());

        // ---- 4. il valore parallelo dei colori
        eV colori = array(stato, "ShipUsesLegacyColours");
        if (colori != null && colori.size() == numeroNaviPrima) {
            colori.remove(c.getIndiceNave());
        }

        // ---- 5. l'indice della nave in uso si sposta
        int primaria = intero(stato, "PrimaryShip", -1);
        if (primaria > c.getIndiceNave()) {
            stato.b("PrimaryShip", Integer.valueOf(primaria - 1));
        }

        // ---- 6. le altre Corvette puntano a indici di nave: si rimappano
        for (int i = 0; i < basi.size(); i++) {
            eY b = basi.V(i);
            if (b == null) {
                continue;
            }
            if (!TIPO_BASE_NAVE.equals(b.getValueAsString("BaseType.PersistentBaseTypes"))) {
                continue;
            }
            int u = intero(b, "UserData", -1);
            if (u > c.getIndiceNave()) {
                b.b("UserData", Integer.valueOf(u - 1));
            }
        }

        verifica(stato, c);
        return p;
    }

    /**
     * Riversa i moduli della Corvette nel deposito della Stazione.
     *
     * I tipi che il deposito ha gia' si accumulano nella voce esistente, fino
     * al suo massimo; quello che avanza, e i tipi nuovi, prendono una cella
     * libera. L'ordine delle celle e' quello di ValidSlotIndices, cosi' il
     * deposito resta ordinato come lo scrive il gioco.
     */
    private static void riversaNelDeposito(eY stato, eY base) {
        if (base == null) {
            throw new IllegalStateException("La Corvette non ha piu' la sua base.");
        }
        Map<String, Integer> perTipo = contaModuli(base);
        if (perTipo.isEmpty()) {
            return;
        }

        eY deposito = oggetto(stato, "CorvetteStorageInventory");
        if (deposito == null) {
            boolean ciSonoModuli = false;
            for (String id : perTipo.keySet()) {
                if (id.startsWith(PREFISSO_CORVETTE)) {
                    ciSonoModuli = true;
                    break;
                }
            }
            if (ciSonoModuli) {
                throw new IllegalStateException("Manca il deposito della Corvette.");
            }
            return;
        }

        eV voci = array(deposito, "Slots");
        if (voci == null) {
            voci = new eV();
            deposito.b("Slots", voci);
        }

        // indice delle voci esistenti per tipo
        Map<String, List<Integer>> vociPerTipo = new LinkedHashMap<String, List<Integer>>();
        Set<String> occupate = new LinkedHashSet<String>();
        for (int i = 0; i < voci.size(); i++) {
            eY v = voci.V(i);
            if (v == null) {
                continue;
            }
            String id = v.getValueAsString("Id");
            if (id != null) {
                List<Integer> l = vociPerTipo.get(id);
                if (l == null) {
                    l = new ArrayList<Integer>();
                    vociPerTipo.put(id, l);
                }
                l.add(Integer.valueOf(i));
            }
            occupate.add(cellaDi(v));
        }

        List<int[]> libere = celleLibere(deposito, occupate);
        int prossima = 0;

        for (Map.Entry<String, Integer> e : perTipo.entrySet()) {
            String id = e.getKey();
            if (!id.startsWith(PREFISSO_CORVETTE)) {
                continue; // le decorazioni spariscono con la base, come nel gioco
            }
            int rimanenti = e.getValue().intValue();

            // prima si riempiono le voci che esistono gia'
            List<Integer> esistenti = vociPerTipo.get(id);
            if (esistenti != null) {
                for (int k = 0; k < esistenti.size() && rimanenti > 0; k++) {
                    eY v = voci.V(esistenti.get(k).intValue());
                    int massimo = Math.max(intero(v, "MaxAmount", MASSIMO_PER_VOCE), 1);
                    int attuale = intero(v, "Amount", 0);
                    int spazio = massimo - attuale;
                    if (spazio <= 0) {
                        continue;
                    }
                    int aggiungo = Math.min(spazio, rimanenti);
                    v.b("Amount", Integer.valueOf(attuale + aggiungo));
                    rimanenti -= aggiungo;
                }
            }

            // poi le celle nuove
            while (rimanenti > 0) {
                if (prossima >= libere.size()) {
                    throw new IllegalStateException("Il deposito si e' riempito prima del "
                            + "previsto: non scrivo nulla.");
                }
                int[] cella = libere.get(prossima++);
                int quanto = Math.min(MASSIMO_PER_VOCE, rimanenti);
                voci.add(nuovaVoce(id, quanto, cella[0], cella[1]));
                rimanenti -= quanto;
            }
        }
    }

    /** Una voce di deposito nuova, della stessa forma di quelle del gioco. */
    private static eY nuovaVoce(String id, int quantita, int x, int y) {
        eY v = new eY();

        eY tipo = new eY();
        tipo.b("InventoryType", "Product");
        v.b("Type", tipo);

        v.b("Id", id);
        v.b("Amount", Integer.valueOf(quantita));
        v.b("MaxAmount", Integer.valueOf(MASSIMO_PER_VOCE));
        v.b("DamageFactor", Double.valueOf(0.0));
        v.b("FullyInstalled", Boolean.TRUE);
        v.b("AddedAutomatically", Boolean.FALSE);

        eY indice = new eY();
        indice.b("X", Integer.valueOf(x));
        indice.b("Y", Integer.valueOf(y));
        v.b("Index", indice);

        return v;
    }

    // -------------------------------------------------------------- verifica

    /**
     * Controlla che il modello sia rimasto coerente. Se qualcosa non torna,
     * l'eccezione fa scattare il ripristino del backup.
     */
    private static void verifica(eY stato, Corvette c) {
        eV basi = array(stato, "PersistentPlayerBases");
        eV navi = array(stato, "ShipOwnership");
        if (basi == null || navi == null) {
            throw new IllegalStateException("Dopo la modifica mancano le basi o le navi.");
        }

        // nessuna Corvette deve piu' puntare alla nave rimossa
        for (int i = 0; i < basi.size(); i++) {
            eY b = basi.V(i);
            if (b == null) {
                continue;
            }
            if (!TIPO_BASE_NAVE.equals(b.getValueAsString("BaseType.PersistentBaseTypes"))) {
                continue;
            }
            int u = intero(b, "UserData", -1);
            if (u < 0 || u >= navi.size()) {
                throw new IllegalStateException("Una Corvette e' rimasta collegata a una nave "
                        + "che non esiste piu' (indice " + u + ").");
            }
        }

        // la nave in uso deve esistere ancora
        int primaria = intero(stato, "PrimaryShip", -1);
        if (primaria >= navi.size()) {
            throw new IllegalStateException("La nave in uso punta oltre l'elenco delle navi.");
        }

        // i colori devono essere lunghi quanto le navi
        eV colori = array(stato, "ShipUsesLegacyColours");
        if (colori != null && colori.size() != navi.size()) {
            throw new IllegalStateException("L'elenco dei colori delle navi ha "
                    + colori.size() + " voci, le navi sono " + navi.size() + ".");
        }

        // il deposito non deve avere due voci nella stessa cella
        eY deposito = oggetto(stato, "CorvetteStorageInventory");
        if (deposito != null) {
            eV voci = array(deposito, "Slots");
            Set<String> viste = new LinkedHashSet<String>();
            if (voci != null) {
                for (int i = 0; i < voci.size(); i++) {
                    String cella = cellaDi(voci.V(i));
                    if (!viste.add(cella)) {
                        throw new IllegalStateException("Due moduli finirebbero nella stessa "
                                + "cella del deposito (" + cella + ").");
                    }
                }
            }
        }
    }

    // --------------------------------------------------------------- aiuti

    /** I moduli costruiti sulla Corvette, contati per tipo. */
    private static Map<String, Integer> contaModuli(eY base) {
        Map<String, Integer> out = new LinkedHashMap<String, Integer>();
        eV oggetti = array(base, "Objects");
        if (oggetti == null) {
            return out;
        }
        for (int i = 0; i < oggetti.size(); i++) {
            eY o = oggetti.V(i);
            if (o == null) {
                continue;
            }
            String id = o.getValueAsString("ObjectID");
            if (id == null || id.isEmpty()) {
                continue;
            }
            Integer n = out.get(id);
            out.put(id, Integer.valueOf(n == null ? 1 : n.intValue() + 1));
        }
        return out;
    }

    /** La cella di una voce, come "x,y". */
    private static String cellaDi(eY voce) {
        if (voce == null) {
            return "?,?";
        }
        try {
            eY idx = voce.H("Index");
            if (idx == null) {
                return "?,?";
            }
            return idx.J("X") + "," + idx.J("Y");
        } catch (Throwable t) {
            return "?,?";
        }
    }

    /** Le celle valide del deposito che non sono occupate, in ordine. */
    private static List<int[]> celleLibere(eY deposito, Set<String> occupate) {
        List<int[]> out = new ArrayList<int[]>();
        eV validi = array(deposito, "ValidSlotIndices");
        if (validi == null) {
            return out;
        }
        for (int i = 0; i < validi.size(); i++) {
            eY v = validi.V(i);
            if (v == null) {
                continue;
            }
            int x = intero(v, "X", -1);
            int y = intero(v, "Y", -1);
            if (x < 0 || y < 0) {
                continue;
            }
            if (!occupate.contains(x + "," + y)) {
                out.add(new int[]{x, y});
            }
        }
        return out;
    }

    private static int contaCelleLibere(eY deposito, Set<String> occupate) {
        return celleLibere(deposito, occupate).size();
    }

    private static eY stato(eY radice) {
        if (radice == null) {
            return null;
        }
        try {
            return radice.H("PlayerStateData");
        } catch (Throwable t) {
            return null;
        }
    }

    private static eY oggetto(eY nodo, String campo) {
        if (nodo == null) {
            return null;
        }
        try {
            return nodo.H(campo);
        } catch (Throwable t) {
            return null;
        }
    }

    private static eV array(eY nodo, String campo) {
        if (nodo == null) {
            return null;
        }
        try {
            return nodo.d(campo);
        } catch (Throwable t) {
            return null;
        }
    }

    private static int intero(eY nodo, String campo, int seAssente) {
        if (nodo == null) {
            return seAssente;
        }
        try {
            return nodo.J(campo);
        } catch (Throwable t) {
            return seAssente;
        }
    }
}
