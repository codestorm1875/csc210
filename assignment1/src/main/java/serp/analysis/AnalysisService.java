package serp.analysis;

import serp.model.FrequencyItem;
import serp.model.Paper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

/** Runs one independent extraction task per paper on a bounded worker pool. */
public final class AnalysisService implements AutoCloseable {
    private final ExecutorService executor;
    private final CrimeFeatureCatalog featureCatalog = new CrimeFeatureCatalog();
    private final HeadingAnalyzer headingAnalyzer = new HeadingAnalyzer();

    public AnalysisService(int threadCount) {
        AtomicInteger number = new AtomicInteger();
        executor = Executors.newFixedThreadPool(threadCount, runnable -> {
            Thread thread = new Thread(runnable, "paper-worker-" + number.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    public AnalysisReport analyze(List<Paper> papers, IntConsumer progress) {
        Instant started = Instant.now();
        AtomicInteger completed = new AtomicInteger();
        List<Callable<PaperAnalysis>> tasks = papers.stream()
                .<Callable<PaperAnalysis>>map(paper -> () -> {
                    PaperAnalysis result = analyzePaper(paper);
                    progress.accept(completed.incrementAndGet());
                    return result;
                })
                .toList();

        List<PaperAnalysis> results = new ArrayList<>();
        try {
            List<Future<PaperAnalysis>> futures = executor.invokeAll(tasks);
            for (Future<PaperAnalysis> future : futures) {
                results.add(future.get());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Analysis was interrupted", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("A paper analysis task failed", exception.getCause());
        }

        int crimeCount = (int) papers.stream().filter(p -> p.category() == Paper.Category.CRIME_REPORTING).count();
        int deepCount = papers.size() - crimeCount;
        Map<String, Integer> featureCounts = new HashMap<>();
        Map<String, Integer> headingCounts = new HashMap<>();
        Set<String> threads = new HashSet<>();
        for (PaperAnalysis result : results) {
            result.features().forEach(feature -> featureCounts.merge(feature, 1, Integer::sum));
            result.headings().forEach(heading -> headingCounts.merge(heading, 1, Integer::sum));
            threads.add(result.threadName());
        }

        return new AnalysisReport(
                frequencies(featureCounts, crimeCount),
                frequencies(headingCounts, deepCount),
                crimeCount,
                deepCount,
                threads,
                Duration.between(started, Instant.now()));
    }

    private PaperAnalysis analyzePaper(Paper paper) {
        Set<String> features = paper.category() == Paper.Category.CRIME_REPORTING
                ? featureCatalog.detect(paper.content()) : Set.of();
        Set<String> headings = paper.category() == Paper.Category.DEEP_LEARNING
                ? headingAnalyzer.normalizeAll(paper.headings()) : Set.of();
        return new PaperAnalysis(features, headings, Thread.currentThread().getName());
    }

    private List<FrequencyItem> frequencies(Map<String, Integer> counts, int total) {
        return counts.entrySet().stream()
                .map(entry -> new FrequencyItem(
                        entry.getKey(),
                        entry.getValue(),
                        total == 0 ? 0.0 : entry.getValue() * 100.0 / total))
                .sorted(java.util.Comparator.comparingInt(FrequencyItem::count).reversed()
                        .thenComparing(FrequencyItem::label))
                .toList();
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private record PaperAnalysis(Set<String> features, Set<String> headings, String threadName) {
    }
}
