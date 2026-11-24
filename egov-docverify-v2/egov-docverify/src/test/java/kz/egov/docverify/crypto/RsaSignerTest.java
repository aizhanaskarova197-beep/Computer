package kz.egov.docverify.crypto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;

import org.junit.jupiter.api.Test;

import kz.egov.docverify.TestSupport;

class RsaSignerTest {
    private final DocumentSigner signer = new RsaSigner();
    private final KeyPair keys = TestSupport.keys();

    @Test
    void signatureVerifies() {
        String sig = signer.sign("сәлем", keys.getPrivate());
        assertTrue(signer.verify("сәлем", sig, keys.getPublic()));
    }

    @Test
    void changedTextFailsVerification() {
        String sig = signer.sign("сома: 450000", keys.getPrivate());
        assertFalse(signer.verify("сома: 950000", sig, keys.getPublic()));
    }

    @Test
    void signatureFromAnotherKeyFails() {
        KeyPair other = KeyManager.generate();
        String sig = signer.sign("мәтін", other.getPrivate());
        assertFalse(signer.verify("мәтін", sig, keys.getPublic()));
    }

    @Test
    void garbageSignatureReturnsFalseInsteadOfCrashing() {
        assertFalse(signer.verify("мәтін", "бұл Base64 емес!!!", keys.getPublic()));
        assertFalse(signer.verify("мәтін", "AAAA", keys.getPublic()));
    }

    @Test
    void sameInputGivesSameSignature() {
        // PKCS#1 v1.5 детерминді; екі түрлі мәтін екі түрлі қолтаңба береді
        assertNotEquals(signer.sign("а", keys.getPrivate()), signer.sign("б", keys.getPrivate()));
    }
}
