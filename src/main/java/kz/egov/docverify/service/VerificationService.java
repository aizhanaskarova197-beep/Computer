package kz.egov.docverify.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

import kz.egov.docverify.crypto.DocumentSigner;
import kz.egov.docverify.exception.DocVerifyException;
import kz.egov.docverify.model.EDocument;
import kz.egov.docverify.model.RegistryEntry;
import kz.egov.docverify.model.SignedDocument;
import kz.egov.docverify.model.VerificationResult;
import kz.egov.docverify.model.VerificationStatus;
import kz.egov.docverify.storage.DocumentCodec;
import kz.egov.docverify.storage.Repository;

/**
 * Тексеру тәртібі: 1) қолтаңба, 2) тізілім, 3) кері қайтару, 4) мерзім.
 * Қолтаңба тексерілмейінше файлдағы деректерге сенуге болмайды.
 */
public class VerificationService {
    private final DocumentSigner signer;
    private final PublicKey publicKey;
    private final Repository<RegistryEntry, String> registry;
    private final Clock clock;

    public VerificationService(DocumentSigner signer, PublicKey publicKey,
                               Repository<RegistryEntry, String> registry, Clock clock) {
        this.signer = signer;
        this.publicKey = publicKey;
        this.registry = registry;
        this.clock = clock;
    }

    public VerificationResult verify(SignedDocument sd) {
        EDocument doc = sd.document();
        if (!signer.verify(DocumentCodec.canonicalText(doc), sd.signature(), publicKey)) {
            return new VerificationResult(VerificationStatus.TAMPERED, doc.id(),
                    "Қолтаңба сәйкес келмейді. Файлға сенуге болмайды.");
        }
        Optional<RegistryEntry> found = registry.findById(doc.id());
        if (found.isEmpty()) {
            return new VerificationResult(VerificationStatus.NOT_FOUND, doc.id(),
                    "Қолтаңба дұрыс, бірақ анықтама тізілімде жоқ.");
        }
        if (found.get().isRevoked()) {
            return new VerificationResult(VerificationStatus.REVOKED, doc.id(),
                    "Анықтама кері қайтарылған.");
        }
        if (LocalDate.now(clock).isAfter(doc.validUntil())) {
            return new VerificationResult(VerificationStatus.EXPIRED, doc.id(),
                    "Анықтаманың мерзімі өткен (" + doc.validUntil() + " дейін).");
        }
        return new VerificationResult(VerificationStatus.VALID, doc.id(),
                "Қолтаңба дұрыс, анықтама тізілімде бар, мерзімі өтпеген.");
    }

    public VerificationResult verifyFile(Path file) {
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            return verify(DocumentCodec.decode(text));
        } catch (DocVerifyException e) {
            return new VerificationResult(VerificationStatus.TAMPERED, "?",
                    "Файл пішімі бүлінген: " + e.getMessage());
        } catch (IOException e) {
            return new VerificationResult(VerificationStatus.TAMPERED, "?",
                    "Файлды оқу мүмкін болмады: " + e.getMessage());
        }
    }
}
