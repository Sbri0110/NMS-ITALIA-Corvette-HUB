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
    private static int proveLayout;
    private static int dimensioniVerificate;
    private static final java.util.List<String> erroriLayout=new java.util.ArrayList<String>();
    private static boolean immagini=true;

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
        immagini=args.length<2 || !"--senza-immagini".equals(args[1]);
        out.mkdirs(); File local=new File(immagini?"build/preview-data":"build/preview-check-data");local.mkdirs();
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
                sequenzaDimensioni("01-salvataggi");
                cattura("01-salvataggi-compatto",1040,720);
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
            verifica(((JButton)campo(rinomina,"rinomina")).isEnabled(),"Corvette attiva consente Rinomina a gioco chiuso");
            verifica(((JButton)campo(esporta,"esporta")).isEnabled(),"Corvette attiva consente Esporta in sola lettura");
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
                sequenzaDimensioni(String.format("%02d-%s",i+2,tabs.getTitleAt(i).toLowerCase()));
            }
            tabs.setSelectedIndex(0);cattura("02-corvette-compatto",1040,720);
            verificaModificheCompatte(editor);
            verificaBarraStatoCompleta();
        });
        System.out.println("PROVE UI SUPERATE: " + prove);
        System.out.println("PROVE LAYOUT SUPERATE: " + proveLayout + " su " + dimensioniVerificate + " ridimensionamenti fisici");
        if(!erroriLayout.isEmpty()) {
            for(String errore:erroriLayout)System.err.println("ERRORE " + errore);
            throw new AssertionError(erroriLayout.size()+" dimensioni non conformi; vedere errori layout sopra");
        }
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
    /** La scala cambia i controlli, NON lo spazio della finestra disponibile. */
    private static void sequenzaDimensioni(String nome) {
        int[][] dimensioni={{1440,900},{800,640},{960,720},{1280,820},{1440,900}};
        java.util.Map<GrigliaModuli,Dimension> griglieAmpie = new java.util.IdentityHashMap<GrigliaModuli,Dimension>();
        java.util.Map<LayoutInventario,String> inventariPrima = new java.util.IdentityHashMap<LayoutInventario,String>();
        if(hub!=null && nome.startsWith("02-")) {
            SchedaCorvette editor=trova(SchedaCorvette.class,hub);
            for(String campo:new String[]{"layoutTecnologie","layoutInventario"}) {
                LayoutInventario inv=(LayoutInventario)campo(editor,campo);
                if(inv!=null)inventariPrima.put(inv,firmaInventario(inv));
            }
        }
        for(int i=0;i<dimensioni.length;i++) {
            int w=dimensioni[i][0],h=dimensioni[i][1];
            prepara(w,h);
            dimensioniVerificate++;
            java.util.List<GrigliaModuli> griglie=tutti(GrigliaModuli.class,shell);
            if(i==0)for(GrigliaModuli griglia:griglie)griglieAmpie.put(griglia,griglia.getSize());
            try {
                verificaLayout(shell,nome+" "+w+"x"+h);
                if(i==dimensioni.length-1)for(GrigliaModuli griglia:griglie) {
                    Dimension iniziale=griglieAmpie.get(griglia);
                    verificaLayout(iniziale!=null && Math.abs(iniziale.width-griglia.getWidth())<=1
                            && Math.abs(iniziale.height-griglia.getHeight())<=1,
                            nome+" griglia ripristina le dimensioni tornando alla larghezza iniziale: "
                            +iniziale+" -> "+griglia.getSize());
                }
                System.out.println("OK RESIZE " + nome + " " + w + "x" + h + " px scala " + Scala.fattore());
            }catch(AssertionError errore) {
                erroriLayout.add(errore.getMessage());
                System.err.println("ERRORE RESIZE " + errore.getMessage());
            }
            if(i<dimensioni.length-1)cattura(nome+(i==0?"":"-"+w),w,h);
        }
        for(java.util.Map.Entry<LayoutInventario,String> inv:inventariPrima.entrySet())
            verificaLayout(inv.getValue().equals(firmaInventario(inv.getKey())),
                    nome+" ridimensionamento conserva coordinate, identità e quantità degli oggetti");
    }
    private static String firmaInventario(LayoutInventario inv) {
        StringBuilder firma=new StringBuilder();
        for(LayoutInventario.Pezzo p:inv.getPezzi())firma.append(p.id).append('|').append(p.tipo).append('|')
                .append(p.quantita).append('|').append(p.x).append(',').append(p.y).append(';');
        return firma.toString();
    }
    /** Il ramo con Salva/Annulla visibili deve restare utilizzabile anche stretto. */
    private static void verificaModificheCompatte(SchedaCorvette editor) {
        LayoutInventario inv=(LayoutInventario)campo(editor,"layoutInventario");
        String prima=firmaInventario(inv);
        LayoutInventario.Pezzo oggetto=inv.getPezzi().get(0);
        boolean spostato=false;
        for(int y=0;y<inv.getAltezza() && !spostato;y++)for(int x=0;x<inv.getLarghezza() && !spostato;x++)
            if(inv.valida(x,y) && inv.a(x,y)==null)spostato=inv.sposta(oggetto.x,oggetto.y,x,y);
        verificaLayout(spostato,"fixture modifica inventario per toolbar compatta");
        try {
            Method mostra=SchedaCorvette.class.getDeclaredMethod("mostra");
            mostra.setAccessible(true);mostra.invoke(editor);
            JButton salva=(JButton)campo(hub,"salvaModifiche"),annulla=(JButton)campo(hub,"annullaModifiche");
            int[][] dimensioni={{1440,900},{800,640},{960,720},{1280,820},{1440,900}};
            for(int[] dimensione:dimensioni) {
                prepara(dimensione[0],dimensione[1]);dimensioniVerificate++;
                try {
                    verificaLayout(salva.isEnabled() && salva.isVisible() && annulla.isEnabled() && annulla.isVisible(),
                            "azioni di modifica presenti a "+dimensione[0]+"px scala "+Scala.fattore());
                    verificaLayout(shell,"Corvette modificata "+dimensione[0]+"x"+dimensione[1]);
                    System.out.println("OK RESIZE MODIFICHE "+dimensione[0]+"x"+dimensione[1]+" px scala "+Scala.fattore());
                }catch(AssertionError errore){erroriLayout.add(errore.getMessage());System.err.println("ERRORE RESIZE MODIFICHE "+errore.getMessage());}
            }
        }catch(ReflectiveOperationException e){throw new RuntimeException(e);}
        finally {editor.annullaModifiche();}
        verificaLayout(prima.equals(firmaInventario(inv)) && !editor.haModifiche(),
                "annulla dopo resize ripristina coordinate, identità e quantità degli oggetti");
    }
    /** Lo stato gioco reale non può sovrapporsi al messaggio dello slot. */
    private static void verificaBarraStatoCompleta() {
        JLabel messaggio=(JLabel)campo(hub,"messaggio"),gioco=(JLabel)campo(hub,"statoGioco");
        String messaggioPrima=messaggio.getText(),giocoPrima=gioco.getText();
        String tooltipPrima=messaggio.getToolTipText();
        try {
            messaggio.setText("Slot 1: 2 Corvette. Nave in uso protetta.");
            gioco.setText("\u25CF Gioco chiuso \u00B7 pronto a lavorare");
            Container barra=messaggio.getParent();
            int[][] dimensioni={{800,640},{960,720}};
            for(int[] dimensione:dimensioni) {
                prepara(dimensione[0],dimensione[1]);
                String contesto="barra stato completa "+dimensione[0]+"px scala "+Scala.fattore();
                Rectangle sinistra=messaggio.getBounds();
                Rectangle destra=SwingUtilities.convertRectangle(gioco.getParent(),gioco.getBounds(),barra);
                Insets insets=barra.getInsets();
                Rectangle disponibile=new Rectangle(insets.left,insets.top,
                        barra.getWidth()-insets.left-insets.right,barra.getHeight()-insets.top-insets.bottom);
                try {
                    verificaLayout(sinistra.width>0 && sinistra.height>0 && destra.width>0 && destra.height>0,
                            contesto+" entrambi i messaggi hanno spazio");
                    verificaLayout(!sinistra.intersects(destra),
                            contesto+" testi sovrapposti: "+sinistra+" e "+destra);
                    verificaLayout(disponibile.contains(sinistra) && disponibile.contains(destra),
                            contesto+" testo fuori contenitore: "+sinistra+" e "+destra+" disponibile "+disponibile);
                    Rectangle barraFinestra=SwingUtilities.convertRectangle(barra.getParent(),barra.getBounds(),shell);
                    verificaLayout(new Rectangle(0,0,shell.getWidth(),shell.getHeight()).contains(barraFinestra),
                            contesto+" barra oltre la finestra");
                    System.out.println("OK BARRA STATO "+dimensione[0]+"x"+dimensione[1]+" px scala "+Scala.fattore());
                }catch(AssertionError errore) {
                    erroriLayout.add(errore.getMessage());System.err.println("ERRORE BARRA STATO "+errore.getMessage());
                }
            }
        }finally {
            messaggio.setText(messaggioPrima);gioco.setText(giocoPrima);messaggio.setToolTipText(tooltipPrima);
        }
    }
    private static void prepara(int w,int h) {
        shell.setSize(w,h);
        Theme.rifinisci(shell);
        // Più passaggi servono alle misure che dipendono dal nuovo viewport.
        for(int i=0;i<5;i++){Theme.aggiornaLayout(shell);layout(shell);}
    }
    private static <T> java.util.List<T> tutti(Class<T> tipo,Container radice) {
        java.util.List<T> risultato=new java.util.ArrayList<T>();
        if(!radice.isVisible())return risultato;
        for(Component c:radice.getComponents()) {
            if(!c.isVisible())continue;
            if(tipo.isInstance(c))risultato.add(tipo.cast(c));
            if(c instanceof Container)risultato.addAll(tutti(tipo,(Container)c));
        }
        return risultato;
    }
    private static void verificaLayout(boolean valore,String nome) {
        if(!valore)throw new AssertionError("LAYOUT " + nome);
        proveLayout++;
    }
    private static boolean haViewport(Component c) {
        for(Container p=c.getParent();p!=null;p=p.getParent())if(p instanceof JViewport)return true;
        return false;
    }
    private static void verificaLayout(Container radice,String contesto) {
        verificaLayout(radice.getWidth()>0 && radice.getHeight()>0,contesto+" dimensioni reali positive");
        for(JTabbedPane schede:tutti(JTabbedPane.class,radice)) {
            Component scheda=schede.getSelectedComponent();
            verificaLayout(scheda!=null && scheda.getHeight()>=Scala.px(120),
                    contesto+" contenuto della scheda troppo basso: "+(scheda==null?0:scheda.getHeight())+"px");
        }
        for(JScrollPane scroll:tutti(JScrollPane.class,radice)) {
            JViewport viewport=scroll.getViewport();Component view=viewport.getView();
            if(view==null || viewport.getWidth()==0)continue;
            // Il contenuto verticale può scorrere; nessuna scheda richiede una
            // barra orizzontale per raggiungere dati o controlli.
            verificaLayout(view.getWidth()<=viewport.getExtentSize().width+1,
                    contesto+" overflow orizzontale: "+view.getClass().getSimpleName()
                    +" "+view.getWidth()+">"+viewport.getExtentSize().width);
            verificaLayout(!scroll.getHorizontalScrollBar().isVisible(),
                    contesto+" barra orizzontale inattesa");
            if(view instanceof JList && ((JList<?>)view).getModel().getSize()>0) {
                JList<?> lista=(JList<?>)view;
                Rectangle riga=lista.getCellBounds(0,0);
                int altezzaRiga=lista.getFixedCellHeight()>0?lista.getFixedCellHeight()
                        :(riga==null?0:riga.height);
                verificaLayout(altezzaRiga>0 && viewport.getHeight()>=altezzaRiga,
                        contesto+" lista non mostra neppure una riga completa: viewport "
                        +viewport.getHeight()+"px, riga "+altezzaRiga+"px");
            }
            if(view instanceof Anteprima)
                verificaLayout(viewport.getHeight()>=Scala.px(48),
                        contesto+" anteprima non utilizzabile: viewport alto "+viewport.getHeight()+"px");
        }
        for(JButton button:tutti(JButton.class,radice)) {
            if(button.getText()==null || button.getText().isEmpty())continue;
            Container parent=button.getParent();Insets insets=parent.getInsets();
            Rectangle bounds=button.getBounds();
            verificaLayout(bounds.width>0 && bounds.height>0,contesto+" pulsante senza spazio: "+button.getText());
            verificaLayout(bounds.x>=insets.left-1 && bounds.y>=insets.top-1
                    && bounds.x+bounds.width<=parent.getWidth()-insets.right+1
                    && bounds.y+bounds.height<=parent.getHeight()-insets.bottom+1,
                    contesto+" pulsante tagliato nel contenitore: "+button.getText()+" "+bounds+" padre "+parent.getSize());
            if(!haViewport(button)) {
                Rectangle inRoot=SwingUtilities.convertRectangle(parent,bounds,radice);
                verificaLayout(inRoot.x>=0 && inRoot.y>=0
                        && inRoot.x+inRoot.width<=radice.getWidth()+1
                        && inRoot.y+inRoot.height<=radice.getHeight()+1,
                        contesto+" pulsante oltre la finestra: "+button.getText()+" "+inRoot
                        +" percorso "+percorso(button));
            }
        }
        for(GrigliaModuli griglia:tutti(GrigliaModuli.class,radice)) {
            verificaLayout(griglia.getWidth()<=griglia.getParent().getWidth()+1,
                    contesto+" griglia più larga del contenitore");
            for(Component cella:griglia.getComponents()) {
                verificaLayout(cella.getWidth()>0 && cella.getHeight()>0
                        && Math.abs(cella.getWidth()-cella.getHeight())<=1,
                        contesto+" cella deformata: "+cella.getSize());
                verificaLayout(cella.getWidth()<=Scala.px(48)+1,
                        contesto+" cella ingrandita oltre la misura compatta: "+cella.getSize());
                verificaLayout(cella.getX()+cella.getWidth()<=griglia.getWidth()+1
                        && cella.getY()+cella.getHeight()<=griglia.getHeight()+1,
                        contesto+" cella oltre la griglia");
                verificaLayout(cellaSotto(griglia,cella.getX()+cella.getWidth()/2,
                        cella.getY()+cella.getHeight()/2)==cella,
                        contesto+" trascinamento individua una cella diversa dopo il resize");
            }
        }
        for(Theme.Carta carta:tutti(Theme.Carta.class,radice)) {
            Rectangle bounds=carta.getBounds();Container parent=carta.getParent();
            verificaLayout(bounds.width>0 && bounds.height>0
                    && bounds.x>=0 && bounds.y>=0
                    && bounds.x+bounds.width<=parent.getWidth()+1
                    && bounds.y+bounds.height<=parent.getHeight()+1,
                    contesto+" carta statistiche tagliata: "+bounds+" padre "+parent.getSize());
            for(JLabel label:tutti(JLabel.class,carta))
                verificaLayout(label.getFont().getSize()<=Scala.px(22)+1,
                        contesto+" font statistiche fuori proporzione: "+label.getText()+" "+label.getFont().getSize());
        }
        for(JSplitPane split:tutti(JSplitPane.class,radice)) {
            Component prima=split.getLeftComponent(),seconda=split.getRightComponent();
            verificaLayout(prima!=null && seconda!=null && prima.getWidth()>0 && prima.getHeight()>0
                    && seconda.getWidth()>0 && seconda.getHeight()>0,
                    contesto+" un pannello dello split è scomparso");
        }
    }
    private static String percorso(Component componente) {
        StringBuilder percorso=new StringBuilder();
        for(Component c=componente;c!=null;c=c.getParent()) {
            if(percorso.length()>0)percorso.append(" <- ");
            percorso.append(c.getClass().getSimpleName()).append(c.getBounds());
        }
        return percorso.toString();
    }
    private static Object cellaSotto(GrigliaModuli griglia,int x,int y) {
        try {
            Method hit=GrigliaModuli.class.getDeclaredMethod("cellaA",int.class,int.class);
            hit.setAccessible(true);return hit.invoke(griglia,x,y);
        }catch(Exception e){throw new RuntimeException(e);}
    }
    private static void cattura(String nome,int w,int h){
        try{
            prepara(w,h);
            if(!immagini)return;
            BufferedImage image=new BufferedImage(shell.getWidth(),shell.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();shell.printAll(g);g.dispose();
            String suffisso=Scala.fattore()==1?"":"-"+(int)(Scala.fattore()*100);
            ImageIO.write(image,"png",new File(out,nome+suffisso+".png"));
            System.out.println("PREVIEW " + nome + suffisso);
        }catch(Exception e){throw new RuntimeException(e);}
    }
}
