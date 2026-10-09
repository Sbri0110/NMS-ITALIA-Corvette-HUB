package it.nmsitalia.corvettehub.ui;

import it.nmsitalia.corvettehub.config.AppConfig;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Properties;

/** Controlli geometrici del layout fluido indipendenti dai salvataggi. */
public final class RegressioniLayout {
    private static int prove;

    public static void main(String[] args) throws Exception {
        Constructor<AppConfig> costruttore=AppConfig.class.getDeclaredConstructor(File.class);
        costruttore.setAccessible(true);
        AppConfig config=costruttore.newInstance(new File("build/layout-fixture.conf"));
        Field props=AppConfig.class.getDeclaredField("props");props.setAccessible(true);
        ((Properties)props.get(config)).setProperty("UiScale",args.length==0?"1":args[0]);
        Scala.inizializza(config);
        SwingUtilities.invokeAndWait(() -> {
            Theme.installa();
            verificaBarraFluida();
        });
        System.out.println("PROVE COMPONENTI LAYOUT SUPERATE: "+prove+" scala "+Scala.fattore());
    }

    private static void verificaBarraFluida() {
        JPanel host=new JPanel(new BorderLayout());
        JPanel barra=new JPanel(new LayoutFluido(Scala.px(6),Scala.px(6)));
        barra.setBorder(Scala.bordo(4));
        JButton[] pulsanti=new JButton[5];
        for(int i=0;i<pulsanti.length;i++) {
            pulsanti[i]=new JButton("Azione "+(i+1));
            pulsanti[i].setPreferredSize(Scala.dim(156,30));
            barra.add(pulsanti[i]);
        }
        host.add(barra,BorderLayout.NORTH);
        host.add(new JPanel(),BorderLayout.CENTER);
        int altezzaAmpia=0,altezzaStretta=0;
        int[] larghezze={1440,800,960,1280,1440};
        for(int i=0;i<larghezze.length;i++) {
            host.setSize(larghezze[i],700);
            for(int passaggio=0;passaggio<4;passaggio++) {
                host.doLayout();barra.doLayout();
            }
            if(i==0)altezzaAmpia=barra.getHeight();
            if(i==1)altezzaStretta=barra.getHeight();
            Insets insets=barra.getInsets();
            int fondo=insets.top;
            for(JButton pulsante:pulsanti) {
                Rectangle b=pulsante.getBounds();
                verifica(b.width>0 && b.height>0,"azione con dimensioni positive");
                verifica(b.x>=insets.left && b.y>=insets.top
                        && b.x+b.width<=barra.getWidth()-insets.right
                        && b.y+b.height<=barra.getHeight()-insets.bottom,
                        "azione contenuta a "+larghezze[i]+" px: "+b+" barra "+barra.getSize());
                fondo=Math.max(fondo,b.y+b.height);
            }
            for(int a=0;a<pulsanti.length;a++)for(int b=a+1;b<pulsanti.length;b++)
                verifica(!pulsanti[a].getBounds().intersects(pulsanti[b].getBounds()),
                        "azioni non sovrapposte");
            verifica(barra.getHeight()>=fondo+insets.bottom,"altezza comprende tutte le righe");
            if(i==larghezze.length-1)verifica(barra.getHeight()==altezzaAmpia,
                    "ritorno a larghezza ampia elimina le righe non più necessarie");
        }
        verifica(altezzaStretta>altezzaAmpia,"riduzione larghezza genera una riga aggiuntiva");
        pulsanti[0].setVisible(false);
        for(int i=0;i<4;i++){host.doLayout();barra.doLayout();}
        verifica(pulsanti[1].getX()==barra.getInsets().left,
                "azione nascosta non riserva spazio nella riga");
    }
    private static void verifica(boolean risultato,String descrizione) {
        if(!risultato)throw new AssertionError(descrizione);
        prove++;
    }
}
