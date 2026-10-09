package it.nmsitalia.corvettehub.ui;

import it.nmsitalia.corvettehub.config.AppConfig;
import it.nmsitalia.corvettehub.detect.SaveLocator;
import it.nmsitalia.corvettehub.domain.*;
import nomanssave.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.lang.reflect.*;
import java.util.Properties;

/** Prove dei comandi reali: dati sintetici, dialoghi fermati prima di scrivere. */
public final class RegressioniPulsanti {
    private static int prove;
    private static SchermataCorvette hub;
    private static JPanel shell;
    private static File builds;
    private static SaveSlotInfo slot;
    private static SaveLocator.Rilevamento rilevamento;

    public static void main(String[] args) throws Exception {
        File local=new File("build/button-data/"+java.util.UUID.randomUUID());
        local.mkdirs();builds=new File(local,"Builds");builds.mkdirs();
        SaveLocator.preparaAmbiente(local);
        Constructor<AppConfig> cc=AppConfig.class.getDeclaredConstructor(File.class);cc.setAccessible(true);
        AppConfig config=cc.newInstance(new File(local,"CorvetteHUB.conf"));
        ((Properties)campo(config,"props")).setProperty("UiScale",args.length==0?"1":args[0]);
        Scala.inizializza(config);
        Method modello=AnteprimePremium.class.getDeclaredMethod("modello");modello.setAccessible(true);
        eY root=(eY)modello.invoke(null);
        // Riproduce il caso segnalato: l'unica Corvette è la nave selezionata.
        root.H("PlayerStateData").b("PrimaryShip",0);
        root.H("PlayerStateData").d("PersistentPlayerBases").remove(1);
        Method makeSlot=AnteprimePremium.class.getDeclaredMethod("slot",int.class,eY.class);makeSlot.setAccessible(true);
        slot=(SaveSlotInfo)makeSlot.invoke(null,0,root);slot.getModello();
        fq storage=new fq(){public File bS(){return local;}public fr bT(){return null;}
            public ft[] bU(){return new ft[0];}public int W(String s){return 0;}public void X(String s){}};
        Constructor<SaveLocator.Rilevamento> rc=SaveLocator.Rilevamento.class.getDeclaredConstructor(File.class,String.class,fq.class);
        rc.setAccessible(true);rilevamento=rc.newInstance(local,"Steam",storage);
        WrapperBuild.scrivi(new File(builds,"Aurora.json"),"Aurora","Test",root.H("PlayerStateData").d("PersistentPlayerBases").V(0).d("Objects"));
        SwingUtilities.invokeAndWait(() -> {
            Theme.installa();hub=new SchermataCorvette(builds,()->{});
            shell=new JPanel(new BorderLayout());shell.add(new TestataPremium(),BorderLayout.NORTH);shell.add(hub);
            hub.aggiorna(rilevamento,slot);
        });
        for(int i=0;i<100;i++) {
            final boolean[] pronto={false};SwingUtilities.invokeAndWait(()->pronto[0]=!hub.isOccupata());
            if(pronto[0])break;Thread.sleep(100);
        }
        SwingUtilities.invokeAndWait(() -> {
            ok(!hub.isOccupata(),"caricamento completato");
            JTabbedPane tabs=(JTabbedPane)campo(hub,"schede");
            for(int width:new int[]{1440,1000,800,1280,1440}) {
                for(int i=0;i<tabs.getTabCount();i++) {
                    tabs.setSelectedIndex(i);prepara(width);
                    Container pagina=(Container)tabs.getSelectedComponent();
                    for(JButton b:tutti(JButton.class,pagina)) if(b.isVisible() && b.isEnabled() && b.getText()!=null && !b.getText().isEmpty()) {
                        Point p=SwingUtilities.convertPoint(b,b.getWidth()/2,b.getHeight()/2,shell);
                        Component target=SwingUtilities.getDeepestComponentAt(shell,p.x,p.y);
                        // I pulsanti nel contenuto scorrevole si verificano dopo averli portati nel viewport.
                        if(target!=b) { b.scrollRectToVisible(new Rectangle(0,0,b.getWidth(),b.getHeight()));prepara(width);
                            p=SwingUtilities.convertPoint(b,b.getWidth()/2,b.getHeight()/2,shell);
                            target=SwingUtilities.getDeepestComponentAt(shell,p.x,p.y); }
                        ok(target==b,"bersaglio clic "+tabs.getTitleAt(i)+" / "+b.getText()+" a "+width+"px; ricevuto "+(target==null?"null":target.getClass().getSimpleName()));
                    }
                }
            }
            SchedaRinomina rinomina=(SchedaRinomina)tabs.getComponentAt(3);
            tabs.setSelectedComponent(rinomina);prepara(1280);
            ((JTextField)campo(rinomina,"nomeNuovo")).setText("Nome di prova");
            dialogo((JButton)campo(rinomina,"rinomina"),"Rinomina");
            SchedaImporta importa=(SchedaImporta)tabs.getComponentAt(1);
            tabs.setSelectedComponent(importa);importa.caricaFile(new File(builds,"Aurora.json"));prepara(1280);
            dialogo((JButton)campo(importa,"scegliFile"),"Scegli file");
            dialogo((JButton)campo(importa,"importa"),"Importa");
            SchedaElimina elimina=(SchedaElimina)tabs.getComponentAt(4);
            tabs.setSelectedComponent(elimina);prepara(1280);
            dialogo((JButton)campo(elimina,"elimina"),"Elimina");
            SchedaEsporta esporta=(SchedaEsporta)tabs.getComponentAt(2);
            tabs.setSelectedComponent(esporta);prepara(1280);
            int generazione=(Integer)campo(hub,"generazione");
            ((JTextField)campo(esporta,"nomeBuild")).setText("Export-pulsante");
            JButton export=(JButton)campo(esporta,"esporta");
            ok(export.isEnabled(),"Esporta abilitato");mouse(export);
            try { ok(WrapperBuild.leggi(new File(builds,"Export-pulsante.json")).getNumeroModuli()>0,"clic Esporta crea progetto leggibile"); }
            catch(Exception e){throw new RuntimeException(e);}
            ok(!hub.isOccupata() && generazione==(Integer)campo(hub,"generazione"),"Esporta conserva slot e non avvia ricaricamenti");
            SchedaLibreria libreria=(SchedaLibreria)tabs.getComponentAt(6);
            ok(((DefaultListModel<?>)campo(libreria,"modello")).size()==2,"Esporta aggiorna la libreria");
            tabs.setSelectedComponent(libreria);((JList<?>)campo(libreria,"lista")).setSelectedIndex(0);
            mouse((JButton)campo(libreria,"importaProgetto"));
            ok(tabs.getSelectedComponent()==importa && campo(importa,"build")!=null,"clic Libreria apre il progetto in Importa");
            hub.setOccupata(true);
            ok(!((JButton)campo(rinomina,"rinomina")).isEnabled(),"scrittura blocca Rinomina");
            hub.setOccupata(false);
            ok(((JButton)campo(rinomina,"rinomina")).isEnabled(),"fine scrittura riabilita Rinomina");
        });
        System.out.println("PROVE PULSANTI SUPERATE: "+prove+" scala "+Scala.fattore());System.exit(0);
    }
    private static void dialogo(JButton b,String nome) {
        ok(b.isEnabled(),nome+" abilitato");boolean aperto=false;
        try {mouse(b);}catch(HeadlessException e){aperto=true;}
        ok(aperto,"clic "+nome+" raggiunge il dialogo senza scritture");
    }
    private static void mouse(JButton b) {
        if(b.getWidth()==0)b.setSize(b.getPreferredSize());
        long now=System.currentTimeMillis();int x=b.getWidth()/2,y=b.getHeight()/2;
        b.dispatchEvent(new MouseEvent(b,MouseEvent.MOUSE_PRESSED,now,MouseEvent.BUTTON1_DOWN_MASK,x,y,1,false,MouseEvent.BUTTON1));
        b.dispatchEvent(new MouseEvent(b,MouseEvent.MOUSE_RELEASED,now+1,0,x,y,1,false,MouseEvent.BUTTON1));
    }
    private static void prepara(int w) {shell.setSize(w,900);for(int n=0;n<6;n++)layout(shell);}
    private static void layout(Container c) {c.doLayout();for(Component x:c.getComponents())if(x instanceof Container)layout((Container)x);}
    private static <T> java.util.List<T> tutti(Class<T> type,Container c) {
        java.util.List<T> out=new java.util.ArrayList<T>();for(Component x:c.getComponents()) {
            if(!x.isVisible())continue;
            if(type.isInstance(x))out.add(type.cast(x));if(x instanceof Container)out.addAll(tutti(type,(Container)x));}return out;
    }
    private static Object campo(Object o,String n) {try{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}catch(Exception e){throw new RuntimeException(e);}}
    private static void ok(boolean v,String m) {if(!v)throw new AssertionError(m);prove++;System.out.println("OK BUTTON "+m);}
}
