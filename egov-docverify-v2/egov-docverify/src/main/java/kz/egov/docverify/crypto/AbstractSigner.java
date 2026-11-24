package kz.egov.docverify.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;

import kz.egov.docverify.exception.DocVerifyException;

/** Абстрактты класс: java.security.Signature арқылы жалпы жұмыс. Алгоритмді мұрагер таңдайды. */
public abstract class AbstractSigner implements DocumentSigner {

    /** Мысалы, "SHA256withRSA". */
    protected abstract String algorithm();

    @Override
    public String sign(String text, PrivateKey key) {
        try {
            Signature s = Signature.getInstance(algorithm());
            s.initSign(key);
            s.update(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(s.sign());
        } catch (GeneralSecurityException e) {
            throw new DocVerifyException("Қолтаңба қою мүмкін болмады: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verify(String text, String signatureBase64, PublicKey key) {
        try {
            byte[] sig = Base64.getDecoder().decode(signatureBase64);
            Signature s = Signature.getInstance(algorithm());
            s.initVerify(key);
            s.update(text.getBytes(StandardCharsets.UTF_8));
            return s.verify(sig);
        } catch (SignatureException | IllegalArgumentException e) {
            return false; // бүлінген қолтаңба: бағдарлама құламайды, жай ғана "жалған"
        } catch (GeneralSecurityException e) {
            throw new DocVerifyException("Тексеру мүмкін болмады: " + e.getMessage(), e);
        }
    }
}
