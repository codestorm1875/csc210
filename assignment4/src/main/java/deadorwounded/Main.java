package deadorwounded;

import deadorwounded.ui.MainFrame;
import deadorwounded.ui.UiKit;

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
                // Swing's cross-platform default remains available.
            }
            UiKit.installDefaults();
            new MainFrame().setVisible(true);
        });
    }
}
