package kz.egov.docverify.model;

import java.time.LocalDate;
import java.util.regex.Pattern;

import kz.egov.docverify.exception.DocVerifyException;

/** Электрондық анықтама. Құрылған кезде деректер тексеріледі. */
public record EDocument(String id, DocumentType type, String ownerName, String ownerIin,
                        String content, LocalDate issuedOn, LocalDate validUntil) {

    private static final Pattern ID = Pattern.compile("[A-Za-z0-9-]{1,40}");
    private static final Pattern IIN = Pattern.compile("\\d{12}");

    public EDocument {
        if (id == null || !ID.matcher(id).matches()) {
            throw new DocVerifyException("ID тек латын әріптері, сандар және '-' белгісінен тұруы тиіс (1-40 таңба).");
        }
        if (type == null) {
            throw new DocVerifyException("Анықтама түрі көрсетілмеген.");
        }
        if (ownerName == null || ownerName.isBlank() || ownerName.length() > 100) {
            throw new DocVerifyException("Аты-жөні бос болмауы тиіс (ең көбі 100 таңба).");
        }
        if (ownerIin == null || !IIN.matcher(ownerIin).matches()) {
            throw new DocVerifyException("ЖСН дәл 12 саннан тұруы тиіс.");
        }
        if (content == null || content.isBlank() || content.length() > 2000) {
            throw new DocVerifyException("Мазмұны бос болмауы тиіс (ең көбі 2000 таңба).");
        }
        if (issuedOn == null || validUntil == null || validUntil.isBefore(issuedOn)) {
            throw new DocVerifyException("Жарамдылық мерзімі берілген күннен ерте болмауы тиіс.");
        }
    }
}
