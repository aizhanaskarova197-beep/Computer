package kz.egov.docverify.service;

import kz.egov.docverify.TestKeys;
import kz.egov.docverify.crypto.RsaDocumentSigner;
import kz.egov.docverify.exception.DocVerifyException;
import kz.egov.docverify.model.DocumentType;
import kz.egov.docverify.model.EDocument;
import kz.egov.docverify.model.SignedDocument;
import kz.egov.docverify.model.VerificationStatus;
import kz.egov.docverify.security.RateLimiter;
import kz.egov.docverify.storage.DocumentRegistry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VerificationLockoutTest {

    private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final RsaDocumentSigner signer =
            new RsaDocumentSigner(TestKeys.pairA().getPrivate(), TestKeys.pairA().getPublic());
    private final DocumentRegistry registry = new DocumentRegistry();
    private final IssuanceService issuance = new IssuanceService(signer, registry, clock);
    private final RateLimiter limiter =
            new RateLimiter(3, Duration.ofMinutes(5), Duration.ofMinutes(60), clock);
    private final VerificationService verification =
            new VerificationService(signer, registry, clock, limiter);

    private SignedDocument issue() throws Exception {
        return issuance.issue(DocumentType.PENSION_CERTIFICATE, "123456789012", "Aizhan",
                "Contributions: 1250000 KZT", 30);
    }

    private SignedDocument forge(SignedDocument signed) {
        EDocument d = signed.document();
        EDocument changed = new EDocument(d.id(), d.type(), d.ownerIin(), d.ownerName(),
                d.issuedAt(), d.expiresAt(), "Contributions: 9999999 KZT");
        return new SignedDocument(changed, signed.signature());
    }

    @Test
    void verificationIsBlockedAfterThreeTamperedChecks() throws Exception {
        SignedDocument signed = issue();
        SignedDocument forged = forge(signed);
        for (int i = 0; i < 3; i++) {
            assertEquals(VerificationStatus.TAMPERED, verification.verify(forged).status());
        }
        assertThrows(DocVerifyException.class, () -> verification.verify(signed));
    }

    @Test
    void validResultClearsFailures() throws Exception {
        SignedDocument signed = issue();
        SignedDocument forged = forge(signed);
        verification.verify(forged);
        verification.verify(forged);
        assertEquals(VerificationStatus.VALID, verification.verify(signed).status());
        verification.verify(forged);
        verification.verify(forged);
        // only two failures since the last VALID, so no block yet
        assertEquals(VerificationStatus.VALID, verification.verify(signed).status());
    }

    @Test
    void revokedDocumentsAreNotCountedAsAttacks() throws Exception {
        SignedDocument signed = issue();
        registry.revoke(signed.document().id());
        for (int i = 0; i < 5; i++) {
            assertEquals(VerificationStatus.REVOKED, verification.verify(signed).status());
        }
    }
}
