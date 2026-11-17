package kz.egov.docverify.service;

import kz.egov.docverify.TestKeys;
import kz.egov.docverify.crypto.RsaDocumentSigner;
import kz.egov.docverify.model.DocumentType;
import kz.egov.docverify.model.EDocument;
import kz.egov.docverify.model.SignedDocument;
import kz.egov.docverify.model.VerificationStatus;
import kz.egov.docverify.storage.DocumentCodec;
import kz.egov.docverify.storage.DocumentRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VerificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

    private final RsaDocumentSigner signer =
            new RsaDocumentSigner(TestKeys.pairA().getPrivate(), TestKeys.pairA().getPublic());
    private final DocumentRegistry registry = new DocumentRegistry();
    private final IssuanceService issuance = new IssuanceService(signer, registry, clockAt(NOW));
    private final VerificationService verification = new VerificationService(signer, registry, clockAt(NOW));

    private static Clock clockAt(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    private SignedDocument issue() throws Exception {
        return issuance.issue(DocumentType.PENSION_CERTIFICATE, "123456789012", "Aizhan",
                "Contributions: 1250000 KZT", 30);
    }

    @Test
    void freshDocumentIsValid() throws Exception {
        assertEquals(VerificationStatus.VALID, verification.verify(issue()).status());
    }

    @Test
    void changedContentIsTampered() throws Exception {
        SignedDocument signed = issue();
        EDocument d = signed.document();
        EDocument changed = new EDocument(d.id(), d.type(), d.ownerIin(), d.ownerName(),
                d.issuedAt(), d.expiresAt(), "Contributions: 9999999 KZT");
        SignedDocument forged = new SignedDocument(changed, signed.signature());
        assertEquals(VerificationStatus.TAMPERED, verification.verify(forged).status());
    }

    @Test
    void extendedExpiryDateIsTampered() throws Exception {
        SignedDocument signed = issue();
        EDocument d = signed.document();
        EDocument changed = new EDocument(d.id(), d.type(), d.ownerIin(), d.ownerName(),
                d.issuedAt(), d.expiresAt().plusSeconds(365L * 24 * 3600), d.content());
        assertEquals(VerificationStatus.TAMPERED,
                verification.verify(new SignedDocument(changed, signed.signature())).status());
    }

    @Test
    void documentAfterExpiryIsExpired() throws Exception {
        SignedDocument signed = issue();
        VerificationService later = new VerificationService(signer, registry,
                clockAt(NOW.plusSeconds(31L * 24 * 3600)));
        assertEquals(VerificationStatus.EXPIRED, later.verify(signed).status());
    }

    @Test
    void revokedDocumentIsRevoked() throws Exception {
        SignedDocument signed = issue();
        registry.revoke(signed.document().id());
        assertEquals(VerificationStatus.REVOKED, verification.verify(signed).status());
    }

    @Test
    void documentMissingFromRegistryIsNotFound() throws Exception {
        SignedDocument signed = issue();
        VerificationService other = new VerificationService(signer, new DocumentRegistry(), clockAt(NOW));
        assertEquals(VerificationStatus.NOT_FOUND, other.verify(signed).status());
    }

    @Test
    void documentSignedWithForeignKeyIsTampered() throws Exception {
        RsaDocumentSigner forger =
                new RsaDocumentSigner(TestKeys.pairB().getPrivate(), TestKeys.pairB().getPublic());
        SignedDocument forged = new IssuanceService(forger, registry, clockAt(NOW)).issue(
                DocumentType.PENSION_CERTIFICATE, "123456789012", "Aizhan", "Fake", 30);
        assertEquals(VerificationStatus.TAMPERED, verification.verify(forged).status());
    }

    @Test
    void fileIsVerifiedAndEditedFileIsRejected(@TempDir Path dir) throws Exception {
        SignedDocument signed = issue();
        Path original = dir.resolve("original.edoc");
        Path edited = dir.resolve("edited.edoc");
        DocumentCodec.write(original, signed);
        Files.writeString(edited, Files.readString(original).replace("1250000", "9999999"));

        assertEquals(VerificationStatus.VALID, verification.verifyFile(original).status());
        assertEquals(VerificationStatus.TAMPERED, verification.verifyFile(edited).status());
    }
}
