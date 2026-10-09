package it.nmsitalia.corvettehub.domain;

import nomanssave.eV;
import nomanssave.eY;

/** Modifiche sul modello fresco ricevuto dallo scrittore, anche per la nave selezionata. */
public final class ModificheCorvette {
    private ModificheCorvette() { }

    private static eY base(eY radice, int indiceBase, int indiceNave) {
        eY stato = radice == null ? null : radice.H("PlayerStateData");
        eV basi = stato == null ? null : stato.d("PersistentPlayerBases");
        eV navi = stato == null ? null : stato.d("ShipOwnership");
        eY base = basi == null || indiceBase < 0 || indiceBase >= basi.size() ? null : basi.V(indiceBase);
        if (base == null || !"PlayerShipBase".equals(base.getValueAsString("BaseType.PersistentBaseTypes"))
                || base.c("UserData", -1) != indiceNave || navi == null
                || indiceNave < 0 || indiceNave >= navi.size() || navi.V(indiceNave) == null) {
            throw new IllegalStateException("La Corvette è cambiata. Ricarica il salvataggio.");
        }
        return base;
    }

    public static void rinomina(eY radice, int indiceBase, int indiceNave, String nome) {
        eY base = base(radice, indiceBase, indiceNave);
        base.b("Name", nome);
        radice.H("PlayerStateData").d("ShipOwnership").V(indiceNave).b("Name", nome);
    }

    public static void importa(eY radice, int indiceBase, int indiceNave,
                               eV oggetti, boolean cambiaNome, String nome) {
        eY base = base(radice, indiceBase, indiceNave);
        eV copia = oggetti.bA();
        base.b("Objects", copia);
        if (cambiaNome) base.b("Name", nome);
        base.b("LastUpdateTimestamp", Integer.valueOf((int)(System.currentTimeMillis()/1000L)));
    }
}
