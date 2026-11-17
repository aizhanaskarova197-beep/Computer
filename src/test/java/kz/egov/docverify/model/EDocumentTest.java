package kz.egov.docverify.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EDocumentTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private EDocument create(String iin, Instant expiresAt) {
        return new EDocument("id-1", DocumentType.PENSION_CERTIFICATE, iin, "Aizhan", NOW, expiresAt, "text");
    }

    @Test
    void validDocumentIsCreated() {
        EDocument document = create("123456789012", NOW.plusSeconds(3600));
        assertEquals("id-1", document.id());
        assertEquals(DocumentType.PENSION_CERTIFICATE, document.type());
        assertEquals("Aizhan", document.ownerName());
    }

    @Test
    void iinWithLettersIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> create("12345678901a", NOW.plusSeconds(60)));
    }

    @Test
    void iinWithWrongLengthIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> create("12345", NOW.plusSeconds(60)));
    }

    @Test
    void expiryNotAfterIssueIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> create("123456789012", NOW));
    }

    @Test
    void blankContentIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new EDocument("id-1",
                DocumentType.EDUCATION_CERTIFICATE, "123456789012", "Aizhan", NOW, NOW.plusSeconds(60), "  "));
    }

    @Test
    void timestampsAreTruncatedToSeconds() {
        Instant withNanos = NOW.plusNanos(123_456_789);
        EDocument document = new EDocument("id-1", DocumentType.PENSION_CERTIFICATE, "123456789012",
                "Aizhan", withNanos, withNanos.plusSeconds(60), "text");
        assertEquals(NOW, document.issuedAt());
    }
}
