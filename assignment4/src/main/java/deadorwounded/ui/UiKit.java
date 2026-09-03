package deadorwounded.ui;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

public final class UiKit {
    private UiKit() {
    }

    public static void installDefaults() {
        UIManager.put("defaultFont", Theme.BODY);
        UIManager.put("Label.font", Theme.BODY);
        UIManager.put("Table.font", Theme.BODY);
        UIManager.put("TableHeader.font", Theme.BODY.deriveFont(java.awt.Font.BOLD));
        UIManager.put("ToolTip.background", Theme.TEXT);
        UIManager.put("ToolTip.foreground", Color.WHITE);
        UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(6, 8, 6, 8));
    }

    static JButton primary(JButton button) {
        return style(button, new FlatButtonUI(Theme.TEAL, Theme.TEAL_DARK,
                Theme.TEAL_DARK, Color.WHITE, Theme.TEAL));
    }

    static JButton secondary(JButton button) {
        return style(button, new FlatButtonUI(Theme.SURFACE, Theme.SURFACE_MUTED,
                new Color(238, 238, 240), Theme.TEXT, Theme.BORDER));
    }

    static JButton keypad(JButton button) {
        button = style(button, new FlatButtonUI(Theme.SURFACE, Theme.SURFACE_MUTED,
                new Color(232, 232, 235), Theme.TEXT, Theme.BORDER));
        button.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        return button;
    }

    static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scrollPane.getViewport().setBackground(Theme.SURFACE);
        styleBar(scrollPane.getVerticalScrollBar());
        styleBar(scrollPane.getHorizontalScrollBar());
    }

    private static JButton style(JButton button, FlatButtonUI ui) {
        button.setFont(Theme.BODY.deriveFont(java.awt.Font.BOLD));
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        button.setUI(ui);
        return button;
    }

    private static void styleBar(JScrollBar bar) {
        bar.setUI(new SlimScrollBarUI());
        bar.setUnitIncrement(18);
        bar.setPreferredSize(bar.getOrientation() == JScrollBar.VERTICAL
                ? new java.awt.Dimension(9, 0) : new java.awt.Dimension(0, 9));
    }

    private static final class FlatButtonUI extends BasicButtonUI {
        private final Color background;
        private final Color hover;
        private final Color pressed;
        private final Color foreground;
        private final Color border;

        private FlatButtonUI(Color background, Color hover, Color pressed,
                Color foreground, Color border) {
            this.background = background;
            this.hover = hover;
            this.pressed = pressed;
            this.foreground = foreground;
            this.border = border;
        }

        @Override
        public void paint(Graphics graphics, JComponent component) {
            AbstractButton button = (AbstractButton) component;
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = !button.isEnabled() ? new Color(228, 228, 231)
                    : button.getModel().isPressed() ? pressed
                    : button.getModel().isRollover() ? hover : background;
            g.setColor(fill);
            g.fillRoundRect(0, 0, button.getWidth() - 1, button.getHeight() - 1, 8, 8);
            g.setColor(button.isEnabled() ? border : new Color(212, 212, 216));
            g.drawRoundRect(0, 0, button.getWidth() - 1, button.getHeight() - 1, 8, 8);
            if (button.hasFocus()) {
                g.setColor(new Color(45, 212, 191));
                g.drawRoundRect(2, 2, button.getWidth() - 5, button.getHeight() - 5, 6, 6);
            }
            g.dispose();
            button.setForeground(button.isEnabled() ? foreground : Theme.MUTED);
            super.paint(graphics, component);
        }
    }

    private static final class SlimScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(190, 190, 198);
            trackColor = Theme.SURFACE;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return zeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return zeroButton();
        }

        @Override
        protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
            if (!scrollbar.isEnabled() || bounds.isEmpty()) return;
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(isDragging ? Theme.MUTED : thumbColor);
            g.fillRoundRect(bounds.x + 2, bounds.y + 2,
                    Math.max(4, bounds.width - 4), Math.max(4, bounds.height - 4), 6, 6);
            g.dispose();
        }

        private JButton zeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new java.awt.Dimension(0, 0));
            return button;
        }
    }
}
