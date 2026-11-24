package kz.egov.docverify.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import kz.egov.docverify.model.DocumentType;
import kz.egov.docverify.model.RegistryEntry;
import kz.egov.docverify.model.VerificationStatus;

/** Stream API арқылы статистика. */
public final class Analytics {
    private Analytics() {
    }

    /** Generic әдіс: кез келген жинақты кез келген белгі бойынша санайды (groupingBy). */
    public static <T, K> Map<K, Long> countBy(Collection<T> items, Function<? super T, ? extends K> classifier) {
        return items.stream().collect(Collectors.groupingBy(classifier, Collectors.counting()));
    }

    /** Жазбаның нақты күйі: кері қайтарылған, мерзімі өткен немесе жарамды. */
    public static VerificationStatus effectiveStatus(RegistryEntry e, LocalDate today) {
        if (e.isRevoked()) {
            return VerificationStatus.REVOKED;
        }
        return today.isAfter(e.validUntil()) ? VerificationStatus.EXPIRED : VerificationStatus.VALID;
    }

    public static Map<VerificationStatus, Long> byStatus(List<RegistryEntry> entries, LocalDate today) {
        return countBy(entries, e -> effectiveStatus(e, today));
    }

    public static Map<DocumentType, Long> byType(List<RegistryEntry> entries) {
        return countBy(entries, RegistryEntry::type);
    }

    /** Алдағы N күнде мерзімі біткелі тұрған жарамды анықтамалар. */
    public static List<RegistryEntry> expiringSoon(List<RegistryEntry> entries, LocalDate today, int days) {
        LocalDate limit = today.plusDays(days);
        return entries.stream()
                .filter(e -> effectiveStatus(e, today) == VerificationStatus.VALID)
                .filter(e -> !e.validUntil().isAfter(limit))
                .sorted(Comparator.comparing(RegistryEntry::validUntil))
                .toList();
    }

    public static double averageValidityDays(List<RegistryEntry> entries) {
        return entries.stream()
                .mapToLong(e -> ChronoUnit.DAYS.between(e.issuedOn(), e.validUntil()))
                .average()
                .orElse(0.0);
    }

    /** Консольге шығаруға дайын жолдар (қазақша). */
    public static List<String> summary(List<RegistryEntry> entries, LocalDate today) {
        List<String> lines = new ArrayList<>();
        lines.add("Барлық анықтама: " + entries.size());
        byStatus(entries, today).entrySet().stream()
                .sorted(Comparator.comparing(en -> en.getKey().name()))
                .forEach(en -> lines.add("  " + en.getKey().titleKk() + ": " + en.getValue()));
        lines.add("Түрлері бойынша:");
        byType(entries).entrySet().stream()
                .sorted(Comparator.comparing(en -> en.getKey().name()))
                .forEach(en -> lines.add("  " + en.getKey().titleKk() + ": " + en.getValue()));
        lines.add(String.format("Орташа жарамдылық мерзімі: %.1f күн", averageValidityDays(entries)));
        List<RegistryEntry> soon = expiringSoon(entries, today, 7);
        lines.add("7 күн ішінде мерзімі бітетіндер: " + soon.size()
                + (soon.isEmpty() ? "" : " (" + soon.stream().map(RegistryEntry::id)
                .collect(Collectors.joining(", ")) + ")"));
        return lines;
    }
}
