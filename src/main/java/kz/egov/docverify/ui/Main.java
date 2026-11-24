package kz.egov.docverify.ui;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Clock;
import java.util.Arrays;

import kz.egov.docverify.crypto.KeyManager;
import kz.egov.docverify.crypto.RsaSigner;
import kz.egov.docverify.exception.DocVerifyException;
import kz.egov.docverify.service.IssuanceService;
import kz.egov.docverify.service.VerificationService;
import kz.egov.docverify.storage.Database;
import kz.egov.docverify.storage.JdbcAuditDao;
import kz.egov.docverify.storage.JdbcRegistryDao;

/** Бағдарламаның кіру нүктесі. Іске қосу: Main.java -> Run. ASCII режимі үшін: --ascii */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws IOException {
        boolean ascii = Arrays.asList(args).contains("--ascii") || System.getenv("DOCVERIFY_ASCII") != null;
        enableUtf8Console();
        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        Ui ui = new Ui(System.in, out, ascii);

        ui.println("Электрондық анықтаманы тексеру жүйесі");
        String password = ui.ask("Кілтті қорғайтын құпиясөз (бірінші рет жаңа кілт жасалады): ");
        if (password.isEmpty()) {
            ui.println("ҚАТЕ: құпиясөз бос болмауы тиіс.");
            return;
        }
        Files.createDirectories(Path.of("data"));
        try (Database db = new Database("jdbc:sqlite:data/registry.db")) {
            KeyPair keys = KeyManager.loadOrCreate(Path.of("keys"), password.toCharArray());
            Clock clock = Clock.systemDefaultZone();
            JdbcRegistryDao registry = new JdbcRegistryDao(db);
            RsaSigner signer = new RsaSigner();
            IssuanceService issuance = new IssuanceService(signer, keys.getPrivate(), registry,
                    new JdbcAuditDao(db), db, clock);
            VerificationService verification = new VerificationService(signer, keys.getPublic(), registry, clock);
            new ConsoleApp(ui, issuance, verification, registry, clock, Path.of("out"), Path.of("reports")).run();
        } catch (DocVerifyException e) {
            ui.println("ҚАТЕ: " + e.getMessage());
        }
    }

    /** Windows консолін UTF-8 (65001) кодтауына ауыстырады, әйтпесе қазақ әріптері бұзылады. */
    private static void enableUtf8Console() {
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            try {
                new ProcessBuilder("cmd", "/c", "chcp 65001 >nul").inheritIO().start().waitFor();
            } catch (IOException ignored) {
                // кодтауды ауыстыру мүмкін болмады: 9-мәзірдегі ASCII режимі көмектеседі
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
