package ayo.model;

import java.util.List;

public record MoveResult(
        Player player,
        int sourcePit,
        int landingPit,
        int capturedSeeds,
        List<Integer> capturedPits,
        String description) {

    public MoveResult {
        capturedPits = List.copyOf(capturedPits);
    }
}
