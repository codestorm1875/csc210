package fleet.model;

/**
 * Mutable coordinate value used internally by the monitor-based tracker.
 * This class is intentionally not thread-safe; MonitorVehicleTracker protects it.
 */
public final class MutablePoint {
    public int x;
    public int y;

    public MutablePoint() {
        this(0, 0);
    }

    public MutablePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public MutablePoint(MutablePoint point) {
        this(point.x, point.y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
