package fleet.ui;

import fleet.data.FleetData;
import fleet.model.MutablePoint;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class VehicleTableModel extends AbstractTableModel {
    private List<Row> rows = List.of();

    void setSnapshot(Map<String, MutablePoint> snapshot,
            Map<String, FleetMapPanel.Telemetry> telemetry, String query, String type) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Row> next = new ArrayList<>();
        snapshot.forEach((id, point) -> {
            String vehicleType = FleetData.typeOf(id);
            boolean matchesQuery = normalizedQuery.isEmpty()
                    || id.toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || vehicleType.toLowerCase(Locale.ROOT).contains(normalizedQuery);
            boolean matchesType = type == null || type.equals("All vehicles") || type.equals(vehicleType);
            if (matchesQuery && matchesType) {
                FleetMapPanel.Telemetry data = telemetry.get(id);
                next.add(new Row(id, vehicleType, point.x, point.y,
                        data == null ? 0 : Math.round(data.speedKmh()),
                        data == null ? "-" : data.heading()));
            }
        });
        rows = List.copyOf(next);
        fireTableDataChanged();
    }

    String vehicleIdAt(int modelRow) {
        return rows.get(modelRow).id();
    }

    int findVehicle(String id) {
        for (int index = 0; index < rows.size(); index++) {
            if (rows.get(index).id().equals(id)) return index;
        }
        return -1;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return 6;
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case 0 -> "Vehicle";
            case 1 -> "Type";
            case 2 -> "X";
            case 3 -> "Y";
            case 4 -> "Speed";
            default -> "Heading";
        };
    }

    @Override
    public Object getValueAt(int row, int column) {
        Row value = rows.get(row);
        return switch (column) {
            case 0 -> value.id();
            case 1 -> value.type();
            case 2 -> value.x();
            case 3 -> value.y();
            case 4 -> value.speed() + " km/h";
            default -> value.heading();
        };
    }

    private record Row(String id, String type, int x, int y, long speed, String heading) {
    }
}
