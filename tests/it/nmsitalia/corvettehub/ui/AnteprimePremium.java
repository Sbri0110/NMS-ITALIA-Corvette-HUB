package it.nmsitalia.corvettehub.ui;

import it.nmsitalia.corvettehub.config.AppConfig;
import it.nmsitalia.corvettehub.detect.SaveLocator;
import it.nmsitalia.corvettehub.domain.*;
import nomanssave.*;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

/** Rendering dei componenti reali su dati sintetici, senza desktop o salvataggi personali. */
public final class AnteprimePremium {
    private static File out = new File("docs/img/premium");
    private static JPanel shell;
    private static SchermataCorvette hub;
    private static SaveLocator.Rilevamento r;
    private static SaveSlotInfo slot;
    private static int prove;

    private static eY inventario(int w,int h,boolean tech) {
        eY inv=new eY();inv.b("Width",w);inv.b("Height",h);
        eV validi=new eV(),voci=new eV();
        String[] ids=tech?new String[]{"^HYPERDRIVE","^SHIPSHIELD","^LAUNCHER","^SHIPJUMP1","^PHOTONIX_CORE","^SHIPGUN1"}
                :new String[]{"^B_COK_A","^B_HAB_A","^B_ENG_A","^B_WNG_A","^B_LND_A","^B_PRT_A","^B_COR_A","^B_COK_B"};
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            eY index=new eY();index.b("X",x);index.b("Y",y);validi.add(index);
            if((x+y)%3==0 || y>1)continue;
            eY v=new eY(),t=new eY();t.b("InventoryType",tech?"Technology":"Product");
            v.b("Type",t);v.b("Id",ids[(x+y)%ids.length]);v.b("Amount",tech?1:8+x*3);
            v.b("MaxAmount",500);v.b("Index",index.bE());voci.add(v);
        }
        inv.b("ValidSlotIndices",validi);inv.b("Slots",voci);
        if(tech){
            eY cl=new eY();cl.b("InventoryClass","S");inv.b("Class",cl);
            eV stats=new eV();String[] stat={"^SHIP_DAMAGE","^SHIP_SHIELD","^SHIP_HYPERDRIVE","^SHIP_AGILE"};
            double[] values={995,995,285,85};
            for(int i=0;i<4;i++){eY s=new eY();s.b("BaseStatID",stat[i]);s.b("Value",values[i]);stats.add(s);}
            inv.b("BaseStatValues",stats);inv.b("SpecialSlots",new eV());
        }
        return inv;
    }
    private static eY modello() {
        eY root=new eY(),stato=new eY(),common=new eY();
        common.b("SaveName","Spedizione Aurora");common.b("TotalPlayTime",669511);
        root.b("CommonStateData",common);root.b("PlayerStateData",stato);stato.b("PrimaryShip",2);
        eV navi=new eV(),basi=new eV();
        for(int i=0;i<3;i++){
            eY nave=new eY();nave.b("Name",i==0?"Aurora":"Orizzonte");
            nave.b("Inventory",inventario(10,4,false));nave.b("Inventory_TechOnly",inventario(10,3,true));
            eY resource=new eY();resource.b("Filename","MODELS/COMMON/SPACECRAFT/BIGGS/BIGGS.SCENE.MBIN");nave.b("Resource",resource);navi.add(nave);
            if(i==2)continue;
            eY base=new eY(),tipo=new eY();tipo.b("PersistentBaseTypes","PlayerShipBase");base.b("BaseType",tipo);
            base.b("Name",i==0?"Aurora":"Orizzonte");base.b("UserData",i);
            base.b("Position",new eV(0,0,0));eV objects=new eV();
            for(int z=0;z<9;z++)for(int x=0;x<5;x++){
                if(Math.abs(x-2)>Math.min(2,z/2))continue;
                eY object=new eY();object.b("ObjectID",z==0?"^B_COK_A":"^B_HAB_A");
                object.b("Position",new eV(x*6,0,z*6));object.b("Up",new eV(0,1,0));object.b("At",new eV(0,0,1));objects.add(object);
            }
            base.b("Objects",objects);basi.add(base);
        }
        stato.b("ShipOwnership",navi);stato.b("PersistentPlayerBases",basi);
        stato.b("ShipUsesLegacyColours",new eV(false,false,false));stato.b("CorvetteStorageInventory",inventario(10,6,false));
        stato.b("KnownProducts",new eV("^B_COK_A","^B_HAB_A")); return root;
    }
    private static SaveSlotInfo slot(final int indice,final eY m) {
        fs f=new fs(){public String K(){return "save.hg";}public fn L(){return fn.lm;}public eY M(){return m;}
            public String b(eY n){throw new UnsupportedOperationException("Fixture sola lettura");}
            public long lastModified(){return 1791460800000L;}public String getName(){return "Spedizione Aurora";}public String getDescription(){return "Aurora";}};
        return new SaveSlotInfo(new ft(){public int getIndex(){return indice;}public boolean isEmpty(){return false;}public fn L(){return fn.lm;}public fs[] bX(){return new fs[]{f};}});
    }
    public static void main(String[] args)throws Exception {
        out.mkdirs(); File local=new File("build/preview-data");local.mkdirs();
        Constructor<AppConfig> configCtor=AppConfig.class.getDeclaredConstructor(File.class);configCtor.setAccessible(true);
        final AppConfig config=configCtor.newInstance(new File(local,"CorvetteHUB.conf"));
        if(args.length>0){Files.write(config.getFile().toPath(),("UiScale="+args[0]).getBytes(StandardCharsets.ISO_8859_1));
            Method carica=AppConfig.class.getDeclaredMethod("carica");
            Field props=AppConfig.class.getDeclaredField("props");props.setAccessible(true);
            ((java.util.Properties)props.get(config)).setProperty("UiScale",args[0]);
        }
        Scala.inizializza(config);
        slot=slot(0,modello());slot.getModello();
        fq storage=new fq(){public File bS(){return local;}public fr bT(){return null;}public ft[] bU(){return new ft[0];}
            public int W(String s){return 0;}public void X(String s){} };
        Constructor<SaveLocator.Rilevamento> ctor=SaveLocator.Rilevamento.class.getDeclaredConstructor(File.class,String.class,fq.class);ctor.setAccessible(true);
        r=ctor.newInstance(local,"Steam",storage);
        File builds=new File(local,"Builds");builds.mkdirs();
        WrapperBuild.scrivi(new File(builds,"Aurora.json"),"Aurora","NMS ITALIA",slot.getModello().H("PlayerStateData").d("PersistentPlayerBases").V(0).d("Objects"));
        SwingUtilities.invokeAndWait(() -> {
            Theme.installa();
            SchermataSalvataggi saves=new SchermataSalvataggi(config,(rl,s)->{},false);
            try {
                Field modelField=SchermataSalvataggi.class.getDeclaredField("modello");modelField.setAccessible(true);
                DefaultListModel<SaveSlotInfo> model=(DefaultListModel<SaveSlotInfo>)modelField.get(saves);
                for(int i=0;i<4;i++){SaveSlotInfo s=slot(i,modello());s.getModello();model.addElement(s);}
                Field listField=SchermataSalvataggi.class.getDeclaredField("lista");listField.setAccessible(true);((JList<?>)listField.get(saves)).setSelectedIndex(0);
                Field stateField=SchermataSalvataggi.class.getDeclaredField("stato");stateField.setAccessible(true);((JLabel)stateField.get(saves)).setText("Steam · 4 partite disponibili");
                shell=new JPanel(new BorderLayout());shell.add(new TestataPremium(),BorderLayout.NORTH);shell.add(saves,BorderLayout.CENTER);
                cattura("01-salvataggi",1440,920);cattura("01-salvataggi-compatto",1040,720);
            }catch(Exception e){throw new RuntimeException(e);}
            hub=new SchermataCorvette(builds,()->{});
            shell.remove(saves);shell.add(hub,BorderLayout.CENTER);
            hub.aggiorna(r,slot);
            verifica(hub.isOccupata(),"caricamento blocca azioni concorrenti");
            verifica(!trova(JTabbedPane.class,hub).isEnabled(),"navigazione bloccata durante caricamento");
        });
        Thread.sleep(1000);
        SwingUtilities.invokeAndWait(() -> {
            JTabbedPane tabs=trova(JTabbedPane.class,hub);
            verifica(!hub.isOccupata() && tabs.isEnabled(),"caricamento riabilita navigazione");
            SchedaCorvette editor=(SchedaCorvette)tabs.getComponentAt(0);
            JComboBox<?> combo=trova(JComboBox.class,editor);
            Object tech=campo(editor,"layoutTecnologie"), inventario=campo(editor,"layoutInventario");
            combo.setSelectedIndex(1);
            verifica(campo(editor,"layoutTecnologie")!=tech,"cambio Corvette rilegge tecnologie della nave scelta");
            verifica(campo(editor,"layoutInventario")!=inventario,"cambio Corvette rilegge inventario della nave scelta");
            combo.setSelectedIndex(0);
            JButton salva=(JButton)campo(hub,"salvaModifiche");
            verifica(!salva.isEnabled(),"nessuna modifica mantiene Salva disabilitato");
            hub.setOccupata(true);
            verifica(!combo.isEnabled() && !tabs.isEnabled(),"scrittura blocca selezione nave e scheda");
            hub.setOccupata(false);
            verifica(combo.isEnabled() && tabs.isEnabled() && !salva.isEnabled(),"fine scrittura ripristina gli stati corretti");
            eY attivo=modello();attivo.H("PlayerStateData").b("PrimaryShip",0);
            SchedaRinomina rinomina=(SchedaRinomina)tabs.getComponentAt(3);
            SchedaEsporta esporta=(SchedaEsporta)tabs.getComponentAt(2);
            SaveSlotInfo attiva=slot(0,attivo);LettoreCorvette.Esito lettura=LettoreCorvette.leggi(attivo);
            rinomina.aggiorna(r,attiva,lettura);esporta.aggiorna(attiva,lettura,"NMS ITALIA");
            verifica(!((JButton)campo(rinomina,"rinomina")).isEnabled(),"Corvette attiva blocca Rinomina");
            verifica(!((JButton)campo(esporta,"esporta")).isEnabled(),"Corvette attiva blocca Esporta");
            trova(JComboBox.class,rinomina).setSelectedIndex(1);
            trova(JComboBox.class,esporta).setSelectedIndex(1);
            verifica(((JButton)campo(rinomina,"rinomina")).isEnabled(),"Corvette inattiva abilita Rinomina");
            verifica(((JButton)campo(esporta,"esporta")).isEnabled(),"Corvette inattiva abilita Esporta");
            rinomina.aggiorna(r,slot,LettoreCorvette.leggi(slot.getModello()));
            esporta.aggiorna(slot,LettoreCorvette.leggi(slot.getModello()),"NMS ITALIA");
            for(int i=0;i<tabs.getTabCount();i++){
                tabs.setSelectedIndex(i);
                if(tabs.getSelectedComponent() instanceof SchedaImporta)((SchedaImporta)tabs.getSelectedComponent()).caricaFile(new File(builds,"Aurora.json"));
                if(tabs.getSelectedComponent() instanceof SchedaLibreria){JList<?> lista=trova(JList.class,(Container)tabs.getSelectedComponent());lista.setSelectedIndex(0);}
                cattura(String.format("%02d-%s",i+2,tabs.getTitleAt(i).toLowerCase()),1440,920);
            }
            tabs.setSelectedIndex(0);cattura("02-corvette-compatto",1040,720);
        });
        System.out.println("PROVE UI SUPERATE: " + prove);
        System.out.println("ANTEPRIME COMPLETATE scala " + Scala.fattore()); System.exit(0);
    }
    private static Object campo(Object o,String nome){try{Field f=o.getClass().getDeclaredField(nome);f.setAccessible(true);return f.get(o);}catch(Exception e){throw new RuntimeException(e);}}
    private static void verifica(boolean valore,String nome){if(!valore)throw new AssertionError(nome);prove++;System.out.println("OK UI " + nome);}
    private static <T> T trova(Class<T> tipo,Container c){for(Component x:c.getComponents()){
        if(tipo.isInstance(x))return tipo.cast(x);if(x instanceof Container){T y=trova(tipo,(Container)x);if(y!=null)return y;}}return null;}
    private static void layout(Container c){
        c.doLayout();
        if (c instanceof JList) {
            JList list = (JList)c;
            if (list.getModel().getSize() > 0) {
                Component renderer = list.getCellRenderer().getListCellRendererComponent(list,list.getModel().getElementAt(0),0,true,false);
                renderer.setSize(list.getWidth(),list.getFixedCellHeight());
                if (renderer instanceof Container) layout((Container)renderer);
            }
        }
        for(Component x:c.getComponents())if(x instanceof Container)layout((Container)x);
    }
    private static void cattura(String nome,int w,int h){
        try{
            shell.setSize(Scala.px(w),Scala.px(h));layout(shell);Theme.rifinisci(shell);layout(shell);
            BufferedImage image=new BufferedImage(shell.getWidth(),shell.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();shell.printAll(g);g.dispose();
            String suffisso=Scala.fattore()==1?"":"-"+(int)(Scala.fattore()*100);
            ImageIO.write(image,"png",new File(out,nome+suffisso+".png"));
            System.out.println("PREVIEW " + nome + suffisso);
        }catch(Exception e){throw new RuntimeException(e);}
    }
}
