package kz.egov.docverify.crypto;

import kz.egov.docverify.TestKeys;
import kz.egov.docverify.exception.DocVerifyException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RsaDocumentSignerTest {

    private static final byte[] DATA = "certificate text".getBytes(StandardCharsets.UTF_8);

    private RsaDocumentSigner signerA() {
        KeyPair keys = TestKeys.pairA();
        return new RsaDocumentSigner(keys.getPrivate(), keys.getPublic());
    }

    @Test
    void validSignatureIsAccepted() throws Exception {
        RsaDocumentSigner signer = signerA();
        assertTrue(signer.verify(DATA, signer.sign(DATA)));
    }

    @Test
    void modifiedDataIsRejected() throws Exception {
        RsaDocumentSigner signer = signerA();
        byte[] signature = signer.sign(DATA);
        assertFalse(signer.verify("certificate text!".getBytes(StandardCharsets.UTF_8), signature));
    }

    @Test
    void signatureFromOtherKeyIsRejected() throws Exception {
        byte[] foreign = new RsaDocumentSigner(TestKeys.pairB().getPrivate(), TestKeys.pairB().getPublic())
                .sign(DATA);
        assertFalse(signerA().verify(DATA, foreign));
    }

    @Test
    void garbageSignatureIsRejected() throws Exception {
        assertFalse(signerA().verify(DATA, new byte[]{1, 2, 3}));
    }

    @Test
    void verifyOnlySignerCannotSign() {
        RsaDocumentSigner verifier = RsaDocumentSigner.verifyOnly(TestKeys.pairA().getPublic());
        assertThrows(DocVerifyException.class, () -> verifier.sign(DATA));
    }

    @Test
    void verifyOnlySignerChecksSignatures() throws Exception {
        byte[] signature = signerA().sign(DATA);
        RsaDocumentSigner verifier = RsaDocumentSigner.verifyOnly(TestKeys.pairA().getPublic());
        assertTrue(verifier.verify(DATA, signature));
    }
}
