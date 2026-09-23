package it.nmsitalia.corvettehub.domain;

import nomanssave.eV;
import nomanssave.eY;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * I dati di una Corvette che interessano all'utente: statistiche, classe,
 * slot supercaricati, modello e depositi montati.
 *
 * Tutto verificato su un salvataggio Xbox reale il 2026-09-23:
 *
 *  - le statistiche NON sono campi della nave: stanno in
 *    ShipOwnership[i].Inventory_TechOnly.BaseStatValues, come elenco di
 *    {BaseStatID, Value} con ^SHIP_DAMAGE, ^SHIP_SHIELD, ^SHIP_HYPERDRIVE,
 *    ^SHIP_AGILE. Valori misurati: 995, 995, 285, 85.
 *  - la classe sta in Inventory_TechOnly.Class come
 *    {InventoryClass: "S"}.
 *  - gli slot supercaricati stanno in Inventory_TechOnly.SpecialSlots, come
 *    elenco di {Type: {InventorySpecialSlotType: "TechBonus"}, Index: {X, Y}}.
 *    Misurati: 60 su una griglia 10 x 6.
 *  - il modello della nave sta in Resource.Filename
 *    (MODELS/COMMON/SPACECRAFT/BIGGS/BIGGS.SCENE.MBIN per le Corvette).
 *
 * Sola lettura.
 */
public final class StatisticheCorvette {

    /** Una statistica con il suo nome in italiano. */
    public static final class Statistica {
        public final String nome;
        public final double valore;
        public final String id;

        Statistica(String nome, double valore, String id) {
            this.nome = nome;
            this.valore = valore;
            this.id = id;
        }

        public boolean disponibile() {
            return valore >= 0;
        }
    }

    private final List<Statistica> statistiche = new ArrayList<Statistica>();
    private String classe = "?";
    private int slotSupercaricati;
    private int colonne;
    private int righe;
    private String modello = "";
    private int indiceNave = -1;
    private int moduliTotali;
    private int tipiDistinti;
    private final List<String> depositiMontati = new ArrayList<String>();

    private StatisticheCorvette() {
    }

    public List<Statistica> getStatistiche() {
        return statistiche;
    }

    public String getClasse() {
        return classe;
    }

    public int getSlotSupercaricati() {
        return slotSupercaricati;
    }

    public int getColonne() {
        return colonne;
    }

    public int getRighe() {
        return righe;
    }

    /** Il modello, ridotto all'ultima parte del percorso. */
    public String getModello() {
        return modello;
    }

    public String getModelloBreve() {
        int i = modello.lastIndexOf('/');
        String s = i >= 0 ? modello.substring(i + 1) : modello;
        int j = s.indexOf('.');
        return j > 0 ? s.substring(0, j) : s;
    }

    public int getIndiceNave() {
        return indiceNave;
    }

    public int getModuliTotali() {
        return moduliTotali;
    }

    public int getTipiDistinti() {
        return tipiDistinti;
    }

    /** Moduli di deposito montati sulla Corvette (di solito nessuno). */
    public List<String> getDepositiMontati() {
        return depositiMontati;
    }

    /** Nome italiano di una statistica del gioco. */
    public static String nomeStatistica(String id) {
        if (id == null) {
            return "?";
        }
        if (id.endsWith("SHIP_DAMAGE")) return "Danni";
        if (id.endsWith("SHIP_SHIELD")) return "Scudi";
        if (id.endsWith("SHIP_HYPERDRIVE")) return "Iperguida";
        if (id.endsWith("SHIP_AGILE")) return "Agilita'";
        if (id.endsWith("SHIP_HEALTH")) return "Integrita'";
        return id.replace("^", "");
    }

    /**
     * Legge i dati di una Corvette.
     *
     * @param radice il modello del salvataggio
     * @param c      la Corvette, da cui si prende l'indice della nave collegata
     */
    public static StatisticheCorvette leggi(eY radice, Corvette c) {
        StatisticheCorvette s = new StatisticheCorvette();
        if (radice == null || c == null) {
            return s;
        }
        s.indiceNave = c.getIndiceNave();
        s.moduliTotali = c.getNumeroModuli();
        s.tipiDistinti = c.getModuliDistinti();

        // i depositi montati si cercano fra i moduli della Corvette
        List<String> parti = c.getParti();
        for (int i = 0; i < parti.size(); i++) {
            String id = parti.get(i);
            if (id == null) {
                continue;
            }
            String u = id.toUpperCase(Locale.ROOT);
            if (u.contains("STORAGE") || u.contains("CHEST") || u.contains("CONTAINER")
                    || u.contains("CRATE") || u.contains("_MAG_")) {
                if (!s.depositiMontati.contains(id)) {
                    s.depositiMontati.add(id);
                }
            }
        }

        eY stato;
        try {
            stato = radice.H("PlayerStateData");
        } catch (Throwable t) {
            return s;
        }
        if (stato == null) {
            return s;
        }

        eY nave = nave(stato, s.indiceNave);
        if (nave == null) {
            return s;
        }

        // modello
        try {
            String f = nave.getValueAsString("Resource.Filename");
            if (f != null) {
                s.modello = f;
            }
        } catch (Throwable ignored) {
            // modello non leggibile
        }

        eY tech = null;
        try {
            tech = nave.H("Inventory_TechOnly");
        } catch (Throwable ignored) {
            tech = null;
        }
        if (tech == null) {
            return s;
        }

        // classe
        try {
            eY cl = tech.H("Class");
            if (cl != null) {
                String v = cl.getValueAsString("InventoryClass");
                if (v != null && !v.isEmpty()) {
                    s.classe = v;
                }
            }
        } catch (Throwable ignored) {
            // classe non leggibile
        }

        // dimensioni della griglia tecnologica
        try {
            s.colonne = tech.J("Width");
            s.righe = tech.J("Height");
        } catch (Throwable ignored) {
            s.colonne = 0;
            s.righe = 0;
        }

        // slot supercaricati
        try {
            eV speciali = tech.d("SpecialSlots");
            if (speciali != null) {
                s.slotSupercaricati = speciali.size();
            }
        } catch (Throwable ignored) {
            s.slotSupercaricati = 0;
        }

        // statistiche
        try {
            eV valori = tech.d("BaseStatValues");
            if (valori != null) {
                for (int i = 0; i < valori.size(); i++) {
                    eY v = valori.V(i);
                    if (v == null) {
                        continue;
                    }
                    String id = v.getValueAsString("BaseStatID");
                    if (id == null) {
                        continue;
                    }
                    double valore = -1;
                    try {
                        valore = v.L("Value");
                    } catch (Throwable ignored) {
                        valore = -1;
                    }
                    s.statistiche.add(new Statistica(nomeStatistica(id), valore, id));
                }
            }
        } catch (Throwable ignored) {
            // statistiche non leggibili
        }
        return s;
    }

    private static eY nave(eY stato, int indice) {
        if (indice < 0) {
            return null;
        }
        try {
            eV navi = stato.d("ShipOwnership");
            if (navi == null || indice >= navi.size()) {
                return null;
            }
            return navi.V(indice);
        } catch (Throwable t) {
            return null;
        }
    }
}
