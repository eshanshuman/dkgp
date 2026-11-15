package com.campusexchange.ui;

import com.campusexchange.dao.UserDAO;
import com.campusexchange.model.User;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Optional;

/**
 * First window: log in with an existing email, or register a new account.
 */
public class LoginFrame extends JFrame {

    private final UserDAO userDAO = new UserDAO();

    private final JTextField loginEmailField = new JTextField(22);
    private final JTextField registerNameField = new JTextField(22);
    private final JTextField registerEmailField = new JTextField(22);
    private final JButton loginButton = new JButton("Log in");
    private final JButton registerButton = new JButton("Register");

    public LoginFrame() {
        super("Campus Resource Exchange - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel heading = new JLabel("Campus Resource Exchange", SwingConstants.CENTER);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 20f));
        heading.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Log in", buildLoginPanel());
        tabs.addTab("Register", buildRegisterPanel());

        setLayout(new BorderLayout());
        add(heading, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        loginButton.addActionListener(e -> login());
        loginEmailField.addActionListener(e -> login());          // Enter key
        registerButton.addActionListener(e -> register());
        registerEmailField.addActionListener(e -> register());

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildLoginPanel() {
        JPanel panel = formPanel();
        addRow(panel, 0, "Email:", loginEmailField);
        addButton(panel, 1, loginButton);
        return panel;
    }

    private JPanel buildRegisterPanel() {
        JPanel panel = formPanel();
        addRow(panel, 0, "Name:", registerNameField);
        addRow(panel, 1, "Email:", registerEmailField);
        addButton(panel, 2, registerButton);
        return panel;
    }

    /** Looks the email up in a background thread, then opens the main window. */
    private void login() {
        String email = loginEmailField.getText();
        if (email.isBlank()) {
            JOptionPane.showMessageDialog(this, "Please enter your email.", "Login", JOptionPane.WARNING_MESSAGE);
            return;
        }
        setBusy(true);
        DbTask.run(this, () -> userDAO.findByEmail(email), (Optional<User> user) -> {
            if (user.isPresent()) {
                openMainWindow(user.get());
            } else {
                JOptionPane.showMessageDialog(this,
                        "No account found for " + email.trim() + ". Use the Register tab to create one.",
                        "Login", JOptionPane.WARNING_MESSAGE);
            }
        }, () -> setBusy(false));
    }

    /** Creates the account in a background thread, then logs straight in. */
    private void register() {
        String name = registerNameField.getText();
        String email = registerEmailField.getText();
        if (name.isBlank() || email.isBlank()) {
            JOptionPane.showMessageDialog(this, "Please enter both name and email.",
                    "Register", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!email.contains("@")) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address.",
                    "Register", JOptionPane.WARNING_MESSAGE);
            return;
        }
        setBusy(true);
        // Errors such as "Email already registered" are shown by DbTask in a JOptionPane.
        DbTask.run(this, () -> userDAO.register(name, email), user -> {
            JOptionPane.showMessageDialog(this, "Welcome aboard, " + user.name() + "!",
                    "Registered", JOptionPane.INFORMATION_MESSAGE);
            openMainWindow(user);
        }, () -> setBusy(false));
    }

    private void openMainWindow(User user) {
        new MainFrame(user).setVisible(true);
        dispose();
    }

    private void setBusy(boolean busy) {
        loginButton.setEnabled(!busy);
        registerButton.setEnabled(!busy);
    }

    // ---- small layout helpers ----

    private static JPanel formPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        return panel;
    }

    private static void addRow(JPanel panel, int row, String label, JTextField field) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.gridy = row;
        c.gridx = 0;
        c.anchor = GridBagConstraints.LINE_END;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        panel.add(field, c);
    }

    private static void addButton(JPanel panel, int row, JButton button) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(12, 6, 6, 6);
        c.gridy = row;
        c.gridx = 1;
        c.anchor = GridBagConstraints.LINE_END;
        panel.add(button, c);
    }
}
