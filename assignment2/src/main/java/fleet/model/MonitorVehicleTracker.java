package fleet.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Thread-safe vehicle tracker implemented with the Java monitor pattern.
 * All access to the private mutable state is guarded by this object's monitor.
 */
public final class MonitorVehicleTracker {
    private final Map<String, MutablePoint> locations;

    public MonitorVehicleTracker(Map<String, MutablePoint> locations) {
        this.locations = mutableDeepCopy(Objects.requireNonNull(locations, "locations"));
    }

    /** Returns a stable snapshot that shares no MutablePoint with the tracker. */
    public synchronized Map<String, MutablePoint> getLocations() {
        return Collections.unmodifiableMap(mutableDeepCopy(locations));
    }

    /** Returns a defensive copy, or null when the vehicle is unknown. */
    public synchronized MutablePoint getLocation(String vehicleId) {
        MutablePoint location = locations.get(vehicleId);
        return location == null ? null : new MutablePoint(location);
    }

    /** Atomically changes both coordinates while holding the tracker monitor. */
    public synchronized void setLocation(String vehicleId, int x, int y) {
        MutablePoint location = locations.get(vehicleId);
        if (location == null) {
            throw new IllegalArgumentException("No such vehicle: " + vehicleId);
        }
        location.x = x;
        location.y = y;
    }

    public synchronized int size() {
        return locations.size();
    }

    private static Map<String, MutablePoint> mutableDeepCopy(Map<String, MutablePoint> source) {
        Map<String, MutablePoint> copy = new LinkedHashMap<>();
        source.forEach((vehicleId, point) -> {
            Objects.requireNonNull(vehicleId, "vehicleId");
            copy.put(vehicleId, new MutablePoint(Objects.requireNonNull(point, "location")));
        });
        return copy;
    }
}
