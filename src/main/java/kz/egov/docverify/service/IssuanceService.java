package kz.egov.docverify.service;

import kz.egov.docverify.crypto.DocumentSigner;
import kz.egov.docverify.exception.DocVerifyException;
import kz.egov.docverify.model.DocumentType;
import kz.egov.docverify.model.EDocument;
import kz.egov.docverify.model.SignedDocument;
import kz.egov.docverify.storage.DocumentCodec;
import kz.egov.docverify.storage.DocumentRegistry;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Issuer side: creates a document, signs it and records it in the registry. */
public class IssuanceService {

    private final DocumentSigner signer;
    private final DocumentRegistry registry;
    private final Clock clock;

    public IssuanceService(DocumentSigner signer, DocumentRegistry registry) {
        this(signer, registry, Clock.systemUTC());
    }

    public IssuanceService(DocumentSigner signer, DocumentRegistry registry, Clock clock) {
        this.signer = signer;
        this.registry = registry;
        this.clock = clock;
    }

    public SignedDocument issue(DocumentType type, String ownerIin, String ownerName,
                                String content, int validDays) throws DocVerifyException {
        if (validDays <= 0) {
            throw new DocVerifyException("Validity period must be a positive number of days");
        }
        Instant now = clock.instant();
        EDocument document;
        try {
            document = new EDocument(UUID.randomUUID().toString(), type, ownerIin, ownerName,
                    now, now.plus(validDays, ChronoUnit.DAYS), content);
        } catch (IllegalArgumentException e) {
            throw new DocVerifyException("Invalid document data: " + e.getMessage(), e);
        }
        byte[] signature = signer.sign(DocumentCodec.canonicalBytes(document));
        registry.register(document); // only after a successful signature
        return new SignedDocument(document, signature);
    }
}
