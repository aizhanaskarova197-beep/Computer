package kz.egov.docverify.ui;

/**
 * Қазақ кириллицасын латынға ауыстырады. Тек консоль әріптерді дұрыс көрсетпесе
 * (кракозябр шықса) қолданылатын запас режимі: ASCII режимі.
 */
public final class Translit {
    private static final String FROM = "аәбвгғдеёжзийкқлмнңоөпрстуұүфхһцчшщъыіьэюя";
    private static final String[] TO = {
        "a", "a", "b", "v", "g", "gh", "d", "e", "yo", "zh", "z", "i", "y", "k", "q", "l", "m", "n", "ng",
        "o", "o", "p", "r", "s", "t", "u", "u", "u", "f", "kh", "h", "ts", "ch", "sh", "shch", "", "y", "i",
        "", "e", "yu", "ya"
    };

    private Translit() {
    }

    public static String toAscii(String text) {
        StringBuilder sb = new StringBuilder();
        for (char ch : text.toCharArray()) {
            char lower = Character.toLowerCase(ch);
            int idx = FROM.indexOf(lower);
            if (idx < 0) {
                sb.append(ch);
            } else {
                String t = TO[idx];
                sb.append(Character.isUpperCase(ch) && !t.isEmpty()
                        ? Character.toUpperCase(t.charAt(0)) + t.substring(1)
                        : t);
            }
        }
        return sb.toString();
    }
}
