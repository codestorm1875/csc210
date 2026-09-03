package fleet.ui;

import fleet.data.FleetData;
import fleet.model.MonitorVehicleTracker;
import fleet.model.MutablePoint;
import fleet.service.FleetSimulator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public final class MainFrame extends JFrame {
    private final Map<String, MutablePoint> initialLocations = FleetData.initialLocations();
    private final MonitorVehicleTracker tracker = new MonitorVehicleTracker(initialLocations);
    private final FleetSimulator simulator = new FleetSimulator(tracker, 4);
    private final VehicleTableModel tableModel = new VehicleTableModel();
    private final FleetMapPanel mapPanel = new FleetMapPanel();
    private final JTable vehicleTable = new JTable(tableModel);
    private final JTextField searchField = new JTextField(18);
    private final JComboBox<String> typeFilter = new JComboBox<>(new String[]{
            "All vehicles", "Taxi", "Police", "Delivery van", "Truck"
    });
    private final JButton simulationButton = primaryButton("Pause updates");
    private final JLabel vehicleCount = metric("8");
    private final JLabel selectedId = valueLabel("No vehicle selected");
    private final JLabel selectedType = valueLabel("-");
    private final JLabel selectedLocation = valueLabel("-");
    private final JLabel selectedMotion = valueLabel("-");
    private final JLabel selectedSignal = valueLabel("-");
    private final JLabel statusLabel = muted("Starting GPS feeds...");
    private final JLabel snapshotLabel = muted("Snapshot 0");
    private final JSpinner xInput = new JSpinner(new SpinnerNumberModel(50, 0, 100, 1));
    private final JSpinner yInput = new JSpinner(new SpinnerNumberModel(50, 0, 100, 1));
    private final Timer refreshTimer;
    private String selectedVehicle;
    private long snapshotNumber;

    public MainFrame() {
        super("Fleet Tracker");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 680));
        setSize(1240, 790);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.BACKGROUND);

        add(createHeader(), BorderLayout.NORTH);
        add(createWorkspace(), BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);
        configureActions();

        refreshTimer = new Timer(350, event -> refreshSnapshot());
        refreshTimer.start();
        simulator.start();
        refreshSnapshot();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                refreshTimer.stop();
                simulator.close();
                dispose();
            }
        });
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setBackground(Theme.SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(13, 22, 13, 22)));

        JPanel identity = new JPanel(new GridLayout(2, 1, 0, 3));
        identity.setOpaque(false);
        JLabel title = new JLabel("Fleet Tracker");
        title.setFont(Theme.HEADING.deriveFont(19f));
        title.setForeground(Theme.TEXT);
        JLabel subtitle = new JLabel("Live dispatch operations");
        subtitle.setFont(Theme.SMALL);
        subtitle.setForeground(Theme.MUTED);
        identity.add(title);
        identity.add(subtitle);

        JPanel metrics = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        metrics.setOpaque(false);
        metrics.add(vehicleCount);
        JLabel tracked = new JLabel("vehicles tracked");
        tracked.setForeground(Theme.MUTED);
        tracked.setFont(Theme.SMALL);
        metrics.add(tracked);
        JLabel live = new JLabel("LIVE");
        live.setFont(Theme.SMALL.deriveFont(Font.BOLD));
        live.setForeground(Theme.SUCCESS);
        metrics.add(live);
        metrics.add(simulationButton);
        header.add(identity, BorderLayout.WEST);
        header.add(metrics, BorderLayout.EAST);
        return header;
    }

    private JPanel createWorkspace() {
        JPanel workspace = new JPanel(new BorderLayout(0, 10));
        workspace.setBackground(Theme.BACKGROUND);
        workspace.setBorder(BorderFactory.createEmptyBorder(16, 18, 12, 18));
        workspace.add(createToolbar(), BorderLayout.NORTH);

        JScrollPane tableScroll = new JScrollPane(createTable());
        UiKit.styleScrollPane(tableScroll);
        tableScroll.setPreferredSize(new Dimension(430, 500));

        JPanel mapArea = new JPanel(new BorderLayout());
        mapArea.setBackground(Theme.SURFACE);
        mapArea.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        JLabel mapTitle = new JLabel("Live dispatch map");
        mapTitle.setFont(Theme.BODY.deriveFont(Font.BOLD));
        mapTitle.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JPanel mapHeader = new JPanel(new BorderLayout());
        mapHeader.setBackground(Theme.SURFACE);
        mapHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        mapHeader.add(mapTitle, BorderLayout.WEST);
        JLabel scope = muted("Isometric city  |  live GPS");
        scope.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        mapHeader.add(scope, BorderLayout.EAST);
        mapArea.add(mapHeader, BorderLayout.NORTH);
        mapArea.add(mapPanel, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, mapArea);
        split.setDividerLocation(430);
        split.setResizeWeight(0.34);
        split.setBorder(null);

        JPanel body = new JPanel(new BorderLayout(12, 0));
        body.setOpaque(false);
        body.add(split, BorderLayout.CENTER);
        body.add(createDetailsPanel(), BorderLayout.EAST);
        workspace.add(body, BorderLayout.CENTER);
        return workspace;
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);
        searchField.setFont(Theme.BODY);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(9, 10, 9, 10)));
        searchField.setBackground(Theme.SURFACE);
        searchField.setToolTipText("Search by vehicle ID or type");
        toolbar.add(searchField, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        typeFilter.setFont(Theme.BODY);
        typeFilter.setPreferredSize(new Dimension(155, 34));
        typeFilter.setBackground(Theme.SURFACE);
        typeFilter.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        JButton reset = secondaryButton("Reset fleet");
        reset.addActionListener(event -> resetFleet());
        actions.add(typeFilter);
        actions.add(reset);
        toolbar.add(actions, BorderLayout.EAST);
        return toolbar;
    }

    private JTable createTable() {
        vehicleTable.setFont(Theme.BODY);
        vehicleTable.setRowHeight(34);
        vehicleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        vehicleTable.setSelectionBackground(new Color(222, 241, 239));
        vehicleTable.setSelectionForeground(Theme.TEXT);
        vehicleTable.setShowVerticalLines(false);
        vehicleTable.setGridColor(Theme.BORDER);
        vehicleTable.getTableHeader().setFont(Theme.BODY.deriveFont(Font.BOLD));
        vehicleTable.getTableHeader().setBackground(Theme.SURFACE_MUTED);
        vehicleTable.getTableHeader().setForeground(Theme.TEXT);
        vehicleTable.getTableHeader().setOpaque(true);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        vehicleTable.getColumnModel().getColumn(2).setCellRenderer(center);
        vehicleTable.getColumnModel().getColumn(3).setCellRenderer(center);
        vehicleTable.getColumnModel().getColumn(4).setCellRenderer(center);
        vehicleTable.getColumnModel().getColumn(5).setCellRenderer(center);
        vehicleTable.getColumnModel().getColumn(0).setPreferredWidth(78);
        vehicleTable.getColumnModel().getColumn(1).setPreferredWidth(82);
        vehicleTable.getColumnModel().getColumn(2).setPreferredWidth(34);
        vehicleTable.getColumnModel().getColumn(3).setPreferredWidth(34);
        vehicleTable.getColumnModel().getColumn(4).setPreferredWidth(70);
        vehicleTable.getColumnModel().getColumn(5).setPreferredWidth(62);
        return vehicleTable;
    }

    private JPanel createDetailsPanel() {
        JPanel details = new JPanel(new BorderLayout(0, 18));
        details.setPreferredSize(new Dimension(238, 500));
        details.setBackground(Theme.SURFACE);
        details.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));

        JPanel values = new JPanel(new GridLayout(10, 1, 0, 3));
        values.setOpaque(false);
        JLabel heading = new JLabel("Selected vehicle");
        heading.setFont(Theme.SMALL.deriveFont(Font.BOLD));
        heading.setForeground(Theme.MUTED);
        values.add(heading);
        values.add(selectedId);
        values.add(muted("Vehicle type"));
        values.add(selectedType);
        values.add(muted("Current coordinates"));
        values.add(selectedLocation);
        values.add(muted("Motion"));
        values.add(selectedMotion);
        values.add(muted("GPS signal"));
        selectedSignal.setForeground(Theme.SUCCESS);
        values.add(selectedSignal);

        JPanel dispatch = new JPanel(new GridLayout(6, 1, 0, 7));
        dispatch.setOpaque(false);
        JLabel dispatchTitle = new JLabel("Dispatch coordinates");
        dispatchTitle.setFont(Theme.BODY.deriveFont(Font.BOLD));
        dispatch.add(dispatchTitle);
        dispatch.add(labelledSpinner("X", xInput));
        dispatch.add(labelledSpinner("Y", yInput));
        JButton move = primaryButton("Move vehicle");
        move.addActionListener(event -> dispatchSelected((int) xInput.getValue(), (int) yInput.getValue()));
        dispatch.add(move);
        dispatch.add(new JLabel());
        dispatch.add(new JLabel());

        details.add(values, BorderLayout.NORTH);
        details.add(dispatch, BorderLayout.SOUTH);
        return details;
    }

    private JPanel labelledSpinner(String label, JSpinner spinner) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        JLabel text = new JLabel(label);
        text.setFont(Theme.BODY);
        spinner.setFont(Theme.BODY);
        row.add(text, BorderLayout.WEST);
        row.add(spinner, BorderLayout.CENTER);
        return row;
    }

    private JPanel createStatusBar() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBackground(Theme.SURFACE);
        status.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)));
        status.add(statusLabel, BorderLayout.WEST);
        status.add(snapshotLabel, BorderLayout.EAST);
        return status;
    }

    private void configureActions() {
        searchField.getDocument().addDocumentListener(new SimpleDocumentListener(this::refreshSnapshot));
        typeFilter.addActionListener(event -> refreshSnapshot());
        simulationButton.addActionListener(event -> toggleSimulation());
        vehicleTable.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || vehicleTable.getSelectedRow() < 0) return;
            int modelRow = vehicleTable.convertRowIndexToModel(vehicleTable.getSelectedRow());
            selectVehicle(tableModel.vehicleIdAt(modelRow));
        });
        mapPanel.setDispatchHandler(this::dispatchSelected);
        mapPanel.setSelectionHandler(this::selectVehicle);
    }

    private void refreshSnapshot() {
        Map<String, MutablePoint> snapshot = tracker.getLocations();
        String previousSelection = selectedVehicle;
        mapPanel.setSnapshot(snapshot);
        tableModel.setSnapshot(snapshot, mapPanel.telemetrySnapshot(),
                searchField.getText(), (String) typeFilter.getSelectedItem());
        vehicleCount.setText(Integer.toString(snapshot.size()));
        snapshotNumber++;
        snapshotLabel.setText("Snapshot " + snapshotNumber + "  |  "
                + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        if (previousSelection != null) {
            int modelRow = tableModel.findVehicle(previousSelection);
            if (modelRow >= 0) {
                int viewRow = vehicleTable.convertRowIndexToView(modelRow);
                vehicleTable.setRowSelectionInterval(viewRow, viewRow);
            }
            updateSelectedDetails(snapshot.get(previousSelection));
        }
        statusLabel.setText((simulator.isRunning() ? "Live GPS updates" : "GPS updates paused")
                + "  |  " + simulator.updateCount() + " updates  |  "
                + simulator.workerNames().size() + " updater threads");
        statusLabel.setForeground(simulator.isRunning() ? Theme.SUCCESS : Theme.GOLD);
    }

    private void selectVehicle(String vehicleId) {
        selectedVehicle = vehicleId;
        mapPanel.setSelectedVehicle(vehicleId);
        updateSelectedDetails(tracker.getLocation(vehicleId));
        int modelRow = tableModel.findVehicle(vehicleId);
        if (modelRow >= 0) {
            int viewRow = vehicleTable.convertRowIndexToView(modelRow);
            if (viewRow >= 0 && vehicleTable.getSelectedRow() != viewRow) {
                vehicleTable.setRowSelectionInterval(viewRow, viewRow);
                vehicleTable.scrollRectToVisible(vehicleTable.getCellRect(viewRow, 0, true));
            }
        }
    }

    private void updateSelectedDetails(MutablePoint point) {
        if (selectedVehicle == null) return;
        selectedId.setText(selectedVehicle);
        selectedType.setText(FleetData.typeOf(selectedVehicle));
        selectedLocation.setText(point == null ? "-" : point.toString());
        FleetMapPanel.Telemetry telemetry = mapPanel.telemetrySnapshot().get(selectedVehicle);
        if (telemetry == null) {
            selectedMotion.setText("-");
            selectedSignal.setText("-");
        } else {
            selectedMotion.setText(Math.round(telemetry.speedKmh()) + " km/h  |  " + telemetry.heading());
            selectedSignal.setText(telemetry.status() + "  |  " + ageText(telemetry.ageMillis()));
            selectedSignal.setForeground(telemetry.ageMillis() < 1_800 ? Theme.SUCCESS : Theme.CORAL);
        }
        if (point != null && !xInput.hasFocus() && !yInput.hasFocus()) {
            xInput.setValue(point.x);
            yInput.setValue(point.y);
        }
    }

    private void dispatchSelected(int x, int y) {
        if (selectedVehicle == null) {
            statusLabel.setText("Select a vehicle before dispatching");
            return;
        }
        tracker.setLocation(selectedVehicle, x, y);
        xInput.setValue(x);
        yInput.setValue(y);
        refreshSnapshot();
        statusLabel.setText(selectedVehicle + " dispatched to (" + x + ", " + y + ")");
    }

    private String ageText(long ageMillis) {
        if (ageMillis < 1_000) return "now";
        return (ageMillis / 1_000) + "s ago";
    }

    private void toggleSimulation() {
        if (simulator.isRunning()) {
            simulator.pause();
            simulationButton.setText("Resume updates");
        } else {
            simulator.resume();
            simulationButton.setText("Pause updates");
        }
        refreshSnapshot();
    }

    private void resetFleet() {
        initialLocations.forEach((id, point) -> tracker.setLocation(id, point.x, point.y));
        refreshSnapshot();
        statusLabel.setText("Fleet returned to initial locations");
    }

    private static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        return UiKit.primary(button);
    }

    private static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        return UiKit.secondary(button);
    }

    private static JLabel metric(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.HEADING);
        label.setForeground(Theme.TEXT);
        return label;
    }

    private static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.SMALL);
        label.setForeground(Theme.MUTED);
        return label;
    }

    private static JLabel valueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.BODY.deriveFont(Font.BOLD, 15f));
        label.setForeground(Theme.TEXT);
        return label;
    }
}
