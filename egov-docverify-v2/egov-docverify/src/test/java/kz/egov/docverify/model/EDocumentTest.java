package kz.egov.docverify.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import kz.egov.docverify.TestSupport;
import kz.egov.docverify.exception.DocVerifyException;

class EDocumentTest {
    private static EDocument make(String id, String name, String iin, String content, LocalDate until) {
        return new EDocument(id, DocumentType.STUDY, name, iin, content, TestSupport.TODAY, until);
    }

    @Test
    void validDocumentIsCreated() {
        EDocument d = TestSupport.doc("DOC-1");
        assertEquals("DOC-1", d.id());
        assertEquals(DocumentType.INCOME, d.type());
    }

    @Test
    void iinMustHaveTwelveDigits() {
        assertThrows(DocVerifyException.class, () -> make("A1", "Аты", "12345", "мәтін", TestSupport.TODAY));
        assertThrows(DocVerifyException.class, () -> make("A1", "Аты", "04010150012x", "мәтін", TestSupport.TODAY));
    }

    @Test
    void blankNameAndContentAreRejected() {
        assertThrows(DocVerifyException.class, () -> make("A1", " ", "040101500123", "мәтін", TestSupport.TODAY));
        assertThrows(DocVerifyException.class, () -> make("A1", "Аты", "040101500123", "", TestSupport.TODAY));
    }

    @Test
    void idWithSpecialCharactersIsRejected() {
        assertThrows(DocVerifyException.class, () -> make("../x", "Аты", "040101500123", "мәтін", TestSupport.TODAY));
    }

    @Test
    void validUntilBeforeIssuedOnIsRejected() {
        assertThrows(DocVerifyException.class,
                () -> make("A1", "Аты", "040101500123", "мәтін", TestSupport.TODAY.minusDays(1)));
    }
}
