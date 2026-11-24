package kz.egov.docverify.service;

import java.security.PrivateKey;
import java.time.Clock;
import java.time.Instant;

import kz.egov.docverify.crypto.DocumentSigner;
import kz.egov.docverify.exception.DocVerifyException;
import kz.egov.docverify.model.AuditEntry;
import kz.egov.docverify.model.EDocument;
import kz.egov.docverify.model.RegistryEntry;
import kz.egov.docverify.model.RegistryStatus;
import kz.egov.docverify.model.SignedDocument;
import kz.egov.docverify.storage.DocumentCodec;
import kz.egov.docverify.storage.Repository;
import kz.egov.docverify.storage.TransactionManager;

/** Анықтама беру және кері қайтару. Тізілім мен журнал бір транзакцияда жазылады. */
public class IssuanceService {
    private final DocumentSigner signer;
    private final PrivateKey privateKey;
    private final Repository<RegistryEntry, String> registry;
    private final Repository<AuditEntry, String> audit;
    private final TransactionManager tx;
    private final Clock clock;

    public IssuanceService(DocumentSigner signer, PrivateKey privateKey,
                           Repository<RegistryEntry, String> registry,
                           Repository<AuditEntry, String> audit,
                           TransactionManager tx, Clock clock) {
        this.signer = signer;
        this.privateKey = privateKey;
        this.registry = registry;
        this.audit = audit;
        this.tx = tx;
        this.clock = clock;
    }

    public SignedDocument issue(EDocument doc) {
        return tx.execute(() -> {
            if (registry.findById(doc.id()).isPresent()) {
                throw new DocVerifyException("Мұндай ID бұрыннан бар: " + doc.id());
            }
            String signature = signer.sign(DocumentCodec.canonicalText(doc), privateKey);
            registry.save(new RegistryEntry(doc.id(), doc.type(), doc.issuedOn(), doc.validUntil(),
                    RegistryStatus.ACTIVE));
            audit.save(AuditEntry.of(doc.id(), "ISSUED", Instant.now(clock)));
            return new SignedDocument(doc, signature);
        });
    }

    public void revoke(String id) {
        tx.execute(() -> {
            RegistryEntry e = registry.findById(id)
                    .orElseThrow(() -> new DocVerifyException("Тізілімде мұндай ID жоқ: " + id));
            if (e.isRevoked()) {
                throw new DocVerifyException("Анықтама әлдеқашан кері қайтарылған: " + id);
            }
            registry.save(e.withStatus(RegistryStatus.REVOKED));
            audit.save(AuditEntry.of(id, "REVOKED", Instant.now(clock)));
            return null;
        });
    }
}
