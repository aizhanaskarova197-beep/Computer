package kz.egov.docverify.model;

/** Types of electronic certificates that can be issued. */
public enum DocumentType {
    PENSION_CERTIFICATE("Pension contributions certificate"),
    PROPERTY_CERTIFICATE("Real estate ownership certificate"),
    NO_CRIMINAL_RECORD("Certificate of no criminal record"),
    EDUCATION_CERTIFICATE("Education certificate");

    private final String title;

    DocumentType(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
