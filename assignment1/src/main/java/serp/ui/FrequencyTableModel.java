package serp.ui;

import serp.model.FrequencyItem;

import javax.swing.table.AbstractTableModel;
import java.util.List;

final class FrequencyTableModel extends AbstractTableModel {
    private final String firstColumn;
    private List<FrequencyItem> items = List.of();

    FrequencyTableModel(String firstColumn) {
        this.firstColumn = firstColumn;
    }

    void setItems(List<FrequencyItem> items) {
        this.items = List.copyOf(items);
        fireTableDataChanged();
    }

    List<FrequencyItem> items() {
        return items;
    }

    @Override
    public int getRowCount() {
        return items.size();
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case 0 -> "Rank";
            case 1 -> firstColumn;
            case 2 -> "Papers";
            default -> "Coverage";
        };
    }

    @Override
    public Object getValueAt(int row, int column) {
        FrequencyItem item = items.get(row);
        return switch (column) {
            case 0 -> row + 1;
            case 1 -> item.label();
            case 2 -> item.count();
            default -> String.format("%.1f%%", item.percentage());
        };
    }
}
