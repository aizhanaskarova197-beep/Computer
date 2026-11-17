package kz.egov.docverify.ui;

import kz.egov.docverify.exception.DocVerifyException;
import kz.egov.docverify.model.DocumentType;
import kz.egov.docverify.model.EDocument;
import kz.egov.docverify.model.RegistryEntry;
import kz.egov.docverify.model.SignedDocument;
import kz.egov.docverify.model.VerificationResult;
import kz.egov.docverify.security.RateLimiter;
import kz.egov.docverify.service.IssuanceService;
import kz.egov.docverify.service.VerificationService;
import kz.egov.docverify.storage.DocumentCodec;
import kz.egov.docverify.storage.DocumentRegistry;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.stream.Stream;

/**
 * Text menu. Messages are in English on purpose: Windows consoles
 * often break Cyrillic and Kazakh letters.
 */
public class ConsoleApp {

    private final Scanner in;
    private final PrintStream out;
    private final IssuanceService issuance;
    private final VerificationService verification;
    private final DocumentRegistry registry;
    private final Path registryFile;
    private final Path outputDir;
    private final RateLimiter limiter;

    public ConsoleApp(Scanner in, PrintStream out, IssuanceService issuance,
                      VerificationService verification, DocumentRegistry registry,
                      Path registryFile, Path outputDir, RateLimiter limiter) {
        this.in = in;
        this.out = out;
        this.issuance = issuance;
        this.verification = verification;
        this.registry = registry;
        this.registryFile = registryFile;
        this.outputDir = outputDir;
        this.limiter = limiter;
    }

    public void run() {
        out.println("=== eGov Document Verifier ===");
        try {
            boolean running = true;
            while (running) {
                printMenu();
                String choice = prompt("Choose an option").trim();
                try {
                    switch (choice) {
                        case "1" -> issueDocument();
                        case "2" -> verifyDocument();
                        case "3" -> revokeDocument();
                        case "4" -> showRegistry();
                        case "5" -> runDemo();
                        case "6" -> runLockoutDemo();
                        case "0" -> running = false;
                        default -> out.println("Unknown option, please try again.");
                    }
                } catch (DocVerifyException e) {
                    out.println("Error: " + e.getMessage());
                }
            }
        } catch (NoSuchElementException e) {
            out.println();
            out.println("Input closed.");
        }
        out.println("Goodbye!");
    }

    private void printMenu() {
        out.println();
        out.println("1. Issue a document");
        out.println("2. Verify a document file");
        out.println("3. Revoke a document");
        out.println("4. Show registry");
        out.println("5. Run demo (issue, verify, tamper, revoke)");
        out.println("6. Run lockout demo (repeated failed checks)");
        out.println("0. Exit");
    }

    private void issueDocument() throws DocVerifyException {
        DocumentType[] types = DocumentType.values();
        out.println("Document types:");
        for (int i = 0; i < types.length; i++) {
            out.printf("  %d. %s%n", i + 1, types[i].getTitle());
        }
        DocumentType type = types[readInt("Type number", 1, types.length) - 1];
        String iin = prompt("Owner IIN (12 digits)").trim();
        String name = prompt("Owner full name").trim();
        String content = prompt("Document text (one line)").trim();
        int days = readIntOrDefault("Validity in days [30]", 30, 1, 3650);

        SignedDocument signed = issuance.issue(type, iin, name, content, days);
        Path file = outputDir.resolve(signed.document().id() + ".edoc");
        DocumentCodec.write(file, signed);
        registry.save(registryFile);
        out.println("Issued. Document ID: " + signed.document().id());
        out.println("File saved to: " + file);
    }

    private void verifyDocument() throws DocVerifyException {
        listOutputFiles();
        String text = prompt("Path to .edoc file").trim();
        if (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
            text = text.substring(1, text.length() - 1); // path dragged into the terminal
        }
        Path file;
        try {
            file = Path.of(text);
        } catch (InvalidPathException e) {
            throw new DocVerifyException("Invalid path: " + text, e);
        }
        printResult(verification.verifyFile(file));
    }

    private void revokeDocument() throws DocVerifyException {
        String id = prompt("Document ID to revoke").trim();
        if (registry.revoke(id)) {
            registry.save(registryFile);
            out.println("Document revoked.");
        } else {
            out.println("No document with this ID in the registry.");
        }
    }

    private void showRegistry() {
        if (registry.size() == 0) {
            out.println("Registry is empty.");
            return;
        }
        out.println("ID                                   | Type                 | Expires              | Status");
        for (RegistryEntry e : registry.list()) {
            out.printf("%-36s | %-20s | %-20s | %s%n", e.id(), e.type(), e.expiresAt(),
                    e.revoked() ? "REVOKED" : "ACTIVE");
        }
    }

    private void runDemo() throws DocVerifyException {
        out.println("--- DEMO: life cycle of one document ---");
        SignedDocument signed = issuance.issue(DocumentType.PENSION_CERTIFICATE, "123456789012",
                "Demo User", "Pension contributions total: 1250000 KZT", 30);
        Path original = outputDir.resolve("demo-original.edoc");
        Path forged = outputDir.resolve("demo-forged.edoc");
        DocumentCodec.write(original, signed);
        registry.save(registryFile);
        out.println("1) Document issued: " + original);

        out.println("2) Verifying the original file:");
        printResult(verification.verifyFile(original));

        try {
            String text = Files.readString(original, StandardCharsets.UTF_8);
            Files.writeString(forged, text.replace("1250000", "9999999"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DocVerifyException("Demo failed: " + e.getMessage(), e);
        }
        out.println("3) Someone changed the amount 1250000 -> 9999999 in a copy: " + forged);
        printResult(verification.verifyFile(forged));

        registry.revoke(signed.document().id());
        registry.save(registryFile);
        out.println("4) The issuer revoked the original document.");
        printResult(verification.verifyFile(original));
        out.println("--- DEMO finished ---");
    }

    private void runLockoutDemo() throws DocVerifyException {
        out.println("--- LOCKOUT DEMO: 3 failed checks block verification for 5 minutes ---");
        limiter.reset(VerificationService.LIMITER_KEY);
        SignedDocument signed = issuance.issue(DocumentType.PENSION_CERTIFICATE, "123456789012",
                "Demo User", "Pension contributions total: 1250000 KZT", 30);
        registry.save(registryFile);
        Path forged = outputDir.resolve("demo-lockout-forged.edoc");
        try {
            String text = DocumentCodec.encode(signed).replace("1250000", "9999999");
            Files.createDirectories(outputDir);
            Files.writeString(forged, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DocVerifyException("Demo failed: " + e.getMessage(), e);
        }
        for (int attempt = 1; attempt <= 4; attempt++) {
            out.println("Attempt " + attempt + ": checking a forged file");
            try {
                out.println("  Result: " + verification.verifyFile(forged).status());
            } catch (DocVerifyException e) {
                out.println("  Error: " + e.getMessage());
            }
        }
        limiter.reset(VerificationService.LIMITER_KEY); // so the menu works normally afterwards
        out.println("--- LOCKOUT DEMO finished (lockout cleared) ---");
    }

    private void printResult(VerificationResult result) {
        out.println("Result : " + result.status() + (result.isValid() ? "  [OK]" : "  [WARNING]"));
        out.println("Details: " + result.status().getDescription());
        EDocument d = result.document();
        out.println(result.isValid() ? "Document data:" : "Data found in the file (NOT TRUSTED):");
        out.println("  ID      : " + d.id());
        out.println("  Type    : " + d.type().getTitle());
        out.println("  Owner   : " + d.ownerName() + " (IIN " + d.ownerIin() + ")");
        out.println("  Issued  : " + d.issuedAt());
        out.println("  Expires : " + d.expiresAt());
        out.println("  Text    : " + d.content());
    }

    private void listOutputFiles() {
        if (!Files.isDirectory(outputDir)) {
            return;
        }
        try (Stream<Path> files = Files.list(outputDir)) {
            out.println("Files in " + outputDir + ":");
            files.filter(p -> p.getFileName().toString().endsWith(".edoc"))
                    .sorted().limit(15).forEach(p -> out.println("  " + p));
        } catch (IOException e) {
            // listing is only a hint, ignore errors
        }
    }

    private String prompt(String text) {
        out.print(text + ": ");
        out.flush();
        return in.nextLine();
    }

    private int readInt(String text, int min, int max) {
        while (true) {
            String line = prompt(text + " (" + min + "-" + max + ")").trim();
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            out.println("Please enter a number from " + min + " to " + max + ".");
        }
    }

    private int readIntOrDefault(String text, int defaultValue, int min, int max) {
        while (true) {
            String line = prompt(text).trim();
            if (line.isEmpty()) {
                return defaultValue;
            }
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            out.println("Please enter a number from " + min + " to " + max + ".");
        }
    }
}
