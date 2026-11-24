package kz.egov.docverify.crypto;

import java.security.PrivateKey;
import java.security.PublicKey;

/** Қолтаңба беру және тексеру интерфейсі. */
public interface DocumentSigner {
    String sign(String text, PrivateKey key);

    boolean verify(String text, String signatureBase64, PublicKey key);
}
