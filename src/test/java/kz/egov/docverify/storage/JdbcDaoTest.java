package kz.egov.docverify.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import kz.egov.docverify.TestSupport;
import kz.egov.docverify.crypto.RsaSigner;
import kz.egov.docverify.exception.DocVerifyException;
import kz.egov.docverify.model.AuditEntry;
import kz.egov.docverify.model.DocumentType;
import kz.egov.docverify.model.RegistryEntry;
import kz.egov.docverify.model.RegistryStatus;
import kz.egov.docverify.service.IssuanceService;

/** SQLite (жадтағы дерекқор) + JDBC DAO + транзакция тесттері. */
class JdbcDaoTest {

    private static RegistryEntry entry(String id) {
        return new RegistryEntry(id, DocumentType.RESIDENCE, TestSupport.TODAY, TestSupport.TODAY.plusDays(20),
                RegistryStatus.ACTIVE);
    }

    @Test
    void registryDaoSavesAndReadsBack() {
        try (Database db = new Database("jdbc:sqlite::memory:")) {
            JdbcRegistryDao dao = new JdbcRegistryDao(db);
            dao.save(entry("A"));
            assertEquals(entry("A"), dao.findById("A").orElseThrow());
            dao.save(entry("A").withStatus(RegistryStatus.REVOKED));
            assertEquals(1, dao.findAll().size());
            assertTrue(dao.findById("A").orElseThrow().isRevoked());
            assertTrue(dao.deleteById("A"));
            assertFalse(dao.findById("A").isPresent());
        }
    }

    @Test
    void sqlInjectionInIdIsHarmless() {
        try (Database db = new Database("jdbc:sqlite::memory:")) {
            JdbcRegistryDao dao = new JdbcRegistryDao(db);
            dao.save(entry("A"));
            assertFalse(dao.findById("x' OR '1'='1").isPresent());
            assertEquals(1, dao.findAll().size());
        }
    }

    @Test
    void auditDaoStoresEntries() {
        try (Database db = new Database("jdbc:sqlite::memory:")) {
            JdbcAuditDao dao = new JdbcAuditDao(db);
            AuditEntry e = AuditEntry.of("A", "ISSUED", TestSupport.CLOCK.instant());
            dao.save(e);
            assertEquals(e, dao.findById(e.id()).orElseThrow());
        }
    }

    @Test
    void transactionCommitsBothWrites() {
        try (Database db = new Database("jdbc:sqlite::memory:")) {
            JdbcRegistryDao registry = new JdbcRegistryDao(db);
            JdbcAuditDao audit = new JdbcAuditDao(db);
            IssuanceService svc = new IssuanceService(new RsaSigner(), TestSupport.keys().getPrivate(),
                    registry, audit, db, TestSupport.CLOCK);
            svc.issue(TestSupport.doc("DOC-1"));
            assertEquals(1, registry.findAll().size());
            assertEquals(1, audit.findAll().size());
        }
    }

    @Test
    void failureInsideTransactionRollsBackRegistryWrite() {
        try (Database db = new Database("jdbc:sqlite::memory:")) {
            JdbcRegistryDao registry = new JdbcRegistryDao(db);
            Repository<AuditEntry, String> failingAudit = new InMemoryRepository<>() {
                @Override
                public void save(AuditEntry item) {
                    throw new DocVerifyException("журнал жазылмады (тест)");
                }
            };
            IssuanceService svc = new IssuanceService(new RsaSigner(), TestSupport.keys().getPrivate(),
                    registry, failingAudit, db, TestSupport.CLOCK);
            assertThrows(DocVerifyException.class, () -> svc.issue(TestSupport.doc("DOC-1")));
            assertTrue(registry.findAll().isEmpty(), "rollback жасалуы тиіс: тізілімде жазба қалмауы керек");
        }
    }
}
