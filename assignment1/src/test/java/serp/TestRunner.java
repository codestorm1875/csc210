package serp;

import serp.analysis.AnalysisReport;
import serp.analysis.AnalysisService;
import serp.analysis.CrimeFeatureCatalog;
import serp.analysis.HeadingAnalyzer;
import serp.data.PaperRepository;
import serp.model.FrequencyItem;
import serp.model.Paper;
import serp.model.RankedPaper;
import serp.search.SemanticSearchEngine;
import serp.ui.BarChartPanel;
import serp.util.CsvExporter;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public final class TestRunner {
    private int passed;

    public static void main(String[] args) throws Exception {
        new TestRunner().run();
    }

    private void run() throws Exception {
        List<Paper> papers = new PaperRepository().load();
        testCorpus(papers);
        testSemanticSearch(papers);
        testFeatureExtraction();
        testHeadingNormalization();
        AnalysisReport report = testConcurrentAnalysis(papers);
        testCsvExport(report.crimeFeatures());
        testChartRendering(report.crimeFeatures());
        System.out.println("All " + passed + " tests passed.");
    }

    private void testCorpus(List<Paper> papers) {
        check(papers.size() == 24, "corpus contains 24 records");
        check(papers.stream().filter(p -> p.category() == Paper.Category.CRIME_REPORTING).count() == 12,
                "corpus contains 12 crime systems");
        check(papers.stream().filter(p -> p.category() == Paper.Category.DEEP_LEARNING).count() == 12,
                "corpus contains 12 deep-learning papers");
        check(papers.stream().map(Paper::id).distinct().count() == papers.size(), "paper IDs are unique");
    }

    private void testSemanticSearch(List<Paper> papers) {
        SemanticSearchEngine engine = new SemanticSearchEngine(papers);
        List<RankedPaper> results = engine.search(
                "anonymous crime location reporting", Paper.Category.CRIME_REPORTING, 10);
        check(!results.isEmpty(), "semantic search returns matches");
        check(results.stream().allMatch(r -> r.paper().category() == Paper.Category.CRIME_REPORTING),
                "search respects category filter");
        check(isDescending(results.stream().map(RankedPaper::relevance).toList()),
                "search results are relevance-ranked");
        check(results.stream().allMatch(r -> !r.summary().isBlank() && r.summary().length() <= 323),
                "search results have bounded summaries");
        check(!engine.search("neural architecture", Paper.Category.DEEP_LEARNING, 5).isEmpty(),
                "query expansion retrieves deep-learning papers");
    }

    private void testFeatureExtraction() {
        Set<String> features = new CrimeFeatureCatalog().detect(
                "Anonymous reporting sends GPS and photo evidence with SMS, encryption and a panic button.");
        check(features.contains("Anonymous reporting"), "detects anonymity synonym");
        check(features.contains("GPS / geolocation mapping"), "detects GPS feature");
        check(features.contains("Multimedia evidence"), "detects photo evidence");
        check(features.contains("Offline / SMS / USSD access"), "detects SMS access");
        check(features.contains("SOS / panic button"), "detects panic button");
    }

    private void testHeadingNormalization() {
        HeadingAnalyzer analyzer = new HeadingAnalyzer();
        check(analyzer.normalize("Literature Review").equals("Background / Related Work"),
                "normalizes literature review");
        check(analyzer.normalize("Experimental Settings").equals("Experiments / Setup"),
                "normalizes experiment heading");
        check(analyzer.normalize("Ablation Studies").equals("Ablation Study"),
                "normalizes plural ablation heading");
        check(analyzer.normalize("Abstract").isBlank(), "excludes abstract from subheadings");
    }

    private AnalysisReport testConcurrentAnalysis(List<Paper> papers) {
        AnalysisReport report;
        try (AnalysisService service = new AnalysisService(4)) {
            report = service.analyze(papers, ignored -> { });
        }
        check(report.crimeFeatures().size() >= 10, "finds at least 10 distinctive features");
        check(report.deepLearningHeadings().size() >= 12, "finds at least 12 distinct subheadings");
        check(isDescending(report.crimeFeatures().stream().map(item -> (double) item.count()).toList()),
                "feature counts are descending");
        check(report.workerThreads().stream().allMatch(name -> name.startsWith("paper-worker-")),
                "analysis ran on named worker threads");
        check(report.workerThreads().size() >= 2, "analysis used multiple worker threads");
        return report;
    }

    private void testCsvExport(List<FrequencyItem> items) throws Exception {
        Path file = Files.createTempFile("serp-features-", ".csv");
        new CsvExporter().write(file, "feature", items);
        String csv = Files.readString(file);
        Files.deleteIfExists(file);
        check(csv.startsWith("rank,\"feature\",paper_count,coverage_percent"), "CSV has expected header");
        check(csv.lines().count() == items.size() + 1L, "CSV exports every result");
    }

    private void testChartRendering(List<FrequencyItem> items) {
        BarChartPanel chart = new BarChartPanel();
        chart.setData(items, "Number of papers", new java.awt.Color(13, 116, 116));
        chart.setSize(760, chart.getPreferredSize().height);
        BufferedImage image = new BufferedImage(chart.getWidth(), chart.getHeight(), BufferedImage.TYPE_INT_RGB);
        chart.paint(image.getGraphics());
        int background = image.getRGB(2, 2);
        boolean hasInk = false;
        outer:
        for (int y = 20; y < image.getHeight(); y += 4) {
            for (int x = 20; x < image.getWidth(); x += 4) {
                if (image.getRGB(x, y) != background) {
                    hasInk = true;
                    break outer;
                }
            }
        }
        check(hasInk, "bar chart renders nonblank pixels");
    }

    private boolean isDescending(List<Double> values) {
        for (int index = 1; index < values.size(); index++) {
            if (values.get(index) > values.get(index - 1)) return false;
        }
        return true;
    }

    private void check(boolean condition, String name) {
        if (!condition) throw new AssertionError("FAILED: " + name);
        passed++;
        System.out.println("PASS  " + name);
    }
}
