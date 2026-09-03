package fleet.data;

import fleet.model.MutablePoint;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FleetData {
    private FleetData() {
    }

    public static Map<String, MutablePoint> initialLocations() {
        Map<String, MutablePoint> locations = new LinkedHashMap<>();
        locations.put("CAB-101", new MutablePoint(18, 15));
        locations.put("CAB-204", new MutablePoint(37, 78));
        locations.put("POL-307", new MutablePoint(49, 40));
        locations.put("POL-412", new MutablePoint(87, 82));
        locations.put("VAN-118", new MutablePoint(77, 15));
        locations.put("VAN-225", new MutablePoint(62, 57));
        locations.put("TRK-016", new MutablePoint(29, 65));
        locations.put("TRK-089", new MutablePoint(87, 68));
        return locations;
    }

    public static String typeOf(String vehicleId) {
        if (vehicleId.startsWith("CAB")) return "Taxi";
        if (vehicleId.startsWith("POL")) return "Police";
        if (vehicleId.startsWith("VAN")) return "Delivery van";
        return "Truck";
    }
}
