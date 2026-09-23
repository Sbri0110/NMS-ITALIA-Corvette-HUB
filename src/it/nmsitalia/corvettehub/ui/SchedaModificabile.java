package it.nmsitalia.corvettehub.ui;

/**
 * Una scheda in cui si possono spostare le cose e poi salvare.
 *
 * La usano la scheda Corvette (inventario e tecnologie) e la scheda Deposito:
 * cosi' i pulsanti "Salva le modifiche" e "Annulla" nella barra in alto
 * funzionano con qualunque scheda sia aperta, senza sapere quale sia.
 */
public interface SchedaModificabile {

    /** Avvisato quando cambia la presenza di spostamenti non salvati. */
    interface AscoltatoreModifiche {
        void modificheCambiate(boolean presenti);
    }

    void setAscoltatoreModifiche(AscoltatoreModifiche a);

    /** Vero se ci sono spostamenti non ancora salvati. */
    boolean haModifiche();

    /** Scrive sul salvataggio la disposizione in lavorazione. */
    void salvaModifiche();

    /** Butta via gli spostamenti non salvati. */
    void annullaModifiche();
}
