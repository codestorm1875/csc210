package serp.analysis;

import serp.model.FrequencyItem;

import java.time.Duration;
import java.util.List;
import java.util.Set;

public record AnalysisReport(
        List<FrequencyItem> crimeFeatures,
        List<FrequencyItem> deepLearningHeadings,
        int crimePaperCount,
        int deepLearningPaperCount,
        Set<String> workerThreads,
        Duration elapsed) {

    public AnalysisReport {
        crimeFeatures = List.copyOf(crimeFeatures);
        deepLearningHeadings = List.copyOf(deepLearningHeadings);
        workerThreads = Set.copyOf(workerThreads);
    }
}
