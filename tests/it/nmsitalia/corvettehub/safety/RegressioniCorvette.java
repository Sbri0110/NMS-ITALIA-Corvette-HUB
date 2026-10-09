package it.nmsitalia.corvettehub.safety;

import it.nmsitalia.corvettehub.domain.*;
import nomanssave.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import it.nmsitalia.corvettehub.detect.SaveLocator;

/** Gestione della Corvette selezionata, su modelli isolati dal salvataggio personale. */
public final class RegressioniCorvette {
    private static int prove;
    private static eY modello() throws Exception {
        Method m=it.nmsitalia.corvettehub.ui.AnteprimePremium.class.getDeclaredMethod("modello");
        m.setAccessible(true);return (eY)m.invoke(null);
    }
    private static void ok(boolean v,String testo) {if(!v)throw new AssertionError(testo);prove++;System.out.println("OK CORVETTE "+testo);}
    private static byte[] firma(eY m) {return fj.g(m);}
    private static void rifiuta(eY root,Runnable azione,String testo) {
        byte[] prima=firma(root);boolean rifiutata=false;
        try{azione.run();}catch(IllegalStateException e){rifiutata=true;}
        ok(rifiutata && Arrays.equals(prima,firma(root)),testo);
    }
    public static void main(String[] args) throws Exception {
        eY root=modello(),stato=root.H("PlayerStateData");stato.b("PrimaryShip",0);
        eY atteso=root.bE();atteso.H("PlayerStateData").d("PersistentPlayerBases").V(0).b("Name","Nome nuovo");
        atteso.H("PlayerStateData").d("ShipOwnership").V(0).b("Name","Nome nuovo");
        ModificheCorvette.rinomina(root,0,0,"Nome nuovo");
        ok(Arrays.equals(firma(root),firma(atteso)),"rinomina nave selezionata cambia esclusivamente i due nomi");
        eV objects=new eV();eY pezzo=new eY();pezzo.b("ObjectID","^B_COK_A");pezzo.b("Position",new eV(0,0,0));
        pezzo.b("Up",new eV(0,1,0));pezzo.b("At",new eV(0,0,1));objects.add(pezzo);
        byte[] inventario=firma(stato.d("ShipOwnership").V(0));
        byte[] deposito=firma(stato.H("CorvetteStorageInventory"));
        ModificheCorvette.importa(root,0,0,objects,false,"Ignorato");
        ok(stato.d("PersistentPlayerBases").V(0).d("Objects").size()==1 && stato.J("PrimaryShip")==0,
                "importazione nave selezionata applica il progetto e conserva la selezione");
        ok("Nome nuovo".equals(stato.d("PersistentPlayerBases").V(0).getValueAsString("Name")),"importazione senza nome conserva il nome");
        ok(Arrays.equals(inventario,firma(stato.d("ShipOwnership").V(0))) && Arrays.equals(deposito,firma(stato.H("CorvetteStorageInventory"))),
                "importazione conserva statistiche, inventari e deposito");
        objects.V(0).b("ObjectID","^B_HAB_A");
        ok("^B_COK_A".equals(stato.d("PersistentPlayerBases").V(0).d("Objects").V(0).getValueAsString("ObjectID")),"progetto importato indipendente dalla sorgente");
        ModificheCorvette.importa(root,0,0,objects,true,"Progetto nuovo");
        ok("Progetto nuovo".equals(stato.d("PersistentPlayerBases").V(0).getValueAsString("Name")),"importazione applica il nome richiesto");
        rifiuta(root,()->ModificheCorvette.rinomina(root,0,1,"Errato"),"riferimento cambiato blocca rinomina senza modificare dati");
        rifiuta(root,()->ModificheCorvette.importa(root,99,0,objects,true,"Errato"),"base inesistente blocca importazione senza modificare dati");
        for(int eliminata=0;eliminata<3;eliminata++) for(int primaria=0;primaria<3;primaria++) {
            eY m=modello(),s=m.H("PlayerStateData");eV basi=s.d("PersistentPlayerBases"),navi=s.d("ShipOwnership");
            eY terza=basi.V(0).bE();terza.b("UserData",2);terza.b("Name","Terza");basi.add(terza);
            for(int i=0;i<3;i++)navi.V(i).b("Name","Nave "+i);
            s.b("PrimaryShip",primaria);
            Corvette c=LettoreCorvette.leggi(m).corvette.get(eliminata);
            byte[] prima=firma(m);int moduliPrima=moduli(s.H("CorvetteStorageInventory"));
            int rimossi=c.getNumeroModuli();
            EliminazioneCorvette.Piano piano=EliminazioneCorvette.pianifica(m,c);
            ok(piano.possibile && Arrays.equals(prima,firma(m)),"piano senza mutazioni: nave "+eliminata+", selezionata "+primaria);
            String nomeSelezionata=navi.V(primaria==eliminata?piano.naveSostitutiva:primaria).getValueAsString("Name");
            EliminazioneCorvette.applica(m,c);
            ok(navi.size()==2 && basi.size()==2 && s.d("ShipUsesLegacyColours").size()==2,"eliminazione mantiene coerenti navi, basi e colori");
            ok(s.J("PrimaryShip")>=0 && s.J("PrimaryShip")<2 && nomeSelezionata.equals(navi.V(s.J("PrimaryShip")).getValueAsString("Name")),
                    "eliminazione conserva o sostituisce correttamente la nave selezionata");
            ok(moduli(s.H("CorvetteStorageInventory"))==moduliPrima+rimossi,"eliminazione restituisce tutti i moduli al deposito");
            for(Corvette rimasta:LettoreCorvette.leggi(m).corvette)
                ok(rimasta.getIndiceNave()>=0 && rimasta.getIndiceNave()<navi.size(),"Corvette rimasta collegata a una nave valida");
        }
        eY ultima=modello(),u=ultima.H("PlayerStateData");u.b("PrimaryShip",0);
        u.d("ShipOwnership").V(1).H("Resource").b("Filename","");u.d("ShipOwnership").V(2).H("Resource").b("Filename","");
        Corvette c=LettoreCorvette.leggi(ultima).corvette.get(0);
        ok(!EliminazioneCorvette.pianifica(ultima,c).possibile,"slot vuoti non sono navi sostitutive");
        rifiuta(ultima,()->EliminazioneCorvette.applica(ultima,c),"ultima nave protetta senza cambiare modello");
        eY pieno=modello(),sp=pieno.H("PlayerStateData");sp.b("PrimaryShip",0);
        sp.H("CorvetteStorageInventory").b("ValidSlotIndices",new eV());sp.H("CorvetteStorageInventory").b("Slots",new eV());
        Corvette cp=LettoreCorvette.leggi(pieno).corvette.get(0);
        rifiuta(pieno,()->EliminazioneCorvette.applica(pieno,cp),"deposito pieno blocca eliminazione senza perdere moduli");
        for(String fixture:args) provaDisco(new File(fixture));
        System.out.println("PROVE CORVETTE SUPERATE: "+prove);
    }
    private static SaveSlotInfo slot(SaveLocator.Rilevamento r) {
        for(ft s:r.storage.bU()) if(s!=null && !s.isEmpty())return new SaveSlotInfo(s);
        throw new AssertionError("Fixture senza slot");
    }
    private static void copia(File da,File a)throws Exception {
        if(da.isDirectory()){a.mkdirs();for(File f:da.listFiles())copia(f,new File(a,f.getName()));}
        else Files.copy(da.toPath(),a.toPath(),StandardCopyOption.COPY_ATTRIBUTES);
    }
    private static Map<String,String> hash(File dir)throws Exception {
        Map<String,String> result=new TreeMap<String,String>();
        try(java.util.stream.Stream<Path> paths=Files.walk(dir.toPath())) {
            paths.filter(Files::isRegularFile).forEach(p->result.put(dir.toPath().relativize(p).toString(),ScrittoreSalvataggio.sha256(p.toFile())));
        }return result;
    }
    private static void provaDisco(File sorgente)throws Exception {
        File test=new File("build/active-write-data/"+UUID.randomUUID());
        File dir=new File(test,"save"),programma=new File(test,"programma");
        copia(sorgente,dir);SaveLocator.preparaAmbiente(programma);
        SaveLocator.Rilevamento r=SaveLocator.apri(dir);SaveSlotInfo sl=slot(r);
        java.util.List<Corvette> corvette=LettoreCorvette.leggi(sl.getModello()).corvette;
        ok(!corvette.isEmpty(),"fixture contiene Corvette: "+sorgente.getName());
        Corvette c=corvette.get(0);final int base=c.getIndiceBase(),nave=c.getIndiceNave();
        // Prepara solo la COPIA, con quella Corvette selezionata e deposito disponibile.
        ScrittoreSalvataggio.Esito setup=ScrittoreSalvataggio.scrivi(r,sl,new ScrittoreSalvataggio.Modifica(){
            public String descrizione(){return "preparazione fixture Corvette selezionata";}
            public void applica(eY m){eY s=m.H("PlayerStateData");s.b("PrimaryShip",nave);s.H("CorvetteStorageInventory").b("Slots",new eV());}
        });
        ok(setup.riuscito,"preparazione copia: "+setup.messaggio);
        r=SaveLocator.apri(dir);sl=slot(r);Map<String,String> prima=hash(dir);
        ScrittoreSalvataggio.Esito rinomina=ScrittoreSalvataggio.scrivi(r,sl,new ScrittoreSalvataggio.Modifica(){
            public String descrizione(){return "prova rinomina Corvette selezionata";}
            public void applica(eY m){ModificheCorvette.rinomina(m,base,nave,"TEST-NAVE-SELEZIONATA");}
        });
        ok(rinomina.riuscito,"rinomina su disco: "+rinomina.messaggio);
        r=SaveLocator.apri(dir);sl=slot(r);
        for(fs f:sl.getFile())ok("TEST-NAVE-SELEZIONATA".equals(f.M().H("PlayerStateData").d("PersistentPlayerBases").V(base).getValueAsString("Name")),"rinomina riletta da ogni file dello slot");
        final eV progetto=sl.getModello().H("PlayerStateData").d("PersistentPlayerBases").V(base).d("Objects").bA();
        while(progetto.size()>1)progetto.remove(progetto.size()-1);
        ScrittoreSalvataggio.Esito importa=ScrittoreSalvataggio.scrivi(r,sl,new ScrittoreSalvataggio.Modifica(){
            public String descrizione(){return "prova importazione Corvette selezionata";}
            public void applica(eY m){ModificheCorvette.importa(m,base,nave,progetto,true,"TEST-PROGETTO");}
        });
        ok(importa.riuscito,"importazione su disco: "+importa.messaggio);
        r=SaveLocator.apri(dir);sl=slot(r);
        for(fs f:sl.getFile())ok(f.M().H("PlayerStateData").d("PersistentPlayerBases").V(base).d("Objects").size()==1,"importazione riletta da ogni file dello slot");
        final Corvette daEliminare=LettoreCorvette.leggi(sl.getModello()).corvette.get(0);
        ok(daEliminare.isAttiva(),"Corvette ancora selezionata prima dell'eliminazione");
        ScrittoreSalvataggio.Esito elimina=ScrittoreSalvataggio.scrivi(r,sl,new ScrittoreSalvataggio.Modifica(){
            public String descrizione(){return "prova eliminazione Corvette selezionata";}
            public void applica(eY m){EliminazioneCorvette.applica(m,daEliminare);}
        });
        ok(elimina.riuscito,"eliminazione su disco: "+elimina.messaggio);
        r=SaveLocator.apri(dir);sl=slot(r);
        for(fs f:sl.getFile()) {
            eY s=f.M().H("PlayerStateData");int primaria=s.J("PrimaryShip");
            ok(primaria>=0 && primaria<s.d("ShipOwnership").size() && !s.d("ShipOwnership").V(primaria).getValueAsString("Resource.Filename").isEmpty(),"nave sostitutiva valida riletta da ogni file dello slot");
        }
        ScrittoreSalvataggio.Esito restore=ScrittoreSalvataggio.ripristina(rinomina.backup,dir);
        ok(restore.riuscito && prima.equals(hash(dir)),"ripristino dopo tre operazioni identico byte per byte");
    }
    private static int moduli(eY deposito) {int totale=0;eV slots=deposito.d("Slots");
        for(int i=0;i<slots.size();i++){eY s=slots.V(i);String id=s.getValueAsString("Id");if(id!=null && id.startsWith("^B_"))totale+=s.J("Amount");}return totale;}
}
