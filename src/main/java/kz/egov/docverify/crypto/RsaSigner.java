package kz.egov.docverify.crypto;

/** RSA + SHA-256 қолтаңбасы (мұрагер класс). */
public class RsaSigner extends AbstractSigner {
    @Override
    protected String algorithm() {
        return "SHA256withRSA";
    }
}
