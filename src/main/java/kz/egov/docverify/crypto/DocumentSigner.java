package kz.egov.docverify.crypto;

import kz.egov.docverify.exception.DocVerifyException;

/** Contract for creating and checking digital signatures. */
public interface DocumentSigner {

    /** Signs the data and returns the raw signature bytes. */
    byte[] sign(byte[] data) throws DocVerifyException;

    /** Returns true only if the signature matches the data. */
    boolean verify(byte[] data, byte[] signature) throws DocVerifyException;
}
