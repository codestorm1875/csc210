package serp.ui;

import serp.model.FrequencyItem;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.List;

/** Dependency-free horizontal bar chart used by both assignment visualizations. */
public final class BarChartPanel extends JPanel {
    private static final int ROW_HEIGHT = 32;
    private static final int LEFT = 210;
    private static final int RIGHT = 58;
    private static final int TOP = 38;
    private List<FrequencyItem> items = List.of();
    private String xAxisLabel = "Number of papers";
    private Color barColor = Theme.ACCENT;

    public BarChartPanel() {
        setBackground(Theme.SURFACE);
        ToolTipManager.sharedInstance().registerComponent(this);
        setPreferredSize(new Dimension(700, 440));
    }

    public void setData(List<FrequencyItem> items, String xAxisLabel, Color color) {
        this.items = List.copyOf(items);
        this.xAxisLabel = xAxisLabel;
        this.barColor = color;
        setPreferredSize(new Dimension(700, Math.max(300, TOP + items.size() * ROW_HEIGHT + 48)));
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(Theme.SMALL);

        int chartWidth = Math.max(80, getWidth() - LEFT - RIGHT);
        int max = Math.max(1, items.stream().mapToInt(FrequencyItem::count).max().orElse(1));
        drawGrid(g, chartWidth, max);

        FontMetrics metrics = g.getFontMetrics();
        for (int index = 0; index < items.size(); index++) {
            FrequencyItem item = items.get(index);
            int y = TOP + index * ROW_HEIGHT;
            String label = ellipsize(item.label(), metrics, LEFT - 24);
            g.setColor(Theme.TEXT);
            g.drawString(label, LEFT - 12 - metrics.stringWidth(label), y + 19);

            int width = (int) Math.round(chartWidth * item.count() / (double) max);
            g.setColor(index % 2 == 0 ? barColor : blend(barColor, Color.WHITE, 0.16));
            g.fillRoundRect(LEFT, y + 5, Math.max(2, width), 20, 6, 6);
            String value = item.count() + "  (" + String.format("%.0f%%", item.percentage()) + ")";
            g.setColor(Theme.TEXT);
            g.drawString(value, Math.min(LEFT + width + 7, getWidth() - metrics.stringWidth(value) - 6), y + 20);
        }

        g.setColor(Theme.MUTED);
        g.drawString(xAxisLabel, LEFT, getHeight() - 14);
        g.dispose();
    }

    private void drawGrid(Graphics2D g, int chartWidth, int max) {
        int bottom = TOP + items.size() * ROW_HEIGHT;
        g.setStroke(new BasicStroke(1f));
        for (int tick = 0; tick <= max; tick++) {
            int x = LEFT + (int) Math.round(chartWidth * tick / (double) max);
            g.setColor(new Color(235, 238, 242));
            g.drawLine(x, TOP - 7, x, bottom);
            g.setColor(Theme.MUTED);
            g.drawString(Integer.toString(tick), x - 3, TOP - 13);
        }
    }

    private String ellipsize(String value, FontMetrics metrics, int maxWidth) {
        if (metrics.stringWidth(value) <= maxWidth) return value;
        String result = value;
        while (result.length() > 3 && metrics.stringWidth(result + "...") > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
    }

    private Color blend(Color first, Color second, double ratio) {
        double inverse = 1.0 - ratio;
        return new Color(
                (int) (first.getRed() * inverse + second.getRed() * ratio),
                (int) (first.getGreen() * inverse + second.getGreen() * ratio),
                (int) (first.getBlue() * inverse + second.getBlue() * ratio));
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        int index = (event.getY() - TOP) / ROW_HEIGHT;
        if (index < 0 || index >= items.size()) return null;
        FrequencyItem item = items.get(index);
        return item.label() + ": " + item.count() + " papers ("
                + String.format("%.1f", item.percentage()) + "%)";
    }
}
