package kz.egov.docverify.crypto;

import kz.egov.docverify.TestKeys;
import kz.egov.docverify.exception.DocVerifyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyManagerTest {

    @Test
    void savedKeysCanBeLoadedAndUsed(@TempDir Path dir) throws Exception {
        KeyPair original = TestKeys.pairA();
        KeyManager.save(original, dir);
        KeyPair loaded = KeyManager.load(dir);

        byte[] data = "data".getBytes(StandardCharsets.UTF_8);
        byte[] signature = new RsaDocumentSigner(loaded.getPrivate(), loaded.getPublic()).sign(data);
        assertTrue(RsaDocumentSigner.verifyOnly(original.getPublic()).verify(data, signature));
    }

    @Test
    void loadOrCreateCreatesKeysOnlyOnce(@TempDir Path dir) throws Exception {
        assertFalse(KeyManager.exists(dir));
        KeyPair first = KeyManager.loadOrCreate(dir);
        assertTrue(KeyManager.exists(dir));
        KeyPair second = KeyManager.loadOrCreate(dir);
        assertTrue(Arrays.equals(first.getPublic().getEncoded(), second.getPublic().getEncoded()));
    }

    @Test
    void loadFromEmptyDirectoryFails(@TempDir Path dir) {
        assertThrows(DocVerifyException.class, () -> KeyManager.load(dir));
    }
}
