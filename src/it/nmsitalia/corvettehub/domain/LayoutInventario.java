package it.nmsitalia.corvettehub.domain;

import nomanssave.eV;
import nomanssave.eY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * La disposizione di un inventario, con la possibilita' di spostare le cose.
 *
 * COME FUNZIONA L'INVENTARIO (verificato sul salvataggio reale)
 * -------------------------------------------------------------
 * In Inventory.Slots NON ci sono tutte le celle: ci sono solo quelle OCCUPATE,
 * e ognuna porta la sua coordinata in Index = {X, Y}. Le celle utilizzabili
 * sono elencate in ValidSlotIndices (nell'inventario della nave: 118 su 120,
 * le due in fondo a destra sono bloccate dal gioco).
 *
 * Quindi spostare un oggetto NON vuol dire togliere e rimettere niente: vuol
 * dire cambiare la sua coordinata. Nessun oggetto viene creato o distrutto, e
 * la classe lo verifica prima di scrivere.
 *
 * ATTENZIONE AL MOMENTO DELLA SCRITTURA
 * -------------------------------------
 * Lo scrittore del salvataggio RILEGGE i modelli dai file, quindi un oggetto
 * di questa classe non vale per il modello che verra' scritto: sono due
 * istanze diverse. Per questo la modifica si conserva come elenco di
 * "da dove a dove" e si applica al modello fresco con {@link #applica(eY)}.
 * La corrispondenza si fa sulla POSIZIONE DI PARTENZA, che nel modello fresco
 * e' identica a quella che abbiamo letto.
 */
public final class LayoutInventario {

    /** Un oggetto dell'inventario: dov'era e dove sta adesso. */
    public static final class Pezzo {
        public final String id;
        public final String tipo;
        public final int quantita;
        /** La cella in cui si trovava quando e' stato letto. */
        public final int xIniziale;
        public final int yIniziale;
        /** La cella in cui si trova adesso. */
        public int x;
        public int y;

        Pezzo(String id, String tipo, int quantita, int x, int y) {
            this.id = id;
            this.tipo = tipo;
            this.quantita = quantita;
            this.xIniziale = x;
            this.yIniziale = y;
            this.x = x;
            this.y = y;
        }

        public boolean spostato() {
            return x != xIniziale || y != yIniziale;
        }
    }

    private final String nome;
    private final int larghezza;
    private final int altezza;
    private final boolean[] valide;
    private final List<Pezzo> pezzi = new ArrayList<Pezzo>();
    /** cella attuale -> oggetto. */
    private final Map<Integer, Pezzo> perCella = new HashMap<Integer, Pezzo>();
    /** cella di partenza -> oggetto: serve per riapplicare al modello fresco. */
    private final Map<Integer, Pezzo> perCellaIniziale = new HashMap<Integer, Pezzo>();

    private LayoutInventario(String nome, int larghezza, int altezza, boolean[] valide) {
        this.nome = nome;
        this.larghezza = larghezza;
        this.altezza = altezza;
        this.valide = valide;
    }

    // ---------------------------------------------------------------- lettura

    /** Legge un inventario. {@code nome} serve solo per l'interfaccia. */
    public static LayoutInventario leggi(eY inventario, String nome) {
        int w = 10;
        int h = 12;
        try {
            w = inventario.J("Width");
            h = inventario.J("Height");
        } catch (Throwable ignored) {
            // si usano i valori di partenza
        }
        if (w < 1) {
            w = 10;
        }
        if (h < 1) {
            h = 12;
        }

        boolean[] valide = new boolean[w * h];
        boolean lette = false;
        try {
            eV validi = inventario.d("ValidSlotIndices");
            if (validi != null && validi.size() > 0) {
                for (int i = 0; i < validi.size(); i++) {
                    eY v = validi.V(i);
                    if (v == null) {
                        continue;
                    }
                    int x = v.J("X");
                    int y = v.J("Y");
                    if (x >= 0 && y >= 0 && x < w && y < h) {
                        valide[y * w + x] = true;
                        lette = true;
                    }
                }
            }
        } catch (Throwable ignored) {
            lette = false;
        }
        if (!lette) {
            for (int i = 0; i < valide.length; i++) {
                valide[i] = true;
            }
        }

        LayoutInventario l = new LayoutInventario(nome, w, h, valide);
        l.carica(inventario);
        return l;
    }

    private void carica(eY inventario) {
        pezzi.clear();
        perCella.clear();
        perCellaIniziale.clear();
        eV slots = inventario.d("Slots");
        if (slots == null) {
            return;
        }
        for (int i = 0; i < slots.size(); i++) {
            eY v = slots.V(i);
            if (v == null) {
                continue;
            }
            eY idx = v.H("Index");
            int x = idx == null ? 0 : idx.J("X");
            int y = idx == null ? 0 : idx.J("Y");
            String tipo = "";
            try {
                eY t = v.H("Type");
                if (t != null) {
                    tipo = t.getValueAsString("InventoryType");
                }
            } catch (Throwable ignored) {
                tipo = "";
            }
            int quantita = 0;
            try {
                quantita = v.J("Amount");
            } catch (Throwable ignored) {
                quantita = 0;
            }
            Pezzo p = new Pezzo(v.getValueAsString("Id"), tipo, quantita, x, y);
            pezzi.add(p);
            if (dentro(x, y)) {
                perCella.put(Integer.valueOf(y * larghezza + x), p);
                perCellaIniziale.put(Integer.valueOf(y * larghezza + x), p);
            }
        }
    }

    private boolean dentro(int x, int y) {
        return x >= 0 && y >= 0 && x < larghezza && y < altezza;
    }

    // ------------------------------------------------------------- domande

    public String getNome() {
        return nome;
    }

    public int getLarghezza() {
        return larghezza;
    }

    public int getAltezza() {
        return altezza;
    }

    public List<Pezzo> getPezzi() {
        return pezzi;
    }

    public int getNumeroPezzi() {
        return pezzi.size();
    }

    /** L'oggetto che sta adesso in una cella, oppure null se e' libera. */
    public Pezzo a(int x, int y) {
        if (!dentro(x, y)) {
            return null;
        }
        return perCella.get(Integer.valueOf(y * larghezza + x));
    }

    /** Vero se il gioco considera utilizzabile questa cella. */
    public boolean valida(int x, int y) {
        if (!dentro(x, y)) {
            return false;
        }
        return valide[y * larghezza + x];
    }

    public boolean modificato() {
        for (int i = 0; i < pezzi.size(); i++) {
            if (pezzi.get(i).spostato()) {
                return true;
            }
        }
        return false;
    }

    public int quantiSpostati() {
        int n = 0;
        for (int i = 0; i < pezzi.size(); i++) {
            if (pezzi.get(i).spostato()) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------ modifica

    /**
     * Sposta l'oggetto da una cella a un'altra.
     *
     * Se la destinazione e' occupata, i due si scambiano di posto: e' quello
     * che ci si aspetta trascinando una cosa sopra un'altra.
     *
     * @return true se lo spostamento e' avvenuto
     */
    public boolean sposta(int daX, int daY, int aX, int aY) {
        if (daX == aX && daY == aY) {
            return false;
        }
        if (!valida(daX, daY) || !valida(aX, aY)) {
            return false;
        }
        Pezzo origine = a(daX, daY);
        if (origine == null) {
            return false;
        }
        Pezzo destinazione = a(aX, aY);

        perCella.remove(Integer.valueOf(daY * larghezza + daX));
        perCella.remove(Integer.valueOf(aY * larghezza + aX));

        origine.x = aX;
        origine.y = aY;
        perCella.put(Integer.valueOf(aY * larghezza + aX), origine);

        if (destinazione != null) {
            destinazione.x = daX;
            destinazione.y = daY;
            perCella.put(Integer.valueOf(daY * larghezza + daX), destinazione);
        }
        return true;
    }

    /** Rimette tutto com'era. */
    public void annulla() {
        for (int i = 0; i < pezzi.size(); i++) {
            Pezzo p = pezzi.get(i);
            p.x = p.xIniziale;
            p.y = p.yIniziale;
        }
        perCella.clear();
        perCella.putAll(perCellaIniziale);
    }

    // ------------------------------------------------------------- scrittura

    /**
     * Applica lo spostamento a un modello fresco, letto dal file.
     *
     * Si riconoscono gli oggetti dalla loro POSIZIONE DI PARTENZA, che nel
     * modello appena letto e' la stessa di quando abbiamo caricato la
     * disposizione. L'elenco viene poi riordinato per coordinata, come lo
     * scrive il gioco.
     *
     * @return quanti oggetti sono stati spostati in questo modello
     */
    public int applica(eY inventarioFresco) {
        eV slots = inventarioFresco.d("Slots");
        if (slots == null) {
            throw new IllegalStateException("L'inventario non ha l'elenco Slots.");
        }
        if (slots.size() != pezzi.size()) {
            throw new IllegalStateException("Il numero di oggetti e' cambiato: "
                    + slots.size() + " nel file, " + pezzi.size()
                    + " nella disposizione. Non scrivo nulla.");
        }

        // per ogni voce del file: la sua posizione attuale e' quella di partenza
        List<eY> nodi = new ArrayList<eY>();
        List<int[]> nuove = new ArrayList<int[]>();
        for (int i = 0; i < slots.size(); i++) {
            eY v = slots.V(i);
            if (v == null) {
                continue;
            }
            eY idx = v.H("Index");
            int x = idx == null ? -1 : idx.J("X");
            int y = idx == null ? -1 : idx.J("Y");
            Pezzo p = dentro(x, y)
                    ? perCellaIniziale.get(Integer.valueOf(y * larghezza + x)) : null;
            if (p == null) {
                throw new IllegalStateException("Nel file c'e' un oggetto in una posizione "
                        + "che non conosco (" + x + "," + y + "): non scrivo nulla.");
            }
            nodi.add(v);
            nuove.add(new int[]{p.x, p.y});
        }

        // ordino per nuova posizione
        List<Integer> ordine = new ArrayList<Integer>();
        for (int i = 0; i < nuove.size(); i++) {
            ordine.add(Integer.valueOf(i));
        }
        final List<int[]> posizioni = nuove;
        Collections.sort(ordine, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                int[] pa = posizioni.get(a.intValue());
                int[] pb = posizioni.get(b.intValue());
                if (pa[1] != pb[1]) {
                    return pa[1] - pb[1];
                }
                return pa[0] - pb[0];
            }
        });

        slots.clear();
        int spostati = 0;
        for (int i = 0; i < ordine.size(); i++) {
            int k = ordine.get(i).intValue();
            eY v = nodi.get(k);
            eY idx = v.H("Index");
            int[] pos = nuove.get(k);
            if (idx.J("X") != pos[0] || idx.J("Y") != pos[1]) {
                spostati++;
            }
            idx.b("X", Integer.valueOf(pos[0]));
            idx.b("Y", Integer.valueOf(pos[1]));
            slots.add(v);
        }
        return spostati;
    }

    /** Descrive lo spostamento, per il log. */
    public String descrizione() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < pezzi.size(); i++) {
            Pezzo p = pezzi.get(i);
            if (p.spostato()) {
                if (b.length() > 0) {
                    b.append(", ");
                }
                b.append(p.id).append(' ').append(p.xIniziale).append(',').append(p.yIniziale)
                        .append(" -> ").append(p.x).append(',').append(p.y);
            }
        }
        return b.toString();
    }
}
