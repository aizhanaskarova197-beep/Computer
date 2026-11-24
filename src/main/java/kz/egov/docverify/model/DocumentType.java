package kz.egov.docverify.model;

/** Анықтама түрлері. */
public enum DocumentType {
    INCOME("Табысы туралы анықтама"),
    RESIDENCE("Тұрғылықты жері туралы анықтама"),
    STUDY("Оқу орнынан анықтама"),
    NO_CONVICTION("Соттылығының жоқтығы туралы анықтама");

    private final String titleKk;

    DocumentType(String titleKk) {
        this.titleKk = titleKk;
    }

    public String titleKk() {
        return titleKk;
    }
}
