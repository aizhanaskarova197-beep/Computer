package kz.egov.docverify.storage;

import kz.egov.docverify.exception.DocVerifyException;
import kz.egov.docverify.model.DocumentType;
import kz.egov.docverify.model.EDocument;
import kz.egov.docverify.model.RegistryEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Issuer's registry: which documents were issued and which were revoked.
 * Stored as a CSV file (id;type;issuedAt;expiresAt;revoked).
 */
public class DocumentRegistry {

    private static final String CSV_HEADER = "id;type;issuedAt;expiresAt;revoked";

    private final Map<String, RegistryEntry> entries = new LinkedHashMap<>();

    public void register(EDocument document) {
        entries.put(document.id(), new RegistryEntry(
                document.id(), document.type(), document.issuedAt(), document.expiresAt(), false));
    }

    public boolean contains(String id) {
        return entries.containsKey(id);
    }

    public boolean isRevoked(String id) {
        RegistryEntry entry = entries.get(id);
        return entry != null && entry.revoked();
    }

    /** Marks the document as revoked. Returns false if the id is unknown. */
    public boolean revoke(String id) {
        RegistryEntry entry = entries.get(id);
        if (entry == null) {
            return false;
        }
        entries.put(id, entry.withRevoked());
        return true;
    }

    public List<RegistryEntry> list() {
        return List.copyOf(entries.values());
    }

    public int size() {
        return entries.size();
    }

    public void save(Path file) throws DocVerifyException {
        List<String> lines = new ArrayList<>();
        lines.add(CSV_HEADER);
        for (RegistryEntry e : entries.values()) {
            lines.add(String.join(";", e.id(), e.type().name(),
                    e.issuedAt().toString(), e.expiresAt().toString(), String.valueOf(e.revoked())));
        }
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DocVerifyException("Cannot save registry: " + e.getMessage(), e);
        }
    }

    /** Loads the registry; a missing file gives an empty registry. */
    public static DocumentRegistry load(Path file) throws DocVerifyException {
        DocumentRegistry registry = new DocumentRegistry();
        if (!Files.exists(file)) {
            return registry;
        }
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty() || line.equals(CSV_HEADER)) {
                    continue;
                }
                String[] p = line.split(";");
                if (p.length != 5) {
                    throw new DocVerifyException("Registry line " + (i + 1) + " is malformed");
                }
                registry.entries.put(p[0], new RegistryEntry(p[0], DocumentType.valueOf(p[1]),
                        Instant.parse(p[2]), Instant.parse(p[3]), Boolean.parseBoolean(p[4])));
            }
        } catch (IOException | RuntimeException e) {
            throw new DocVerifyException("Cannot load registry: " + e.getMessage(), e);
        }
        return registry;
    }
}
