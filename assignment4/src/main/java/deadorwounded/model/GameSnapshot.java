package deadorwounded.model;

import java.util.List;
import java.util.Optional;

public record GameSnapshot(
        List<GuessResult> guesses,
        GameStatus status,
        Optional<String> revealedSecret) {

    public GameSnapshot {
        guesses = List.copyOf(guesses);
        revealedSecret = revealedSecret == null ? Optional.empty() : revealedSecret;
    }

    public int attempts() {
        return guesses.size();
    }

    public GuessResult lastGuess() {
        return guesses.isEmpty() ? null : guesses.get(guesses.size() - 1);
    }
}
