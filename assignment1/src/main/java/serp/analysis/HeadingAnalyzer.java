package serp.analysis;

import serp.search.TextProcessor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Normalizes stylistic heading variants so equivalent paper sections are counted together. */
public final class HeadingAnalyzer {
    public Set<String> normalizeAll(List<String> headings) {
        Set<String> normalized = new LinkedHashSet<>();
        headings.stream()
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .forEach(normalized::add);
        return Set.copyOf(normalized);
    }

    public String normalize(String heading) {
        String value = TextProcessor.normalize(heading);
        if (value.isBlank() || value.equals("abstract")) return "";
        if (value.contains("introduction")) return "Introduction";
        if (containsAny(value, "related work", "literature review", "background")) return "Background / Related Work";
        if (containsAny(value, "dataset", "data collection")) return "Dataset / Data Collection";
        if (value.contains("preprocessing")) return "Data Preprocessing";
        if (containsAny(value, "model architecture", "network architecture", "bert architecture", "efficientnet architecture")) return "Model Architecture";
        if (containsAny(value, "methodology", "proposed method", "review methodology", "problem formulation", "general design pipeline")) return "Methodology / Proposed Method";
        if (containsAny(value, "training", "pre training", "fine tuning", "implementation")) return "Training / Implementation";
        if (containsAny(value, "experimental setup", "experimental settings", "experiments")) return "Experiments / Setup";
        if (value.contains("ablation")) return "Ablation Study";
        if (containsAny(value, "comparison with baseline", "evaluation metrics")) return "Evaluation / Baseline Comparison";
        if (value.contains("result")) return "Results";
        if (containsAny(value, "discussion", "analysis", "advantages and disadvantages")) return "Discussion / Analysis";
        if (value.contains("limitation")) return "Limitations";
        if (containsAny(value, "future work", "future direction")) return "Future Work";
        if (value.contains("conclusion")) return "Conclusion";
        return titleCase(value);
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) return true;
        }
        return false;
    }

    private String titleCase(String value) {
        StringBuilder result = new StringBuilder();
        for (String word : value.split(" ")) {
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
}
