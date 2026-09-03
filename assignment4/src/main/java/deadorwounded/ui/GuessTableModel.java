package deadorwounded.ui;

import deadorwounded.model.GuessResult;

import javax.swing.table.AbstractTableModel;
import java.util.List;

final class GuessTableModel extends AbstractTableModel {
    private List<GuessResult> guesses = List.of();

    void setGuesses(List<GuessResult> guesses) {
        this.guesses = List.copyOf(guesses);
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return guesses.size();
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case 0 -> "#";
            case 1 -> "Guess";
            case 2 -> "Dead";
            default -> "Wounded";
        };
    }

    @Override
    public Object getValueAt(int row, int column) {
        GuessResult result = guesses.get(row);
        return switch (column) {
            case 0 -> result.attempt();
            case 1 -> result.guess();
            case 2 -> result.feedback().dead();
            default -> result.feedback().wounded();
        };
    }
}
