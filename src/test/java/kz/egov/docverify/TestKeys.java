package kz.egov.docverify;

import kz.egov.docverify.crypto.KeyManager;
import kz.egov.docverify.exception.DocVerifyException;

import java.security.KeyPair;

/** Two RSA key pairs shared by all tests (generating keys is slow). */
public final class TestKeys {

    private static KeyPair pairA;
    private static KeyPair pairB;

    private TestKeys() {
    }

    public static synchronized KeyPair pairA() {
        if (pairA == null) {
            pairA = generate();
        }
        return pairA;
    }

    public static synchronized KeyPair pairB() {
        if (pairB == null) {
            pairB = generate();
        }
        return pairB;
    }

    private static KeyPair generate() {
        try {
            return KeyManager.generate();
        } catch (DocVerifyException e) {
            throw new IllegalStateException(e);
        }
    }
}
