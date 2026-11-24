package kz.egov.docverify.storage;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import kz.egov.docverify.exception.DocVerifyException;
import kz.egov.docverify.model.DocumentType;
import kz.egov.docverify.model.EDocument;
import kz.egov.docverify.model.SignedDocument;

/**
 * .edoc мәтіндік пішімі: "кілт=мән" жолдары. Қатаң талдаушы:
 * белгісіз және қайталанатын өрістерге рұқсат жоқ. Жаңа жол мен '\' экрандалады.
 */
public final class DocumentCodec {
    private static final List<String> FIELDS =
            List.of("id", "type", "ownerName", "ownerIin", "content", "issuedOn", "validUntil");

    private DocumentCodec() {
    }

    /** Қолтаңба қойылатын мәтін: барлық өрістер бекітілген ретпен. */
    public static String canonicalText(EDocument d) {
        StringBuilder sb = new StringBuilder();
        line(sb, "id", d.id());
        line(sb, "type", d.type().name());
        line(sb, "ownerName", d.ownerName());
        line(sb, "ownerIin", d.ownerIin());
        line(sb, "content", d.content());
        line(sb, "issuedOn", d.issuedOn().toString());
        line(sb, "validUntil", d.validUntil().toString());
        return sb.toString();
    }

    public static String encode(SignedDocument s) {
        return canonicalText(s.document()) + "signature=" + s.signature() + "\n";
    }

    public static SignedDocument decode(String text) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String raw : text.split("\n", -1)) {
            String line = raw.endsWith("\r") ? raw.substring(0, raw.length() - 1) : raw;
            if (line.isEmpty()) {
                continue;
            }
            int i = line.indexOf('=');
            if (i <= 0) {
                throw new DocVerifyException("Файл жолының пішімі қате.");
            }
            String key = line.substring(0, i);
            if (!FIELDS.contains(key) && !key.equals("signature")) {
                throw new DocVerifyException("Белгісіз өріс: " + key);
            }
            if (map.put(key, line.substring(i + 1)) != null) {
                throw new DocVerifyException("Қайталанатын өріс: " + key);
            }
        }
        for (String f : FIELDS) {
            if (!map.containsKey(f)) {
                throw new DocVerifyException("Өріс жоқ: " + f);
            }
        }
        if (!map.containsKey("signature")) {
            throw new DocVerifyException("Қолтаңба жоқ.");
        }
        try {
            EDocument doc = new EDocument(
                    unescape(map.get("id")),
                    DocumentType.valueOf(unescape(map.get("type"))),
                    unescape(map.get("ownerName")),
                    unescape(map.get("ownerIin")),
                    unescape(map.get("content")),
                    LocalDate.parse(map.get("issuedOn")),
                    LocalDate.parse(map.get("validUntil")));
            return new SignedDocument(doc, map.get("signature"));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw new DocVerifyException("Өріс мәні жарамсыз: " + e.getMessage(), e);
        }
    }

    private static void line(StringBuilder sb, String key, String value) {
        sb.append(key).append('=').append(escape(value)).append('\n');
    }

    static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    static String unescape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (++i >= s.length()) {
                throw new DocVerifyException("Экрандау қатесі.");
            }
            switch (s.charAt(i)) {
                case '\\' -> sb.append('\\');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                default -> throw new DocVerifyException("Белгісіз экрандау тізбегі.");
            }
        }
        return sb.toString();
    }
}
