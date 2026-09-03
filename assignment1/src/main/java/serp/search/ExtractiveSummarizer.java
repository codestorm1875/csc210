package serp.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Selects the sentences with the greatest semantic overlap with the query. */
public final class ExtractiveSummarizer {
    private static final int MAX_LENGTH = 320;

    public String summarize(String content, Set<String> queryTerms) {
        String[] sentences = content.trim().split("(?<=[.!?])\\s+");
        if (sentences.length <= 2) {
            return abbreviate(content.trim());
        }

        List<ScoredSentence> scored = new ArrayList<>();
        for (int index = 0; index < sentences.length; index++) {
            Set<String> sentenceTerms = new HashSet<>(TextProcessor.tokens(sentences[index]));
            long matches = queryTerms.stream().filter(sentenceTerms::contains).count();
            double coverage = sentenceTerms.isEmpty() ? 0.0 : (double) matches / sentenceTerms.size();
            double positionBonus = index == 0 ? 0.18 : 0.0;
            scored.add(new ScoredSentence(index, sentences[index], matches + coverage + positionBonus));
        }

        List<ScoredSentence> selected = scored.stream()
                .sorted(Comparator.comparingDouble(ScoredSentence::score).reversed())
                .limit(2)
                .sorted(Comparator.comparingInt(ScoredSentence::index))
                .toList();
        return abbreviate(String.join(" ", selected.stream().map(ScoredSentence::text).toList()));
    }

    private String abbreviate(String value) {
        if (value.length() <= MAX_LENGTH) {
            return value;
        }
        int boundary = value.lastIndexOf(' ', MAX_LENGTH - 3);
        return value.substring(0, Math.max(boundary, MAX_LENGTH / 2)) + "...";
    }

    private record ScoredSentence(int index, String text, double score) {
    }
}
