package serp.ui;

import serp.model.RankedPaper;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;

/** Makes the generated summary visually primary while retaining citation metadata. */
final class SearchResultRenderer extends JPanel implements ListCellRenderer<RankedPaper> {
    private final JLabel summary = new JLabel();
    private final JLabel metadata = new JLabel();
    private final JLabel score = new JLabel();

    SearchResultRenderer() {
        setLayout(new BorderLayout(16, 0));
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        summary.setFont(Theme.BODY.deriveFont(14f));
        metadata.setFont(Theme.SMALL);
        text.add(summary);
        text.add(Box.createRigidArea(new Dimension(0, 8)));
        text.add(metadata);
        score.setFont(Theme.BODY.deriveFont(java.awt.Font.BOLD));
        score.setOpaque(true);
        score.setHorizontalAlignment(JLabel.CENTER);
        score.setPreferredSize(new Dimension(66, 28));
        score.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        JPanel scoreWrap = new JPanel(new GridBagLayout());
        scoreWrap.setOpaque(false);
        scoreWrap.add(score);
        add(text, BorderLayout.CENTER);
        add(scoreWrap, BorderLayout.EAST);
        setPreferredSize(new Dimension(800, 112));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends RankedPaper> list, RankedPaper value,
            int index, boolean selected, boolean focus) {
        summary.setText("<html><body style='width:700px'>" + escape(value.summary()) + "</body></html>");
        metadata.setText(value.paper().title() + "  |  " + value.paper().authors() + "  |  " + value.paper().year());
        score.setText(String.format("%.1f%%", value.relevance() * 100));
        setBackground(selected ? new java.awt.Color(240, 253, 250) : Theme.SURFACE);
        metadata.setForeground(Theme.MUTED);
        score.setForeground(Theme.ACCENT_DARK);
        score.setBackground(new java.awt.Color(236, 253, 245));
        return this;
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
