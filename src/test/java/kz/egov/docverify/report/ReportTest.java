package kz.egov.docverify.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import kz.egov.docverify.TestSupport;
import kz.egov.docverify.model.DocumentType;
import kz.egov.docverify.model.RegistryEntry;
import kz.egov.docverify.model.RegistryStatus;

class ReportTest {
    private final List<RegistryEntry> entries = List.of(
            new RegistryEntry("A-1", DocumentType.INCOME, TestSupport.TODAY, TestSupport.TODAY.plusDays(30),
                    RegistryStatus.ACTIVE),
            new RegistryEntry("B-2", DocumentType.STUDY, TestSupport.TODAY, TestSupport.TODAY.plusDays(5),
                    RegistryStatus.REVOKED));

    @Test
    void csvHasHeaderAndOneLinePerEntry(@TempDir Path dir) throws Exception {
        Path f = new CsvReport().export(entries, dir.resolve("r.csv"));
        List<String> lines = Files.readAllLines(f, StandardCharsets.UTF_8);
        assertEquals(3, lines.size());
        assertEquals("id,type,issued_on,valid_until,status", lines.get(0));
        assertTrue(lines.get(2).startsWith("B-2,STUDY,"));
        assertTrue(lines.get(2).endsWith(",REVOKED"));
    }

    @Test
    void jsonContainsAllEntries(@TempDir Path dir) throws Exception {
        Path f = new JsonReport().export(entries, dir.resolve("sub/r.json"));
        String text = Files.readString(f, StandardCharsets.UTF_8);
        assertTrue(text.startsWith("["));
        assertTrue(text.contains("\"id\": \"A-1\""));
        assertTrue(text.contains("\"status\": \"REVOKED\""));
    }

    @Test
    void emptyListGivesValidFiles(@TempDir Path dir) throws Exception {
        String csv = Files.readString(new CsvReport().export(List.of(), dir.resolve("e.csv")));
        String json = Files.readString(new JsonReport().export(List.of(), dir.resolve("e.json")));
        assertEquals("id,type,issued_on,valid_until,status\n", csv);
        assertEquals("[\n]\n", json);
    }

    @Test
    void polymorphicUseThroughBaseClass(@TempDir Path dir) throws Exception {
        for (Report r : List.of(new CsvReport(), new JsonReport())) {
            Path f = r.export(entries, dir.resolve("p." + r.extension()));
            assertTrue(Files.size(f) > 0);
        }
    }
}
