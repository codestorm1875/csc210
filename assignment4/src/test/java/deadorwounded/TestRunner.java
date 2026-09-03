package deadorwounded;

import deadorwounded.model.DeadOrWoundedGame;
import deadorwounded.model.Feedback;
import deadorwounded.model.GameSnapshot;
import deadorwounded.model.GameStatus;
import deadorwounded.model.GuessResult;
import deadorwounded.ui.CodeRackPanel;

import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.Random;

public final class TestRunner {
    private int passed;

    public static void main(String[] args) {
        new TestRunner().run();
    }

    private void run() {
        testExactGuess();
        testAllWounded();
        testMixedFeedback();
        testNoMatchingDigits();
        testAttemptHistory();
        testSecretHiddenDuringPlay();
        testWinningLifecycle();
        testGiveUpLifecycle();
        testGuessAfterEndRejected();
        testInvalidLength();
        testNonDigitRejected();
        testLeadingZeroRejected();
        testRepeatedDigitRejected();
        testRandomSecrets();
        testSnapshotHistoryImmutable();
        testRackRendering();
        System.out.println("All " + passed + " tests passed.");
    }

    private void testExactGuess() {
        Feedback feedback = new DeadOrWoundedGame("4271").submitGuess("4271").feedback();
        check(feedback.dead() == 4 && feedback.wounded() == 0, "exact guess scores four dead");
        check(feedback.solved(), "four dead marks feedback as solved");
    }

    private void testAllWounded() {
        Feedback feedback = new DeadOrWoundedGame("4271").submitGuess("1742").feedback();
        check(feedback.dead() == 0 && feedback.wounded() == 4,
                "same digits in different positions score four wounded");
    }

    private void testMixedFeedback() {
        Feedback feedback = new DeadOrWoundedGame("4271").submitGuess("4217").feedback();
        check(feedback.dead() == 2 && feedback.wounded() == 2,
                "mixed guess separates dead and wounded");
    }

    private void testNoMatchingDigits() {
        Feedback feedback = new DeadOrWoundedGame("4271").submitGuess("5689").feedback();
        check(feedback.dead() == 0 && feedback.wounded() == 0,
                "absent digits score zero feedback");
    }

    private void testAttemptHistory() {
        DeadOrWoundedGame game = new DeadOrWoundedGame("4271");
        GuessResult first = game.submitGuess("5689");
        GuessResult second = game.submitGuess("4217");
        check(first.attempt() == 1 && second.attempt() == 2, "attempt numbers increase");
        check(game.snapshot().guesses().size() == 2, "snapshot contains every submitted guess");
        check(game.snapshot().lastGuess().equals(second), "snapshot identifies latest guess");
    }

    private void testSecretHiddenDuringPlay() {
        GameSnapshot snapshot = new DeadOrWoundedGame("4271").snapshot();
        check(snapshot.revealedSecret().isEmpty(), "secret stays hidden during active play");
        check(snapshot.status() == GameStatus.IN_PROGRESS, "new game is in progress");
    }

    private void testWinningLifecycle() {
        DeadOrWoundedGame game = new DeadOrWoundedGame("4271");
        game.submitGuess("4271");
        GameSnapshot snapshot = game.snapshot();
        check(snapshot.status() == GameStatus.WON, "correct guess wins the game");
        check(snapshot.revealedSecret().orElseThrow().equals("4271"), "winning reveals the secret");
    }

    private void testGiveUpLifecycle() {
        DeadOrWoundedGame game = new DeadOrWoundedGame("4271");
        game.giveUp();
        GameSnapshot snapshot = game.snapshot();
        check(snapshot.status() == GameStatus.GAVE_UP, "give up ends the game");
        check(snapshot.revealedSecret().orElseThrow().equals("4271"), "give up reveals the secret");
    }

    private void testGuessAfterEndRejected() {
        DeadOrWoundedGame game = new DeadOrWoundedGame("4271");
        game.giveUp();
        checkThrows(IllegalStateException.class, () -> game.submitGuess("4217"),
                "guesses are rejected after the game ends");
    }

    private void testInvalidLength() {
        checkThrows(IllegalArgumentException.class,
                () -> new DeadOrWoundedGame("4271").submitGuess("427"),
                "short guess is rejected");
    }

    private void testNonDigitRejected() {
        checkThrows(IllegalArgumentException.class,
                () -> new DeadOrWoundedGame("4271").submitGuess("42A1"),
                "nondigit guess is rejected");
    }

    private void testLeadingZeroRejected() {
        checkThrows(IllegalArgumentException.class,
                () -> new DeadOrWoundedGame("4271").submitGuess("0421"),
                "leading zero is rejected");
    }

    private void testRepeatedDigitRejected() {
        checkThrows(IllegalArgumentException.class,
                () -> new DeadOrWoundedGame("4271").submitGuess("4221"),
                "repeated digit is rejected");
        checkThrows(IllegalArgumentException.class,
                () -> new DeadOrWoundedGame("1123"),
                "invalid repeated-digit secret is rejected");
    }

    private void testRandomSecrets() {
        Random random = new Random(210);
        boolean allFourDigits = true;
        boolean noLeadingZero = true;
        boolean allDistinct = true;
        for (int index = 0; index < 250; index++) {
            String secret = DeadOrWoundedGame.generateSecret(random);
            allFourDigits &= secret.length() == 4;
            noLeadingZero &= secret.charAt(0) != '0';
            allDistinct &= secret.chars().distinct().count() == 4;
        }
        check(allFourDigits, "generated secrets have four digits");
        check(noLeadingZero, "generated secrets have no leading zero");
        check(allDistinct, "generated secrets have distinct digits");
    }

    private void testSnapshotHistoryImmutable() {
        DeadOrWoundedGame game = new DeadOrWoundedGame("4271");
        game.submitGuess("5689");
        boolean rejected = false;
        try {
            game.snapshot().guesses().clear();
        } catch (UnsupportedOperationException expected) {
            rejected = true;
        }
        check(rejected, "snapshot guess history is immutable");
        check(game.snapshot().attempts() == 1, "failed mutation does not alter the game");
    }

    private void testRackRendering() {
        CodeRackPanel rack = new CodeRackPanel();
        rack.setState("421", Optional.empty(), new Feedback(1, 2));
        rack.setSize(500, 190);
        BufferedImage image = new BufferedImage(500, 190, BufferedImage.TYPE_INT_RGB);
        rack.paint(image.getGraphics());
        int background = image.getRGB(2, 2);
        boolean hasContent = false;
        outer:
        for (int y = 8; y < 182; y += 4) {
            for (int x = 8; x < 492; x += 4) {
                if (image.getRGB(x, y) != background) {
                    hasContent = true;
                    break outer;
                }
            }
        }
        check(hasContent, "code rack renders at compact height");
    }

    private void checkThrows(Class<? extends Throwable> expected, Runnable action, String name) {
        boolean thrown = false;
        try {
            action.run();
        } catch (Throwable actual) {
            thrown = expected.isInstance(actual);
        }
        check(thrown, name);
    }

    private void check(boolean condition, String name) {
        if (!condition) throw new AssertionError("FAILED: " + name);
        passed++;
        System.out.println("PASS  " + name);
    }
}
