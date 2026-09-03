package serp;

import serp.ui.MainFrame;
import serp.ui.UiKit;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception ignored) {
                // The cross-platform Swing look and feel remains available.
            }
            UiKit.installDefaults();
            new MainFrame().setVisible(true);
        });
    }
}
