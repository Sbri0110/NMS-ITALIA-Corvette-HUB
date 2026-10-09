package it.nmsitalia.corvettehub.safety;

import it.nmsitalia.corvettehub.detect.SaveLocator;
import it.nmsitalia.corvettehub.domain.*;
import it.nmsitalia.corvettehub.ui.*;
import nomanssave.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Regressioni isolate: mai scritture nei percorsi dei salvataggi dell'utente. */
public final class Regressioni {
    private static int prove;
    private static final File DIR = new File("build/test-data/" + UUID.randomUUID());
    private static void ok(boolean condizione, String descrizione) {
        prove++;
        if (!condizione) throw new AssertionError(descrizione);
        System.out.println("OK " + descrizione);
    }
    private static void rifiuta(Runnable azione, String descrizione) {
        boolean rifiutato = false;
        try { azione.run(); } catch (RuntimeException e) { rifiutato = true; }
        ok(rifiutato, descrizione);
    }
    private static eY json(String s) { return eY.E(s); }
    private static File file(String n, String s) throws IOException {
        File f = new File(DIR,n); FileSicuri.scrivi(f,s.getBytes(StandardCharsets.UTF_8)); return f;
    }
    private static void buildRifiutata(String s, String descrizione) throws IOException {
        boolean r = false;
        try { WrapperBuild.leggi(file("nonvalida.json",s)); } catch (IOException e) { r = true; }
        ok(r,descrizione);
    }
    private static final String MODULO = "{\"ObjectID\":\"^B_COK_A\",\"Position\":[0,0,0],\"Up\":[0,1,0],\"At\":[0,0,1]}";

    public static void main(String[] args) throws Exception {
        if (!DIR.mkdirs()) throw new IOException("Directory test non creata");
        SaveLocator.preparaAmbiente(new File(DIR,"programma"));
        atomico(); wrapper(); inventario(); lettura(); indice(); contenitore();
        if (args.length == 2) {
            smoke(new File(args[0]),"steam",false);
            smoke(new File(args[1]),"xbox",true);
        }
        System.out.println("PROVE SUPERATE: " + prove);
    }

    private static void atomico() throws Exception {
        File f = file("atomic.txt","prima");
        FileSicuri.scrivi(f,"dopo".getBytes(StandardCharsets.UTF_8));
        ok(new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8).equals("dopo"),"sostituzione atomica");
        File directory = new File(DIR,"destinazione-directory"); directory.mkdir();
        File originale = new File(directory,"originale.txt"); FileSicuri.scrivi(originale,new byte[]{1,2,3});
        boolean errore = false;
        try { FileSicuri.scrivi(directory,new byte[]{4}); } catch (IOException e) { errore = true; }
        ok(errore && Arrays.equals(Files.readAllBytes(originale.toPath()),new byte[]{1,2,3}),"fallimento sostituzione conserva originale");
        ok(DIR.listFiles((d,n) -> n.endsWith(".hub-tmp")).length == 0,"nessun temporaneo residuo");
        ok(!ScrittoreSalvataggio.sha256(f).isEmpty(),"SHA-256 leggibile");
        ok(ScrittoreSalvataggio.sha256(new File(DIR,"inesistente")).isEmpty(),"errore hash non produce impronta valida");
    }
    private static void wrapper() throws Exception {
        eV oggetti = eV.D("[" + MODULO + "]");
        File f = new File(DIR,"progetto.json");
        ok(WrapperBuild.scrivi(f,"Aurora","NMS ITALIA",oggetti)==1,"esportazione wrapper");
        WrapperBuild b=WrapperBuild.leggi(f);
        ok(b.getNome().equals("Aurora") && b.getAutore().equals("NMS ITALIA"),"round trip metadati");
        ok(b.getPartiRichieste().contains("^B_COK_A"),"round trip parti");
        ok(WrapperBuild.leggi(file("raw.json","["+MODULO+"]")).getNumeroModuli()==1,"lettura array grezzo");
        ok(WrapperBuild.leggi(file("maiuscole.json","{\"Objects\":["+MODULO+"]}")).getNumeroModuli()==1,"wrapper community Objects");
        ok(WrapperBuild.leggi(file("bom.json","\ufeff{\"objects\":["+MODULO+"]}")).getNumeroModuli()==1,"lettura UTF-8 BOM");
        buildRifiutata("{\"format\":\"altro\",\"objects\":["+MODULO+"]}","formato sconosciuto bloccato");
        buildRifiutata("{\"version\":99,\"objects\":["+MODULO+"]}","versione futura bloccata");
        buildRifiutata("{\"version\":0.5,\"objects\":["+MODULO+"]}","versione frazionaria bloccata");
        buildRifiutata("{\"version\":\"uno\",\"objects\":["+MODULO+"]}","versione testuale bloccata");
        buildRifiutata("{\"objects\":[]}","build vuota bloccata");
        buildRifiutata("{\"objects\":[null]}","modulo null bloccato");
        buildRifiutata("{\"objects\":[{\"Position\":[0,0,0]}]}","modulo senza id bloccato");
        buildRifiutata("{\"objects\":["+MODULO.replace("[0,0,0]","[0,0]")+"]}","vettore corto bloccato");
        buildRifiutata("{\"objects\":["+MODULO.replace("[0,0,0]","[1000000000,0,0]")+"]}","coordinate fuori limite bloccate");
        ok(WrapperBuild.nomeFileSicuro("CON").equals("_CON.json"),"nome Windows riservato");
        ok(WrapperBuild.nomeFileSicuro("LPT1.test").equals("_LPT1.test.json"),"nome riservato con estensione");
        ok(WrapperBuild.nomeFileSicuro("../Aurora:*? ").indexOf('/')<0,"nome privo di separatori");
        ok(WrapperBuild.nomeFileSicuro("...").equals("build.json"),"nome solo punti");
        ok(!WrapperBuild.nomeFileSicuro("a\n\tb").contains("\n"),"controlli nel nome rimossi");
    }
    private static eY inventarioBase() {
        return json("{\"Width\":2,\"Height\":2,\"ValidSlotIndices\":[{\"X\":0,\"Y\":0},{\"X\":1,\"Y\":0},{\"X\":0,\"Y\":1}],\"Slots\":["
                +"{\"Id\":\"^A\",\"Amount\":7,\"Type\":{\"InventoryType\":\"Product\"},\"Index\":{\"X\":0,\"Y\":0}},"
                +"{\"Id\":\"^B\",\"Amount\":12,\"Type\":{\"InventoryType\":\"Product\"},\"Index\":{\"X\":1,\"Y\":0}}]}");
    }
    private static void inventario() {
        eY inv=inventarioBase(); LayoutInventario l=LayoutInventario.leggi(inv,"Test");
        ok(!l.valida(1,1),"cella bloccata mantenuta");
        ok(!l.sposta(0,0,1,1),"spostamento su cella bloccata rifiutato");
        ok(l.sposta(0,0,1,0),"scambio oggetti");
        ok(l.a(1,0).id.equals("^A") && l.a(0,0).id.equals("^B"),"identita conservate nello scambio");
        ok(l.applica(inv)==2 && inv.d("Slots").size()==2,"applicazione conserva numero oggetti");
        ok(inv.d("Slots").V(0).J("Amount")==12 && inv.d("Slots").V(1).J("Amount")==7,"quantita conservate");
        l.annulla(); ok(!l.modificato() && l.a(0,0).id.equals("^A"),"annullamento ripristina disposizione");
        LayoutInventario stale=LayoutInventario.leggi(inventarioBase(),"Test"); stale.sposta(0,0,0,1);
        eY diverso=inventarioBase(); diverso.d("Slots").V(0).b("Id","^C");
        rifiuta(() -> stale.applica(diverso),"identita oggetto cambiata blocca scrittura");
        eY quantita=inventarioBase(); quantita.d("Slots").V(0).b("Amount",9);
        rifiuta(() -> stale.applica(quantita),"quantita cambiata blocca scrittura");
        rifiuta(() -> stale.applica(null),"inventario assente blocca scrittura");
        eY vuoto=inventarioBase(); vuoto.b("ValidSlotIndices",new eV());
        LayoutInventario bloccato=LayoutInventario.leggi(vuoto,"Test");
        ok(!bloccato.valida(0,0) && !bloccato.valida(1,1),"lista celle valide vuota non sblocca inventario");
        rifiuta(() -> LayoutInventario.leggi(json("{\"Width\":2147483647,\"Height\":10}"),"Test"),"dimensione anomala bloccata");
        eY duplicate=inventarioBase();duplicate.d("Slots").V(1).b("Index",duplicate.d("Slots").V(0).H("Index").bE());
        rifiuta(() -> LayoutInventario.leggi(duplicate,"Test"),"celle duplicate bloccate");
        eY celleCambiate=inventarioBase();celleCambiate.d("ValidSlotIndices").remove(2);
        rifiuta(() -> stale.applica(celleCambiate),"cella bloccata dopo il caricamento rifiutata");
    }
    private static void lettura() {
        LettoreCorvette.Esito e=LettoreCorvette.leggi(json("{\"PlayerStateData\":{\"PersistentPlayerBases\":[{\"BaseType\":{\"PersistentBaseTypes\":\"PlayerShipBase\"},\"Name\":\"Aurora\",\"Objects\":[]}]}}"));
        ok(e.ok() && e.naveAttiva==-1,"PrimaryShip assente non diventa nave zero");
        ok(e.corvette.get(0).getIndiceNave()==-1 && !e.corvette.get(0).isAttiva(),"UserData assente non diventa indice zero");
        ok(SaveLocator.apri(null,"Steam")==null,"percorso manuale null");
        ok(Comunita.DISCORD.equals("https://discord.gg/uvDTR3wRMg"),"invito Discord richiesto");
        ok(SaveSlotInfo.nomeModalita(fn.ln).equals("Sopravvivenza"),"modalita Sopravvivenza corretta");
        ok(SaveSlotInfo.nomeModalita(fn.lo).equals("Creativa"),"modalita Creativa corretta");
        ok(SaveSlotInfo.nomeModalita(fn.lr).equals("Spedizione"),"modalita Spedizione corretta");
    }
    private static void indice() {
        String nome="18C4F9F24DEB4BDF965C25D080DAF634";
        byte[] dati=new byte[96]; long totale=123456;
        ScrittoreSalvataggio.scriviLong(dati,0,totale); // esca: stessa dimensione fuori dal record
        UUID u=UUID.fromString("18C4F9F2-4DEB-4BDF-965C-25D080DAF634");
        byte[] guid=new byte[16]; java.nio.ByteBuffer.wrap(guid).putLong(u.getMostSignificantBits()).putLong(u.getLeastSignificantBits());
        int[] ordine={3,2,1,0,5,4,7,6,8,9,10,11,12,13,14,15};
        for(int i=0;i<16;i++)dati[16+i]=guid[ordine[i]];
        ScrittoreSalvataggio.scriviLong(dati,48,totale);
        ok(ScrittoreSalvataggio.offsetTotale(dati,nome,totale)==48,"indice identifica GUID anziche dimensione-esca");
        ok(ScrittoreSalvataggio.offsetTotale(dati,nome,totale+1)==-1,"dimensione indice incoerente bloccata");
        ok(ScrittoreSalvataggio.offsetTotale(dati,"Z"+nome.substring(1),totale)==-1,"GUID non esadecimale bloccato");
        ok(ScrittoreSalvataggio.offsetTotale(new byte[10],nome,totale)==-1,"indice troncato bloccato");
    }
    private static void contenitore() throws Exception {
        File base=new File(DIR,"contenitori");base.mkdir();
        File c=new File(base,"00112233445566778899AABBCCDDEEFF"); c.mkdir();
        byte[] json="{\"x\":1}".getBytes(StandardCharsets.UTF_8);
        byte[] raw=Arrays.copyOf(json,json.length+1);
        File meta=new File(c,"00112233445566778899AABBCCDDEE00");FileSicuri.scrivi(meta,new byte[360]);
        byte[] payload=ScrittoreSalvataggio.comprimi(raw);
        // Il payload fittizio deve essere >360 byte per distinguere i due file.
        byte[] grande=new byte[8000]; new Random(123).nextBytes(grande);
        FileSicuri.scrivi(new File(c,"ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ"),new byte[360]);
        FileSicuri.scrivi(new File(c,"00112233445566778899AABBCCDDEE01"),ScrittoreSalvataggio.comprimi(grande));
        ok(ScrittoreSalvataggio.trovaContenitore(base,grande)[0].equals(meta),"file non esadecimale ignorato nel rilevamento WGS");
        ok(ScrittoreSalvataggio.trovaContenitore(base,Arrays.copyOf(grande,100))==null,"prefisso payload non basta per scegliere contenitore");
        // Dati casuali in un campo JSON fanno superare la soglia anche dopo LZ4.
        StringBuilder random=new StringBuilder();Random casuale=new Random(987);
        for(int i=0;i<2000;i++)random.append((char)('a'+casuale.nextInt(26)));
        String modello="{\"x\":1,\"unknown\":\""+random+"\"}";
        byte[] differente=("{ \"x\" : 1, \"unknown\" : \""+random+"\" }\r\n\u0000").getBytes(StandardCharsets.UTF_8);
        FileSicuri.scrivi(new File(c,"00112233445566778899AABBCCDDEE01"),ScrittoreSalvataggio.comprimi(differente));
        ok(ScrittoreSalvataggio.trovaContenitore(base,fj.g(json(modello)))!=null,"WGS riconosce modello completo con diversa formattazione e coda");
        ok(ScrittoreSalvataggio.trovaContenitore(base,fj.g(json(modello.replace("\"x\":1","\"x\":2"))))==null,"WGS rifiuta un modello con dati differenti");
        ok(Arrays.equals(raw,ScrittoreSalvataggio.decomprimi(fileBytes("lz4.bin",payload))),"round trip compressione LZ4");
        ok(Arrays.equals(ScrittoreSalvataggio.codaOriginale(raw),new byte[]{0}),"coda JSON conservata");
    }
    private static File fileBytes(String n,byte[] b)throws IOException{File f=new File(DIR,n);FileSicuri.scrivi(f,b);return f;}
    private static void copiaAlbero(File da, File a) throws IOException {
        if (da.isDirectory()) { a.mkdirs(); for(File f:da.listFiles())copiaAlbero(f,new File(a,f.getName())); }
        else Files.copy(da.toPath(),a.toPath(),StandardCopyOption.COPY_ATTRIBUTES);
    }
    private static SaveSlotInfo primo(SaveLocator.Rilevamento r) {
        for(ft s:r.storage.bU())if(s!=null&&!s.isEmpty())return new SaveSlotInfo(s);
        throw new AssertionError("Nessuno slot");
    }
    private static Map<String,String> hash(File dir) {
        Map<String,String> result=new TreeMap<String,String>();hash(dir,dir,result);return result;
    }
    private static void hash(File root,File dir,Map<String,String> result){
        for(File f:dir.listFiles())if(f.isDirectory())hash(root,f,result);
        else result.put(root.toPath().relativize(f.toPath()).toString(),ScrittoreSalvataggio.sha256(f));
    }
    private static void smoke(File sorgente,String nome,boolean wgs)throws Exception{
        if(!sorgente.isDirectory())throw new AssertionError("Fixture mancante: "+sorgente);
        File copia=new File(DIR,nome); copiaAlbero(sorgente,copia);
        SaveLocator.Rilevamento r=SaveLocator.apri(copia);
        ok(r!=null,nome+" fixture leggibile"); SaveSlotInfo slot=primo(r); slot.getModello();
        SaveSlotInfo obsoleto=primo(SaveLocator.apri(copia));obsoleto.getModello();
        Map<String,String> prima=hash(copia);
        ScrittoreSalvataggio.Esito e=ScrittoreSalvataggio.scrivi(r,slot,new ScrittoreSalvataggio.Modifica(){
            public String descrizione(){return "regressione su copia isolata";}
            public void applica(eY m){m.H("PlayerStateData").d("PersistentPlayerBases").V(0).b("Name","AUDIT-123");}
        });
        ok(e.riuscito,nome+" scrittura verificata: "+e.messaggio);
        SaveSlotInfo letto=primo(SaveLocator.apri(copia));
        ok("AUDIT-123".equals(letto.getModello().H("PlayerStateData").d("PersistentPlayerBases").V(0).getValueAsString("Name")),nome+" rilettura modifica dal disco");
        ok("AUDIT-123".equals(slot.getModello().H("PlayerStateData").d("PersistentPlayerBases").V(0).getValueAsString("Name")),nome+" cache invalidata dopo scrittura");
        ok(new File(e.backup,"backup.txt").isFile(),nome+" backup completo marcato");
        Map<String,String> scritto=hash(copia);
        ScrittoreSalvataggio.Esito stale=ScrittoreSalvataggio.scrivi(r,obsoleto,new ScrittoreSalvataggio.Modifica(){
            public String descrizione(){return "tentativo con modello obsoleto";}
            public void applica(eY m){throw new AssertionError("La modifica obsoleta non deve essere applicata");}
        });
        ok(!stale.riuscito && stale.messaggio.contains("cambiato"),nome+" modello obsoleto rifiutato");
        ok(scritto.equals(hash(copia)),nome+" rifiuto modello obsoleto conserva ogni byte");
        if(wgs) {
            for(File c:copia.listFiles())if(c.isDirectory())ok(new File(e.backup,c.getName()).isDirectory(),"backup globale include "+c.getName());
            Map<String,String> stato=hash(copia);AtomicInteger chiamate=new AtomicInteger();
            ScrittoreSalvataggio.Esito fallito=ScrittoreSalvataggio.scrivi(SaveLocator.apri(copia),letto,new ScrittoreSalvataggio.Modifica(){
                public String descrizione(){return "errore iniettato sul secondo file";}
                public void applica(eY m){if(chiamate.incrementAndGet()==2)throw new IllegalStateException("errore test");m.H("PlayerStateData").d("PersistentPlayerBases").V(0).b("Name","AUDIT-456");}
            });
            ok(!fallito.riuscito && chiamate.get()==2,"WGS errore dopo prima scrittura");
            ok(stato.equals(hash(copia)),"WGS rollback ripristina ogni byte dopo errore sul secondo file");
            File contenitore=null;
            for(File c:copia.listFiles())if(c.isDirectory()){contenitore=c;break;}
            File spostato=new File(DIR,"contenitore-assente");
            Files.move(contenitore.toPath(),spostato.toPath());
            try {
                Map<String,String> incompleto=hash(copia);
                ScrittoreSalvataggio.Esito mancante=ScrittoreSalvataggio.ripristina(e.backup,copia);
                ok(!mancante.riuscito && mancante.messaggio.contains("contenitore"),"WGS contenitore mancante blocca ripristino");
                ok(incompleto.equals(hash(copia)),"WGS ripristino incompleto non modifica indice e file");
            } finally { Files.move(spostato.toPath(),contenitore.toPath()); }
        }
        ScrittoreSalvataggio.Esito restore=ScrittoreSalvataggio.ripristina(e.backup,copia);
        ok(restore.riuscito,nome+" ripristino verificato: "+restore.messaggio);
        ok(prima.equals(hash(copia)),nome+" ripristino byte per byte inclusi manifest e indice");
    }
}
