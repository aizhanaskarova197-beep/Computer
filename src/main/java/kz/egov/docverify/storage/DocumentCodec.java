package kz.egov.docverify.storage;

import kz.egov.docverify.exception.DocVerifyException;
import kz.egov.docverify.model.DocumentType;
import kz.egov.docverify.model.EDocument;
import kz.egov.docverify.model.SignedDocument;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Text format of an electronic document (.edoc):
 * a fixed header, key=value lines and the Base64 signature at the end.
 * The signature always covers the "canonical text" built from the fields,
 * so any change of a field breaks the signature.
 */
public final class DocumentCodec {

    public static final String HEADER = "EDOC/1";

    private static final Set<String> KNOWN_KEYS = Set.of(
            "id", "type", "ownerIin", "ownerName", "issuedAt", "expiresAt", "content", "signature");

    private DocumentCodec() {
    }

    /** The exact text that is signed (everything except the signature line). */
    public static String canonicalText(EDocument d) {
        return HEADER + "\n"
                + "id=" + escape(d.id()) + "\n"
                + "type=" + d.type().name() + "\n"
                + "ownerIin=" + d.ownerIin() + "\n"
                + "ownerName=" + escape(d.ownerName()) + "\n"
                + "issuedAt=" + d.issuedAt() + "\n"
                + "expiresAt=" + d.expiresAt() + "\n"
                + "content=" + escape(d.content()) + "\n";
    }

    public static byte[] canonicalBytes(EDocument d) {
        return canonicalText(d).getBytes(StandardCharsets.UTF_8);
    }

    public static String encode(SignedDocument signed) {
        return canonicalText(signed.document()) + "signature=" + signed.signatureBase64() + "\n";
    }

    public static SignedDocument decode(String text) throws DocVerifyException {
        if (text.startsWith("\uFEFF")) { // BOM added by some editors
            text = text.substring(1);
        }
        String[] lines = text.replace("\r\n", "\n").replace("\r", "\n").split("\n");
        if (!HEADER.equals(lines[0].trim())) {
            throw new DocVerifyException("Not an " + HEADER + " file: header is missing or unsupported");
        }

        Map<String, String> fields = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isBlank()) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                throw new DocVerifyException("Line " + (i + 1) + " is not in key=value form");
            }
            String key = line.substring(0, eq);
            if (!KNOWN_KEYS.contains(key)) {
                throw new DocVerifyException("Unknown field: " + key);
            }
            if (fields.put(key, line.substring(eq + 1)) != null) {
                throw new DocVerifyException("Duplicate field: " + key);
            }
        }

        String id = unescape(require(fields, "id"));
        String type = require(fields, "type");
        String ownerIin = require(fields, "ownerIin");
        String ownerName = unescape(require(fields, "ownerName"));
        String issuedAt = require(fields, "issuedAt");
        String expiresAt = require(fields, "expiresAt");
        String content = unescape(require(fields, "content"));
        String signature = require(fields, "signature");

        try {
            EDocument document = new EDocument(id, DocumentType.valueOf(type), ownerIin, ownerName,
                    Instant.parse(issuedAt), Instant.parse(expiresAt), content);
            return new SignedDocument(document, Base64.getDecoder().decode(signature));
        } catch (RuntimeException e) {
            throw new DocVerifyException("Malformed document: " + e.getMessage(), e);
        }
    }

    public static void write(Path file, SignedDocument signed) throws DocVerifyException {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, encode(signed), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DocVerifyException("Cannot write file " + file + ": " + e.getMessage(), e);
        }
    }

    public static SignedDocument read(Path file) throws DocVerifyException {
        try {
            return decode(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new DocVerifyException("Cannot read file " + file + ": " + e.getMessage(), e);
        }
    }

    private static String require(Map<String, String> fields, String key) throws DocVerifyException {
        String value = fields.get(key);
        if (value == null) {
            throw new DocVerifyException("Required field is missing: " + key);
        }
        return value;
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

    static String unescape(String s) throws DocVerifyException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (i + 1 >= s.length()) {
                throw new DocVerifyException("Bad escape sequence at the end of a value");
            }
            char next = s.charAt(++i);
            switch (next) {
                case '\\' -> sb.append('\\');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                default -> throw new DocVerifyException("Bad escape sequence: \\" + next);
            }
        }
        return sb.toString();
    }
}
