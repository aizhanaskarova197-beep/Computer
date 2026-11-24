package kz.egov.docverify.report;

import java.util.List;
import java.util.stream.Collectors;

import kz.egov.docverify.model.RegistryEntry;

/** CSV есеп. */
public class CsvReport extends Report {
    @Override
    protected String render(List<RegistryEntry> entries) {
        String rows = entries.stream()
                .map(e -> String.join(",", cell(e.id()), cell(e.type().name()), cell(e.issuedOn().toString()),
                        cell(e.validUntil().toString()), cell(e.status().name())))
                .collect(Collectors.joining("\n"));
        return "id,type,issued_on,valid_until,status\n" + rows + (rows.isEmpty() ? "" : "\n");
    }

    @Override
    public String extension() {
        return "csv";
    }

    private static String cell(String v) {
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
