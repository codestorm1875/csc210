package ayo.model;

import java.util.Arrays;

/** Immutable view of one complete game state. */
public record GameSnapshot(
        int[] board,
        int southCaptured,
        int northCaptured,
        Player currentPlayer,
        boolean gameOver,
        Player winner,
        int moveNumber,
        String message) {

    public GameSnapshot {
        if (board.length != 12) throw new IllegalArgumentException("Ayo requires 12 pits");
        board = Arrays.copyOf(board, board.length);
    }

    @Override
    public int[] board() {
        return Arrays.copyOf(board, board.length);
    }

    public int seedsAt(int pit) {
        return board[pit];
    }

    public int capturedBy(Player player) {
        return player == Player.SOUTH ? southCaptured : northCaptured;
    }

    public int seedsOwnedBy(Player player) {
        int total = 0;
        for (int pit = 0; pit < board.length; pit++) {
            if (player.owns(pit)) total += board[pit];
        }
        return total;
    }
}
