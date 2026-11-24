package kz.egov.docverify.exception;

/** Жобаның өз ерекшелігі (unchecked): қате енгізу, бүлінген файл, дерекқор қатесі. */
public class DocVerifyException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DocVerifyException(String message) {
        super(message);
    }

    public DocVerifyException(String message, Throwable cause) {
        super(message, cause);
    }
}
