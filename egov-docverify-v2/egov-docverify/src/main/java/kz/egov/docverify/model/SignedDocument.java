package kz.egov.docverify.model;

/** Анықтама + оның RSA қолтаңбасы (Base64). */
public record SignedDocument(EDocument document, String signature) {
}
