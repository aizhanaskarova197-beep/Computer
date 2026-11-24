package kz.egov.docverify.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import kz.egov.docverify.exception.DocVerifyException;

class KeyManagerTest {

    @Test
    void createdKeysAreReloadedWithSamePassword(@TempDir Path dir) {
        KeyPair first = KeyManager.loadOrCreate(dir, "құпия123".toCharArray());
        KeyPair second = KeyManager.loadOrCreate(dir, "құпия123".toCharArray());
        assertArrayEquals(first.getPrivate().getEncoded(), second.getPrivate().getEncoded());
        assertArrayEquals(first.getPublic().getEncoded(), second.getPublic().getEncoded());
    }

    @Test
    void wrongPasswordIsRejected(@TempDir Path dir) {
        KeyManager.loadOrCreate(dir, "дұрыс-құпиясөз".toCharArray());
        assertThrows(DocVerifyException.class, () -> KeyManager.loadOrCreate(dir, "қате".toCharArray()));
    }

    @Test
    void privateKeyFileIsNotStoredAsPlainKey(@TempDir Path dir) throws Exception {
        KeyPair pair = KeyManager.loadOrCreate(dir, "pass123".toCharArray());
        byte[] stored = Base64.getDecoder().decode(Files.readString(dir.resolve("private.enc")).trim());
        byte[] plain = pair.getPrivate().getEncoded();
        String storedStr = new String(stored, java.nio.charset.StandardCharsets.ISO_8859_1);
        String plainPrefix = new String(plain, 0, 32, java.nio.charset.StandardCharsets.ISO_8859_1);
        assertFalse(storedStr.contains(plainPrefix));
        assertTrue(Files.exists(dir.resolve("public.key")));
    }

    @Test
    void corruptedKeyFileIsRejected(@TempDir Path dir) throws Exception {
        KeyManager.loadOrCreate(dir, "pass123".toCharArray());
        Files.writeString(dir.resolve("private.enc"), "AAAA");
        assertThrows(DocVerifyException.class, () -> KeyManager.loadOrCreate(dir, "pass123".toCharArray()));
    }
}
