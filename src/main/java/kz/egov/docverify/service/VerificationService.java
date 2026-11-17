package kz.egov.docverify.service;

import kz.egov.docverify.crypto.DocumentSigner;
import kz.egov.docverify.exception.DocVerifyException;
import kz.egov.docverify.model.EDocument;
import kz.egov.docverify.model.SignedDocument;
import kz.egov.docverify.model.VerificationResult;
import kz.egov.docverify.model.VerificationStatus;
import kz.egov.docverify.security.RateLimiter;
import kz.egov.docverify.storage.DocumentCodec;
import kz.egov.docverify.storage.DocumentRegistry;

import java.nio.file.Path;
import java.time.Clock;

/**
 * Verifier side. Checks in this order:
 * 1) signature (integrity), 2) registry, 3) revocation, 4) expiry.
 * <p>
 * If a {@link RateLimiter} is given, repeated suspicious checks (TAMPERED, NOT_FOUND
 * or a malformed file) lead to a temporary lockout. A VALID result clears the failures.
 * REVOKED and EXPIRED are normal document states and are not counted.
 */
public class VerificationService {

    /** One verifier in the console app; in a real system this would be an IP or an account. */
    public static final String LIMITER_KEY = "verifier";

    private final DocumentSigner signer;
    private final DocumentRegistry registry;
    private final Clock clock;
    private final RateLimiter limiter; // null = no lockout

    public VerificationService(DocumentSigner signer, DocumentRegistry registry) {
        this(signer, registry, Clock.systemUTC(), null);
    }

    public VerificationService(DocumentSigner signer, DocumentRegistry registry, Clock clock) {
        this(signer, registry, clock, null);
    }

    public VerificationService(DocumentSigner signer, DocumentRegistry registry,
                               Clock clock, RateLimiter limiter) {
        this.signer = signer;
        this.registry = registry;
        this.clock = clock;
        this.limiter = limiter;
    }

    public VerificationResult verify(SignedDocument signed) throws DocVerifyException {
        ensureNotBlocked();
        VerificationResult result = check(signed);
        if (limiter != null) {
            switch (result.status()) {
                case VALID -> limiter.reset(LIMITER_KEY);
                case TAMPERED, NOT_FOUND -> limiter.registerFailure(LIMITER_KEY);
                default -> { /* REVOKED and EXPIRED are not attacks */ }
            }
        }
        return result;
    }

    public VerificationResult verifyFile(Path file) throws DocVerifyException {
        ensureNotBlocked();
        SignedDocument signed;
        try {
            signed = DocumentCodec.read(file);
        } catch (DocVerifyException e) {
            if (limiter != null) {
                limiter.registerFailure(LIMITER_KEY); // garbage files count as failures too
            }
            throw e;
        }
        return verify(signed);
    }

    private void ensureNotBlocked() throws DocVerifyException {
        if (limiter != null && limiter.isBlocked(LIMITER_KEY)) {
            throw new DocVerifyException("Too many failed checks. Verification is blocked, try again in "
                    + limiter.secondsLeft(LIMITER_KEY) + " seconds");
        }
    }

    private VerificationResult check(SignedDocument signed) throws DocVerifyException {
        EDocument document = signed.document();
        boolean signatureOk = signer.verify(DocumentCodec.canonicalBytes(document), signed.signature());
        if (!signatureOk) {
            return new VerificationResult(VerificationStatus.TAMPERED, document);
        }
        if (!registry.contains(document.id())) {
            return new VerificationResult(VerificationStatus.NOT_FOUND, document);
        }
        if (registry.isRevoked(document.id())) {
            return new VerificationResult(VerificationStatus.REVOKED, document);
        }
        if (clock.instant().isAfter(document.expiresAt())) {
            return new VerificationResult(VerificationStatus.EXPIRED, document);
        }
        return new VerificationResult(VerificationStatus.VALID, document);
    }
}
