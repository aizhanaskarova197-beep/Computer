package kz.egov.docverify.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import kz.egov.docverify.TestSupport;
import kz.egov.docverify.crypto.RsaSigner;
import kz.egov.docverify.exception.DocVerifyException;
import kz.egov.docverify.model.AuditEntry;
import kz.egov.docverify.model.DocumentType;
import kz.egov.docverify.model.EDocument;
import kz.egov.docverify.model.RegistryEntry;
import kz.egov.docverify.model.SignedDocument;
import kz.egov.docverify.model.VerificationStatus;
import kz.egov.docverify.storage.InMemoryRepository;
import kz.egov.docverify.storage.NoTransaction;
import kz.egov.docverify.storage.Repository;

class ServicesTest {
    private final Repository<RegistryEntry, String> registry = new InMemoryRepository<>();
    private final Repository<AuditEntry, String> audit = new InMemoryRepository<>();
    private final RsaSigner signer = new RsaSigner();
    private final IssuanceService issuance = new IssuanceService(signer, TestSupport.keys().getPrivate(),
            registry, audit, new NoTransaction(), TestSupport.CLOCK);

    private VerificationService verifier(Clock clock) {
        return new VerificationService(signer, TestSupport.keys().getPublic(), registry, clock);
    }

    private VerificationStatus statusOf(SignedDocument sd) {
        return verifier(TestSupport.CLOCK).verify(sd).status();
    }

    @Test
    void freshDocumentIsValid() {
        assertEquals(VerificationStatus.VALID, statusOf(issuance.issue(TestSupport.doc("DOC-1"))));
    }

    @Test
    void changedContentIsTampered() {
        SignedDocument sd = issuance.issue(TestSupport.doc("DOC-1"));
        EDocument d = sd.document();
        EDocument forged = new EDocument(d.id(), d.type(), d.ownerName(), d.ownerIin(),
                "Айлық табысы: 950000 теңге", d.issuedOn(), d.validUntil());
        assertEquals(VerificationStatus.TAMPERED, statusOf(new SignedDocument(forged, sd.signature())));
    }

    @Test
    void extendedExpiryDateIsTampered() {
        SignedDocument sd = issuance.issue(TestSupport.doc("DOC-1"));
        EDocument d = sd.document();
        EDocument forged = new EDocument(d.id(), d.type(), d.ownerName(), d.ownerIin(), d.content(),
                d.issuedOn(), d.validUntil().plusYears(5));
        assertEquals(VerificationStatus.TAMPERED, statusOf(new SignedDocument(forged, sd.signature())));
    }

    @Test
    void documentSignedButNotInRegistryIsNotFound() {
        SignedDocument sd = issuance.issue(TestSupport.doc("DOC-1"));
        registry.deleteById("DOC-1");
        assertEquals(VerificationStatus.NOT_FOUND, statusOf(sd));
    }

    @Test
    void revokedDocumentIsRevoked() {
        SignedDocument sd = issuance.issue(TestSupport.doc("DOC-1"));
        issuance.revoke("DOC-1");
        assertEquals(VerificationStatus.REVOKED, statusOf(sd));
    }

    @Test
    void documentAfterValidUntilIsExpired() {
        SignedDocument sd = issuance.issue(TestSupport.doc("DOC-1"));
        Clock later = Clock.offset(TestSupport.CLOCK, Duration.ofDays(31));
        assertEquals(VerificationStatus.EXPIRED, verifier(later).verify(sd).status());
    }

    @Test
    void duplicateIdIsRejected() {
        issuance.issue(TestSupport.doc("DOC-1"));
        assertThrows(DocVerifyException.class, () -> issuance.issue(TestSupport.doc("DOC-1")));
    }

    @Test
    void revokeUnknownOrTwiceIsRejected() {
        assertThrows(DocVerifyException.class, () -> issuance.revoke("NOPE"));
        issuance.issue(TestSupport.doc("DOC-1"));
        issuance.revoke("DOC-1");
        assertThrows(DocVerifyException.class, () -> issuance.revoke("DOC-1"));
    }

    @Test
    void issuingAndRevokingWriteAuditLog() {
        issuance.issue(TestSupport.doc("DOC-1"));
        issuance.revoke("DOC-1");
        List<String> actions = audit.findAll().stream().map(AuditEntry::action).sorted().toList();
        assertEquals(List.of("ISSUED", "REVOKED"), actions);
    }

    @Test
    void registryDoesNotStorePersonalData() {
        issuance.issue(TestSupport.doc("DOC-1"));
        String stored = registry.findById("DOC-1").orElseThrow().toString();
        assertTrue(!stored.contains("040101500123") && !stored.contains("Тест Азамат"));
    }

    @Test
    void typeIsKeptInRegistry() {
        issuance.issue(TestSupport.doc("DOC-1"));
        assertEquals(DocumentType.INCOME, registry.findById("DOC-1").orElseThrow().type());
    }
}
