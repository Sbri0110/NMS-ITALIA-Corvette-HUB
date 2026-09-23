package it.nmsitalia.corvettehub.domain;

import nomanssave.eY;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Il catalogo delle parti costruibili su una Corvette.
 *
 * Fonte: res/parts_catalog.json, ricavato da basebuildingobjectstable
 * dell'installazione di gioco con MBINCompiler. Contiene 757 voci con prefisso
 * B_, ognuna con i campi CorvetteBaseLimit, IsDecoration,
 * BuildableInShipStructural, BuildableInShipDecorative e scene.
 *
 * Numeri misurati (2026-09-23):
 *   622 voci costruibili come strutturali
 *    42 voci costruibili come decorative
 *     3 voci presenti in entrambi gli elenchi
 *   ---
 *   661 voci distinte costruibili su una nave
 *
 * Quindi il totale da mostrare e' 661, non 664: sommare 622 e 42 conterebbe
 * tre parti due volte.
 */
public final class CatalogoParti {

    /** Una parte del catalogo. */
    public static final class Parte {
        public final String id;
        public final String categoria;
        public final boolean strutturale;
        public final boolean decorativa;

        Parte(String id, String categoria, boolean strutturale, boolean decorativa) {
            this.id = id;
            this.categoria = categoria;
            this.strutturale = strutturale;
            this.decorativa = decorativa;
        }
    }

    private static CatalogoParti istanza;

    private final Map<String, Parte> perId = new LinkedHashMap<String, Parte>();
    private final List<String> categorie = new ArrayList<String>();
    private int strutturali;
    private int decorative;
    private int inEntrambe;

    private CatalogoParti() {
    }

    public static synchronized CatalogoParti get() {
        if (istanza == null) {
            istanza = new CatalogoParti();
            istanza.carica();
        }
        return istanza;
    }

    public int getTotale() {
        return perId.size();
    }

    public int getStrutturali() {
        return strutturali;
    }

    public int getDecorative() {
        return decorative;
    }

    public int getInEntrambe() {
        return inEntrambe;
    }

    public Parte get(String id) {
        return id == null ? null : perId.get(normalizza(id));
    }

    public boolean contiene(String id) {
        return id != null && perId.containsKey(normalizza(id));
    }

    /**
     * Nel catalogo le chiavi sono senza il carattere ^ (B_COK_A), mentre nel
     * salvataggio gli identificativi lo portano (^B_COK_A). Normalizziamo
     * togliendolo, cosi' i due mondi si parlano.
     */
    private static String normalizza(String id) {
        return id.startsWith("^") ? id.substring(1) : id;
    }

    /** Categorie note, in ordine di importanza. */
    public List<String> getCategorie() {
        return Collections.unmodifiableList(categorie);
    }

    // ------------------------------------------------------------- interno

    private void carica() {
        String testo = leggiRisorsa("/res/parts_catalog.json");
        if (testo == null) {
            return;
        }
        eY radice;
        try {
            radice = eY.E(testo);
        } catch (Throwable t) {
            return;
        }
        if (radice == null) {
            return;
        }
        List<String> nomi = radice.names();
        for (int i = 0; i < nomi.size(); i++) {
            String id = nomi.get(i);
            eY v = radice.H(id);
            if (v == null) {
                continue;
            }
            boolean str = "true".equals(v.getValueAsString("BuildableInShipStructural"));
            boolean dec = "true".equals(v.getValueAsString("BuildableInShipDecorative"));
            if (!str && !dec) {
                continue;
            }
            if (str) {
                strutturali++;
            }
            if (dec) {
                decorative++;
            }
            if (str && dec) {
                inEntrambe++;
            }
            String cat = categoria(id);
            perId.put(normalizza(id), new Parte(id, cat, str, dec));
            if (!categorie.contains(cat)) {
                categorie.add(cat);
            }
        }
        Collections.sort(categorie);
    }

    /**
     * Categoria di una parte, ricavata dal prefisso dell'identificativo.
     * I prefissi sono quelli misurati sul catalogo, non inventati.
     */
    public static String categoria(String id) {
        if (id == null) {
            return "Altro";
        }
        String s = id.startsWith("^") ? id.substring(1) : id;
        if (!s.startsWith("B_")) {
            return "Altro";
        }
        String c = s.substring(2);
        if (c.startsWith("COK")) return "Cockpit";
        if (c.startsWith("CANOPY")) return "Cabina";
        if (c.startsWith("HAB")) return "Moduli abitativi";
        if (c.startsWith("HOP")) return "Moduli abitativi";
        if (c.startsWith("ALK")) return "Corridoi e passaggi";
        if (c.startsWith("WALL")) return "Pareti";
        if (c.startsWith("DOOR")) return "Porte";
        if (c.startsWith("LND")) return "Carrelli di atterraggio";
        if (c.startsWith("LAN")) return "Carrelli di atterraggio";
        if (c.startsWith("CARRIAGEWHEEL")) return "Carrelli di atterraggio";
        if (c.startsWith("WNG")) return "Ali";
        if (c.startsWith("TRU")) return "Propulsori";
        if (c.startsWith("TUR")) return "Torrette";
        if (c.startsWith("SHL")) return "Scudi";
        if (c.startsWith("GEN")) return "Reattori";
        if (c.startsWith("CON")) return "Connettori";
        if (c.startsWith("STR")) return "Corazze e profili";
        if (c.startsWith("DECO")) return "Decorazioni";
        if (c.startsWith("MAG")) return "Magazzini";
        if (c.startsWith("PALLET")) return "Pedane";
        if (c.startsWith("SAIL")) return "Vele";
        if (c.startsWith("ANTENNA")) return "Antenne";
        if (c.startsWith("TRYE")) return "Cargo";
        return "Altro";
    }

    private static String leggiRisorsa(String percorso) {
        InputStream in = null;
        try {
            in = CatalogoParti.class.getResourceAsStream(percorso);
            if (in == null) {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), "UTF-8");
        } catch (Throwable t) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {
                    // nulla da fare
                }
            }
        }
    }
}
