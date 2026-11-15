package com.campusexchange.ui;

import com.campusexchange.dao.RequestDAO;
import com.campusexchange.model.BorrowRecord;
import com.campusexchange.model.User;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

/**
 * Modal dialog listing everything the logged-in user has borrowed.
 */
public class MyRequestsDialog extends JDialog {

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Request ID", "Resource ID", "Title", "Category", "Borrowed On", "Current Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JLabel summary = new JLabel("Loading...");

    public MyRequestsDialog(JFrame owner, User user) {
        super(owner, "My Requests - " + user.name(), true);

        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);

        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        JPanel south = new JPanel(new BorderLayout());
        south.add(summary, BorderLayout.WEST);
        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closePanel.add(close);
        south.add(closePanel, BorderLayout.EAST);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        content.add(south, BorderLayout.SOUTH);
        setContentPane(content);

        setSize(720, 360);
        setLocationRelativeTo(owner);

        // Load in the background; the dialog appears immediately and fills in when ready.
        DbTask.run(this, () -> new RequestDAO().historyForUser(user.userId()), this::fill);
    }

    private void fill(List<BorrowRecord> records) {
        model.setRowCount(0);
        for (BorrowRecord r : records) {
            model.addRow(new Object[]{r.requestId(), r.resourceId(), r.title(), r.category(),
                    r.requestDate(), r.currentStatus()});
        }
        summary.setText(records.isEmpty()
                ? "You haven't borrowed anything yet."
                : "You have made " + records.size() + " borrow request(s).");
    }
}
