package it.nmsitalia.corvettehub.ui;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Le icone del gioco.
 *
 * Le 3211 icone ufficiali sono in lib/nms-icons.jar, nel percorso
 * nomanssave/icons/, nominate <TIPO>-<ID>.PNG dove TIPO e' uno fra
 * PRODUCT, TECHNOLOGY, SUBSTANCE, TECHBOX e UI. L'ID e' lo stesso che compare
 * nel salvataggio, senza il carattere ^.
 *
 * Esempi verificati:
 *   ^B_COK_A   -> PRODUCT-B_COK_A.PNG
 *   ^LAUNCHER  -> TECHNOLOGY-LAUNCHER.PNG
 *   ^OXYGEN    -> SUBSTANCE-OXYGEN.PNG
 *
 * Le voci procedurali portano un suffisso (^UP_LAUN4#67946) e spesso non hanno
 * icona: in quel caso si prova il nome senza suffisso e, se non basta, si
 * disegna un ripiego colorato per categoria, mai un'icona generica di sistema.
 */
public final class Icone {

    private static final String[] TIPI = {"PRODUCT", "TECHNOLOGY", "SUBSTANCE", "TECHBOX"};

    private static final Map<String, Icon> cache = new HashMap<String, Icon>();

    private static final Map<Integer, Icon> cacheLogo = new HashMap<Integer, Icon>();

    private Icone() {
    }

    /**
     * Il logo di NMS ITALIA Corvette HUB, ridimensionato al lato indicato.
     * Il file in res/logo.png ha lo sfondo trasparente.
     */
    public static Icon logo(int lato) {
        Integer chiave = Integer.valueOf(lato);
        Icon cached = cacheLogo.get(chiave);
        if (cached != null) {
            return cached;
        }
        Icon risultato = null;
        try {
            java.net.URL u = Icone.class.getResource("/res/logo.png");
            if (u != null) {
                java.awt.image.BufferedImage src = javax.imageio.ImageIO.read(u);
                if (src != null) {
                    java.awt.image.BufferedImage scalata =
                            new java.awt.image.BufferedImage(lato, lato,
                                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    java.awt.Graphics2D g = scalata.createGraphics();
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);
                    g.drawImage(src, 0, 0, lato, lato, null);
                    g.dispose();
                    risultato = new ImageIcon(scalata);
                }
            }
        } catch (Throwable t) {
            risultato = null;
        }
        cacheLogo.put(chiave, risultato);
        return risultato;
    }

    /**
     * Icona per un identificativo, provando prima il tipo indicato.
     *
     * @param id   identificativo dell'oggetto, con o senza ^
     * @param tipo tipo di inventario: Product, Technology, Substance (puo' essere null)
     */
    public static Icon per(String id, String tipo) {
        if (id == null || id.isEmpty()) {
            return ripiego("?", "Altro");
        }
        String chiave = (tipo == null ? "" : tipo) + "|" + id;
        Icon cached = cache.get(chiave);
        if (cached != null) {
            return cached;
        }

        String pulito = id.startsWith("^") ? id.substring(1) : id;
        // le voci procedurali hanno un suffisso #numero che l'icona non porta
        int hash = pulito.indexOf('#');
        String senzaSuffisso = hash > 0 ? pulito.substring(0, hash) : pulito;

        String primo = tipo == null ? null : tipo.toUpperCase(java.util.Locale.ROOT);
        Icon trovata = null;

        if (primo != null) {
            trovata = prova(primo, pulito);
            if (trovata == null) {
                trovata = prova(primo, senzaSuffisso);
            }
        }
        for (int i = 0; i < TIPI.length && trovata == null; i++) {
            if (TIPI[i].equals(primo)) {
                continue;
            }
            trovata = prova(TIPI[i], pulito);
            if (trovata == null) {
                trovata = prova(TIPI[i], senzaSuffisso);
            }
        }
        // I moduli di potenziamento (UP_LAUN4#67946, CV_SCI3#64180...) non hanno
        // un disegno proprio: il gioco, per un modulo installato, mostra il
        // DISEGNO DELLA TECNOLOGIA BASE a cui appartiene. Un modulo
        // dell'iperguida mostra i motori, uno degli scudi mostra lo scudo.
        // (Le icone TECHBOX NON vanno usate qui: sono quelle da negozio, prima
        // di installare la tecnologia.)
        if (trovata == null && "TECHNOLOGY".equals(primo)) {
            String base = tecnologiaBase(senzaSuffisso);
            if (base != null) {
                trovata = prova(primo, base);
                if (trovata == null) {
                    trovata = prova("TECHNOLOGY", base);
                }
            }
        }
        if (trovata == null) {
            trovata = ripiego(pulito, it.nmsitalia.corvettehub.domain.CatalogoParti.categoria(id));
        }
        cache.put(chiave, trovata);
        return trovata;
    }

    /**
     * La tecnologia base di un modulo di potenziamento.
     *
     * I nomi dei moduli portano la famiglia in chiaro: UP_HYP4#37225 e' un
     * potenziamento dell'iperguida, UP_S_SHL4 degli scudi, UP_SMINIX del
     * minigun. Da qui si risale al disegno da mostrare.
     *
     * L'ordine della tabella conta: "S_SHL" va provato prima di "SHL",
     * altrimenti catturerebbe anche le altre voci che contengono "SHL".
     *
     * @return l'identificativo della tecnologia base, o null se non la riconosco
     */
    private static String tecnologiaBase(String id) {
        String s = id.toUpperCase(java.util.Locale.ROOT);
        String[][] tabella = {
                // scudi
                {"S_SHL", "SHIPSHIELD"},
                {"SHIELD", "SHIPSHIELD"},
                {"SHL", "SHIPSHIELD"},
                // armi della nave
                {"SMINI", "SHIPMINIGUN"},
                {"MINIGUN", "SHIPMINIGUN"},
                {"SHIPGUN", "SHIPGUN1"},
                {"SHIPLASER", "SHIPGUN1"},
                {"ROCKET", "SHIPROCKETS"},
                {"CANNON", "SHIPGUN1"},
                // navigazione e propulsione
                {"HYP", "HYPERDRIVE"},
                // dell'impulso l'archivio ha solo UT_PULSEFUEL: UT_PULSESPEED non c'e'
                {"PULSE", "UT_PULSEFUEL"},
                {"DRIFT", "UT_PULSEFUEL"},
                {"LAUN", "LAUNCHER"},
                {"BOOST", "LAUNCHER"},
                // strumenti della nave
                {"SCI", "SCANBINOC1"},
                {"SCAN", "SCANBINOC1"},
                {"INV", "CARGOSHIELD"},
                {"FIT", "SHIP_TELEPORT"},
                {"TELEPORT", "SHIP_TELEPORT"},
                {"CARGOSHIELD", "CARGOSHIELD"},
                // tuta e ambiente
                {"JET", "JETPACK"},
                {"HEALTH", "HEALTH"},
                {"LIFE", "LIFESUPPORT"},
                {"COLD", "COLD"},
                {"HEAT", "HEAT"},
                {"RADIO", "RADIOACTIVE"},
                {"TOXIC", "TOXIC"},
                // del lander acquatico l'archivio ha solo la variante aliena
                {"WATER_LAND", "WATERLAND_ALIEN"},
                {"WATER", "UT_WATER"},
                // armi a piedi
                {"RAIL", "RAILGUN"},
                {"SHOTGUN", "SHOTGUN"},
                {"SMG", "SMG"},
                {"LASER", "LASER"},
                {"GRENADE", "GRENADE"},
        };
        for (int i = 0; i < tabella.length; i++) {
            if (s.contains(tabella[i][0])) {
                return tabella[i][1];
            }
        }
        return null;
    }

    public static Icon per(String id) {
        return per(id, null);
    }

    /**
     * Da dove viene l'icona di un oggetto: il nome del file usato, oppure
     * "ripiego" se non ne esiste una e il programma la disegna.
     *
     * Serve a capire perche' un oggetto mostra una targhetta invece dell'icona.
     */
    public static String fonte(String id, String tipo) {
        if (id == null || id.isEmpty()) {
            return "ripiego";
        }
        String pulito = id.startsWith("^") ? id.substring(1) : id;
        int hash = pulito.indexOf('#');
        String senza = hash > 0 ? pulito.substring(0, hash) : pulito;

        String primo = tipo == null ? null : tipo.toUpperCase(java.util.Locale.ROOT);
        if (primo != null) {
            if (risorsa(primo, pulito) != null) {
                return primo + "-" + pulito;
            }
            if (risorsa(primo, senza) != null) {
                return primo + "-" + senza;
            }
        }
        for (int i = 0; i < TIPI.length; i++) {
            if (TIPI[i].equals(primo)) {
                continue;
            }
            if (risorsa(TIPI[i], pulito) != null) {
                return TIPI[i] + "-" + pulito;
            }
            if (risorsa(TIPI[i], senza) != null) {
                return TIPI[i] + "-" + senza;
            }
        }
        if ("TECHNOLOGY".equals(primo)) {
            String base = tecnologiaBase(senza);
            // il file va verificato: non tutte le tecnologie base hanno un disegno
            if (base != null && risorsa("TECHNOLOGY", base) != null) {
                return "TECHNOLOGY-" + base + "  (tecnologia base)";
            }
        }
        return "ripiego";
    }

    /**
     * Icona ridimensionata al lato indicato.
     *
     * Le icone ufficiali sono 128x128: in una tabella con righe da 48 px
     * verrebbero tagliate, quindi vanno rimpicciolite. Il ridimensionamento
     * e' memorizzato, altrimenti si rifarebbe a ogni ridisegno.
     */
    public static Icon per(String id, String tipo, int lato) {
        String chiave = "s" + lato + "|" + (tipo == null ? "" : tipo) + "|" + id;
        Icon cached = cache.get(chiave);
        if (cached != null) {
            return cached;
        }
        Icon base = per(id, tipo);
        Icon scalata = base;
        if (base != null && (base.getIconWidth() != lato || base.getIconHeight() != lato)) {
            scalata = ridimensiona(base, lato);
        }
        cache.put(chiave, scalata);
        return scalata;
    }

    private static Icon ridimensiona(Icon base, int lato) {
        try {
            java.awt.image.BufferedImage src = new java.awt.image.BufferedImage(
                    base.getIconWidth(), base.getIconHeight(),
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D gs = src.createGraphics();
            base.paintIcon(null, gs, 0, 0);
            gs.dispose();

            java.awt.image.BufferedImage dst = new java.awt.image.BufferedImage(
                    lato, lato, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D gd = dst.createGraphics();
            gd.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            gd.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            gd.drawImage(src, 0, 0, lato, lato, null);
            gd.dispose();
            return new ImageIcon(dst);
        } catch (Throwable t) {
            return base;
        }
    }

    /** Vero se esiste un'icona ufficiale per questo identificativo. */
    public static boolean esiste(String id, String tipo) {
        if (id == null) {
            return false;
        }
        String pulito = id.startsWith("^") ? id.substring(1) : id;
        int hash = pulito.indexOf('#');
        String senza = hash > 0 ? pulito.substring(0, hash) : pulito;
        for (int i = 0; i < TIPI.length; i++) {
            if (risorsa(TIPI[i], pulito) != null || risorsa(TIPI[i], senza) != null) {
                return true;
            }
        }
        // i moduli di potenziamento usano il disegno della tecnologia base
        return tecnologiaBase(senza) != null;
    }

    private static Icon prova(String tipo, String id) {
        java.net.URL u = risorsa(tipo, id);
        if (u == null) {
            return null;
        }
        try {
            return new ImageIcon(u);
        } catch (Throwable t) {
            return null;
        }
    }

    private static java.net.URL risorsa(String tipo, String id) {
        return Icone.class.getResource("/nomanssave/icons/" + tipo + "-" + id + ".PNG");
    }

    /** Icona disegnata, usata solo quando quella ufficiale non esiste. */
    public static Icon ripiego(String id, String categoria) {
        BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Color c = coloreCategoria(categoria);
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 60));
        g.fillRoundRect(1, 1, 22, 22, 8, 8);
        g.setColor(c);
        g.drawRoundRect(1, 1, 21, 21, 8, 8);

        String iniziali = iniziali(id);
        g.setFont(new Font("Segoe UI", Font.BOLD, iniziali.length() > 1 ? 9 : 11));
        int w = g.getFontMetrics().stringWidth(iniziali);
        g.drawString(iniziali, 12 - w / 2, 12 + g.getFontMetrics().getAscent() / 2 - 1);
        g.dispose();
        return new ImageIcon(img);
    }

    private static String iniziali(String id) {
        if (id == null || id.isEmpty()) {
            return "?";
        }
        String s = id.startsWith("^") ? id.substring(1) : id;
        StringBuilder b = new StringBuilder();
        String[] pezzi = s.split("[_\\-]");
        for (int i = 0; i < pezzi.length && b.length() < 2; i++) {
            if (!pezzi[i].isEmpty()) {
                b.append(Character.toUpperCase(pezzi[i].charAt(0)));
            }
        }
        return b.length() == 0 ? "?" : b.toString();
    }

    /** Un colore per categoria, coerente in tutta l'interfaccia. */
    public static Color coloreCategoria(String categoria) {
        if (categoria == null) {
            return new Color(0x9A97A0);
        }
        if ("Cockpit".equals(categoria) || "Cabina".equals(categoria)) return new Color(0x6FB3E0);
        if ("Moduli abitativi".equals(categoria)) return new Color(0x5FBF7F);
        if ("Corridoi e passaggi".equals(categoria)) return new Color(0x8FBF5F);
        if ("Pareti".equals(categoria)) return new Color(0xB0A88F);
        if ("Porte".equals(categoria)) return new Color(0xC9A227);
        if ("Carrelli di atterraggio".equals(categoria)) return new Color(0xD08B4A);
        if ("Ali".equals(categoria)) return new Color(0x7FA8D8);
        if ("Propulsori".equals(categoria)) return new Color(0xE0834A);
        if ("Torrette".equals(categoria)) return new Color(0xE05A5A);
        if ("Scudi".equals(categoria)) return new Color(0x5FC7D8);
        if ("Reattori".equals(categoria)) return new Color(0xD8B45F);
        if ("Connettori".equals(categoria)) return new Color(0x9A97A0);
        if ("Corazze e profili".equals(categoria)) return new Color(0x8A8A96);
        if ("Decorazioni".equals(categoria)) return new Color(0xC78FD8);
        if ("Magazzini".equals(categoria)) return new Color(0xB8A46F);
        return new Color(0x9A97A0);
    }
}
