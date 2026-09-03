package ayo;

import ayo.ai.AyoAi;
import ayo.model.AyoGame;
import ayo.model.GameSnapshot;
import ayo.model.MoveResult;
import ayo.model.Player;
import ayo.ui.AyoBoardPanel;

import java.awt.image.BufferedImage;
import java.util.List;

public final class TestRunner {
    private int passed;

    public static void main(String[] args) {
        new TestRunner().run();
    }

    private void run() {
        testInitialSetup();
        testCounterClockwiseSowing();
        testSourcePitSkippedOnFullLap();
        testSingleCapture();
        testChainCapture();
        testGrandSlamCaptureCancelled();
        testMandatoryFeeding();
        testWrongSideRejected();
        testVictoryAfterMajority();
        testUndo();
        testSnapshotIsDefensive();
        testComputerChoosesLegalMove();
        testBoardRendering();
        System.out.println("All " + passed + " tests passed.");
    }

    private void testInitialSetup() {
        AyoGame game = new AyoGame();
        GameSnapshot snapshot = game.snapshot();
        check(snapshot.currentPlayer() == Player.SOUTH, "South starts the game");
        check(java.util.Arrays.stream(snapshot.board()).sum() == 48, "board starts with 48 seeds");
        check(java.util.Arrays.stream(snapshot.board()).allMatch(value -> value == 4),
                "every pit starts with four seeds");
        check(game.legalMoves().equals(List.of(0, 1, 2, 3, 4, 5)),
                "all six South pits are initially legal");
    }

    private void testCounterClockwiseSowing() {
        AyoGame game = new AyoGame();
        MoveResult result = game.play(0);
        int[] board = game.snapshot().board();
        check(result.landingPit() == 4, "four seeds land in the fourth following pit");
        check(board[0] == 0 && board[1] == 5 && board[2] == 5 && board[3] == 5 && board[4] == 5,
                "seeds are sown one-by-one in counter-clockwise order");
        check(game.snapshot().currentPlayer() == Player.NORTH, "turn changes after a move");
    }

    private void testSourcePitSkippedOnFullLap() {
        int[] board = {12, 0, 0, 0, 0, 0, 6, 6, 6, 6, 6, 6};
        AyoGame game = AyoGame.fromPosition(board, 0, 0, Player.SOUTH);
        MoveResult result = game.play(0);
        int[] after = game.snapshot().board();
        check(after[0] == 0, "source pit is skipped when sowing wraps around");
        check(after[1] == 2 && result.landingPit() == 1,
                "twelfth seed continues after the skipped source");
    }

    private void testSingleCapture() {
        int[] board = {45, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0};
        AyoGame game = AyoGame.fromPosition(board, 0, 0, Player.SOUTH);
        MoveResult result = game.play(5);
        check(result.capturedSeeds() == 2 && result.capturedPits().equals(List.of(6)),
                "last seed captures an opponent pit containing two");
        check(game.snapshot().southCaptured() == 2 && game.snapshot().seedsAt(6) == 0,
                "captured seeds move into South's store");
    }

    private void testChainCapture() {
        int[] board = {43, 0, 0, 0, 0, 2, 1, 1, 1, 0, 0, 0};
        AyoGame game = AyoGame.fromPosition(board, 0, 0, Player.SOUTH);
        MoveResult result = game.play(5);
        check(result.capturedSeeds() == 4, "chain capture collects both qualifying pits");
        check(result.capturedPits().equals(List.of(7, 6)), "chain capture works backward");
        check(game.snapshot().seedsAt(6) == 0 && game.snapshot().seedsAt(7) == 0,
                "all chain-captured pits are emptied");
    }

    private void testGrandSlamCaptureCancelled() {
        int[] board = {44, 0, 0, 0, 0, 2, 1, 1, 0, 0, 0, 0};
        AyoGame game = AyoGame.fromPosition(board, 0, 0, Player.SOUTH);
        MoveResult result = game.play(5);
        check(result.capturedSeeds() == 0, "capture is cancelled when it would starve the opponent");
        check(game.snapshot().seedsAt(6) == 2 && game.snapshot().seedsAt(7) == 2,
                "grand-slam seeds remain on the opponent side");
    }

    private void testMandatoryFeeding() {
        int[] board = {0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 47};
        AyoGame game = AyoGame.fromPosition(board, 0, 0, Player.NORTH);
        check(game.legalMoves().equals(List.of(11)), "only a move that feeds the empty opponent is legal");
    }

    private void testWrongSideRejected() {
        AyoGame game = new AyoGame();
        boolean rejected = false;
        try {
            game.play(6);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        check(rejected, "a player cannot choose an opponent pit");
    }

    private void testVictoryAfterMajority() {
        int[] board = {21, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0};
        AyoGame game = AyoGame.fromPosition(board, 24, 0, Player.SOUTH);
        game.play(5);
        check(game.snapshot().gameOver(), "game ends when a player captures more than half");
        check(game.snapshot().winner() == Player.SOUTH, "majority capture declares the correct winner");
    }

    private void testUndo() {
        AyoGame game = new AyoGame();
        GameSnapshot before = game.snapshot();
        game.play(2);
        check(game.undo(), "completed move can be undone");
        check(java.util.Arrays.equals(before.board(), game.snapshot().board()),
                "undo restores every pit");
        check(game.snapshot().currentPlayer() == Player.SOUTH, "undo restores the turn");
    }

    private void testSnapshotIsDefensive() {
        AyoGame game = new AyoGame();
        int[] exposed = game.snapshot().board();
        exposed[0] = 99;
        check(game.snapshot().seedsAt(0) == 4, "snapshot board array cannot mutate the game");
    }

    private void testComputerChoosesLegalMove() {
        AyoGame game = new AyoGame();
        game.play(0);
        List<Integer> legal = game.legalMoves();
        int chosen = new AyoAi(4).chooseMove(game.snapshot());
        check(legal.contains(chosen), "computer selects a legal move");
    }

    private void testBoardRendering() {
        AyoGame game = new AyoGame();
        AyoBoardPanel board = new AyoBoardPanel();
        board.setGame(game.snapshot(), game.legalMoves(), true);
        board.setSize(900, 470);
        BufferedImage image = new BufferedImage(900, 470, BufferedImage.TYPE_INT_RGB);
        board.paint(image.getGraphics());
        int background = image.getRGB(2, 2);
        boolean hasContent = false;
        outer:
        for (int y = 10; y < 460; y += 5) {
            for (int x = 10; x < 890; x += 5) {
                if (image.getRGB(x, y) != background) {
                    hasContent = true;
                    break outer;
                }
            }
        }
        check(hasContent, "Ayo board renders nonblank pixels");
    }

    private void check(boolean condition, String name) {
        if (!condition) throw new AssertionError("FAILED: " + name);
        passed++;
        System.out.println("PASS  " + name);
    }
}
