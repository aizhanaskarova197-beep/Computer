package kz.egov.docverify.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import kz.egov.docverify.TestSupport;
import kz.egov.docverify.exception.DocVerifyException;
import kz.egov.docverify.model.DocumentType;
import kz.egov.docverify.model.EDocument;
import kz.egov.docverify.model.SignedDocument;

class DocumentCodecTest {

    private static SignedDocument signed(EDocument d) {
        return new SignedDocument(d, "c2lnbmF0dXJl");
    }

    @Test
    void encodeThenDecodeGivesSameDocument() {
        SignedDocument s = signed(TestSupport.doc("DOC-1"));
        assertEquals(s, DocumentCodec.decode(DocumentCodec.encode(s)));
    }

    @Test
    void multilineContentWithBackslashSurvivesRoundTrip() {
        EDocument d = new EDocument("DOC-2", DocumentType.RESIDENCE, "Тест", "040101500123",
                "1-жол\n2-жол\\n бұл әріптер\r\nownerIin=999", TestSupport.TODAY, TestSupport.TODAY.plusDays(5));
        assertEquals(d, DocumentCodec.decode(DocumentCodec.encode(signed(d))).document());
    }

    @Test
    void newlineInContentCannotInjectFakeField() {
        EDocument d = new EDocument("DOC-3", DocumentType.RESIDENCE, "Тест", "040101500123",
                "мәтін\nownerIin=111111111111", TestSupport.TODAY, TestSupport.TODAY.plusDays(5));
        String encoded = DocumentCodec.encode(signed(d));
        assertEquals(1, encoded.lines().filter(l -> l.startsWith("ownerIin=")).count());
    }

    @Test
    void unknownFieldIsRejected() {
        String text = DocumentCodec.encode(signed(TestSupport.doc("DOC-1"))) + "admin=true\n";
        assertThrows(DocVerifyException.class, () -> DocumentCodec.decode(text));
    }

    @Test
    void duplicateFieldIsRejected() {
        String text = DocumentCodec.encode(signed(TestSupport.doc("DOC-1"))) + "id=DOC-2\n";
        assertThrows(DocVerifyException.class, () -> DocumentCodec.decode(text));
    }

    @Test
    void missingSignatureIsRejected() {
        String text = DocumentCodec.canonicalText(TestSupport.doc("DOC-1"));
        assertThrows(DocVerifyException.class, () -> DocumentCodec.decode(text));
    }

    @Test
    void windowsLineEndingsAreAccepted() {
        SignedDocument s = signed(TestSupport.doc("DOC-1"));
        String crlf = DocumentCodec.encode(s).replace("\n", "\r\n");
        assertEquals(s, DocumentCodec.decode(crlf));
    }
}
