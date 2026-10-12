package it.nmsitalia.corvettehub.ui;

import it.nmsitalia.corvettehub.config.AppConfig;
import it.nmsitalia.corvettehub.detect.SaveLocator;
import it.nmsitalia.corvettehub.domain.*;
import nomanssave.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Scatta la scheda "Deposito Corvette" alla scala 1.0.
 *
 * La scheda dei depositi montati ha una pagina sua, con i contenitori a
 * sinistra e la griglia del contenitore scelto a destra. Questo scatto la
 * prende intera, con contenitori pieni, vuoti e senza inventario insieme.
 */
public final class AnteprimaDepositi {

    public static void main(String[] args) throws Exception {
        File local = new File("build/preview-data");
        local.mkdirs();
        Constructor<AppConfig> ctor = AppConfig.class.getDeclaredConstructor(File.class);
        ctor.setAccessible(true);
        final AppConfig config = ctor.newInstance(new File(local, "CorvetteHUB.conf"));
        Files.write(config.getFile().toPath(), "UiScale=1".getBytes(StandardCharsets.ISO_8859_1));
        Method carica = AppConfig.class.getDeclaredMethod("carica");
        carica.setAccessible(true);
        Field props = AppConfig.class.getDeclaredField("props");
        props.setAccessible(true);
        ((java.util.Properties) props.get(config)).setProperty("UiScale", "1");
        Scala.inizializza(config);

        final eY mod = modello();
        final SaveSlotInfo slot = slot(mod);
        final SaveLocator.Rilevamento r = rilevamento(local);

        final BufferedImage[] img = new BufferedImage[1];
        final SchermataCorvette[] hubRif = new SchermataCorvette[1];
        final JPanel[] shellRif = new JPanel[1];

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
                Theme.installa();
                SchermataCorvette hub = new SchermataCorvette(new File(local, "Builds"),
                        new SchermataCorvette.Ascoltatore() {
                            @Override public void cambiaSalvataggio() { }
                        });
                JPanel shell = new JPanel(new BorderLayout());
                shell.setSize(1180, 1400);
                shell.add(hub, BorderLayout.CENTER);
                hubRif[0] = hub;
                shellRif[0] = shell;
                hub.aggiorna(r, slot);
            }
        });

        // l'aggiornamento gira fuori dall'EDT: si aspetta il caricamento vero
        for (int attesa = 0; attesa < 100 && hubRif[0].isOccupata(); attesa++) {
            Thread.sleep(100);
        }

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
                JPanel shell = shellRif[0];
                SchermataCorvette hub = hubRif[0];
                try {
                    SchedaDepositoCorvette scheda = trovaScheda(hub);
                    if (scheda == null) {
                        throw new IllegalStateException("scheda Deposito Corvette non trovata");
                    }
                    // La scheda segue la Corvette scelta nella scheda Corvette:
                    // per l'anteprima si forza quella con i contenitori montati.
                    LettoreCorvette.Esito esito = LettoreCorvette.leggi(slot.getModello());
                    scheda.cambiaCorvette(0, esito);
                    scheda.setSize(1080, 660);
                    // La lista dei contenitori si disegna solo dopo un giro di
                    // validazione vero: senza, le righe escono come rettangoli
                    // vuoti perche' il renderer non e' mai stato dimensionato.
                    scheda.addNotify();
                    scheda.validate();
                    Theme.rifinisci((Component) scheda);
                    for (int i = 0; i < 6; i++) {
                        Theme.aggiornaLayout((Component) scheda);
                        layout((Container) scheda);
                        scheda.validate();
                    }
                    scheda.validate();

                    BufferedImage out = new BufferedImage(
                            Math.max(scheda.getWidth(), 400),
                            Math.max(scheda.getHeight(), 200), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = out.createGraphics();
                    g.setColor(Theme.ABISSO);
                    g.fillRect(0, 0, out.getWidth(), out.getHeight());
                    scheda.printAll(g);
                    g.dispose();
                    img[0] = out;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        File dir = new File("docs/img/premium");
        dir.mkdirs();
        ImageIO.write(img[0], "png", new File(dir, "02-depositi-sezione.png"));
        System.out.println("Scritto 02-depositi-sezione.png " + img[0].getWidth() + "x" + img[0].getHeight());
        System.exit(0);
    }

    /** La scheda "Deposito Corvette" dentro l'albero delle schede. */
    private static SchedaDepositoCorvette trovaScheda(Container c) {
        for (Component x : c.getComponents()) {
            if (x instanceof SchedaDepositoCorvette) {
                return (SchedaDepositoCorvette) x;
            }
            if (x instanceof Container) {
                SchedaDepositoCorvette y = trovaScheda((Container) x);
                if (y != null) return y;
            }
        }
        return null;
    }

    private static void layout(Container c) {
        c.doLayout();
        for (Component x : c.getComponents()) if (x instanceof Container) layout((Container) x);
    }

    private static eY inventario(int w, int h, boolean tech) {
        eY inv = new eY(); inv.b("Width", w); inv.b("Height", h);
        eV validi = new eV(), voci = new eV();
        String[] ids = tech ? new String[]{"^HYPERDRIVE", "^SHIPSHIELD", "^LAUNCHER"}
                : new String[]{"^FUEL1", "^CARBON", "^OXYGEN", "^SODIUM", "^FERRITE", "^COPPER"};
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            eY index = new eY(); index.b("X", x); index.b("Y", y); validi.add(index);
            if ((x + y) % 3 == 0 || y > 1) continue;
            eY v = new eY(), t = new eY();
            t.b("InventoryType", tech ? "Technology" : "Product");
            v.b("Type", t); v.b("Id", ids[(x + y) % ids.length]); v.b("Amount", 12 + x * 7);
            v.b("MaxAmount", 500); v.b("Index", index.bE()); voci.add(v);
        }
        inv.b("ValidSlotIndices", validi); inv.b("Slots", voci);
        return inv;
    }

    /** Tre contenitori: uno pieno, uno vuoto, uno senza inventario. */
    private static eY modello() {
        eY root = new eY(), stato = new eY(), common = new eY();
        common.b("SaveName", "Spedizione Aurora"); common.b("TotalPlayTime", 669511);
        root.b("CommonStateData", common); root.b("PlayerStateData", stato);
        stato.b("PrimaryShip", 0);

        eV navi = new eV(), basi = new eV();
        for (int i = 0; i < 2; i++) {
            eY nave = new eY(); nave.b("Name", i == 0 ? "Aurora" : "Orizzonte");
            nave.b("Inventory", inventario(10, 4, false));
            nave.b("Inventory_TechOnly", inventario(10, 3, true));
            eY resource = new eY();
            resource.b("Filename", "MODELS/COMMON/SPACECRAFT/BIGGS/BIGGS.SCENE.MBIN");
            nave.b("Resource", resource); navi.add(nave);
        }
        eY base = new eY(), tipo = new eY();
        tipo.b("PersistentBaseTypes", "PlayerShipBase"); base.b("BaseType", tipo);
        base.b("Name", "Aurora"); base.b("UserData", 0);
        base.b("Position", new eV(0, 0, 0));
        eV objects = new eV();
        String[] montati = {"^B_COK_A", "^CONTAINER0", "^CONTAINER1", "^CONTAINER2"};
        for (int k = 0; k < montati.length; k++) {
            eY o = new eY(); o.b("ObjectID", montati[k]);
            o.b("Position", new eV(k * 6, 0, 0)); o.b("Up", new eV(0, 1, 0)); o.b("At", new eV(0, 0, 1));
            objects.add(o);
        }
        base.b("Objects", objects); basi.add(base);

        stato.b("ShipOwnership", navi); stato.b("PersistentPlayerBases", basi);
        stato.b("CorvetteStorageInventory", inventario(10, 6, false));
        eY chest2 = inventario(10, 6, false);
        chest2.b("Slots", new eV());                            // vuoto: nessuna voce
        stato.b("Chest1Inventory", inventario(10, 6, false));   // pieno
        stato.b("Chest2Inventory", chest2);
        // Chest3Inventory volutamente assente (^CONTAINER2): il contenitore
        // c'e' ma l'inventario non l'ha ancora creato il gioco
        stato.b("ShipUsesLegacyColours", new eV(false, false, false));
        stato.b("KnownProducts", new eV("^B_COK_A"));
        return root;
    }

    private static SaveSlotInfo slot(final eY m) {
        fs f = new fs() {
            public String K() { return "save.hg"; }
            public fn L() { return fn.lm; }
            public eY M() { return m; }
            public String b(eY n) { throw new UnsupportedOperationException("Fixture sola lettura"); }
            public long lastModified() { return 1791460800000L; }
            public String getName() { return "Spedizione Aurora"; }
            public String getDescription() { return "Aurora"; }
        };
        return new SaveSlotInfo(new ft() {
            public int getIndex() { return 0; }
            public boolean isEmpty() { return false; }
            public fn L() { return fn.lm; }
            public fs[] bX() { return new fs[]{f}; }
        });
    }

    private static SaveLocator.Rilevamento rilevamento(final File local) throws Exception {
        fq storage = new fq() {
            public File bS() { return local; }
            public fr bT() { return null; }
            public ft[] bU() { return new ft[0]; }
            public int W(String s) { return 0; }
            public void X(String s) { }
        };
        Constructor<SaveLocator.Rilevamento> c = SaveLocator.Rilevamento.class
                .getDeclaredConstructor(File.class, String.class, fq.class);
        c.setAccessible(true);
        return c.newInstance(local, "Steam", storage);
    }
}
