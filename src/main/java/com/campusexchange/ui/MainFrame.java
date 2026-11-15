package com.campusexchange.ui;

import com.campusexchange.dao.ResourceDAO;
import com.campusexchange.model.Resource;
import com.campusexchange.model.User;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

/**
 * Main window shown after login: browse/search resources, add your own,
 * borrow other people's, mark your lent items as returned, and view your history.
 *
 * <p>The UI never touches SQL: it only calls the DAO classes, always through
 * {@link DbTask} so the database work happens off the Event Dispatch Thread.
 */
public class MainFrame extends JFrame {

    /** Category choices offered when adding a new resource. */
    private static final String[] ADD_CATEGORIES =
            {"Books", "Calculators", "Lab Equipment", "Notes", "Electronics", "Other"};
    private static final String ALL = "All";

    // Table column indexes
    private static final int COL_ID = 0;
    private static final int COL_STATUS = 3;

    private final User user;
    private final ResourceDAO resourceDAO = new ResourceDAO();

    // Search bar
    private final JTextField searchField = new JTextField(18);
    private final JComboBox<String> categoryFilter = new JComboBox<>(new String[]{ALL});
    /** True while we refill the category combo, so its listener doesn't fire a search. */
    private boolean updatingFilter;

    // Resource table (read-only)
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Title", "Category", "Status", "Owner"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    // Add-resource form
    private final JTextField titleField = new JTextField(20);
    private final JComboBox<String> addCategoryBox = new JComboBox<>(ADD_CATEGORIES);

    public MainFrame(User user) {
        super("Campus Resource Exchange");
        this.user = user;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(new BorderLayout(8, 8));
        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildTablePanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        setSize(900, 560);
        setLocationRelativeTo(null);

        refreshAll();
    }

    // ------------------------------------------------------------------ layout

    private JPanel buildTopPanel() {
        JLabel welcome = new JLabel("Welcome, " + user.name());
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 18f));

        JPanel header = new JPanel(new BorderLayout());
        header.add(welcome, BorderLayout.WEST);
        header.add(new JLabel(user.email()), BorderLayout.EAST);

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> loadResources());
        searchField.addActionListener(e -> loadResources());   // Enter key
        categoryFilter.addActionListener(e -> {
            if (!updatingFilter) {
                loadResources();
            }
        });

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        searchBar.add(new JLabel("Search title:"));
        searchBar.add(searchField);
        searchBar.add(new JLabel("Category:"));
        searchBar.add(categoryFilter);
        searchBar.add(searchButton);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        top.add(header);
        top.add(Box.createVerticalStrut(10));
        top.add(searchBar);
        header.setAlignmentX(LEFT_ALIGNMENT);
        searchBar.setAlignmentX(LEFT_ALIGNMENT);
        return top;
    }

    private JScrollPane buildTablePanel() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(COL_ID).setMaxWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(320);   // Title gets the most room

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 12, 0, 12),
                BorderFactory.createTitledBorder("Resources")));
        return scroll;
    }

    private JPanel buildBottomPanel() {
        // "Add resource" form
        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> addResource());
        titleField.addActionListener(e -> addResource());

        JPanel addForm = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        addForm.setBorder(BorderFactory.createTitledBorder("Add resource"));
        addForm.add(new JLabel("Title:"));
        addForm.add(titleField);
        addForm.add(new JLabel("Category:"));
        addForm.add(addCategoryBox);
        addForm.add(addButton);

        // Action buttons
        JButton borrowButton = new JButton("Borrow");
        JButton returnButton = new JButton("Mark Returned");
        JButton requestsButton = new JButton("My Requests");
        JButton refreshButton = new JButton("Refresh");
        JButton logoutButton = new JButton("Logout");

        borrowButton.addActionListener(e -> borrowSelected());
        returnButton.addActionListener(e -> returnSelected());
        requestsButton.addActionListener(e -> new MyRequestsDialog(this, user).setVisible(true));
        refreshButton.addActionListener(e -> refreshAll());
        logoutButton.addActionListener(e -> logout());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        actions.add(borrowButton);
        actions.add(returnButton);
        actions.add(requestsButton);
        actions.add(refreshButton);
        actions.add(logoutButton);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        bottom.add(addForm, BorderLayout.CENTER);
        bottom.add(actions, BorderLayout.SOUTH);
        return bottom;
    }

    // ------------------------------------------------------------------ data loading

    /** Reloads the category filter and then the table. */
    private void refreshAll() {
        DbTask.run(this, resourceDAO::categories, categories -> {
            Object previous = categoryFilter.getSelectedItem();
            updatingFilter = true;
            categoryFilter.removeAllItems();
            categoryFilter.addItem(ALL);
            for (String category : categories) {
                categoryFilter.addItem(category);
            }
            // Keep the user's filter if that category still exists
            categoryFilter.setSelectedItem(categories.contains(previous) ? previous : ALL);
            updatingFilter = false;
            loadResources();
        });
    }

    /** Runs the current search (keyword + category) and fills the table. */
    private void loadResources() {
        String keyword = searchField.getText();
        String category = (String) categoryFilter.getSelectedItem();
        DbTask.run(this, () -> resourceDAO.search(keyword, category), this::fillTable);
    }

    private void fillTable(List<Resource> resources) {
        tableModel.setRowCount(0);
        for (Resource r : resources) {
            String owner = r.ownerId() == user.userId() ? r.ownerName() + " (you)" : r.ownerName();
            tableModel.addRow(new Object[]{r.resourceId(), r.title(), r.category(), r.status(), owner});
        }
    }

    // ------------------------------------------------------------------ actions

    private void addResource() {
        String title = titleField.getText();
        String category = (String) addCategoryBox.getSelectedItem();
        if (title.isBlank()) {
            JOptionPane.showMessageDialog(this, "Please enter a title.", "Add resource",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        DbTask.run(this, () -> resourceDAO.add(title, category, user.userId()), id -> {
            titleField.setText("");
            refreshAll();   // a brand-new category may need to appear in the filter
        });
    }

    private void borrowSelected() {
        Integer id = selectedResourceId();
        if (id == null) {
            return;
        }
        DbTask.run(this, () -> {
            resourceDAO.borrow(id, user.userId());
            return null;
        }, ignored -> {
            JOptionPane.showMessageDialog(this, "Borrowed! Please collect it from the owner.",
                    "Borrow", JOptionPane.INFORMATION_MESSAGE);
            loadResources();
        }, this::loadResources);   // refresh even on failure so the status shown is current
    }

    private void returnSelected() {
        Integer id = selectedResourceId();
        if (id == null) {
            return;
        }
        DbTask.run(this, () -> {
            resourceDAO.markReturned(id, user.userId());
            return null;
        }, ignored -> {
            JOptionPane.showMessageDialog(this, "Marked as returned - it is available again.",
                    "Mark Returned", JOptionPane.INFORMATION_MESSAGE);
            loadResources();
        }, this::loadResources);
    }

    /** Returns the ID of the selected row, or null (after telling the user) if none. */
    private Integer selectedResourceId() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a resource in the table first.",
                    "No selection", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);   // table may be sorted
        return (Integer) tableModel.getValueAt(modelRow, COL_ID);
    }

    private void logout() {
        dispose();
        new LoginFrame().setVisible(true);
    }
}
