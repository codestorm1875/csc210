package deadorwounded.model;

public record Feedback(int dead, int wounded) {
    public Feedback {
        if (dead < 0 || wounded < 0 || dead + wounded > DeadOrWoundedGame.CODE_LENGTH) {
            throw new IllegalArgumentException("Invalid feedback counts");
        }
    }

    public boolean solved() {
        return dead == DeadOrWoundedGame.CODE_LENGTH;
    }
}
