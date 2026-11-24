package kz.egov.docverify.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import kz.egov.docverify.TestSupport;
import kz.egov.docverify.model.DocumentType;
import kz.egov.docverify.model.RegistryEntry;
import kz.egov.docverify.model.RegistryStatus;
import kz.egov.docverify.model.VerificationStatus;

class AnalyticsTest {
    private static final LocalDate T = TestSupport.TODAY;

    private static RegistryEntry e(String id, DocumentType type, int issuedDaysAgo, int validDays, RegistryStatus st) {
        return new RegistryEntry(id, type, T.minusDays(issuedDaysAgo), T.minusDays(issuedDaysAgo).plusDays(validDays), st);
    }

    private final List<RegistryEntry> entries = List.of(
            e("A", DocumentType.INCOME, 0, 30, RegistryStatus.ACTIVE),
            e("B", DocumentType.INCOME, 0, 5, RegistryStatus.ACTIVE),
            e("C", DocumentType.STUDY, 40, 10, RegistryStatus.ACTIVE),
            e("D", DocumentType.STUDY, 1, 30, RegistryStatus.REVOKED));

    @Test
    void countsByEffectiveStatus() {
        Map<VerificationStatus, Long> m = Analytics.byStatus(entries, T);
        assertEquals(2L, (long) m.get(VerificationStatus.VALID));
        assertEquals(1L, (long) m.get(VerificationStatus.EXPIRED));
        assertEquals(1L, (long) m.get(VerificationStatus.REVOKED));
    }

    @Test
    void countsByType() {
        Map<DocumentType, Long> m = Analytics.byType(entries);
        assertEquals(2L, (long) m.get(DocumentType.INCOME));
        assertEquals(2L, (long) m.get(DocumentType.STUDY));
    }

    @Test
    void expiringSoonFindsOnlyActiveEntriesInWindow() {
        List<RegistryEntry> soon = Analytics.expiringSoon(entries, T, 7);
        assertEquals(1, soon.size());
        assertEquals("B", soon.get(0).id());
    }

    @Test
    void averageValidityIsComputed() {
        assertEquals(18.75, Analytics.averageValidityDays(entries), 0.0001);
        assertEquals(0.0, Analytics.averageValidityDays(List.of()), 0.0001);
    }

    @Test
    void genericCountByWorksForAnyCollection() {
        Map<Integer, Long> byLength = Analytics.countBy(List.of("а", "бб", "вв", "ггг"), String::length);
        assertEquals(2L, (long) byLength.get(2));
    }
}
