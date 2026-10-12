package it.nmsitalia.corvettehub.ui;

import it.nmsitalia.corvettehub.config.AppConfig;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Scatta il selettore file a una scala, senza aprire la finestra.
 *
 * Uso: AnteprimaSelettore [scala] [file|cartella|salva]
 */
public final class AnteprimaSelettore {

    public static void main(String[] args) throws Exception {
        final String scala = args.length > 0 ? args[0] : "1";
        final String modo = args.length > 1 ? args[1] : "file";

        File local = new File("build/preview-selector");
        local.mkdirs();
        final File prova = new File(local, "Builds");
        prova.mkdirs();
        Files.write(new File(prova, "Aurora.json").toPath(), "{}".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(prova, "Orizzonte.json").toPath(), "{}".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(prova, "Crimson.json").toPath(), "{}".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(prova, "leggimi.txt").toPath(), "x".getBytes(StandardCharsets.UTF_8));
        new File(prova, "Backup").mkdirs();
        new File(prova, "Esportazioni").mkdirs();

        Constructor<AppConfig> ctor = AppConfig.class.getDeclaredConstructor(File.class);
        ctor.setAccessible(true);
        AppConfig config = ctor.newInstance(new File(local, "CorvetteHUB.conf"));
        Files.write(config.getFile().toPath(), ("UiScale=" + scala).getBytes(StandardCharsets.ISO_8859_1));
        Method carica = AppConfig.class.getDeclaredMethod("carica");
        carica.setAccessible(true);
        Field props = AppConfig.class.getDeclaredField("props");
        props.setAccessible(true);
        ((java.util.Properties) props.get(config)).setProperty("UiScale", scala);
        Scala.inizializza(config);

        final BufferedImage[] img = new BufferedImage[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
                Theme.installa();
                JComponent p;
                JPanel finto = new JPanel();
                if ("cartella".equals(modo)) {
                    p = SelettoreFile.pannelloDiProva(finto, "Indica la cartella dei salvataggi",
                            prova, true, null, null);
                } else if ("salva".equals(modo)) {
                    p = SelettoreFile.pannelloDiProva(finto, "Salva l'elenco del deposito",
                            prova, false, null, "deposito-corvette.csv");
                } else {
                    p = SelettoreFile.pannelloDiProva(finto, "Scegli il progetto Corvette",
                            prova, false, "json", null);
                }
                p.setSize(Scala.dim(760, 520));
                Theme.rifinisci(p);
                p.doLayout();
                layout(p);
                BufferedImage out = new BufferedImage(p.getWidth(), p.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D g = out.createGraphics();
                g.setColor(Theme.ABISSO);
                g.fillRect(0, 0, out.getWidth(), out.getHeight());
                p.printAll(g);
                g.dispose();
                img[0] = out;
            }
        });

        File dir = new File("docs/img/premium");
        dir.mkdirs();
        String nome = "90-selettore-" + modo + (scala.equals("1") ? "" : "-" + scala.replace(".", ""));
        ImageIO.write(img[0], "png", new File(dir, nome + ".png"));
        System.out.println("Scritto " + nome + ".png " + img[0].getWidth() + "x" + img[0].getHeight());
        System.exit(0);
    }

    private static void layout(Container c) {
        c.doLayout();
        for (Component x : c.getComponents()) if (x instanceof Container) layout((Container) x);
    }
}
