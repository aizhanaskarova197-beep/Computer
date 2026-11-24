package kz.egov.docverify;

import java.security.KeyPair;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import kz.egov.docverify.crypto.KeyManager;
import kz.egov.docverify.model.DocumentType;
import kz.egov.docverify.model.EDocument;

/** Тесттерге арналған ортақ құралдар. */
public final class TestSupport {
    public static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-24T10:00:00Z"), ZoneOffset.UTC);
    public static final LocalDate TODAY = LocalDate.of(2026, 11, 24);
    private static KeyPair keys;

    private TestSupport() {
    }

    /** RSA кілтін бір рет жасап, қайта қолданады (тесттер жылдам жұмыс істесін деп). */
    public static synchronized KeyPair keys() {
        if (keys == null) {
            keys = KeyManager.generate();
        }
        return keys;
    }

    public static EDocument doc(String id) {
        return new EDocument(id, DocumentType.INCOME, "Тест Азамат", "040101500123",
                "Айлық табысы: 450000 теңге", TODAY, TODAY.plusDays(30));
    }
}
