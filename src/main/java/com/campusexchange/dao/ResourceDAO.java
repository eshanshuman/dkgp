package com.campusexchange.dao;

import com.campusexchange.db.Db;
import com.campusexchange.model.Resource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for the resources table, including the borrow / return workflow.
 */
public class ResourceDAO {

    /** Columns shared by every listing query; joins users to show the owner's name. */
    private static final String SELECT_WITH_OWNER =
            "SELECT r.resource_id, r.title, r.category, r.status, r.owner_id, u.name AS owner_name "
            + "FROM resources r JOIN users u ON u.user_id = r.owner_id ";

    /** Every resource, oldest first. */
    public List<Resource> findAll() throws SQLException {
        String sql = SELECT_WITH_OWNER + "ORDER BY r.resource_id";
        try (Connection con = Db.get();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return readResources(rs);
        }
    }

    /**
     * Searches by title keyword and/or category.
     *
     * @param keyword  matched anywhere in the title; null/blank means "any title"
     * @param category exact category; null, blank or "All" means "any category"
     */
    public List<Resource> search(String keyword, String category) throws SQLException {
        String pattern = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        boolean anyCategory = category == null || category.isBlank() || category.equals("All");

        // The SQL text is fixed; user input only ever goes into ? parameters.
        // LOWER() on both sides makes the match case-insensitive on any database.
        String sql = SELECT_WITH_OWNER
                + "WHERE LOWER(r.title) LIKE LOWER(?) "
                + (anyCategory ? "" : "AND r.category = ? ")
                + "ORDER BY r.resource_id";

        try (Connection con = Db.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pattern);
            if (!anyCategory) {
                ps.setString(2, category);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return readResources(rs);
            }
        }
    }

    /** Lists a new resource owned by {@code ownerId}; returns the generated resource_id. */
    public int add(String title, String category, int ownerId) throws SQLException {
        String sql = "INSERT INTO resources (title, category, status, owner_id) VALUES (?, ?, ?, ?)";
        try (Connection con = Db.get();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, UserDAO.requireText(title, "Title"));
            ps.setString(2, UserDAO.requireText(category, "Category"));
            ps.setString(3, Resource.AVAILABLE);
            ps.setInt(4, ownerId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /**
     * Borrows a resource as ONE transaction:
     * <ol>
     *   <li>reject borrowing your own item,</li>
     *   <li>flip status Available -> Borrowed (the WHERE clause makes this atomic, so two
     *       students clicking at the same time cannot both succeed),</li>
     *   <li>record the borrow in requests with today's date.</li>
     * </ol>
     * Any failure rolls everything back.
     */
    public void borrow(int resourceId, int borrowerId) throws SQLException {
        String ownerSql = "SELECT owner_id FROM resources WHERE resource_id = ?";
        String updateSql = "UPDATE resources SET status = 'Borrowed' "
                + "WHERE resource_id = ? AND status = 'Available'";
        String insertSql = "INSERT INTO requests (resource_id, borrower_id, request_date) "
                + "VALUES (?, ?, CURDATE())";

        try (Connection con = Db.get()) {
            con.setAutoCommit(false);
            try {
                // 1. Owner check
                try (PreparedStatement ps = con.prepareStatement(ownerSql)) {
                    ps.setInt(1, resourceId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Resource not found");
                        }
                        if (rs.getInt("owner_id") == borrowerId) {
                            throw new SQLException("You cannot borrow your own resource");
                        }
                    }
                }

                // 2. Mark as borrowed only if it is still available
                try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                    ps.setInt(1, resourceId);
                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("Resource is not available");
                    }
                }

                // 3. Record the request
                try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                    ps.setInt(1, resourceId);
                    ps.setInt(2, borrowerId);
                    ps.executeUpdate();
                }

                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    /**
     * Marks a borrowed resource as Available again. Only the owner may do this,
     * and only while the item is Borrowed.
     */
    public void markReturned(int resourceId, int ownerId) throws SQLException {
        String sql = "UPDATE resources SET status = 'Available' "
                + "WHERE resource_id = ? AND owner_id = ? AND status = 'Borrowed'";
        try (Connection con = Db.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, resourceId);
            ps.setInt(2, ownerId);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("Only the owner can mark a resource as returned, "
                        + "and only while it is Borrowed");
            }
        }
    }

    /** Distinct categories currently in use, alphabetically (fills the filter combo box). */
    public List<String> categories() throws SQLException {
        String sql = "SELECT DISTINCT category FROM resources ORDER BY category";
        try (Connection con = Db.get();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<String> list = new ArrayList<>();
            while (rs.next()) {
                list.add(rs.getString("category"));
            }
            return list;
        }
    }

    /** Converts result rows into Resource records. */
    private static List<Resource> readResources(ResultSet rs) throws SQLException {
        List<Resource> list = new ArrayList<>();
        while (rs.next()) {
            list.add(new Resource(
                    rs.getInt("resource_id"),
                    rs.getString("title"),
                    rs.getString("category"),
                    rs.getString("status"),
                    rs.getInt("owner_id"),
                    rs.getString("owner_name")));
        }
        return list;
    }
}
