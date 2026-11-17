package kz.egov.docverify.exception;

/** Checked exception for all expected errors of the application. */
public class DocVerifyException extends Exception {

    public DocVerifyException(String message) {
        super(message);
    }

    public DocVerifyException(String message, Throwable cause) {
        super(message, cause);
    }
}
