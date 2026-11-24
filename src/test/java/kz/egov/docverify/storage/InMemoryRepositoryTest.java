package kz.egov.docverify.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import kz.egov.docverify.TestSupport;
import kz.egov.docverify.model.AuditEntry;
import kz.egov.docverify.model.DocumentType;
import kz.egov.docverify.model.RegistryEntry;
import kz.egov.docverify.model.RegistryStatus;

class InMemoryRepositoryTest {

    private static RegistryEntry entry(String id) {
        return new RegistryEntry(id, DocumentType.STUDY, TestSupport.TODAY, TestSupport.TODAY.plusDays(10),
                RegistryStatus.ACTIVE);
    }

    @Test
    void saveAndFindById() {
        Repository<RegistryEntry, String> repo = new InMemoryRepository<>();
        repo.save(entry("A"));
        assertTrue(repo.findById("A").isPresent());
        assertFalse(repo.findById("B").isPresent());
    }

    @Test
    void saveWithSameIdReplacesEntry() {
        Repository<RegistryEntry, String> repo = new InMemoryRepository<>();
        repo.save(entry("A"));
        repo.save(entry("A").withStatus(RegistryStatus.REVOKED));
        assertEquals(1, repo.findAll().size());
        assertTrue(repo.findById("A").orElseThrow().isRevoked());
    }

    @Test
    void deleteRemovesEntry() {
        Repository<RegistryEntry, String> repo = new InMemoryRepository<>();
        repo.save(entry("A"));
        assertTrue(repo.deleteById("A"));
        assertFalse(repo.deleteById("A"));
    }

    @Test
    void sameGenericClassWorksForAnotherType() {
        Repository<AuditEntry, String> audit = new InMemoryRepository<>();
        AuditEntry e = AuditEntry.of("A", "ISSUED", TestSupport.CLOCK.instant());
        audit.save(e);
        assertEquals(e, audit.findById(e.id()).orElseThrow());
    }
}
