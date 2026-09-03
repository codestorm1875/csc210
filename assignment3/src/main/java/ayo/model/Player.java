package ayo.model;

public enum Player {
    SOUTH("South", 0, 5),
    NORTH("North", 6, 11);

    private final String displayName;
    private final int firstPit;
    private final int lastPit;

    Player(String displayName, int firstPit, int lastPit) {
        this.displayName = displayName;
        this.firstPit = firstPit;
        this.lastPit = lastPit;
    }

    public String displayName() {
        return displayName;
    }

    public boolean owns(int pit) {
        return pit >= firstPit && pit <= lastPit;
    }

    public Player opponent() {
        return this == SOUTH ? NORTH : SOUTH;
    }
}
