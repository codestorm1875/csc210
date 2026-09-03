package serp.search;

import serp.model.Paper;
import serp.model.RankedPaper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A small, explainable TF-IDF search engine with domain-specific query expansion. */
public final class SemanticSearchEngine {
    private static final Map<String, Set<String>> SYNONYMS = Map.ofEntries(
            Map.entry("crime", Set.of("incident", "safety", "police", "emergency")),
            Map.entry("report", Set.of("reporting", "complaint", "evidence", "witness")),
            Map.entry("feature", Set.of("function", "capability", "system", "platform")),
            Map.entry("deep", Set.of("neural", "learning", "model")),
            Map.entry("learning", Set.of("neural", "training", "model")),
            Map.entry("heading", Set.of("section", "subheading", "structure")),
            Map.entry("paper", Set.of("journal", "study", "research")),
            Map.entry("anonymous", Set.of("anonymity", "privacy")),
            Map.entry("location", Set.of("gps", "geolocation", "geotag", "map")));

    private final List<Paper> papers;
    private final Map<String, Double> inverseDocumentFrequency;
    private final ExtractiveSummarizer summarizer = new ExtractiveSummarizer();

    public SemanticSearchEngine(List<Paper> papers) {
        this.papers = List.copyOf(papers);
        this.inverseDocumentFrequency = calculateIdf(papers);
    }

    public List<RankedPaper> search(String query, Paper.Category category, int limit) {
        Set<String> queryTerms = expandQuery(query);
        Map<String, Double> queryVector = vector(new ArrayList<>(queryTerms));

        return papers.stream()
                .filter(paper -> category == null || paper.category() == category)
                .map(paper -> new RankedPaper(
                        paper,
                        cosine(queryVector, vector(TextProcessor.tokens(paper.searchableText()))),
                        summarizer.summarize(paper.content(), queryTerms)))
                .filter(result -> queryTerms.isEmpty() || result.relevance() > 0.0)
                .sorted(Comparator.comparingDouble(RankedPaper::relevance).reversed()
                        .thenComparing(result -> result.paper().title()))
                .limit(limit)
                .toList();
    }

    private Set<String> expandQuery(String query) {
        Set<String> expanded = new LinkedHashSet<>(TextProcessor.tokens(query));
        List<String> original = List.copyOf(expanded);
        for (String term : original) {
            for (String synonym : SYNONYMS.getOrDefault(term, Set.of())) {
                expanded.addAll(TextProcessor.tokens(synonym));
            }
        }
        return expanded;
    }

    private Map<String, Double> calculateIdf(List<Paper> documents) {
        Map<String, Integer> documentFrequency = new HashMap<>();
        for (Paper paper : documents) {
            Set<String> unique = new HashSet<>(TextProcessor.tokens(paper.searchableText()));
            unique.forEach(term -> documentFrequency.merge(term, 1, Integer::sum));
        }
        Map<String, Double> result = new HashMap<>();
        documentFrequency.forEach((term, frequency) ->
                result.put(term, Math.log((documents.size() + 1.0) / (frequency + 1.0)) + 1.0));
        return Map.copyOf(result);
    }

    private Map<String, Double> vector(List<String> terms) {
        Map<String, Integer> frequency = new HashMap<>();
        terms.forEach(term -> frequency.merge(term, 1, Integer::sum));
        Map<String, Double> result = new HashMap<>();
        frequency.forEach((term, count) ->
                result.put(term, count * inverseDocumentFrequency.getOrDefault(term, 1.0)));
        return result;
    }

    private double cosine(Map<String, Double> left, Map<String, Double> right) {
        double dot = 0.0;
        for (Map.Entry<String, Double> entry : left.entrySet()) {
            dot += entry.getValue() * right.getOrDefault(entry.getKey(), 0.0);
        }
        double leftLength = Math.sqrt(left.values().stream().mapToDouble(value -> value * value).sum());
        double rightLength = Math.sqrt(right.values().stream().mapToDouble(value -> value * value).sum());
        return leftLength == 0.0 || rightLength == 0.0 ? 0.0 : dot / (leftLength * rightLength);
    }
}
