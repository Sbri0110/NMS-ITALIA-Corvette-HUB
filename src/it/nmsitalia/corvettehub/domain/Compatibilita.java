package it.nmsitalia.corvettehub.domain;

import nomanssave.eV;
import nomanssave.eY;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Il controllo di compatibilita' di una build con un salvataggio.
 *
 * E' il punto di forza di questo tool rispetto agli strumenti esistenti.
 * Per ogni parte richiesta da una build dice in quale delle tre condizioni si
 * trova:
 *
 *   1. NEL DEPOSITO              ce l'hai gia', pronta all'uso
 *   2. SBLOCCATA, NON IN DEPOSITO la conosci, puoi costruirla alla stazione
 *   3. MANCANTE                  non l'hai mai vista: la build risultera' incompleta
 *
 * Le prime due si ricavano incrociando CorvetteStorageInventory con
 * KnownProducts e SeenBaseBuildingObjects. La terza e' il complemento.
 *
 * Fonte della struttura: gz.java di Atlante e ispezione diretta di un
 * salvataggio Xbox reale (2026-09-23).
 */
public final class Compatibilita {

    public static final int NEL_DEPOSITO = 1;
    public static final int SBLOCCATA = 2;
    public static final int MANCANTE = 3;

    /** Lo stato di una singola parte. */
    public static final class EsitoParte {
        public final String id;
        public final int stato;
        public final String categoria;

        EsitoParte(String id, int stato, String categoria) {
            this.id = id;
            this.stato = stato;
            this.categoria = categoria;
        }

        public String descrizioneStato() {
            switch (stato) {
                case NEL_DEPOSITO:
                    return "nel deposito";
                case SBLOCCATA:
                    return "sbloccata, non in deposito";
                default:
                    return "mancante";
            }
        }
    }

    private final List<EsitoParte> parti = new ArrayList<EsitoParte>();
    private int nelDeposito;
    private int sbloccate;
    private int mancanti;
    private boolean datiDisponibili = true;
    private String nota;

    private Compatibilita() {
    }

    public List<EsitoParte> getParti() {
        return parti;
    }

    public int getNelDeposito() {
        return nelDeposito;
    }

    public int getSbloccate() {
        return sbloccate;
    }

    public int getMancanti() {
        return mancanti;
    }

    public int getTotaleRichieste() {
        return parti.size();
    }

    /** Vero se TUTTE le parti richieste sono almeno sbloccate. */
    public boolean isCompleta() {
        return mancanti == 0;
    }

    /** Vero se non e' stato possibile leggere il catalogo del salvataggio. */
    public boolean isDatiIncompleti() {
        return !datiDisponibili;
    }

    public String getNota() {
        return nota;
    }

    public List<String> getPartiMancanti() {
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < parti.size(); i++) {
            if (parti.get(i).stato == MANCANTE) {
                out.add(parti.get(i).id);
            }
        }
        return out;
    }

    public List<String> getPartiSbloccateNonInDeposito() {
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < parti.size(); i++) {
            if (parti.get(i).stato == SBLOCCATA) {
                out.add(parti.get(i).id);
            }
        }
        return out;
    }

    /**
     * Calcola la compatibilita' di un insieme di parti richieste con un
     * salvataggio.
     *
     * @param richieste gli ObjectID richiesti dalla build
     * @param radice    il modello del salvataggio di destinazione
     */
    public static Compatibilita calcola(Set<String> richieste, eY radice) {
        Compatibilita c = new Compatibilita();
        if (richieste == null || richieste.isEmpty()) {
            c.nota = "La build non richiede parti.";
            return c;
        }
        if (radice == null) {
            c.datiDisponibili = false;
            c.nota = "Salvataggio non leggibile: non posso verificare la compatibilita'.";
            for (String id : richieste) {
                c.parti.add(new EsitoParte(id, MANCANTE, CatalogoParti.categoria(id)));
            }
            c.mancanti = richieste.size();
            return c;
        }

        Set<String> deposito = idsDeposito(radice);
        Set<String> conosciute = idsConosciute(radice);

        if (deposito.isEmpty() && conosciute.isEmpty()) {
            c.datiDisponibili = false;
            c.nota = "Il salvataggio non espone ne' il deposito ne' il catalogo "
                    + "delle parti conosciute: la verifica non e' attendibile.";
        }

        for (String id : richieste) {
            int stato;
            if (deposito.contains(id)) {
                stato = NEL_DEPOSITO;
                c.nelDeposito++;
            } else if (conosciute.contains(id)) {
                stato = SBLOCCATA;
                c.sbloccate++;
            } else {
                stato = MANCANTE;
                c.mancanti++;
            }
            c.parti.add(new EsitoParte(id, stato, CatalogoParti.categoria(id)));
        }
        return c;
    }

    /** Identificativi presenti nel deposito della Stazione Spaziale. */
    public static Set<String> idsDeposito(eY radice) {
        Set<String> out = new LinkedHashSet<String>();
        eY stato = stato(radice);
        if (stato == null) {
            return out;
        }
        eY dep = stato.H("CorvetteStorageInventory");
        List<LettoreInventari.Voce> voci = LettoreInventari.leggiVoci(dep);
        for (int i = 0; i < voci.size(); i++) {
            out.add(voci.get(i).id);
        }
        return out;
    }

    /**
     * Identificativi che il salvataggio conosce: l'unione di KnownProducts e
     * SeenBaseBuildingObjects. Questi due elenchi sono la chiave del controllo.
     */
    public static Set<String> idsConosciute(eY radice) {
        Set<String> out = new LinkedHashSet<String>();
        eY stato = stato(radice);
        if (stato == null) {
            return out;
        }
        raccogli(stato, "KnownProducts", out);
        raccogli(stato, "SeenBaseBuildingObjects", out);
        raccogli(stato, "BaseBuildingObjects", out);
        return out;
    }

    private static void raccogli(eY stato, String campo, Set<String> out) {
        eV elenco;
        try {
            elenco = stato.d(campo);
        } catch (Throwable t) {
            return;
        }
        if (elenco == null) {
            return;
        }
        for (int i = 0; i < elenco.size(); i++) {
            Object v;
            try {
                v = elenco.get(i);
            } catch (Throwable t) {
                continue;
            }
            if (v == null) {
                continue;
            }
            if (v instanceof String) {
                String s = (String) v;
                if (!s.isEmpty()) {
                    out.add(s);
                }
            } else if (v instanceof eY) {
                // alcune voci sono oggetti: proviamo i nomi di campo plausibili
                eY o = (eY) v;
                String[] campi = {"Id", "ProductId", "ObjectID", "Name"};
                for (int k = 0; k < campi.length; k++) {
                    String s = null;
                    try {
                        s = o.getValueAsString(campi[k]);
                    } catch (Throwable ignored) {
                        // campo assente
                    }
                    if (s != null && !s.isEmpty()) {
                        out.add(s);
                        break;
                    }
                }
            }
        }
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
}
