package serp.search;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class TextProcessor {
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
            "has", "in", "is", "it", "of", "on", "or", "that", "the", "their",
            "this", "to", "was", "were", "will", "with", "using", "use");

    private TextProcessor() {
    }

    public static List<String> tokens(String text) {
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return List.of();
        }
        return Arrays.stream(normalized.split("\\s+"))
                .filter(token -> token.length() > 1)
                .filter(token -> !STOP_WORDS.contains(token))
                .map(TextProcessor::stem)
                .toList();
    }

    public static String normalize(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private static String stem(String token) {
        if (token.length() > 5 && token.endsWith("ies")) {
            return token.substring(0, token.length() - 3) + "y";
        }
        if (token.length() > 5 && token.endsWith("ing")) {
            return token.substring(0, token.length() - 3);
        }
        if (token.length() > 4 && token.endsWith("ed")) {
            return token.substring(0, token.length() - 2);
        }
        if (token.length() > 4 && token.endsWith("s") && !token.endsWith("ss")) {
            return token.substring(0, token.length() - 1);
        }
        return token;
    }
}
