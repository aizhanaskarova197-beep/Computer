package kz.egov.docverify.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import kz.egov.docverify.TestSupport;
import kz.egov.docverify.crypto.RsaSigner;
import kz.egov.docverify.exception.DocVerifyException;
import kz.egov.docverify.model.AuditEntry;
import kz.egov.docverify.model.RegistryEntry;
import kz.egov.docverify.model.SignedDocument;
import kz.egov.docverify.model.VerificationStatus;
import kz.egov.docverify.storage.DocumentCodec;
import kz.egov.docverify.storage.InMemoryRepository;
import kz.egov.docverify.storage.NoTransaction;

class BatchVerifierTest {

    @Test
    void manyFilesAreVerifiedInParallel(@TempDir Path dir) throws Exception {
        var registry = new InMemoryRepository<RegistryEntry, String>();
        var signer = new RsaSigner();
        var issuance = new IssuanceService(signer, TestSupport.keys().getPrivate(), registry,
                new InMemoryRepository<AuditEntry, String>(), new NoTransaction(), TestSupport.CLOCK);
        List<Path> files = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            SignedDocument sd = issuance.issue(TestSupport.doc("DOC-" + i));
            Path f = dir.resolve("DOC-" + i + ".edoc");
            Files.writeString(f, DocumentCodec.encode(sd), StandardCharsets.UTF_8);
            files.add(f);
        }
        issuance.revoke("DOC-3");
        Files.writeString(files.get(7), DocumentCodec.encode(issuance.issue(TestSupport.doc("X-1")))
                .replace("450000", "950000"), StandardCharsets.UTF_8);

        var verifier = new VerificationService(signer, TestSupport.keys().getPublic(), registry, TestSupport.CLOCK);
        BatchVerifier.BatchResult res = new BatchVerifier(verifier).verifyAll(files, 4);

        assertEquals(20, res.results().size());
        assertEquals(VerificationStatus.REVOKED, res.results().get("DOC-3.edoc").status());
        assertEquals(VerificationStatus.TAMPERED, res.results().get("DOC-7.edoc").status());
        assertEquals(VerificationStatus.VALID, res.results().get("DOC-0.edoc").status());
        assertTrue(res.threadNames().size() >= 1 && res.threadNames().size() <= 4);
    }

    @Test
    void invalidThreadCountIsRejected() {
        var verifier = new VerificationService(new RsaSigner(), TestSupport.keys().getPublic(),
                new InMemoryRepository<RegistryEntry, String>(), TestSupport.CLOCK);
        assertThrows(DocVerifyException.class, () -> new BatchVerifier(verifier).verifyAll(List.of(), 0));
    }

    @Test
    void brokenFileGivesTamperedInsteadOfCrash(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("bad.edoc");
        Files.writeString(f, "бұл анықтама емес", StandardCharsets.UTF_8);
        var verifier = new VerificationService(new RsaSigner(), TestSupport.keys().getPublic(),
                new InMemoryRepository<RegistryEntry, String>(), TestSupport.CLOCK);
        assertEquals(VerificationStatus.TAMPERED, verifier.verifyFile(f).status());
    }
}
