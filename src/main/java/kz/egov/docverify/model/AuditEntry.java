package kz.egov.docverify.model;

import java.time.Instant;
import java.util.UUID;

/** Әрекеттер журналы: "ISSUED" немесе "REVOKED". */
public record AuditEntry(String id, String docId, String action, Instant at) implements Identifiable<String> {

    public static AuditEntry of(String docId, String action, Instant at) {
        return new AuditEntry(UUID.randomUUID().toString(), docId, action, at);
    }
}
