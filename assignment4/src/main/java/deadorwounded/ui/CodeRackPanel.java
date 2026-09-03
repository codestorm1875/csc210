package deadorwounded.ui;

import deadorwounded.model.Feedback;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.Optional;

/** Draws the hidden secret, current input, and latest dead/wounded feedback. */
public final class CodeRackPanel extends JPanel {
    private String input = "";
    private Optional<String> revealedSecret = Optional.empty();

    public CodeRackPanel() {
        setBackground(Theme.SURFACE);
        setPreferredSize(new Dimension(620, 230));
        setMinimumSize(new Dimension(420, 190));
    }

    public void setState(String input, Optional<String> revealedSecret, Feedback feedback) {
        this.input = input == null ? "" : input;
        this.revealedSecret = revealedSecret == null ? Optional.empty() : revealedSecret;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int center = getWidth() / 2;
        int slotHeight = Math.max(52, Math.min(66, (getHeight() - 72) / 2));
        int slotWidth = Math.max(48, Math.min(66, (getWidth() - 82) / 4));
        int firstLabel = 20;
        int firstRow = 29;
        int secondLabel = firstRow + slotHeight + 25;
        int secondRow = secondLabel + 9;
        drawLabel(g, "SECRET CODE", center, firstLabel);
        drawSlots(g, revealedSecret.orElse("????"), center, firstRow, true, slotWidth, slotHeight);
        drawLabel(g, "YOUR GUESS", center, secondLabel);
        drawSlots(g, paddedInput(), center, secondRow, false, slotWidth, slotHeight);
        g.dispose();
    }

    private void drawSlots(Graphics2D g, String value, int center, int y, boolean secret,
            int slotWidth, int slotHeight) {
        int gap = Math.max(8, Math.min(13, (getWidth() - slotWidth * 4) / 7));
        int total = slotWidth * 4 + gap * 3;
        int startX = center - total / 2;
        for (int index = 0; index < 4; index++) {
            int x = startX + index * (slotWidth + gap);
            Color fill = secret ? Theme.HEADER : Color.WHITE;
            g.setColor(fill);
            g.fill(new RoundRectangle2D.Double(x, y, slotWidth, slotHeight, 8, 8));
            g.setColor(secret ? Theme.HEADER : Theme.TEAL);
            g.setStroke(new BasicStroke(secret ? 1.5f : 2f));
            g.draw(new RoundRectangle2D.Double(x, y, slotWidth, slotHeight, 8, 8));

            char digit = value.charAt(index);
            g.setFont(Theme.HEADING.deriveFont(Font.BOLD, 29f));
            FontMetrics metrics = g.getFontMetrics();
            g.setColor(secret ? Color.WHITE : Theme.TEXT);
            g.drawString(String.valueOf(digit),
                    x + (slotWidth - metrics.charWidth(digit)) / 2,
                    y + (slotHeight + metrics.getAscent() - metrics.getDescent()) / 2);
        }
    }

    private void drawLabel(Graphics2D g, String text, int center, int y) {
        g.setFont(Theme.SMALL.deriveFont(Font.BOLD));
        FontMetrics metrics = g.getFontMetrics();
        g.setColor(Theme.MUTED);
        g.drawString(text, center - metrics.stringWidth(text) / 2, y);
    }

    private String paddedInput() {
        StringBuilder padded = new StringBuilder(input);
        while (padded.length() < 4) padded.append(' ');
        return padded.substring(0, 4);
    }
}
