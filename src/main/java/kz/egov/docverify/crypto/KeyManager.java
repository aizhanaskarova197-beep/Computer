package kz.egov.docverify.crypto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import kz.egov.docverify.exception.DocVerifyException;

/**
 * RSA-2048 кілттер жұбын жасайды және сақтайды.
 * Жабық кілт құпиясөзбен қорғалады: PBKDF2WithHmacSHA256 -> AES-256-GCM.
 */
public final class KeyManager {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_LEN = 16;
    private static final int IV_LEN = 12;
    private static final int ITERATIONS = 120_000;

    private KeyManager() {
    }

    public static KeyPair generate() {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
            g.initialize(2048, RANDOM);
            return g.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new DocVerifyException("RSA кілтін жасау мүмкін болмады.", e);
        }
    }

    public static KeyPair loadOrCreate(Path dir, char[] password) {
        Path priv = dir.resolve("private.enc");
        Path pub = dir.resolve("public.key");
        try {
            Files.createDirectories(dir);
            if (Files.exists(priv) && Files.exists(pub)) {
                return load(priv, pub, password);
            }
            KeyPair pair = generate();
            save(priv, pub, pair, password);
            return pair;
        } catch (IOException e) {
            throw new DocVerifyException("Кілт файлдарымен жұмыс қатесі: " + e.getMessage(), e);
        }
    }

    private static void save(Path priv, Path pub, KeyPair pair, char[] password) throws IOException {
        byte[] salt = random(SALT_LEN);
        byte[] iv = random(IV_LEN);
        byte[] encrypted = aes(Cipher.ENCRYPT_MODE, pair.getPrivate().getEncoded(), password, salt, iv);
        byte[] blob = new byte[SALT_LEN + IV_LEN + encrypted.length];
        System.arraycopy(salt, 0, blob, 0, SALT_LEN);
        System.arraycopy(iv, 0, blob, SALT_LEN, IV_LEN);
        System.arraycopy(encrypted, 0, blob, SALT_LEN + IV_LEN, encrypted.length);
        Files.writeString(priv, Base64.getEncoder().encodeToString(blob), StandardCharsets.UTF_8);
        Files.writeString(pub, Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()),
                StandardCharsets.UTF_8);
    }

    private static KeyPair load(Path priv, Path pub, char[] password) throws IOException {
        try {
            byte[] blob = Base64.getDecoder().decode(Files.readString(priv, StandardCharsets.UTF_8).trim());
            if (blob.length < SALT_LEN + IV_LEN + 16) {
                throw new DocVerifyException("Құпиясөз қате немесе кілт файлы бүлінген.");
            }
            byte[] salt = Arrays.copyOfRange(blob, 0, SALT_LEN);
            byte[] iv = Arrays.copyOfRange(blob, SALT_LEN, SALT_LEN + IV_LEN);
            byte[] data = Arrays.copyOfRange(blob, SALT_LEN + IV_LEN, blob.length);
            byte[] pkcs8 = aes(Cipher.DECRYPT_MODE, data, password, salt, iv);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            var privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
            byte[] x509 = Base64.getDecoder().decode(Files.readString(pub, StandardCharsets.UTF_8).trim());
            var publicKey = kf.generatePublic(new X509EncodedKeySpec(x509));
            return new KeyPair(publicKey, privateKey);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new DocVerifyException("Құпиясөз қате немесе кілт файлы бүлінген.", e);
        }
    }

    private static byte[] aes(int mode, byte[] data, char[] password, byte[] salt, byte[] iv) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, 256);
        try {
            byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            return c.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new DocVerifyException("Құпиясөз қате немесе кілт файлы бүлінген.", e);
        } finally {
            spec.clearPassword();
        }
    }

    private static byte[] random(int n) {
        byte[] b = new byte[n];
        RANDOM.nextBytes(b);
        return b;
    }
}
