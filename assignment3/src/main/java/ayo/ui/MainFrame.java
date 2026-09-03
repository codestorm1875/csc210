package ayo.ui;

import ayo.ai.AyoAi;
import ayo.model.AyoGame;
import ayo.model.GameSnapshot;
import ayo.model.MoveResult;
import ayo.model.Player;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.net.URL;

public final class MainFrame extends JFrame {
    private final AyoBoardPanel boardPanel = new AyoBoardPanel();
    private final AyoAi computer = new AyoAi(6);
    private final JComboBox<GameMode> modeBox = new JComboBox<>(GameMode.values());
    private final JButton undoButton = secondaryButton("Undo");
    private final JLabel northScore = scoreLabel("North  0");
    private final JLabel southScore = scoreLabel("South  0");
    private final JLabel turnLabel = new JLabel("South to move", SwingConstants.CENTER);
    private final JLabel guideLabel = new JLabel(
            "Choose a gold-ringed pit on the bottom row", SwingConstants.CENTER);
    private final JLabel statusLabel = muted("Game ready");
    private final DefaultListModel<String> historyModel = new DefaultListModel<>();
    private AyoGame game = new AyoGame();
    private SwingWorker<Integer, Void> aiWorker;

    public MainFrame() {
        super("CSC210 Ayo Olopon");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 680));
        setSize(1240, 790);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.BACKGROUND);

        add(createHeader(), BorderLayout.NORTH);
        add(createWorkspace(), BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);

        boardPanel.setMoveHandler(this::playHumanMove);
        modeBox.addActionListener(event -> newGame());
        undoButton.addActionListener(event -> undo());
        updateView();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setBackground(Theme.SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(13, 22, 13, 22)));

        JPanel identity = new JPanel(new GridLayout(2, 1, 0, 2));
        identity.setOpaque(false);
        JLabel title = new JLabel("Ayo Olopon");
        title.setFont(Theme.HEADING.deriveFont(19f));
        title.setForeground(Theme.TEXT);
        JLabel subtitle = new JLabel("Yoruba strategy board");
        subtitle.setFont(Theme.SMALL);
        subtitle.setForeground(Theme.MUTED);
        identity.add(title);
        identity.add(subtitle);

        JPanel scores = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        scores.setOpaque(false);
        scores.add(northScore);
        scores.add(southScore);
        header.add(identity, BorderLayout.WEST);
        header.add(scores, BorderLayout.EAST);
        return header;
    }

    private JPanel createWorkspace() {
        JPanel workspace = new JPanel(new BorderLayout(0, 10));
        workspace.setBackground(Theme.BACKGROUND);
        workspace.setBorder(BorderFactory.createEmptyBorder(16, 18, 12, 18));
        workspace.add(createToolbar(), BorderLayout.NORTH);

        JPanel boardArea = new JPanel(new BorderLayout());
        boardArea.setBackground(Theme.SURFACE);
        boardArea.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        JPanel turnGuide = new JPanel(new GridLayout(2, 1, 0, 2));
        turnGuide.setBackground(Theme.SURFACE);
        turnGuide.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        turnLabel.setFont(Theme.BODY.deriveFont(Font.BOLD, 15f));
        turnLabel.setForeground(Theme.GREEN_DARK);
        guideLabel.setFont(Theme.SMALL);
        guideLabel.setForeground(Theme.MUTED);
        turnGuide.add(turnLabel);
        turnGuide.add(guideLabel);
        boardArea.add(turnGuide, BorderLayout.NORTH);
        boardArea.add(boardPanel, BorderLayout.CENTER);

        JPanel history = new JPanel(new BorderLayout(0, 8));
        history.setBackground(Theme.SURFACE);
        history.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        history.setPreferredSize(new Dimension(250, 500));
        JLabel historyTitle = new JLabel("Move history");
        historyTitle.setFont(Theme.BODY.deriveFont(Font.BOLD));
        JList<String> historyList = new JList<>(historyModel);
        historyList.setFont(Theme.SMALL);
        historyList.setFixedCellHeight(27);
        historyList.setBackground(Theme.SURFACE);
        JScrollPane historyScroll = new JScrollPane(historyList);
        UiKit.styleScrollPane(historyScroll);
        history.add(historyTitle, BorderLayout.NORTH);
        history.add(historyScroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, boardArea, history);
        split.setResizeWeight(0.80);
        split.setDividerLocation(920);
        split.setBorder(null);
        workspace.add(split, BorderLayout.CENTER);
        return workspace;
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JPanel gameControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        gameControls.setOpaque(false);
        modeBox.setFont(Theme.BODY);
        modeBox.setPreferredSize(new Dimension(180, 35));
        modeBox.setBackground(Theme.SURFACE);
        modeBox.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        JButton newGame = primaryButton("New game");
        newGame.addActionListener(event -> newGame());
        gameControls.add(modeBox);
        gameControls.add(newGame);
        gameControls.add(undoButton);

        JButton rules = secondaryButton("How to play");
        rules.addActionListener(event -> showRules());
        toolbar.add(gameControls, BorderLayout.WEST);
        toolbar.add(rules, BorderLayout.EAST);
        return toolbar;
    }

    private JPanel createStatusBar() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBackground(Theme.SURFACE);
        status.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)));
        status.add(statusLabel, BorderLayout.WEST);
        JLabel seedTotal = muted("48 seeds  |  12 pits  |  Counter-clockwise");
        status.add(seedTotal, BorderLayout.EAST);
        return status;
    }

    private void playHumanMove(int pit) {
        if (aiWorker != null && !aiWorker.isDone()) return;
        if (selectedMode() == GameMode.COMPUTER && game.snapshot().currentPlayer() == Player.NORTH) return;
        performMove(pit);
        if (!game.snapshot().gameOver() && selectedMode() == GameMode.COMPUTER
                && game.snapshot().currentPlayer() == Player.NORTH) {
            startComputerMove();
        }
    }

    private void performMove(int pit) {
        try {
            MoveResult result = game.play(pit);
            historyModel.addElement((historyModel.size() + 1) + ". " + result.description());
            boardPanel.highlightMove(result.sourcePit(), result.landingPit(), result.capturedPits());
            updateView();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    private void startComputerMove() {
        GameSnapshot position = game.snapshot();
        statusLabel.setText("North is thinking...");
        boardPanel.setGame(position, game.legalMoves(), false);
        aiWorker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() {
                return computer.chooseMove(position);
            }

            @Override
            protected void done() {
                if (isCancelled()) return;
                try {
                    performMove(get());
                } catch (Exception exception) {
                    statusLabel.setText("Computer move failed: " + exception.getMessage());
                }
            }
        };
        aiWorker.execute();
    }

    private void updateView() {
        GameSnapshot snapshot = game.snapshot();
        northScore.setText("North  " + snapshot.northCaptured());
        southScore.setText("South  " + snapshot.southCaptured());
        if (snapshot.gameOver()) {
            turnLabel.setText(snapshot.winner() == null ? "Draw" : snapshot.winner().displayName() + " wins");
            turnLabel.setForeground(snapshot.winner() == null ? Theme.MUTED : Theme.GREEN_DARK);
            guideLabel.setText("Start a new game to play again");
        } else {
            boolean computerTurn = selectedMode() == GameMode.COMPUTER
                    && snapshot.currentPlayer() == Player.NORTH;
            turnLabel.setText(computerTurn ? "Computer's turn  |  North"
                    : snapshot.currentPlayer() == Player.SOUTH
                    ? "Your turn  |  South" : "North player's turn");
            turnLabel.setForeground(snapshot.currentPlayer() == Player.SOUTH
                    ? Theme.GREEN_DARK : new Color(49, 89, 155));
            guideLabel.setText(computerTurn
                    ? "Watch the highlighted source and landing pits"
                    : snapshot.currentPlayer() == Player.SOUTH
                    ? "Choose a gold-ringed pit on the bottom row; seeds sow to the right"
                    : "Choose a gold-ringed pit on the top row; seeds sow to the left");
        }
        boolean humanTurn = selectedMode() == GameMode.LOCAL
                || snapshot.currentPlayer() == Player.SOUTH;
        boardPanel.setPlayerRoles(selectedMode() == GameMode.COMPUTER ? "COMPUTER" : "PLAYER 2",
                selectedMode() == GameMode.COMPUTER ? "YOU" : "PLAYER 1");
        boardPanel.setGame(snapshot, game.legalMoves(), humanTurn && !snapshot.gameOver());
        undoButton.setEnabled(game.canUndo());
        statusLabel.setText(snapshot.message());
    }

    private void newGame() {
        cancelComputer();
        game = new AyoGame();
        historyModel.clear();
        boardPanel.clearHighlights();
        updateView();
    }

    private void undo() {
        cancelComputer();
        int undone = 0;
        if (game.undo()) undone++;
        if (selectedMode() == GameMode.COMPUTER
                && game.snapshot().currentPlayer() == Player.NORTH && game.undo()) {
            undone++;
        }
        for (int index = 0; index < undone && !historyModel.isEmpty(); index++) {
            historyModel.remove(historyModel.size() - 1);
        }
        boardPanel.clearHighlights();
        updateView();
    }

    private void cancelComputer() {
        if (aiWorker != null && !aiWorker.isDone()) aiWorker.cancel(true);
        aiWorker = null;
    }

    private GameMode selectedMode() {
        GameMode mode = (GameMode) modeBox.getSelectedItem();
        return mode == null ? GameMode.COMPUTER : mode;
    }

    private void showRules() {
        JPanel content = new JPanel(new BorderLayout(16, 0));
        content.setBackground(Theme.SURFACE);
        URL resource = MainFrame.class.getResource("/images/reference-ayo-board.png");
        if (resource != null) {
            Image image = new ImageIcon(resource).getImage().getScaledInstance(245, 222, Image.SCALE_SMOOTH);
            content.add(new JLabel(new ImageIcon(image)), BorderLayout.WEST);
        }
        JLabel rules = new JLabel("<html><body style='width:360px'>"
                + "<h2>Ayo Olopon</h2>"
                + "<p><b>1. You are South.</b> Your six pits are the bottom row. Gold rings show the pits you may choose.</p>"
                + "<p><b>2. Click one gold-ringed pit.</b> Every seed in it is placed one-by-one around the board counter-clockwise: right across your row, then left across North's row.</p>"
                + "<p><b>3. Capture on North's side.</b> If your final seed leaves exactly two or three seeds in a North pit, you capture them. Adjacent pits with two or three may also be captured backward.</p>"
                + "<p><b>4. Win by capturing.</b> The first player to capture at least 25 of the 48 seeds wins.</p>"
                + "<p>The move history records the chosen pit, landing pit and captured seeds.</p>"
                + "</body></html>");
        rules.setFont(Theme.BODY);
        content.add(rules, BorderLayout.CENTER);
        JOptionPane.showMessageDialog(this, content, "How to play Ayo", JOptionPane.PLAIN_MESSAGE);
    }

    private static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        return UiKit.primary(button);
    }

    private static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        return UiKit.secondary(button);
    }

    private static JLabel scoreLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.BODY.deriveFont(Font.BOLD, 14f));
        label.setForeground(Theme.TEXT);
        label.setOpaque(true);
        label.setBackground(Theme.SURFACE_MUTED);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(7, 12, 7, 12)));
        return label;
    }

    private static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.SMALL);
        label.setForeground(Theme.MUTED);
        return label;
    }

    private enum GameMode {
        COMPUTER("South vs computer"),
        LOCAL("Local two-player");

        private final String label;

        GameMode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
