package com.campusexchange.dao;

import com.campusexchange.db.Db;
import com.campusexchange.model.BorrowRecord;
import com.campusexchange.model.Resource;
import com.campusexchange.model.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DAO tests against an in-memory H2 database running in MySQL compatibility mode.
 * The real sql/schema.sql is loaded before every test, so these tests also check the schema.
 */
class DaoTest {

    private static final String H2_URL =
            "jdbc:h2:mem:campus_exchange;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    private final UserDAO userDAO = new UserDAO();
    private final ResourceDAO resourceDAO = new ResourceDAO();
    private final RequestDAO requestDAO = new RequestDAO();

    private User alice;
    private User bob;

    @BeforeAll
    static void pointDbAtH2() {
        // Db.get() lets system properties override db.properties
        System.setProperty("db.url", H2_URL);
        System.setProperty("db.user", "sa");
        System.setProperty("db.password", "");
    }

    @AfterAll
    static void clearOverrides() {
        System.clearProperty("db.url");
        System.clearProperty("db.user");
        System.clearProperty("db.password");
    }

    @BeforeEach
    void freshSchema() throws Exception {
        try (Connection con = Db.get(); Statement st = con.createStatement()) {
            st.execute("DROP ALL OBJECTS");
            for (String sql : schemaStatements()) {
                st.execute(sql);
            }
        }
        alice = userDAO.register("Alice", "alice@srm.edu");
        bob = userDAO.register("Bob", "bob@srm.edu");
    }

    /** Reads sql/schema.sql, skipping comments and the MySQL-only CREATE DATABASE / USE lines. */
    private static List<String> schemaStatements() throws Exception {
        StringBuilder cleaned = new StringBuilder();
        for (String line : Files.readAllLines(Path.of("sql", "schema.sql"))) {
            String t = line.trim().toUpperCase();
            if (t.startsWith("--") || t.startsWith("CREATE DATABASE") || t.startsWith("USE ")) {
                continue;
            }
            cleaned.append(line).append('\n');
        }
        return java.util.Arrays.stream(cleaned.toString().split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static int countRequests() throws SQLException {
        try (Connection con = Db.get();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM requests");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static String statusOf(List<Resource> list, int id) {
        return list.stream().filter(r -> r.resourceId() == id).findFirst().orElseThrow().status();
    }

    // ------------------------------------------------------------------ users

    @Test
    void registerAndFindByEmail() throws SQLException {
        User found = userDAO.findByEmail("ALICE@srm.edu").orElseThrow();
        assertEquals(alice.userId(), found.userId());
        assertEquals("Alice", found.name());
        assertTrue(userDAO.findByEmail("nobody@srm.edu").isEmpty());
    }

    @Test
    void duplicateEmailIsRejected() {
        SQLException e = assertThrows(SQLException.class,
                () -> userDAO.register("Alice Again", "alice@srm.edu"));
        assertTrue(e.getMessage().contains("already registered"), e.getMessage());
    }

    // ------------------------------------------------------------------ add + search

    @Test
    void addAndSearch() throws SQLException {
        int javaBook = resourceDAO.add("Head First Java", "Books", alice.userId());
        resourceDAO.add("Casio fx-991EX", "Calculators", alice.userId());
        resourceDAO.add("Java Lab Notes", "Notes", bob.userId());

        assertEquals(3, resourceDAO.findAll().size());

        List<Resource> javaAnything = resourceDAO.search("java", "All");
        assertEquals(2, javaAnything.size());

        List<Resource> javaBooks = resourceDAO.search("Java", "Books");
        assertEquals(1, javaBooks.size());
        Resource r = javaBooks.get(0);
        assertEquals(javaBook, r.resourceId());
        assertEquals(Resource.AVAILABLE, r.status());
        assertEquals("Alice", r.ownerName());

        assertEquals(3, resourceDAO.search("", null).size());
        assertEquals(List.of("Books", "Calculators", "Notes"), resourceDAO.categories());
    }

    @Test
    void sqlInjectionInSearchReturnsNothingExtra() throws SQLException {
        resourceDAO.add("Head First Java", "Books", alice.userId());
        resourceDAO.add("Vernier Caliper", "Lab Equipment", bob.userId());

        assertTrue(resourceDAO.search("' OR '1'='1", "All").isEmpty());
        assertTrue(resourceDAO.search("x%' OR 1=1 --", "All").isEmpty());
        assertTrue(resourceDAO.search("", "Books' OR '1'='1").isEmpty());
        // Tables are untouched
        assertEquals(2, resourceDAO.findAll().size());
    }

    // ------------------------------------------------------------------ borrow

    @Test
    void borrowMarksBorrowedAndRecordsRequest() throws SQLException {
        int id = resourceDAO.add("Lab Coat", "Lab Equipment", alice.userId());

        resourceDAO.borrow(id, bob.userId());

        assertEquals(Resource.BORROWED, statusOf(resourceDAO.findAll(), id));
        List<BorrowRecord> history = requestDAO.historyForUser(bob.userId());
        assertEquals(1, history.size());
        assertEquals("Lab Coat", history.get(0).title());
        assertEquals(LocalDate.now(), history.get(0).requestDate());
        assertTrue(requestDAO.historyForUser(alice.userId()).isEmpty());
    }

    @Test
    void borrowingAlreadyBorrowedItemFailsWithoutNewRequest() throws SQLException {
        User carol = userDAO.register("Carol", "carol@srm.edu");
        int id = resourceDAO.add("Arduino Kit", "Electronics", alice.userId());
        resourceDAO.borrow(id, bob.userId());
        assertEquals(1, countRequests());

        SQLException e = assertThrows(SQLException.class, () -> resourceDAO.borrow(id, carol.userId()));
        assertEquals("Resource is not available", e.getMessage());
        assertEquals(1, countRequests(), "rolled back: no extra request row");
        assertTrue(requestDAO.historyForUser(carol.userId()).isEmpty());
    }

    @Test
    void cannotBorrowOwnItem() throws SQLException {
        int id = resourceDAO.add("My Notes", "Notes", alice.userId());

        SQLException e = assertThrows(SQLException.class, () -> resourceDAO.borrow(id, alice.userId()));
        assertTrue(e.getMessage().contains("own"), e.getMessage());
        assertEquals(Resource.AVAILABLE, statusOf(resourceDAO.findAll(), id));
        assertEquals(0, countRequests());
    }

    // ------------------------------------------------------------------ return

    @Test
    void ownerCanMarkReturned() throws SQLException {
        int id = resourceDAO.add("Multimeter", "Electronics", alice.userId());
        resourceDAO.borrow(id, bob.userId());

        // Borrower is not the owner -> rejected
        assertThrows(SQLException.class, () -> resourceDAO.markReturned(id, bob.userId()));
        assertEquals(Resource.BORROWED, statusOf(resourceDAO.findAll(), id));

        resourceDAO.markReturned(id, alice.userId());
        assertEquals(Resource.AVAILABLE, statusOf(resourceDAO.findAll(), id));

        // Already available -> rejected
        assertThrows(SQLException.class, () -> resourceDAO.markReturned(id, alice.userId()));

        // Can be borrowed again; history keeps both rows
        resourceDAO.borrow(id, bob.userId());
        assertEquals(2, requestDAO.historyForUser(bob.userId()).size());
        assertFalse(requestDAO.historyForUser(bob.userId()).isEmpty());
    }
}
