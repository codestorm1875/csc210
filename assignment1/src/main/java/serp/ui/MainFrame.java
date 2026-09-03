package serp.ui;

import serp.analysis.AnalysisReport;
import serp.analysis.AnalysisService;
import serp.data.PaperRepository;
import serp.model.FrequencyItem;
import serp.model.Paper;
import serp.model.RankedPaper;
import serp.search.SemanticSearchEngine;
import serp.util.CsvExporter;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

public final class MainFrame extends JFrame {
    private final List<Paper> papers = new PaperRepository().load();
    private final SemanticSearchEngine searchEngine = new SemanticSearchEngine(papers);
    private final int threadCount = Math.min(4, Math.max(2, Runtime.getRuntime().availableProcessors()));
    private final AnalysisService analysisService = new AnalysisService(threadCount);

    private final JTextField queryField = new JTextField("crime reporting system features", 34);
    private final JComboBox<CategoryOption> categoryBox = new JComboBox<>();
    private final DefaultListModel<RankedPaper> searchModel = new DefaultListModel<>();
    private final JLabel searchCount = mutedLabel("Ready");

    private final FrequencyTableModel crimeModel = new FrequencyTableModel("Distinctive feature");
    private final FrequencyTableModel headingModel = new FrequencyTableModel("Normalized subheading");
    private final BarChartPanel crimeChart = new BarChartPanel();
    private final BarChartPanel headingChart = new BarChartPanel();
    private final JLabel crimeMetric = metricValue("-");
    private final JLabel headingMetric = metricValue("-");
    private final JLabel featureMetric = metricValue("-");
    private final JLabel threadMetric = metricValue("-");
    private final JLabel statusLabel = mutedLabel("Preparing analysis...");
    private final JProgressBar progressBar = new JProgressBar(0, papers.size());
    private final JButton analyzeButton = primaryButton("Run analysis");

    public MainFrame() {
        super("SERP Insight");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(980, 680));
        setSize(1240, 800);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BACKGROUND);
        setLayout(new BorderLayout());

        add(createHeader(), BorderLayout.NORTH);
        add(createTabs(), BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                analysisService.close();
                dispose();
            }
        });

        configureCategories();
        queryField.addActionListener(event -> runSearch());
        analyzeButton.addActionListener(event -> runAnalysis());
        SwingUtilities.invokeLater(() -> {
            runSearch();
            runAnalysis();
        });
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setBackground(Theme.SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(13, 22, 13, 22)));

        JLabel brand = new JLabel("SERP Insight");
        brand.setFont(Theme.HEADING.deriveFont(19f));
        brand.setForeground(Theme.TEXT);
        JLabel subtitle = new JLabel("Research intelligence workspace");
        subtitle.setFont(Theme.SMALL);
        subtitle.setForeground(Theme.MUTED);
        JPanel identity = new JPanel(new GridLayout(2, 1, 0, 2));
        identity.setOpaque(false);
        identity.add(brand);
        identity.add(subtitle);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        JLabel corpus = mutedLabel("24 papers indexed");
        corpus.setForeground(Theme.SUCCESS);
        corpus.setFont(Theme.SMALL.deriveFont(Font.BOLD));
        actions.add(corpus);
        analyzeButton.setPreferredSize(new Dimension(128, 36));
        actions.add(analyzeButton);
        header.add(identity, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JTabbedPane createTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.BODY);
        tabs.setBorder(BorderFactory.createEmptyBorder(0, 18, 8, 18));
        UiKit.styleTabs(tabs);
        tabs.addTab("Search summaries", createSearchPanel());
        tabs.addTab("Crime features", createAnalysisPanel(
                "Crime-reporting systems",
                "Ranked by the number of systems containing each feature",
                crimeMetric,
                "systems analyzed",
                crimeModel,
                crimeChart,
                Theme.CORAL,
                "crime-features.csv"));
        tabs.addTab("Deep-learning headings", createAnalysisPanel(
                "Journal paper subheadings",
                "Equivalent section names are normalized before counting",
                headingMetric,
                "papers analyzed",
                headingModel,
                headingChart,
                Theme.ACCENT,
                "deep-learning-headings.csv"));
        return tabs;
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 4, 4, 4));

        JPanel controls = new JPanel(new BorderLayout(10, 0));
        controls.setOpaque(false);
        queryField.setFont(Theme.BODY);
        queryField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(9, 11, 9, 11)));
        queryField.setBackground(Theme.SURFACE);
        queryField.setForeground(Theme.TEXT);
        controls.add(queryField, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        categoryBox.setFont(Theme.BODY);
        categoryBox.setPreferredSize(new Dimension(180, 36));
        categoryBox.setBackground(Theme.SURFACE);
        categoryBox.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        JButton searchButton = primaryButton("Search papers");
        searchButton.setPreferredSize(new Dimension(130, 36));
        searchButton.addActionListener(event -> runSearch());
        actions.add(categoryBox);
        actions.add(searchButton);
        controls.add(actions, BorderLayout.EAST);

        JList<RankedPaper> results = new JList<>(searchModel);
        results.setCellRenderer(new SearchResultRenderer());
        results.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        results.setBackground(Theme.SURFACE);
        JScrollPane scroll = new JScrollPane(results);
        UiKit.styleScrollPane(scroll);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(searchCount, BorderLayout.WEST);
        JLabel scoreNote = mutedLabel("Relevance uses TF-IDF cosine similarity with query expansion");
        footer.add(scoreNote, BorderLayout.EAST);

        panel.add(controls, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createAnalysisPanel(String title, String subtitle, JLabel metric, String metricLabel,
            FrequencyTableModel model, BarChartPanel chart, Color color, String exportName) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 4, 4, 4));

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel titleBlock = new JPanel(new GridLayout(2, 1));
        titleBlock.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.HEADING);
        JLabel subtitleLabel = mutedLabel(subtitle);
        titleBlock.add(titleLabel);
        titleBlock.add(subtitleLabel);

        JPanel metricBlock = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        metricBlock.setOpaque(false);
        metricBlock.add(metric);
        metricBlock.add(mutedLabel(metricLabel));
        JButton export = secondaryButton("Export CSV");
        export.addActionListener(event -> export(model, exportName));
        metricBlock.add(export);
        heading.add(titleBlock, BorderLayout.WEST);
        heading.add(metricBlock, BorderLayout.EAST);

        JTable table = createFrequencyTable(model);
        JScrollPane tableScroll = new JScrollPane(table);
        UiKit.styleScrollPane(tableScroll);
        JScrollPane chartScroll = new JScrollPane(chart);
        UiKit.styleScrollPane(chartScroll);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, chartScroll);
        split.setResizeWeight(0.42);
        split.setDividerLocation(460);
        split.setBorder(null);
        split.setBackground(Theme.BACKGROUND);

        panel.add(heading, BorderLayout.NORTH);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(14, 0));
        bar.setBackground(Theme.SURFACE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)));

        progressBar.setPreferredSize(new Dimension(150, 8));
        progressBar.setStringPainted(false);
        progressBar.setForeground(Theme.ACCENT);
        progressBar.setBackground(Theme.BORDER);
        JPanel state = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        state.setOpaque(false);
        state.add(statusLabel);
        state.add(progressBar);

        JPanel metrics = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        metrics.setOpaque(false);
        metrics.add(featureMetric);
        metrics.add(mutedLabel("features"));
        metrics.add(new JLabel("  |  "));
        metrics.add(threadMetric);
        metrics.add(mutedLabel("worker threads"));
        bar.add(state, BorderLayout.WEST);
        bar.add(metrics, BorderLayout.EAST);
        return bar;
    }

    private JTable createFrequencyTable(FrequencyTableModel model) {
        JTable table = new JTable(model);
        table.setFont(Theme.BODY);
        table.setRowHeight(30);
        table.setShowVerticalLines(false);
        table.setGridColor(Theme.BORDER);
        table.setSelectionBackground(new Color(229, 242, 242));
        table.setSelectionForeground(Theme.TEXT);
        table.getTableHeader().setFont(Theme.BODY.deriveFont(Font.BOLD));
        table.getTableHeader().setBackground(Theme.SURFACE_MUTED);
        table.getTableHeader().setForeground(Theme.TEXT);
        table.getTableHeader().setOpaque(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(44);
        table.getColumnModel().getColumn(1).setPreferredWidth(270);
        table.getColumnModel().getColumn(2).setPreferredWidth(60);
        table.getColumnModel().getColumn(3).setPreferredWidth(72);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(center);
        table.getColumnModel().getColumn(2).setCellRenderer(center);
        table.getColumnModel().getColumn(3).setCellRenderer(center);
        return table;
    }

    private void configureCategories() {
        categoryBox.setModel(new DefaultComboBoxModel<>(new CategoryOption[]{
                new CategoryOption("All papers", null),
                new CategoryOption("Crime reporting", Paper.Category.CRIME_REPORTING),
                new CategoryOption("Deep learning", Paper.Category.DEEP_LEARNING)
        }));
        categoryBox.setSelectedIndex(1);
    }

    private void runSearch() {
        CategoryOption selected = (CategoryOption) categoryBox.getSelectedItem();
        List<RankedPaper> results = searchEngine.search(
                queryField.getText(), selected == null ? null : selected.category(), 20);
        searchModel.clear();
        results.forEach(searchModel::addElement);
        searchCount.setText(results.size() + " relevant papers from " + papers.size() + " indexed records");
    }

    private void runAnalysis() {
        analyzeButton.setEnabled(false);
        progressBar.setValue(0);
        progressBar.setMaximum(papers.size());
        statusLabel.setText("Analyzing paper 0 of " + papers.size());

        SwingWorker<AnalysisReport, Void> worker = new SwingWorker<>() {
            @Override
            protected AnalysisReport doInBackground() {
                return analysisService.analyze(papers, count -> SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(count);
                    statusLabel.setText("Analyzing paper " + count + " of " + papers.size());
                }));
            }

            @Override
            protected void done() {
                try {
                    displayReport(get());
                } catch (Exception exception) {
                    statusLabel.setText("Analysis failed");
                    JOptionPane.showMessageDialog(MainFrame.this,
                            exception.getMessage(), "Analysis error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    analyzeButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void displayReport(AnalysisReport report) {
        crimeModel.setItems(report.crimeFeatures());
        headingModel.setItems(report.deepLearningHeadings());
        crimeChart.setData(report.crimeFeatures(), "Number of crime-reporting systems", Theme.CORAL);
        headingChart.setData(report.deepLearningHeadings(), "Number of deep-learning papers", Theme.ACCENT);
        crimeMetric.setText(Integer.toString(report.crimePaperCount()));
        headingMetric.setText(Integer.toString(report.deepLearningPaperCount()));
        featureMetric.setText(Integer.toString(report.crimeFeatures().size()));
        threadMetric.setText(Integer.toString(report.workerThreads().size()));
        progressBar.setValue(papers.size());
        statusLabel.setText("Completed in " + report.elapsed().toMillis() + " ms using "
                + String.join(", ", report.workerThreads()));
    }

    private void export(FrequencyTableModel model, String suggestedName) {
        if (model.items().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Run the analysis before exporting.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(suggestedName));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            String header = model == crimeModel ? "distinctive_feature" : "normalized_subheading";
            new CsvExporter().write(chooser.getSelectedFile().toPath(), header, model.items());
            statusLabel.setText("Exported " + chooser.getSelectedFile().getName());
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Export error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        return UiKit.primary(button);
    }

    private static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        return UiKit.secondary(button);
    }

    private static JLabel mutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.SMALL);
        label.setForeground(Theme.MUTED);
        return label;
    }

    private static JLabel metricValue(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.HEADING);
        label.setForeground(Theme.TEXT);
        return label;
    }

    private record CategoryOption(String label, Paper.Category category) {
        @Override
        public String toString() {
            return label;
        }
    }
}
