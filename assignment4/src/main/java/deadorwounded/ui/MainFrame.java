package deadorwounded.ui;

import deadorwounded.model.DeadOrWoundedGame;
import deadorwounded.model.Feedback;
import deadorwounded.model.GameSnapshot;
import deadorwounded.model.GameStatus;
import deadorwounded.model.GuessResult;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.time.Duration;

public final class MainFrame extends JFrame {
    private final CodeRackPanel rackPanel = new CodeRackPanel();
    private final GuessTableModel tableModel = new GuessTableModel();
    private final JTable historyTable = new JTable(tableModel);
    private final JLabel attemptLabel = metricLabel("0 attempts");
    private final JLabel timeLabel = metricLabel("00:00");
    private final JLabel statusLabel = muted("Enter a four-digit guess");
    private final JLabel deadFeedback = feedbackLabel("0 dead", Theme.DEAD);
    private final JLabel woundedFeedback = feedbackLabel("0 wounded", Theme.WOUNDED);
    private final JButton submitButton = primaryButton("Check guess");
    private final JButton giveUpButton = secondaryButton("Give up");
    private final StringBuilder input = new StringBuilder();
    private final Timer timer;
    private DeadOrWoundedGame game = new DeadOrWoundedGame();
    private long startedAtNanos = System.nanoTime();
    private Feedback latestFeedback;

    public MainFrame() {
        super("CSC210 Dead or Wounded");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(940, 680));
        setSize(1160, 780);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.BACKGROUND);

        add(createHeader(), BorderLayout.NORTH);
        add(createWorkspace(), BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);
        configureKeyboard();

        submitButton.addActionListener(event -> submitGuess());
        giveUpButton.addActionListener(event -> giveUp());
        timer = new Timer(500, event -> updateTime());
        timer.start();
        updateView();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setBackground(Theme.SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(13, 20, 13, 20)));

        JPanel identity = new JPanel(new GridLayout(2, 1, 0, 2));
        identity.setOpaque(false);
        JLabel title = new JLabel("Dead or Wounded");
        title.setFont(Theme.HEADING.deriveFont(20f));
        title.setForeground(Theme.TEXT);
        JLabel subtitle = new JLabel("CSC210  |  Logic deduction workspace");
        subtitle.setFont(Theme.SMALL);
        subtitle.setForeground(Theme.MUTED);
        identity.add(title);
        identity.add(subtitle);

        JPanel metrics = new JPanel(new FlowLayout(FlowLayout.RIGHT, 13, 0));
        metrics.setOpaque(false);
        metrics.add(attemptLabel);
        metrics.add(timeLabel);
        header.add(identity, BorderLayout.WEST);
        header.add(metrics, BorderLayout.EAST);
        return header;
    }

    private JPanel createWorkspace() {
        JPanel workspace = new JPanel(new BorderLayout(0, 10));
        workspace.setBackground(Theme.BACKGROUND);
        workspace.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        workspace.add(createToolbar(), BorderLayout.NORTH);

        JPanel playArea = new JPanel(new BorderLayout(0, 8));
        playArea.setBackground(Theme.SURFACE);
        playArea.setBorder(BorderFactory.createEmptyBorder(12, 16, 16, 16));
        playArea.add(rackPanel, BorderLayout.CENTER);
        playArea.add(createKeypadArea(), BorderLayout.SOUTH);

        JPanel history = new JPanel(new BorderLayout(0, 8));
        history.setBackground(Theme.SURFACE);
        history.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel heading = new JLabel("Attempt history", SwingConstants.LEFT);
        heading.setFont(Theme.BODY.deriveFont(Font.BOLD, 14f));
        configureTable();
        JScrollPane scroll = new JScrollPane(historyTable);
        UiKit.styleScrollPane(scroll);
        history.add(heading, BorderLayout.NORTH);
        history.add(scroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, playArea, history);
        split.setResizeWeight(0.67);
        split.setDividerLocation(720);
        split.setBorder(null);
        split.setDividerSize(1);
        split.setBackground(Theme.BORDER);
        workspace.add(split, BorderLayout.CENTER);
        return workspace;
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JButton newGame = primaryButton("New game");
        newGame.addActionListener(event -> newGame());
        JButton rules = secondaryButton("Rules");
        rules.addActionListener(event -> showRules());
        left.add(newGame);
        left.add(giveUpButton);
        toolbar.add(left, BorderLayout.WEST);
        toolbar.add(rules, BorderLayout.EAST);
        return toolbar;
    }

    private JPanel createKeypadArea() {
        JPanel area = new JPanel(new BorderLayout(0, 9));
        area.setOpaque(false);
        JPanel feedback = new JPanel(new FlowLayout(FlowLayout.CENTER, 22, 0));
        feedback.setOpaque(false);
        feedback.setBorder(BorderFactory.createEmptyBorder(1, 0, 2, 0));
        feedback.add(deadFeedback);
        feedback.add(woundedFeedback);

        JPanel keypad = new JPanel(new GridLayout(4, 3, 8, 8));
        keypad.setOpaque(false);
        String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "Clear", "0", "Back"};
        for (String key : keys) {
            JButton button = keypadButton(key);
            button.addActionListener(event -> handleKey(key));
            keypad.add(button);
        }
        keypad.setPreferredSize(new Dimension(380, 208));

        JPanel centered = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centered.setOpaque(false);
        centered.add(keypad);
        submitButton.setPreferredSize(new Dimension(380, 40));
        JPanel controls = new JPanel(new BorderLayout(0, 9));
        controls.setOpaque(false);
        controls.add(feedback, BorderLayout.NORTH);
        controls.add(centered, BorderLayout.CENTER);
        area.add(controls, BorderLayout.CENTER);
        area.add(submitButton, BorderLayout.SOUTH);
        return area;
    }

    private JPanel createStatusBar() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBackground(Theme.SURFACE);
        status.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(9, 20, 9, 20)));
        status.add(statusLabel, BorderLayout.WEST);
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        legend.setOpaque(false);
        JLabel dead = new JLabel("Dead: right position");
        dead.setFont(Theme.SMALL);
        dead.setForeground(Theme.DEAD);
        JLabel wounded = new JLabel("Wounded: wrong position");
        wounded.setFont(Theme.SMALL);
        wounded.setForeground(new Color(151, 103, 10));
        legend.add(dead);
        legend.add(wounded);
        status.add(legend, BorderLayout.EAST);
        return status;
    }

    private void configureTable() {
        historyTable.setFont(Theme.BODY);
        historyTable.setRowHeight(32);
        historyTable.setShowVerticalLines(false);
        historyTable.setShowHorizontalLines(true);
        historyTable.setIntercellSpacing(new Dimension(0, 0));
        historyTable.setGridColor(Theme.BORDER);
        historyTable.setSelectionBackground(new Color(220, 252, 231));
        historyTable.setSelectionForeground(Theme.TEXT);
        historyTable.setFillsViewportHeight(true);
        historyTable.getTableHeader().setFont(Theme.BODY.deriveFont(Font.BOLD));
        historyTable.getTableHeader().setBackground(Theme.SURFACE_MUTED);
        historyTable.getTableHeader().setForeground(Theme.TEXT);
        historyTable.getTableHeader().setPreferredSize(new Dimension(0, 34));
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int column = 0; column < 4; column++) {
            historyTable.getColumnModel().getColumn(column).setCellRenderer(center);
        }
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(36);
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(90);
    }

    private void configureKeyboard() {
        JComponent root = getRootPane();
        for (char digit = '0'; digit <= '9'; digit++) {
            final char value = digit;
            String actionName = "digit-" + digit;
            root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(digit), actionName);
            root.getActionMap().put(actionName, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    appendDigit(value);
                }
            });
        }
        bindKey(root, KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "back", this::backspace);
        bindKey(root, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "clear", this::clearInput);
        bindKey(root, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "submit", this::submitGuess);
    }

    private void bindKey(JComponent component, KeyStroke key, String name, Runnable action) {
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        component.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
            }
        });
    }

    private void handleKey(String key) {
        switch (key) {
            case "Clear" -> clearInput();
            case "Back" -> backspace();
            default -> appendDigit(key.charAt(0));
        }
    }

    private void appendDigit(char digit) {
        if (game.snapshot().status() != GameStatus.IN_PROGRESS || input.length() >= 4) return;
        if (input.length() == 0 && digit == '0') {
            setStatus("A four-digit number cannot begin with zero", Theme.ERROR);
            return;
        }
        if (input.indexOf(String.valueOf(digit)) >= 0) {
            setStatus("Each digit must be different", Theme.ERROR);
            return;
        }
        input.append(digit);
        setStatus(input.length() == 4 ? "Guess ready" : "Enter " + (4 - input.length()) + " more digits",
                input.length() == 4 ? Theme.SUCCESS : Theme.MUTED);
        updateRack();
    }

    private void backspace() {
        if (!input.isEmpty() && game.snapshot().status() == GameStatus.IN_PROGRESS) {
            input.deleteCharAt(input.length() - 1);
            setStatus("Digit removed", Theme.MUTED);
            updateRack();
        }
    }

    private void clearInput() {
        if (game.snapshot().status() != GameStatus.IN_PROGRESS) return;
        input.setLength(0);
        setStatus("Guess cleared", Theme.MUTED);
        updateRack();
    }

    private void submitGuess() {
        if (game.snapshot().status() != GameStatus.IN_PROGRESS) return;
        if (input.length() != 4) {
            setStatus("Enter all four distinct digits", Theme.ERROR);
            return;
        }
        try {
            GuessResult result = game.submitGuess(input.toString());
            latestFeedback = result.feedback();
            input.setLength(0);
            updateView();
            if (result.feedback().solved()) {
                timer.stop();
                setStatus("Code solved in " + result.attempt() + " attempts", Theme.SUCCESS);
            } else {
                setStatus(result.feedback().dead() + " dead, "
                        + result.feedback().wounded() + " wounded", Theme.TEXT);
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            setStatus(exception.getMessage(), Theme.ERROR);
        }
    }

    private void giveUp() {
        if (game.snapshot().status() != GameStatus.IN_PROGRESS) return;
        game.giveUp();
        timer.stop();
        updateView();
        setStatus("Secret revealed", Theme.ERROR);
    }

    private void newGame() {
        game = new DeadOrWoundedGame();
        input.setLength(0);
        latestFeedback = null;
        startedAtNanos = System.nanoTime();
        timeLabel.setText("00:00");
        timer.start();
        setStatus("New secret generated", Theme.SUCCESS);
        updateView();
    }

    private void updateView() {
        GameSnapshot snapshot = game.snapshot();
        tableModel.setGuesses(snapshot.guesses());
        attemptLabel.setText(snapshot.attempts() + (snapshot.attempts() == 1 ? " attempt" : " attempts"));
        submitButton.setEnabled(snapshot.status() == GameStatus.IN_PROGRESS);
        giveUpButton.setEnabled(snapshot.status() == GameStatus.IN_PROGRESS);
        int dead = latestFeedback == null ? 0 : latestFeedback.dead();
        int wounded = latestFeedback == null ? 0 : latestFeedback.wounded();
        deadFeedback.setText("\u25cf  " + dead + " dead");
        woundedFeedback.setText("\u25cf  " + wounded + " wounded");
        updateRack();
    }

    private void updateRack() {
        GameSnapshot snapshot = game.snapshot();
        rackPanel.setState(input.toString(), snapshot.revealedSecret(), latestFeedback);
    }

    private void updateTime() {
        if (game.snapshot().status() != GameStatus.IN_PROGRESS) return;
        long seconds = Duration.ofNanos(System.nanoTime() - startedAtNanos).toSeconds();
        timeLabel.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    private void showRules() {
        JLabel rules = new JLabel("<html><body style='width:430px'>"
                + "<h2>Dead or Wounded</h2>"
                + "<p>The computer chooses a four-digit number. All digits are distinct and the first digit is not zero.</p>"
                + "<p><b>Dead:</b> a correct digit in the correct position.</p>"
                + "<p><b>Wounded:</b> a correct digit in the wrong position.</p>"
                + "<p>Use each clue to deduce the code. Four dead digits wins the game.</p>"
                + "</body></html>");
        rules.setFont(Theme.BODY);
        JOptionPane.showMessageDialog(this, rules, "Rules", JOptionPane.PLAIN_MESSAGE);
    }

    private static JButton primaryButton(String text) {
        return UiKit.primary(new JButton(text));
    }

    private static JButton secondaryButton(String text) {
        return UiKit.secondary(new JButton(text));
    }

    private static JButton keypadButton(String text) {
        JButton button = UiKit.keypad(new JButton(text));
        button.setFont(Theme.BODY.deriveFont(Font.BOLD, text.length() == 1 ? 17f : 12f));
        return button;
    }

    private static JLabel metricLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.BODY.deriveFont(Font.BOLD, 13f));
        label.setForeground(Theme.TEXT);
        label.setBackground(Theme.SURFACE_MUTED);
        label.setOpaque(true);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(7, 11, 7, 11)));
        return label;
    }

    private static JLabel feedbackLabel(String text, Color color) {
        JLabel label = new JLabel("\u25cf  " + text);
        label.setFont(Theme.BODY.deriveFont(Font.BOLD, 12f));
        label.setForeground(color);
        return label;
    }

    private static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.SMALL);
        label.setForeground(Theme.MUTED);
        return label;
    }
}
