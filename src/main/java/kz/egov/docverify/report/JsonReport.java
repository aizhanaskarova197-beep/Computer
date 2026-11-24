package kz.egov.docverify.report;

import java.util.List;
import java.util.stream.Collectors;

import kz.egov.docverify.model.RegistryEntry;

/** JSON есеп (сыртқы кітапханасыз). */
public class JsonReport extends Report {
    @Override
    protected String render(List<RegistryEntry> entries) {
        String body = entries.stream()
                .map(e -> "  {\"id\": " + str(e.id()) + ", \"type\": " + str(e.type().name())
                        + ", \"issuedOn\": " + str(e.issuedOn().toString())
                        + ", \"validUntil\": " + str(e.validUntil().toString())
                        + ", \"status\": " + str(e.status().name()) + "}")
                .collect(Collectors.joining(",\n"));
        return "[\n" + body + (body.isEmpty() ? "" : "\n") + "]\n";
    }

    @Override
    public String extension() {
        return "json";
    }

    private static String str(String v) {
        return "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
