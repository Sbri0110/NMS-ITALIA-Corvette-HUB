package it.nmsitalia.corvettehub.domain;

import nomanssave.eV;
import nomanssave.eY;

import java.util.ArrayList;
import java.util.List;

/**
 * Lettura delle Corvette da un salvataggio.
 *
 * Fatti verificati (fonte: codice di Atlante, gz.java righe 1059-1114, e
 * un salvataggio Xbox reale estratto il 2026-09-13):
 *
 *  - una Corvette e' una base in PlayerStateData.PersistentPlayerBases[]
 *    con BaseType.PersistentBaseTypes == "PlayerShipBase";
 *  - il campo UserData di quella base e' l'indice della nave in
 *    PlayerStateData.ShipOwnership[];
 *  - il nome sta in base.Name;
 *  - i moduli stanno in base.Objects[], ognuno con ObjectID, Position, Up, At;
 *  - la nave in uso e' indicata da PlayerStateData.PrimaryShip.
 *
 * Questa classe non scrive nulla.
 */
public final class LettoreCorvette {

    private static final String TIPO_BASE_NAVE = "PlayerShipBase";

    private LettoreCorvette() {
    }

    /** Risultato complessivo della lettura di uno slot. */
    public static final class Esito {
        public final List<Corvette> corvette = new ArrayList<Corvette>();
        public int naveAttiva = -1;
        public int naveCorrente = -1;
        public String errore;

        public boolean ok() {
            return errore == null;
        }
    }

    /**
     * Legge le Corvette da un modello di salvataggio gia' convertito.
     *
     * @param radice il modello restituito dalla conversione del file .hg
     */
    public static Esito leggi(eY radice) {
        Esito esito = new Esito();
        if (radice == null) {
            esito.errore = "Il salvataggio non e' stato convertito.";
            return esito;
        }

        eY stato;
        try {
            stato = radice.H("PlayerStateData");
        } catch (Throwable t) {
            esito.errore = "Struttura del salvataggio inattesa: " + t;
            return esito;
        }
        if (stato == null) {
            esito.errore = "Il salvataggio non contiene PlayerStateData.";
            return esito;
        }

        try {
            esito.naveAttiva = stato.J("PrimaryShip");
        } catch (Throwable t) {
            esito.naveAttiva = -1;
        }
        try {
            esito.naveCorrente = stato.J("CurrentShip");
        } catch (Throwable t) {
            esito.naveCorrente = -1;
        }

        eV basi;
        try {
            basi = stato.d("PersistentPlayerBases");
        } catch (Throwable t) {
            esito.errore = "Impossibile leggere PersistentPlayerBases: " + t;
            return esito;
        }
        if (basi == null) {
            esito.errore = "Il salvataggio non contiene basi.";
            return esito;
        }

        for (int i = 0; i < basi.size(); i++) {
            eY base;
            try {
                base = basi.V(i);
            } catch (Throwable t) {
                continue;
            }
            if (base == null) {
                continue;
            }
            String tipo;
            try {
                tipo = base.getValueAsString("BaseType.PersistentBaseTypes");
            } catch (Throwable t) {
                continue;
            }
            if (!TIPO_BASE_NAVE.equals(tipo)) {
                continue;
            }
            esito.corvette.add(daBase(i, base, esito.naveAttiva));
        }
        return esito;
    }

    private static Corvette daBase(int indiceBase, eY base, int naveAttiva) {
        int indiceNave = -1;
        try {
            indiceNave = base.J("UserData");
        } catch (Throwable t) {
            indiceNave = -1;
        }

        String nome = null;
        try {
            nome = base.getValueAsString("Name");
        } catch (Throwable t) {
            nome = null;
        }

        List<String> parti = new ArrayList<String>();
        List<double[]> posizioni = new ArrayList<double[]>();
        try {
            eV oggetti = base.d("Objects");
            if (oggetti != null) {
                for (int i = 0; i < oggetti.size(); i++) {
                    eY o = oggetti.V(i);
                    if (o == null) {
                        continue;
                    }
                    String id = o.getValueAsString("ObjectID");
                    if (id != null && !id.isEmpty()) {
                        parti.add(id);
                        posizioni.add(posizioneDi(o));
                    }
                }
            }
        } catch (Throwable t) {
            // nessun modulo leggibile: la Corvette resta con zero parti
        }

        double[] pos = new double[]{0, 0, 0};
        try {
            eV p = base.d("Position");
            if (p != null && p.size() >= 3) {
                pos[0] = p.aa(0);
                pos[1] = p.aa(1);
                pos[2] = p.aa(2);
            }
        } catch (Throwable t) {
            // posizione non leggibile: resta a zero
        }

        Corvette c = new Corvette(indiceBase, indiceNave, nome, parti, posizioni,
                naveAttiva >= 0 && indiceNave == naveAttiva, pos);
        c.setNodo(base);
        return c;
    }

    /** Posizione di un modulo dentro la Corvette, in metri. */
    private static double[] posizioneDi(eY o) {
        double[] p = new double[]{0, 0, 0};
        try {
            eV v = o.d("Position");
            if (v != null && v.size() >= 3) {
                p[0] = v.aa(0);
                p[1] = v.aa(1);
                p[2] = v.aa(2);
            }
        } catch (Throwable t) {
            // posizione non leggibile: resta a zero
        }
        return p;
    }
}
