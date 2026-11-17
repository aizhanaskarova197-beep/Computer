package kz.egov.docverify.model;

import java.time.Instant;

/**
 * One line of the issuer's registry. Only the minimum is stored:
 * no personal data and no document text.
 */
public record RegistryEntry(String id,
                            DocumentType type,
                            Instant issuedAt,
                            Instant expiresAt,
                            boolean revoked) {

    public RegistryEntry withRevoked() {
        return new RegistryEntry(id, type, issuedAt, expiresAt, true);
    }
}
