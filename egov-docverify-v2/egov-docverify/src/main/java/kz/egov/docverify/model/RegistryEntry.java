package kz.egov.docverify.model;

import java.time.LocalDate;

/** Тізілімдегі жазба. Жеке деректер (ЖСН, аты-жөні) сақталмайды. */
public record RegistryEntry(String id, DocumentType type, LocalDate issuedOn,
                            LocalDate validUntil, RegistryStatus status) implements Identifiable<String> {

    public RegistryEntry withStatus(RegistryStatus newStatus) {
        return new RegistryEntry(id, type, issuedOn, validUntil, newStatus);
    }

    public boolean isRevoked() {
        return status == RegistryStatus.REVOKED;
    }
}
