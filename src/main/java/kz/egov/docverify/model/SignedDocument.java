package kz.egov.docverify.model;

import java.util.Base64;
import java.util.Objects;

/** A document together with the issuer's digital signature. */
public record SignedDocument(EDocument document, byte[] signature) {

    public SignedDocument {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(signature, "signature");
        signature = signature.clone();
    }

    @Override
    public byte[] signature() {
        return signature.clone();
    }

    public String signatureBase64() {
        return Base64.getEncoder().encodeToString(signature);
    }
}
