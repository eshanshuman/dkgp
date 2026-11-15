package com.campusexchange.dao;

import com.campusexchange.db.Db;
import com.campusexchange.model.BorrowRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for the requests table (borrow history).
 */
public class RequestDAO {

    /** Everything {@code userId} has borrowed, newest first, with resource titles. */
    public List<BorrowRecord> historyForUser(int userId) throws SQLException {
        String sql = "SELECT q.request_id, q.resource_id, r.title, r.category, q.request_date, r.status "
                + "FROM requests q JOIN resources r ON r.resource_id = q.resource_id "
                + "WHERE q.borrower_id = ? "
                + "ORDER BY q.request_date DESC, q.request_id DESC";
        try (Connection con = Db.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<BorrowRecord> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new BorrowRecord(
                            rs.getInt("request_id"),
                            rs.getInt("resource_id"),
                            rs.getString("title"),
                            rs.getString("category"),
                            rs.getDate("request_date").toLocalDate(),
                            rs.getString("status")));
                }
                return list;
            }
        }
    }
}
