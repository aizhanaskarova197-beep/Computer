package kz.egov.docverify.crypto;

import kz.egov.docverify.exception.DocVerifyException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** Generates, stores and loads the issuer's RSA key pair (Base64 text files). */
public final class KeyManager {

    private static final String PRIVATE_FILE = "private.key";
    private static final String PUBLIC_FILE = "public.key";
    private static final int KEY_SIZE = 2048;

    private KeyManager() {
    }

    public static KeyPair generate() throws DocVerifyException {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(KEY_SIZE, new SecureRandom());
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new DocVerifyException("Key generation failed: " + e.getMessage(), e);
        }
    }

    public static void save(KeyPair pair, Path dir) throws DocVerifyException {
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(PRIVATE_FILE),
                    Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()));
            Files.writeString(dir.resolve(PUBLIC_FILE),
                    Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
        } catch (IOException e) {
            throw new DocVerifyException("Cannot save keys: " + e.getMessage(), e);
        }
    }

    public static KeyPair load(Path dir) throws DocVerifyException {
        try {
            byte[] privateBytes = Base64.getDecoder()
                    .decode(Files.readString(dir.resolve(PRIVATE_FILE)).trim());
            byte[] publicBytes = Base64.getDecoder()
                    .decode(Files.readString(dir.resolve(PUBLIC_FILE)).trim());
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return new KeyPair(
                    factory.generatePublic(new X509EncodedKeySpec(publicBytes)),
                    factory.generatePrivate(new PKCS8EncodedKeySpec(privateBytes)));
        } catch (IOException | GeneralSecurityException | IllegalArgumentException e) {
            throw new DocVerifyException("Cannot load keys from " + dir + ": " + e.getMessage(), e);
        }
    }

    public static boolean exists(Path dir) {
        return Files.isRegularFile(dir.resolve(PRIVATE_FILE))
                && Files.isRegularFile(dir.resolve(PUBLIC_FILE));
    }

    /** Loads the keys if they exist, otherwise creates and saves a new pair. */
    public static KeyPair loadOrCreate(Path dir) throws DocVerifyException {
        if (exists(dir)) {
            return load(dir);
        }
        KeyPair pair = generate();
        save(pair, dir);
        return pair;
    }
}
