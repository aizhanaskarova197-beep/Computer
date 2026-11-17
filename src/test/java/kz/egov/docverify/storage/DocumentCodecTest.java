package kz.egov.docverify.storage;

import kz.egov.docverify.exception.DocVerifyException;
import kz.egov.docverify.model.DocumentType;
import kz.egov.docverify.model.EDocument;
import kz.egov.docverify.model.SignedDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentCodecTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private SignedDocument sample(String content) {
        EDocument document = new EDocument("id-1", DocumentType.PROPERTY_CERTIFICATE, "123456789012",
                "Aizhan Askarova", NOW, NOW.plusSeconds(86_400), content);
        return new SignedDocument(document, new byte[]{1, 2, 3, 4});
    }

    @Test
    void roundTripKeepsAllFields() throws Exception {
        SignedDocument original = sample("one line");
        SignedDocument decoded = DocumentCodec.decode(DocumentCodec.encode(original));
        assertEquals(original.document(), decoded.document());
        assertTrue(Arrays.equals(original.signature(), decoded.signature()));
    }

    @Test
    void multilineContentWithBackslashSurvivesRoundTrip() throws Exception {
        SignedDocument original = sample("line 1\nline 2 with \\ backslash\r\nline 3");
        SignedDocument decoded = DocumentCodec.decode(DocumentCodec.encode(original));
        assertEquals(original.document().content(), decoded.document().content());
    }

    @Test
    void missingFieldIsRejected() {
        String text = DocumentCodec.encode(sample("text")).replace("ownerName=Aizhan Askarova\n", "");
        assertThrows(DocVerifyException.class, () -> DocumentCodec.decode(text));
    }

    @Test
    void wrongHeaderIsRejected() {
        String text = DocumentCodec.encode(sample("text")).replace("EDOC/1", "OTHER/9");
        assertThrows(DocVerifyException.class, () -> DocumentCodec.decode(text));
    }

    @Test
    void unknownFieldIsRejected() {
        String text = DocumentCodec.encode(sample("text")) + "extra=1\n";
        assertThrows(DocVerifyException.class, () -> DocumentCodec.decode(text));
    }

    @Test
    void windowsLineEndingsAreAccepted() throws Exception {
        SignedDocument original = sample("text");
        String windowsText = DocumentCodec.encode(original).replace("\n", "\r\n");
        assertEquals(original.document(), DocumentCodec.decode(windowsText).document());
    }

    @Test
    void fileWriteAndReadWork(@TempDir Path dir) throws Exception {
        SignedDocument original = sample("saved to disk");
        Path file = dir.resolve("sub").resolve("doc.edoc");
        DocumentCodec.write(file, original);
        assertEquals(original.document(), DocumentCodec.read(file).document());
    }
}
