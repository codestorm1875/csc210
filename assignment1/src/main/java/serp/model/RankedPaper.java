package serp.model;

public record RankedPaper(Paper paper, double relevance, String summary) {
}
