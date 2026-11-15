package com.campusexchange;

import com.campusexchange.ui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point: applies the Nimbus look and feel and opens the login window
 * on the Swing Event Dispatch Thread (EDT).
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            useNimbusLookAndFeel();
            new LoginFrame().setVisible(true);
        });
    }

    /** Switches to Nimbus if this JDK provides it; otherwise keeps the default look. */
    private static void useNimbusLookAndFeel() {
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                try {
                    UIManager.setLookAndFeel(info.getClassName());
                } catch (Exception e) {
                    System.err.println("Nimbus unavailable, using default look and feel: " + e.getMessage());
                }
                return;
            }
        }
    }
}
