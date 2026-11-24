package kz.egov.docverify.model;

/** Тексеру нәтижесі. */
public record VerificationResult(VerificationStatus status, String documentId, String message) {
}
