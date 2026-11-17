package kz.egov.docverify.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

/**
 * Electronic certificate (immutable). All fields are validated on creation.
 * Timestamps are truncated to seconds so that the text form is stable.
 */
public record EDocument(String id,
                        DocumentType type,
                        String ownerIin,
                        String ownerName,
                        Instant issuedAt,
                        Instant expiresAt,
                        String content) {

    private static final Pattern IIN_PATTERN = Pattern.compile("\\d{12}");

    public EDocument {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Document id must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Document type must not be null");
        }
        if (ownerIin == null || !IIN_PATTERN.matcher(ownerIin).matches()) {
            throw new IllegalArgumentException("IIN must contain exactly 12 digits");
        }
        if (ownerName == null || ownerName.isBlank()) {
            throw new IllegalArgumentException("Owner name must not be blank");
        }
        if (issuedAt == null || expiresAt == null) {
            throw new IllegalArgumentException("Dates must not be null");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Document content must not be blank");
        }
        issuedAt = issuedAt.truncatedTo(ChronoUnit.SECONDS);
        expiresAt = expiresAt.truncatedTo(ChronoUnit.SECONDS);
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("Expiry date must be after the issue date");
        }
    }
}
