package it.nmsitalia.corvettehub.safety;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Sostituzione senza cancellare prima l'originale, con temporanei univoci. */
public final class FileSicuri {
    private FileSicuri() { }

    public static void scrivi(File destinazione, byte[] dati) throws IOException {
        Path target = destinazione.toPath().toAbsolutePath();
        Path temp = Files.createTempFile(target.getParent(), ".corvette-", ".hub-tmp");
        try {
            try (FileOutputStream out = new FileOutputStream(temp.toFile())) {
                out.write(dati);
                out.getFD().sync();
            }
            for (int tentativo = 0; ; tentativo++) {
                try {
                    try {
                        Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE,
                                StandardCopyOption.REPLACE_EXISTING);
                    } catch (AtomicMoveNotSupportedException e) {
                        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                    break;
                } catch (java.nio.file.FileSystemException e) {
                    // Lettori e antivirus su Windows possono trattenere il file.
                    if (tentativo >= 8 || !Files.isRegularFile(target)) throw e;
                    try { Thread.sleep(50L * (tentativo + 1)); }
                    catch (InterruptedException interrotto) {
                        Thread.currentThread().interrupt(); throw new IOException("Scrittura interrotta", interrotto);
                    }
                }
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
