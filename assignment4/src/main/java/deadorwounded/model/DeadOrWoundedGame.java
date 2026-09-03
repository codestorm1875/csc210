package deadorwounded.model;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/** Rules engine for the four-digit Dead or Wounded deduction game. */
public final class DeadOrWoundedGame {
    public static final int CODE_LENGTH = 4;

    private final String secret;
    private final List<GuessResult> guesses = new ArrayList<>();
    private GameStatus status = GameStatus.IN_PROGRESS;

    public DeadOrWoundedGame() {
        this(generateSecret(new SecureRandom()));
    }

    public DeadOrWoundedGame(String secret) {
        validateCode(secret, "Secret");
        this.secret = secret;
    }

    public synchronized GuessResult submitGuess(String guess) {
        if (status != GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("The game has already ended");
        }
        validateCode(guess, "Guess");

        int dead = 0;
        int commonDigits = 0;
        for (int index = 0; index < CODE_LENGTH; index++) {
            char digit = guess.charAt(index);
            if (digit == secret.charAt(index)) dead++;
            if (secret.indexOf(digit) >= 0) commonDigits++;
        }
        Feedback feedback = new Feedback(dead, commonDigits - dead);
        GuessResult result = new GuessResult(guesses.size() + 1, guess, feedback);
        guesses.add(result);
        if (feedback.solved()) status = GameStatus.WON;
        return result;
    }

    public synchronized void giveUp() {
        if (status == GameStatus.IN_PROGRESS) status = GameStatus.GAVE_UP;
    }

    public synchronized GameSnapshot snapshot() {
        Optional<String> revealed = status == GameStatus.IN_PROGRESS
                ? Optional.empty() : Optional.of(secret);
        return new GameSnapshot(guesses, status, revealed);
    }

    public static String generateSecret(Random random) {
        List<Character> digits = new ArrayList<>();
        for (char digit = '0'; digit <= '9'; digit++) digits.add(digit);
        Collections.shuffle(digits, random);
        if (digits.get(0) == '0') {
            int replacement = 1;
            while (digits.get(replacement) == '0') replacement++;
            Collections.swap(digits, 0, replacement);
        }
        StringBuilder secret = new StringBuilder(CODE_LENGTH);
        for (int index = 0; index < CODE_LENGTH; index++) secret.append(digits.get(index));
        return secret.toString();
    }

    public static void validateCode(String code, String label) {
        if (code == null || !code.matches("\\d{" + CODE_LENGTH + "}")) {
            throw new IllegalArgumentException(label + " must contain exactly four digits");
        }
        if (code.charAt(0) == '0') {
            throw new IllegalArgumentException(label + " must not begin with zero");
        }
        if (code.chars().distinct().count() != CODE_LENGTH) {
            throw new IllegalArgumentException(label + " must not repeat digits");
        }
    }
}
