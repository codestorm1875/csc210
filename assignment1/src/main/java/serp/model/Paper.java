package serp.model;

import java.util.List;

/** A normalized paper record captured from a search-engine results snapshot. */
public record Paper(
        String id,
        Category category,
        String title,
        String authors,
        int year,
        String url,
        List<String> headings,
        String content) {

    public enum Category {
        CRIME_REPORTING,
        DEEP_LEARNING
    }

    public Paper {
        headings = List.copyOf(headings);
    }

    public String searchableText() {
        return title + " " + String.join(" ", headings) + " " + content;
    }
}
