package kz.egov.docverify.model;

/**
 * Result of verification. The document is the data found in the file;
 * it must NOT be trusted unless the status is VALID.
 */
public record VerificationResult(VerificationStatus status, EDocument document) {

    public boolean isValid() {
        return status == VerificationStatus.VALID;
    }
}
