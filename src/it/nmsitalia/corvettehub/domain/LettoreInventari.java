package it.nmsitalia.corvettehub.domain;

import nomanssave.eV;
import nomanssave.eY;

import java.util.ArrayList;
import java.util.List;

/**
 * Lettura degli inventari che interessano il tool: le tecnologie installate,
 * il deposito della Corvette e i depositi delle basi.
 *
 * Struttura verificata su un salvataggio Xbox reale il 2026-09-23:
 *
 *  - ogni inventario ha i campi Slots, Width, Height, ValidSlotIndices;
 *  - ogni voce di Slots ha Type (oggetto con InventoryType: "Product" oppure
 *    "Technology"), Id e Amount;
 *  - le tecnologie della Corvette stanno nella nave collegata, in
 *    ShipOwnership[i].Inventory_TechOnly;
 *  - il deposito della Corvette sta in CorvetteStorageInventory;
 *  - i depositi delle basi stanno in Chest1Inventory .. Chest10Inventory,
 *    e nell'elenco Objects della base compaiono i moduli ^CONTAINER0 .. 9
 *    che li rendono visibili in gioco.
 *
 * Sola lettura.
 */
public final class LettoreInventari {

    /** Una voce di inventario. */
    public static final class Voce {
        public final String tipo;
        public final String id;
        public final int quantita;

        Voce(String tipo, String id, int quantita) {
            this.tipo = tipo;
            this.id = id;
            this.quantita = quantita;
        }

        public boolean isTecnologia() {
            return "Technology".equalsIgnoreCase(tipo);
        }

        @Override
        public String toString() {
            return id + " x" + quantita;
        }
    }

    /** Un deposito letto dal salvataggio. */
    public static final class Deposito {
        public final String nome;
        public final String origine;
        public final List<Voce> voci = new ArrayList<Voce>();
        public int slotTotali;

        public Deposito(String nome, String origine) {
            this.nome = nome;
            this.origine = origine;
        }

        public int getTotalePezzi() {
            int n = 0;
            for (int i = 0; i < voci.size(); i++) {
                n += voci.get(i).quantita;
            }
            return n;
        }

        public int getModuliDistinti() {
            java.util.Set<String> s = new java.util.LinkedHashSet<String>();
            for (int i = 0; i < voci.size(); i++) {
                s.add(voci.get(i).id);
            }
            return s.size();
        }
    }

    private LettoreInventari() {
    }

    /** Legge tutte le voci non vuote di un inventario. */
    public static List<Voce> leggiVoci(eY inventario) {
        List<Voce> out = new ArrayList<Voce>();
        if (inventario == null) {
            return out;
        }
        eV slots;
        try {
            slots = inventario.d("Slots");
        } catch (Throwable t) {
            return out;
        }
        if (slots == null) {
            return out;
        }
        for (int i = 0; i < slots.size(); i++) {
            eY s;
            try {
                s = slots.V(i);
            } catch (Throwable t) {
                continue;
            }
            if (s == null) {
                continue;
            }
            String id;
            try {
                id = s.getValueAsString("Id");
            } catch (Throwable t) {
                continue;
            }
            if (id == null || id.isEmpty()) {
                continue;
            }
            String tipo = "?";
            try {
                eY t = s.H("Type");
                if (t != null) {
                    String it = t.getValueAsString("InventoryType");
                    if (it != null) {
                        tipo = it;
                    }
                }
            } catch (Throwable ignored) {
                // tipo non leggibile: resta "?"
            }
            int quantita = 1;
            try {
                quantita = s.J("Amount");
                if (quantita <= 0) {
                    quantita = 1;
                }
            } catch (Throwable ignored) {
                // quantita' non leggibile: si assume 1
            }
            out.add(new Voce(tipo, id, quantita));
        }
        return out;
    }

    /**
     * Tecnologie installate sulla nave collegata a una Corvette.
     *
     * @param radice    modello del salvataggio
     * @param indiceNave indice in ShipOwnership (campo UserData della base)
     */
    public static Deposito tecnologieCorvette(eY radice, int indiceNave) {
        Deposito d = new Deposito("Tecnologie installate",
                "ShipOwnership[" + indiceNave + "].Inventory_TechOnly");
        eY nave = nave(radice, indiceNave);
        if (nave == null) {
            return d;
        }
        eY inv = nave.H("Inventory_TechOnly");
        d.slotTotali = contaSlot(inv);
        d.voci.addAll(leggiVoci(inv));
        return d;
    }

    /** Il deposito della Stazione Spaziale: sola lettura. */
    public static Deposito depositoCorvette(eY radice) {
        Deposito d = new Deposito("Deposito moduli Corvette",
                "PlayerStateData.CorvetteStorageInventory");
        eY inv = stato(radice);
        if (inv == null) {
            return d;
        }
        eY dep = inv.H("CorvetteStorageInventory");
        d.slotTotali = contaSlot(dep);
        d.voci.addAll(leggiVoci(dep));
        return d;
    }

    /**
     * Le dimensioni della griglia di un inventario, come le mostra il gioco.
     *
     * @return {larghezza, altezza}, oppure {10, 16} se non leggibili: 160 e' il
     *         numero di slot del deposito della Corvette.
     */
    public static int[] dimensioni(eY inventario) {
        int larghezza = 0;
        int altezza = 0;
        if (inventario != null) {
            try {
                larghezza = inventario.J("Width");
                altezza = inventario.J("Height");
            } catch (Throwable ignored) {
                larghezza = 0;
                altezza = 0;
            }
        }
        if (larghezza < 1) {
            larghezza = 10;
        }
        if (altezza < 1) {
            altezza = 16;
        }
        return new int[]{larghezza, altezza};
    }

    /** Le dimensioni del deposito della Corvette. */
    public static int[] dimensioniDepositoCorvette(eY radice) {
        eY inv = stato(radice);
        return dimensioni(inv == null ? null : inv.H("CorvetteStorageInventory"));
    }

    /** Le dimensioni di un inventario di una nave. */
    public static int[] dimensioniInventario(eY radice, int indiceNave, String campo) {
        eY nave = nave(radice, indiceNave);
        return dimensioni(nave == null ? null : nave.H(campo));
    }

    /**
     * I depositi delle basi: per ogni base che contiene moduli ^CONTAINER,
     * i relativi inventari Chest.
     *
     * La corrispondenza verificata e': ^CONTAINER0 -> Chest1Inventory,
     * ^CONTAINER1 -> Chest2Inventory, e cosi' via fino a ^CONTAINER9 ->
     * Chest10Inventory. Aggiungiamo anche i due contenitori "magic", che nel
     * salvataggio hanno inventari propri.
     */
    public static List<Deposito> depositiBasi(eY radice) {
        List<Deposito> out = new ArrayList<Deposito>();
        eY stato = stato(radice);
        if (stato == null) {
            return out;
        }
        eV basi;
        try {
            basi = stato.d("PersistentPlayerBases");
        } catch (Throwable t) {
            return out;
        }
        if (basi == null) {
            return out;
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
            String tipo = base.getValueAsString("BaseType.PersistentBaseTypes");
            if ("PlayerShipBase".equals(tipo)) {
                continue; // la Corvette la trattiamo a parte
            }
            String nomeBase = base.getValueAsString("Name");
            if (nomeBase == null || nomeBase.trim().isEmpty()) {
                nomeBase = "Base " + (i + 1);
            }
            eV oggetti = base.d("Objects");
            if (oggetti == null) {
                continue;
            }
            for (int k = 0; k < oggetti.size(); k++) {
                eY o = oggetti.V(k);
                if (o == null) {
                    continue;
                }
                String id = o.getValueAsString("ObjectID");
                if (id == null) {
                    continue;
                }
                String campo = campoInventario(id);
                if (campo == null) {
                    continue;
                }
                eY inv = stato.H(campo);
                List<Voce> voci = leggiVoci(inv);
                if (voci.isEmpty()) {
                    continue;
                }
                Deposito d = new Deposito(id, nomeBase + " · " + campo);
                d.slotTotali = contaSlot(inv);
                d.voci.addAll(voci);
                out.add(d);
            }
        }
        return out;
    }

    /** Mappa il modulo base al nome del suo inventario nel salvataggio. */
    public static String campoInventario(String objectId) {
        if (objectId == null) {
            return null;
        }
        if (objectId.startsWith("^CONTAINER")) {
            String n = objectId.substring("^CONTAINER".length());
            try {
                int i = Integer.parseInt(n);
                return "Chest" + (i + 1) + "Inventory";
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if ("^MAGICCONTAINER".equals(objectId)) {
            return "ChestMagicInventory";
        }
        if ("^MAGICCONTAINER2".equals(objectId)) {
            return "ChestMagic2Inventory";
        }
        return null;
    }

    // ------------------------------------------------------------- interni

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

    private static eY nave(eY radice, int indice) {
        eY stato = stato(radice);
        if (stato == null || indice < 0) {
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

    private static int contaSlot(eY inventario) {
        if (inventario == null) {
            return 0;
        }
        try {
            eV s = inventario.d("Slots");
            return s == null ? 0 : s.size();
        } catch (Throwable t) {
            return 0;
        }
    }
}
