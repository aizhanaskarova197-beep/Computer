package kz.egov.docverify.model;

/** Тексеру нәтижесінің күйі. */
public enum VerificationStatus {
    VALID("ЖАРАМДЫ"),
    TAMPERED("ӨЗГЕРТІЛГЕН / БҮЛІНГЕН"),
    NOT_FOUND("ТІЗІЛІМДЕ ЖОҚ"),
    REVOKED("КЕРІ ҚАЙТАРЫЛҒАН"),
    EXPIRED("МЕРЗІМІ ӨТКЕН");

    private final String titleKk;

    VerificationStatus(String titleKk) {
        this.titleKk = titleKk;
    }

    public String titleKk() {
        return titleKk;
    }
}
