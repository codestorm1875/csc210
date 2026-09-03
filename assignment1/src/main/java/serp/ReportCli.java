package serp;

import serp.analysis.AnalysisReport;
import serp.analysis.AnalysisService;
import serp.data.PaperRepository;
import serp.model.FrequencyItem;
import serp.model.Paper;

import java.util.List;

/** Headless companion that makes the GUI's calculated values easy to audit. */
public final class ReportCli {
    private ReportCli() {
    }

    public static void main(String[] args) {
        List<Paper> papers = new PaperRepository().load();
        try (AnalysisService service = new AnalysisService(4)) {
            AnalysisReport report = service.analyze(papers, ignored -> { });
            print("CRIME-REPORTING FEATURES", report.crimeFeatures());
            print("DEEP-LEARNING SUBHEADINGS", report.deepLearningHeadings());
            System.out.println("\nWorkers: " + String.join(", ", report.workerThreads()));
            System.out.println("Elapsed: " + report.elapsed().toMillis() + " ms");
        }
    }

    private static void print(String heading, List<FrequencyItem> items) {
        System.out.println("\n" + heading);
        System.out.printf("%-5s %-42s %6s %10s%n", "Rank", "Item", "Papers", "Coverage");
        for (int index = 0; index < items.size(); index++) {
            FrequencyItem item = items.get(index);
            System.out.printf("%-5d %-42s %6d %9.1f%%%n",
                    index + 1, item.label(), item.count(), item.percentage());
        }
    }
}
