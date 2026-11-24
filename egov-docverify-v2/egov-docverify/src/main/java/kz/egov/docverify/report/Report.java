package kz.egov.docverify.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import kz.egov.docverify.model.RegistryEntry;

/** Абстрактты класс (Template Method): файлға жазуды базалық класс жасайды, пішімді мұрагер таңдайды. */
public abstract class Report {

    /** Мәтінді жасау (CSV немесе JSON). */
    protected abstract String render(List<RegistryEntry> entries);

    /** Файл кеңейтімі, мысалы "csv". */
    public abstract String extension();

    public final Path export(List<RegistryEntry> entries, Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, render(entries), StandardCharsets.UTF_8);
        return file;
    }
}
