package com.campusexchange.dao;

import com.campusexchange.db.Db;
import com.campusexchange.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.Optional;

/**
 * Data access for the users table.
 */
public class UserDAO {

    /**
     * Registers a new user and returns it with its generated id.
     *
     * @throws SQLException "Email already registered" if the email is taken
     */
    public User register(String name, String email) throws SQLException {
        String cleanName = requireText(name, "Name");
        String cleanEmail = requireText(email, "Email").toLowerCase();

        String sql = "INSERT INTO users (name, email) VALUES (?, ?)";
        try (Connection con = Db.get();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cleanName);
            ps.setString(2, cleanEmail);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new User(keys.getInt(1), cleanName, cleanEmail);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            // UNIQUE(email) was violated
            throw new SQLException("Email already registered: " + cleanEmail, e);
        }
    }

    /** Looks a user up by email (case-insensitive because emails are stored lower-case). */
    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT user_id, name, email FROM users WHERE email = ?";
        try (Connection con = Db.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, requireText(email, "Email").toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(rs.getInt("user_id"), rs.getString("name"), rs.getString("email")));
                }
                return Optional.empty();
            }
        }
    }

    /** Trims input and rejects blanks with a readable message. */
    static String requireText(String value, String field) throws SQLException {
        if (value == null || value.isBlank()) {
            throw new SQLException(field + " must not be empty");
        }
        return value.trim();
    }
}
