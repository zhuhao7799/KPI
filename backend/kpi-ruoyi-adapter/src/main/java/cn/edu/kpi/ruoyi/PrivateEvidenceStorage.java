package cn.edu.kpi.ruoyi;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/** Local private-file adapter; no web route maps to its storage directory. */
public final class PrivateEvidenceStorage {
    private static final long MAX_BYTES = 20L * 1024 * 1024;
    private final Path root;

    public PrivateEvidenceStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
        try { Files.createDirectories(this.root); }
        catch (IOException error) { throw new IllegalStateException("cannot prepare private evidence directory", error); }
    }

    public StoredFile store(MultipartFile upload) {
        if (upload == null || upload.isEmpty() || upload.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("material must be between 1 byte and 20 MiB");
        }
        String key = UUID.randomUUID().toString();
        Path target = root.resolve(key);
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            long written;
            try (InputStream input = new DigestInputStream(upload.getInputStream(), digest);
                 var output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                written = input.transferTo(output);
            }
            if (written <= 0 || written > MAX_BYTES) throw new IOException("material size is outside limit");
            return new StoredFile(key, written, HexFormat.of().formatHex(digest.digest()));
        } catch (IOException | NoSuchAlgorithmException error) {
            discard(key);
            throw new IllegalStateException("cannot store private material", error);
        }
    }

    public byte[] read(String key) {
        Path path = path(key);
        try { return Files.readAllBytes(path); }
        catch (IOException error) { throw new IllegalStateException("cannot read private material", error); }
    }

    public void discard(String key) {
        try { Files.deleteIfExists(path(key)); }
        catch (IOException error) { throw new IllegalStateException("cannot remove incomplete material", error); }
    }

    private Path path(String key) {
        if (key == null || !key.matches("[0-9a-fA-F-]{36}")) throw new IllegalArgumentException("invalid private storage key");
        return root.resolve(key);
    }

    public record StoredFile(String storageKey, long byteSize, String sha256) { }
}
