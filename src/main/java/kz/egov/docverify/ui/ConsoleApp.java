package kz.egov.docverify.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import kz.egov.docverify.exception.DocVerifyException;
import kz.egov.docverify.model.DocumentType;
import kz.egov.docverify.model.EDocument;
import kz.egov.docverify.model.RegistryEntry;
import kz.egov.docverify.model.SignedDocument;
import kz.egov.docverify.model.VerificationResult;
import kz.egov.docverify.model.VerificationStatus;
import kz.egov.docverify.report.CsvReport;
import kz.egov.docverify.report.JsonReport;
import kz.egov.docverify.report.Report;
import kz.egov.docverify.service.Analytics;
import kz.egov.docverify.service.BatchVerifier;
import kz.egov.docverify.service.IssuanceService;
import kz.egov.docverify.service.VerificationService;
import kz.egov.docverify.storage.DocumentCodec;
import kz.egov.docverify.storage.Repository;

/** Консольдік мәзір (қазақ тілінде). */
public class ConsoleApp {
    private final Ui ui;
    private final IssuanceService issuance;
    private final VerificationService verification;
    private final BatchVerifier batch;
    private final Repository<RegistryEntry, String> registry;
    private final Clock clock;
    private final Path outDir;
    private final Path reportDir;

    public ConsoleApp(Ui ui, IssuanceService issuance, VerificationService verification,
                      Repository<RegistryEntry, String> registry, Clock clock, Path outDir, Path reportDir) {
        this.ui = ui;
        this.issuance = issuance;
        this.verification = verification;
        this.batch = new BatchVerifier(verification);
        this.registry = registry;
        this.clock = clock;
        this.outDir = outDir;
        this.reportDir = reportDir;
    }

    public void run() {
        while (true) {
            printMenu();
            String choice = ui.ask("Таңдауыңыз: ");
            if (!ui.hasInput() && choice.isEmpty()) {
                return;
            }
            try {
                switch (choice) {
                    case "1" -> issue();
                    case "2" -> verifyFile();
                    case "3" -> revoke();
                    case "4" -> listRegistry();
                    case "5" -> statistics();
                    case "6" -> export();
                    case "7" -> batchVerify();
                    case "8" -> demo();
                    case "9" -> toggleAscii();
                    case "0" -> {
                        ui.println("Сау болыңыз!");
                        return;
                    }
                    default -> ui.println("Белгісіз таңдау. 0-ден 9-ға дейінгі санды енгізіңіз.");
                }
            } catch (DocVerifyException e) {
                ui.println("ҚАТЕ: " + e.getMessage());
            } catch (IOException e) {
                ui.println("ФАЙЛ ҚАТЕСІ: " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        ui.println("");
        ui.println("===== ЭЛЕКТРОНДЫҚ АНЫҚТАМАНЫ ТЕКСЕРУ =====");
        ui.println("1. Анықтама беру (RSA қолтаңбасымен)");
        ui.println("2. Анықтама файлын тексеру");
        ui.println("3. Анықтаманы кері қайтару");
        ui.println("4. Тізілімді көрсету");
        ui.println("5. Статистика (Stream API)");
        ui.println("6. Есепті экспорттау (CSV / JSON)");
        ui.println("7. Папканы көп ағынмен тексеру");
        ui.println("8. Демо (барлығын автоматты түрде көрсету)");
        ui.println("9. ASCII режимі (әріптер дұрыс көрінбесе): " + (ui.isAscii() ? "қосулы" : "өшірулі"));
        ui.println("0. Шығу");
    }

    private void issue() throws IOException {
        String id = ui.ask("Анықтама ID (мысалы, DOC-001): ");
        ui.println("Түрі:");
        DocumentType[] types = DocumentType.values();
        for (int i = 0; i < types.length; i++) {
            ui.println("  " + (i + 1) + ". " + types[i].titleKk());
        }
        int t = parseInt(ui.ask("Түр нөмірі: "), "Түр нөмірі сан болуы тиіс.");
        if (t < 1 || t > types.length) {
            throw new DocVerifyException("Түр нөмірі 1 мен " + types.length + " аралығында болуы тиіс.");
        }
        String name = ui.ask("Азаматтың аты-жөні: ");
        String iin = ui.ask("ЖСН (12 сан): ");
        String content = ui.ask("Мазмұны: ");
        int days = parseInt(ui.ask("Жарамдылық мерзімі (күн саны, мысалы 30): "), "Күн саны сан болуы тиіс.");
        LocalDate today = LocalDate.now(clock);
        EDocument doc = new EDocument(id, types[t - 1], name, iin, content, today, today.plusDays(days));
        Path file = saveToFile(issuance.issue(doc));
        ui.println("Анықтама берілді. Файл сақталды: " + file);
    }

    private Path saveToFile(SignedDocument sd) throws IOException {
        Files.createDirectories(outDir);
        Path file = outDir.resolve(sd.document().id() + ".edoc");
        Files.writeString(file, DocumentCodec.encode(sd), StandardCharsets.UTF_8);
        return file;
    }

    private void verifyFile() {
        String path = ui.ask("Файл жолы (мысалы, out/DOC-001.edoc): ");
        printResult(verification.verifyFile(Path.of(path)));
    }

    private void printResult(VerificationResult r) {
        ui.println("Нәтиже: " + r.status().titleKk() + "  [" + r.status().name() + "]");
        ui.println("ID: " + r.documentId());
        ui.println(r.message());
        if (r.status() == VerificationStatus.TAMPERED) {
            ui.println("СЕНІМСІЗ: бұл файлдың мазмұнына сенуге болмайды!");
        }
    }

    private void revoke() {
        String id = ui.ask("Кері қайтарылатын анықтама ID: ");
        issuance.revoke(id);
        ui.println("Анықтама кері қайтарылды: " + id);
    }

    private void listRegistry() {
        List<RegistryEntry> all = registry.findAll();
        if (all.isEmpty()) {
            ui.println("Тізілім бос.");
            return;
        }
        LocalDate today = LocalDate.now(clock);
        ui.println(String.format("%-20s %-14s %-12s %-12s %s", "ID", "ТҮРІ", "БЕРІЛГЕН", "МЕРЗІМІ", "КҮЙІ"));
        for (RegistryEntry e : all) {
            ui.println(String.format("%-20s %-14s %-12s %-12s %s", e.id(), e.type().name(), e.issuedOn(),
                    e.validUntil(), Analytics.effectiveStatus(e, today).titleKk()));
        }
    }

    private void statistics() {
        Analytics.summary(registry.findAll(), LocalDate.now(clock)).forEach(ui::println);
    }

    private void export() throws IOException {
        String format = ui.ask("Пішім (csv немесе json): ").toLowerCase();
        Report report = switch (format) {
            case "csv" -> new CsvReport();
            case "json" -> new JsonReport();
            default -> throw new DocVerifyException("Тек csv немесе json таңдауға болады.");
        };
        Path file = report.export(registry.findAll(), reportDir.resolve("registry." + report.extension()));
        ui.println("Есеп сақталды: " + file);
    }

    private void batchVerify() throws IOException {
        String folder = ui.ask("Папка жолы (Enter = " + outDir + "): ");
        Path dir = folder.isEmpty() ? outDir : Path.of(folder);
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(p -> p.toString().endsWith(".edoc")).sorted().toList();
        }
        if (files.isEmpty()) {
            ui.println("Папкада .edoc файлдары жоқ.");
            return;
        }
        long start = System.nanoTime();
        BatchVerifier.BatchResult res = batch.verifyAll(files, 4);
        long ms = (System.nanoTime() - start) / 1_000_000;
        res.results().forEach((name, r) -> ui.println(String.format("%-28s %s", name, r.status().titleKk())));
        ui.println("Тексерілді: " + files.size() + " файл, ағын саны: " + res.threadNames().size()
                + ", уақыты: " + ms + " мс");
    }

    private void toggleAscii() {
        ui.setAscii(!ui.isAscii());
        ui.println("ASCII режимі: " + (ui.isAscii() ? "қосулы" : "өшірулі"));
    }

    private void demo() throws IOException {
        LocalDate today = LocalDate.now(clock);
        String id = "DEMO-" + (System.currentTimeMillis() % 100000);
        ui.println("--- 1. Анықтама беру ---");
        EDocument doc = new EDocument(id, DocumentType.INCOME, "Айгерім Сәрсенова", "040101500123",
                "Айлық табысы: 450000 теңге", today, today.plusDays(30));
        Path file = saveToFile(issuance.issue(doc));
        ui.println("Файл: " + file);

        ui.println("--- 2. Түпнұсқаны тексеру ---");
        printResult(verification.verifyFile(file));

        ui.println("--- 3. Файлды өзгерту (450000 -> 950000) және тексеру ---");
        Path fake = outDir.resolve(id + "-fake.edoc");
        Files.writeString(fake, Files.readString(file, StandardCharsets.UTF_8).replace("450000", "950000"),
                StandardCharsets.UTF_8);
        printResult(verification.verifyFile(fake));

        ui.println("--- 4. Анықтаманы кері қайтару және тексеру ---");
        issuance.revoke(id);
        printResult(verification.verifyFile(file));

        ui.println("--- 5. Статистика ---");
        statistics();

        ui.println("--- 6. Папканы көп ағынмен тексеру ---");
        List<Path> files;
        try (Stream<Path> s = Files.list(outDir)) {
            files = s.filter(p -> p.toString().endsWith(".edoc")).sorted().toList();
        }
        BatchVerifier.BatchResult res = batch.verifyAll(files, 4);
        res.results().forEach((n, r) -> ui.println(String.format("%-28s %s", n, r.status().titleKk())));
        ui.println("Қатысқан ағындар: " + res.threadNames().size());

        ui.println("--- 7. Есепті экспорттау ---");
        for (Report r : List.of(new CsvReport(), new JsonReport())) {
            ui.println("Сақталды: " + r.export(registry.findAll(), reportDir.resolve("registry." + r.extension())));
        }
    }

    private static int parseInt(String s, String error) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new DocVerifyException(error);
        }
    }
}
