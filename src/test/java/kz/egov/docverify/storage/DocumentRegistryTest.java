package kz.egov.docverify.storage;

import kz.egov.docverify.model.DocumentType;
import kz.egov.docverify.model.EDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentRegistryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private EDocument document(String id) {
        return new EDocument(id, DocumentType.NO_CRIMINAL_RECORD, "123456789012", "Aizhan",
                NOW, NOW.plusSeconds(3600), "text");
    }

    @Test
    void registeredDocumentIsFound() {
        DocumentRegistry registry = new DocumentRegistry();
        registry.register(document("a"));
        assertTrue(registry.contains("a"));
        assertFalse(registry.contains("b"));
        assertEquals(1, registry.size());
    }

    @Test
    void revokeMarksDocumentAsRevoked() {
        DocumentRegistry registry = new DocumentRegistry();
        registry.register(document("a"));
        assertFalse(registry.isRevoked("a"));
        assertTrue(registry.revoke("a"));
        assertTrue(registry.isRevoked("a"));
    }

    @Test
    void unknownIdCannotBeRevoked() {
        assertFalse(new DocumentRegistry().revoke("missing"));
    }

    @Test
    void saveAndLoadKeepEntriesAndRevocations(@TempDir Path dir) throws Exception {
        DocumentRegistry registry = new DocumentRegistry();
        registry.register(document("a"));
        registry.register(document("b"));
        registry.revoke("b");
        Path file = dir.resolve("data").resolve("registry.csv");
        registry.save(file);

        DocumentRegistry loaded = DocumentRegistry.load(file);
        assertEquals(2, loaded.size());
        assertFalse(loaded.isRevoked("a"));
        assertTrue(loaded.isRevoked("b"));
    }

    @Test
    void missingFileGivesEmptyRegistry(@TempDir Path dir) throws Exception {
        assertEquals(0, DocumentRegistry.load(dir.resolve("none.csv")).size());
    }
}
