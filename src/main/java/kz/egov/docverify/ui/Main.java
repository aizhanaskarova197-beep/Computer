package kz.egov.docverify.ui;

import kz.egov.docverify.crypto.DocumentSigner;
import kz.egov.docverify.crypto.KeyManager;
import kz.egov.docverify.crypto.RsaDocumentSigner;
import kz.egov.docverify.exception.DocVerifyException;
import kz.egov.docverify.security.RateLimiter;
import kz.egov.docverify.service.IssuanceService;
import kz.egov.docverify.service.VerificationService;
import kz.egov.docverify.storage.DocumentRegistry;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Clock;
import java.util.Scanner;

/** Entry point. Run this class in VS Code (Run button above main). */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        try {
            Path keysDir = Path.of("keys");
            Path registryFile = Path.of("data", "registry.csv");
            Path outputDir = Path.of("output");

            KeyPair keys = KeyManager.loadOrCreate(keysDir);
            DocumentSigner signer = new RsaDocumentSigner(keys.getPrivate(), keys.getPublic());
            DocumentRegistry registry = DocumentRegistry.load(registryFile);

            RateLimiter limiter = new RateLimiter();

            new ConsoleApp(new Scanner(System.in, StandardCharsets.UTF_8), out,
                    new IssuanceService(signer, registry),
                    new VerificationService(signer, registry, Clock.systemUTC(), limiter),
                    registry, registryFile, outputDir, limiter).run();
        } catch (DocVerifyException e) {
            out.println("Fatal error: " + e.getMessage());
            System.exit(1);
        }
    }
}
