package kz.egov.docverify.ui;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/** Консоль: UTF-8 кіріс/шығыс, қажет болса ASCII (транслит) режимі. */
public final class Ui {
    private final PrintStream out;
    private final Scanner in;
    private boolean ascii;

    public Ui(InputStream input, PrintStream output, boolean ascii) {
        this.in = new Scanner(input, StandardCharsets.UTF_8);
        this.out = output;
        this.ascii = ascii;
    }

    public void println(String s) {
        out.println(ascii ? Translit.toAscii(s) : s);
    }

    public void print(String s) {
        out.print(ascii ? Translit.toAscii(s) : s);
        out.flush();
    }

    public boolean hasInput() {
        return in.hasNextLine();
    }

    public String ask(String prompt) {
        print(prompt);
        return in.hasNextLine() ? in.nextLine().trim() : "";
    }

    public boolean isAscii() {
        return ascii;
    }

    public void setAscii(boolean ascii) {
        this.ascii = ascii;
    }
}
