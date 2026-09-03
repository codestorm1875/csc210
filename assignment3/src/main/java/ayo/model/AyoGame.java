package ayo.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/** Rules engine for the two-row Yoruba Ayo/Oware sow-and-capture game. */
public final class AyoGame {
    public static final int PIT_COUNT = 12;
    public static final int STARTING_SEEDS = 4;
    public static final int TOTAL_SEEDS = 48;
    private static final int MOVE_LIMIT = 250;

    private final int[] board = new int[PIT_COUNT];
    private final Deque<GameSnapshot> history = new ArrayDeque<>();
    private int southCaptured;
    private int northCaptured;
    private Player currentPlayer;
    private boolean gameOver;
    private Player winner;
    private int moveNumber;
    private String message;

    public AyoGame() {
        Arrays.fill(board, STARTING_SEEDS);
        currentPlayer = Player.SOUTH;
        message = "South to move";
    }

    public AyoGame(GameSnapshot snapshot) {
        restore(snapshot);
    }

    public static AyoGame fromPosition(int[] board, int southCaptured, int northCaptured,
            Player currentPlayer) {
        int total = southCaptured + northCaptured + Arrays.stream(board).sum();
        if (total != TOTAL_SEEDS) {
            throw new IllegalArgumentException("Position must contain exactly 48 seeds");
        }
        return new AyoGame(new GameSnapshot(board, southCaptured, northCaptured,
                currentPlayer, false, null, 0, currentPlayer.displayName() + " to move"));
    }

    public synchronized GameSnapshot snapshot() {
        return new GameSnapshot(board, southCaptured, northCaptured, currentPlayer,
                gameOver, winner, moveNumber, message);
    }

    public synchronized List<Integer> legalMoves() {
        return legalMovesFor(currentPlayer);
    }

    public synchronized MoveResult play(int sourcePit) {
        if (gameOver) throw new IllegalStateException("The game is over");
        if (sourcePit < 0 || sourcePit >= PIT_COUNT) {
            throw new IllegalArgumentException("Pit index must be between 0 and 11");
        }
        if (!currentPlayer.owns(sourcePit)) {
            throw new IllegalArgumentException("Choose a pit on " + currentPlayer.displayName() + "'s side");
        }
        if (!legalMovesFor(currentPlayer).contains(sourcePit)) {
            throw new IllegalArgumentException(board[sourcePit] == 0
                    ? "That pit is empty" : "This move must feed the opponent");
        }

        history.push(snapshot());
        Player mover = currentPlayer;
        int landingPit = sow(sourcePit);
        List<Integer> capturedPits = captureCandidates(mover, landingPit);
        int capturedSeeds = capturedPits.stream().mapToInt(pit -> board[pit]).sum();

        // A move may not capture every seed on the opponent's side.
        if (capturedSeeds == seedsOwnedBy(mover.opponent())) {
            capturedPits = List.of();
            capturedSeeds = 0;
        } else {
            for (int pit : capturedPits) board[pit] = 0;
            addCaptured(mover, capturedSeeds);
        }

        moveNumber++;
        message = mover.displayName() + " moved pit " + displayPit(sourcePit)
                + (capturedSeeds == 0 ? "" : " and captured " + capturedSeeds);
        updateWinnerAfterCapture();

        if (!gameOver) {
            currentPlayer = mover.opponent();
            if (moveNumber >= MOVE_LIMIT || legalMovesFor(currentPlayer).isEmpty()) {
                collectRemainingSeeds();
                determineWinner();
            }
        }

        return new MoveResult(mover, sourcePit, landingPit, capturedSeeds,
                capturedPits, message);
    }

    public synchronized boolean canUndo() {
        return !history.isEmpty();
    }

    public synchronized boolean undo() {
        if (history.isEmpty()) return false;
        GameSnapshot previous = history.pop();
        restore(previous);
        message = "Move undone; " + currentPlayer.displayName() + " to move";
        return true;
    }

    public synchronized AyoGame copy() {
        return new AyoGame(snapshot());
    }

    private int sow(int sourcePit) {
        int seeds = board[sourcePit];
        board[sourcePit] = 0;
        int pit = sourcePit;
        while (seeds > 0) {
            pit = (pit + 1) % PIT_COUNT;
            if (pit == sourcePit) continue;
            board[pit]++;
            seeds--;
        }
        return pit;
    }

    private List<Integer> captureCandidates(Player mover, int landingPit) {
        List<Integer> captured = new ArrayList<>();
        int pit = landingPit;
        while (mover.opponent().owns(pit) && (board[pit] == 2 || board[pit] == 3)) {
            captured.add(pit);
            pit = (pit + PIT_COUNT - 1) % PIT_COUNT;
        }
        return captured;
    }

    private List<Integer> legalMovesFor(Player player) {
        if (gameOver) return List.of();
        boolean opponentEmpty = seedsOwnedBy(player.opponent()) == 0;
        List<Integer> moves = new ArrayList<>();
        for (int pit = 0; pit < PIT_COUNT; pit++) {
            if (player.owns(pit) && board[pit] > 0
                    && (!opponentEmpty || moveFeedsOpponent(player, pit))) {
                moves.add(pit);
            }
        }
        return List.copyOf(moves);
    }

    private boolean moveFeedsOpponent(Player player, int sourcePit) {
        int seeds = board[sourcePit];
        int pit = sourcePit;
        while (seeds > 0) {
            pit = (pit + 1) % PIT_COUNT;
            if (pit == sourcePit) continue;
            if (player.opponent().owns(pit)) return true;
            seeds--;
        }
        return false;
    }

    private int seedsOwnedBy(Player player) {
        int total = 0;
        for (int pit = 0; pit < PIT_COUNT; pit++) {
            if (player.owns(pit)) total += board[pit];
        }
        return total;
    }

    private void addCaptured(Player player, int seeds) {
        if (player == Player.SOUTH) southCaptured += seeds;
        else northCaptured += seeds;
    }

    private void updateWinnerAfterCapture() {
        if (southCaptured >= 25 || northCaptured >= 25) {
            determineWinner();
        } else if (southCaptured == 24 && northCaptured == 24) {
            gameOver = true;
            winner = null;
            message = "Draw: 24 seeds each";
        }
    }

    private void collectRemainingSeeds() {
        for (int pit = 0; pit < PIT_COUNT; pit++) {
            addCaptured(Player.SOUTH.owns(pit) ? Player.SOUTH : Player.NORTH, board[pit]);
            board[pit] = 0;
        }
    }

    private void determineWinner() {
        gameOver = true;
        if (southCaptured > northCaptured) winner = Player.SOUTH;
        else if (northCaptured > southCaptured) winner = Player.NORTH;
        else winner = null;
        message = winner == null ? "Draw: " + southCaptured + " seeds each"
                : winner.displayName() + " wins " + Math.max(southCaptured, northCaptured)
                + " to " + Math.min(southCaptured, northCaptured);
    }

    private void restore(GameSnapshot snapshot) {
        int[] source = snapshot.board();
        System.arraycopy(source, 0, board, 0, PIT_COUNT);
        southCaptured = snapshot.southCaptured();
        northCaptured = snapshot.northCaptured();
        currentPlayer = snapshot.currentPlayer();
        gameOver = snapshot.gameOver();
        winner = snapshot.winner();
        moveNumber = snapshot.moveNumber();
        message = snapshot.message();
    }

    private int displayPit(int pit) {
        return pit < 6 ? pit + 1 : 12 - pit;
    }
}
