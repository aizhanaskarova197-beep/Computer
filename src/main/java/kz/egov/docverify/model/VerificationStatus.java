package kz.egov.docverify.model;

/** Possible outcomes of document verification. */
public enum VerificationStatus {
    VALID("Document is authentic and still active."),
    TAMPERED("Signature is invalid: the document was modified or forged."),
    NOT_FOUND("Signature is valid, but the document is not in the registry."),
    REVOKED("The document was revoked by the issuer."),
    EXPIRED("The validity period of the document has ended.");

    private final String description;

    VerificationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
