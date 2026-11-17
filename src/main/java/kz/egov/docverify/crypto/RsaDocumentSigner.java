package kz.egov.docverify.crypto;

import kz.egov.docverify.exception.DocVerifyException;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Objects;

/**
 * RSA signatures through the standard JCA (SHA256withRSA).
 * A signer created with {@link #verifyOnly(PublicKey)} can only check signatures.
 */
public class RsaDocumentSigner implements DocumentSigner {

    public static final String ALGORITHM = "SHA256withRSA";

    private final PrivateKey privateKey; // null for a verify-only signer
    private final PublicKey publicKey;

    public RsaDocumentSigner(PrivateKey privateKey, PublicKey publicKey) {
        this.privateKey = privateKey;
        this.publicKey = Objects.requireNonNull(publicKey, "publicKey");
    }

    public static RsaDocumentSigner verifyOnly(PublicKey publicKey) {
        return new RsaDocumentSigner(null, publicKey);
    }

    @Override
    public byte[] sign(byte[] data) throws DocVerifyException {
        if (privateKey == null) {
            throw new DocVerifyException("This signer has no private key and cannot sign");
        }
        try {
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initSign(privateKey);
            signature.update(data);
            return signature.sign();
        } catch (GeneralSecurityException e) {
            throw new DocVerifyException("Signing failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verify(byte[] data, byte[] signatureBytes) throws DocVerifyException {
        try {
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(data);
            return signature.verify(signatureBytes);
        } catch (SignatureException e) {
            // malformed signature bytes are simply an invalid signature
            return false;
        } catch (GeneralSecurityException e) {
            throw new DocVerifyException("Verification failed: " + e.getMessage(), e);
        }
    }
}
