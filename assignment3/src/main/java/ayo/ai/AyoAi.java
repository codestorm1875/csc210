package ayo.ai;

import ayo.model.AyoGame;
import ayo.model.GameSnapshot;
import ayo.model.Player;

import java.util.Comparator;
import java.util.List;

/** Depth-limited minimax computer player with alpha-beta pruning. */
public final class AyoAi {
    private final int searchDepth;

    public AyoAi(int searchDepth) {
        if (searchDepth < 1) throw new IllegalArgumentException("Search depth must be positive");
        this.searchDepth = searchDepth;
    }

    public int chooseMove(GameSnapshot snapshot) {
        AyoGame root = new AyoGame(snapshot);
        List<Integer> moves = orderedMoves(root);
        if (moves.isEmpty()) throw new IllegalStateException("No legal computer move");

        Player perspective = snapshot.currentPlayer();
        int bestMove = moves.get(0);
        int bestScore = Integer.MIN_VALUE;
        for (int move : moves) {
            AyoGame child = root.copy();
            child.play(move);
            int score = minimax(child, searchDepth - 1, Integer.MIN_VALUE + 1,
                    Integer.MAX_VALUE, perspective);
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private int minimax(AyoGame game, int depth, int alpha, int beta, Player perspective) {
        GameSnapshot snapshot = game.snapshot();
        if (depth == 0 || snapshot.gameOver()) return evaluate(snapshot, perspective);

        List<Integer> moves = orderedMoves(game);
        boolean maximizing = snapshot.currentPlayer() == perspective;
        int best = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (int move : moves) {
            AyoGame child = game.copy();
            child.play(move);
            int score = minimax(child, depth - 1, alpha, beta, perspective);
            if (maximizing) {
                best = Math.max(best, score);
                alpha = Math.max(alpha, best);
            } else {
                best = Math.min(best, score);
                beta = Math.min(beta, best);
            }
            if (beta <= alpha) break;
        }
        return best;
    }

    private List<Integer> orderedMoves(AyoGame game) {
        return game.legalMoves().stream()
                .sorted(Comparator.comparingInt((Integer move) -> immediateGain(game, move)).reversed()
                        .thenComparingInt(Integer::intValue))
                .toList();
    }

    private int immediateGain(AyoGame game, int move) {
        AyoGame copy = game.copy();
        int before = copy.snapshot().capturedBy(copy.snapshot().currentPlayer());
        Player mover = copy.snapshot().currentPlayer();
        copy.play(move);
        return copy.snapshot().capturedBy(mover) - before;
    }

    private int evaluate(GameSnapshot snapshot, Player perspective) {
        Player opponent = perspective.opponent();
        if (snapshot.gameOver()) {
            if (snapshot.winner() == perspective) return 100_000;
            if (snapshot.winner() == opponent) return -100_000;
            return 0;
        }
        int captureBalance = snapshot.capturedBy(perspective) - snapshot.capturedBy(opponent);
        int boardBalance = snapshot.seedsOwnedBy(perspective) - snapshot.seedsOwnedBy(opponent);
        return captureBalance * 30 + boardBalance;
    }
}
