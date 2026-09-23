package it.nmsitalia.corvettehub.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Una Corvette letta da un salvataggio.
 *
 * In sola lettura: questa classe descrive, non modifica. Il nodo del
 * salvataggio e' tenuto come riferimento per le fasi successive (rinomina e
 * import), che vivranno altrove e con le dovute cautele.
 */
public final class Corvette {

    private final int indiceBase;
    private final int indiceNave;
    private final String nome;
    private final List<String> parti;
    private final List<double[]> posizioniModuli;
    private final boolean attiva;
    private final double[] posizione;

    private Object nodo;

    public Corvette(int indiceBase, int indiceNave, String nome,
                    List<String> parti, List<double[]> posizioniModuli,
                    boolean attiva, double[] posizione) {
        this.indiceBase = indiceBase;
        this.indiceNave = indiceNave;
        this.nome = nome;
        this.parti = Collections.unmodifiableList(new ArrayList<String>(parti));
        this.posizioniModuli = Collections.unmodifiableList(
                new ArrayList<double[]>(posizioniModuli));
        this.attiva = attiva;
        this.posizione = posizione;
    }

    /** Posizione della base nell'elenco PersistentPlayerBases. */
    public int getIndiceBase() {
        return indiceBase;
    }

    /** Indice della nave collegata, in ShipOwnership (campo UserData della base). */
    public int getIndiceNave() {
        return indiceNave;
    }

    public String getNome() {
        if (nome == null || nome.trim().isEmpty()) {
            return "(senza nome)";
        }
        return nome;
    }

    /** Elenco degli ObjectID, uno per modulo installato, nell'ordine del file. */
    public List<String> getParti() {
        return parti;
    }

    /**
     * Posizioni dei moduli, nello stesso ordine di getParti().
     * Servono a disegnare l'anteprima della Corvette.
     */
    public List<double[]> getPosizioniModuli() {
        return posizioniModuli;
    }

    /** Numero di moduli installati (con ripetizioni). */
    public int getNumeroModuli() {
        return parti.size();
    }

    /** Numero di moduli distinti. */
    public int getModuliDistinti() {
        return new java.util.LinkedHashSet<String>(parti).size();
    }

    /**
     * Vero se questa e' la Corvette che il giocatore sta usando.
     * La specifica vieta di esportarla o di importarci dentro.
     */
    public boolean isAttiva() {
        return attiva;
    }

    public double[] getPosizione() {
        return posizione;
    }

    public Object getNodo() {
        return nodo;
    }

    public void setNodo(Object nodo) {
        this.nodo = nodo;
    }

    @Override
    public String toString() {
        return "Corvette " + (indiceBase + 1) + " · " + getNome()
                + " · " + getNumeroModuli() + " moduli" + (attiva ? " · ATTIVA" : "");
    }
}
