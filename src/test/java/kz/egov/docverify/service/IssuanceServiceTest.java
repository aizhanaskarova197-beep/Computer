package kz.egov.docverify.service;

import kz.egov.docverify.TestKeys;
import kz.egov.docverify.crypto.RsaDocumentSigner;
import kz.egov.docverify.exception.DocVerifyException;
import kz.egov.docverify.model.DocumentType;
import kz.egov.docverify.model.SignedDocument;
import kz.egov.docverify.storage.DocumentCodec;
import kz.egov.docverify.storage.DocumentRegistry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IssuanceServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final RsaDocumentSigner signer =
            new RsaDocumentSigner(TestKeys.pairA().getPrivate(), TestKeys.pairA().getPublic());
    private final DocumentRegistry registry = new DocumentRegistry();
    private final IssuanceService service = new IssuanceService(signer, registry, clock);

    @Test
    void issuedDocumentIsSignedAndRegistered() throws Exception {
        SignedDocument signed = service.issue(DocumentType.PENSION_CERTIFICATE,
                "123456789012", "Aizhan", "text", 30);

        assertTrue(registry.contains(signed.document().id()));
        assertEquals(NOW, signed.document().issuedAt());
        assertEquals(NOW.plusSeconds(30L * 24 * 3600), signed.document().expiresAt());
        assertTrue(signer.verify(DocumentCodec.canonicalBytes(signed.document()), signed.signature()));
    }

    @Test
    void zeroValidityIsRejected() {
        assertThrows(DocVerifyException.class, () -> service.issue(DocumentType.PENSION_CERTIFICATE,
                "123456789012", "Aizhan", "text", 0));
    }

    @Test
    void invalidIinIsRejectedAndNothingIsRegistered() {
        assertThrows(DocVerifyException.class, () -> service.issue(DocumentType.PENSION_CERTIFICATE,
                "abc", "Aizhan", "text", 30));
        assertEquals(0, registry.size());
    }
}
